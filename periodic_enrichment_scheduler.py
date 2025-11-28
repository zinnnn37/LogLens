#!/usr/bin/env python3
"""
Periodic Enrichment Scheduler for ERROR Log Vectorization

This service:
1. Runs every 10 seconds (configurable for testing/production)
2. Queries OpenSearch for ERROR logs without log_vector field
3. Processes 100 logs at a time
4. Generates embeddings using text-embedding-3-large model
5. Updates OpenSearch documents with log_vector field

Unlike enrichment_consumer.py (event-driven via Kafka),
this scheduler proactively processes existing ERROR logs
that don't have vectors yet.

Usage:
    python periodic_enrichment_scheduler.py
"""

import asyncio
import sys
import time
import logging
from datetime import datetime
from typing import List, Dict, Any
from dotenv import load_dotenv
from apscheduler.schedulers.asyncio import AsyncIOScheduler
from apscheduler.triggers.interval import IntervalTrigger

# Load environment variables
load_dotenv()

# Import services
from app.core.opensearch import opensearch_client
from app.services.embedding_service import embedding_service

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler('periodic_enrichment.log')
    ]
)
logger = logging.getLogger(__name__)


class PeriodicEnrichmentScheduler:
    """
    Scheduler that periodically vectorizes ERROR logs

    Runs every 10 seconds and processes up to 100 ERROR logs
    that don't have log_vector field yet.
    """

    def __init__(self):
        self.batch_size = 100  # Process 100 logs per run
        self.interval_seconds = 60  # Run every 60 seconds (1 minute)

        # Performance metrics
        self.stats = {
            'runs': 0,
            'total_vectors_created': 0,
            'total_errors': 0,
            'start_time': time.time()
        }

        logger.info("Initializing Periodic Enrichment Scheduler")
        logger.info(f"  Batch size: {self.batch_size} logs")
        logger.info(f"  Interval: {self.interval_seconds} seconds")
        logger.info(f"  Model: text-embedding-3-large (1536 dimensions)")

    async def find_error_logs_without_vectors(self) -> List[Dict[str, Any]]:
        """
        Query OpenSearch for ERROR logs without log_vector field

        Returns:
            List of ERROR log documents (max 100)
        """
        try:
            # Search across all indices with wildcard
            # Pattern: *_YYYY_MM (matches all project indices)
            query = {
                "size": self.batch_size,
                "query": {
                    "bool": {
                        "must": [
                            {"term": {"level": "ERROR"}}
                        ],
                        "must_not": [
                            {"exists": {"field": "log_vector"}}
                        ]
                    }
                },
                "sort": [
                    {"timestamp": {"order": "desc"}}  # Process most recent first
                ],
                "_source": [
                    "log_id", "message", "trace_id", "timestamp",
                    "project_uuid", "safe_project_uuid", "level"
                ]
            }

            # Search across all indices
            result = opensearch_client.search(
                index="*_*",  # Match all project indices (pattern: {project_uuid}_YYYY_MM)
                body=query
            )

            hits = result.get('hits', {}).get('hits', [])
            logs = []

            for hit in hits:
                source = hit['_source']
                source['_index'] = hit['_index']  # Store index name for update
                source['_id'] = hit['_id']  # Store document ID for update
                logs.append(source)

            logger.info(f"  Found {len(logs)} ERROR logs without vectors")
            return logs

        except Exception as e:
            logger.error(f"  ❌ Failed to query OpenSearch: {e}")
            return []

    async def generate_embeddings_batch(self, logs: List[Dict[str, Any]]) -> Dict[str, List[float]]:
        """
        Generate embeddings for a batch of logs

        Args:
            logs: List of log documents

        Returns:
            Dictionary mapping log_id to embedding vector
        """
        embeddings_map = {}

        try:
            for log in logs:
                log_id = log.get('log_id', '')
                message = log.get('message', '')
                trace_id = log.get('trace_id')

                # Prepare text for embedding (same logic as enrichment_consumer)
                text_to_embed = message
                if trace_id:
                    text_to_embed = f"{text_to_embed} [trace: {trace_id}]"

                # Generate embedding using cached service
                embedding = await embedding_service.embed_query(text_to_embed)
                embeddings_map[log_id] = embedding

            logger.info(f"  ✅ Generated {len(embeddings_map)} embeddings")
            return embeddings_map

        except Exception as e:
            logger.error(f"  ❌ Failed to generate embeddings: {e}")
            return embeddings_map

    async def update_opensearch_with_vectors(self, logs: List[Dict[str, Any]], embeddings_map: Dict[str, List[float]]):
        """
        Update OpenSearch documents with log_vector field

        Args:
            logs: Original log documents
            embeddings_map: Mapping of log_id to embedding vector
        """
        success_count = 0
        error_count = 0

        for log in logs:
            log_id = log.get('log_id', '')
            embedding = embeddings_map.get(log_id)

            if not embedding:
                logger.warning(f"  ⚠️  No embedding for log_id {log_id}")
                continue

            try:
                # Update document in OpenSearch
                index_name = log.get('_index')
                doc_id = log.get('_id')

                opensearch_client.update(
                    index=index_name,
                    id=doc_id,
                    body={
                        "doc": {
                            "log_vector": embedding,
                            "ai_enriched_at": datetime.utcnow().isoformat() + 'Z',
                            "ai_enrichment_status": "success",
                            "ai_enrichment_method": "periodic_scheduler"
                        }
                    }
                )

                success_count += 1

            except Exception as e:
                logger.warning(f"  ⚠️  Failed to update log_id {log_id}: {e}")
                error_count += 1

        logger.info(f"  ✅ OpenSearch updated: {success_count} success, {error_count} failed")

        self.stats['total_vectors_created'] += success_count
        self.stats['total_errors'] += error_count

    async def run_enrichment_cycle(self):
        """
        Run one enrichment cycle

        This method is called every 10 minutes by the scheduler.
        """
        self.stats['runs'] += 1
        run_num = self.stats['runs']

        logger.info("=" * 80)
        logger.info(f"🔄 Periodic Enrichment Cycle #{run_num} Started")
        logger.info(f"   Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        logger.info("=" * 80)

        start_time = time.time()

        try:
            # Step 1: Find ERROR logs without vectors
            logger.info("[1/3] Querying OpenSearch for ERROR logs without vectors...")
            logs = await self.find_error_logs_without_vectors()

            if not logs:
                logger.info("  ✅ No ERROR logs need vectorization (all up to date)")
                logger.info("=" * 80)
                return

            # Step 2: Generate embeddings
            logger.info(f"[2/3] Generating embeddings for {len(logs)} logs...")
            embeddings_map = await self.generate_embeddings_batch(logs)

            if not embeddings_map:
                logger.warning("  ⚠️  No embeddings generated, skipping update")
                logger.info("=" * 80)
                return

            # Step 3: Update OpenSearch
            logger.info(f"[3/3] Updating OpenSearch documents...")
            await self.update_opensearch_with_vectors(logs, embeddings_map)

            # Performance summary
            elapsed = time.time() - start_time
            total_elapsed = time.time() - self.stats['start_time']
            avg_rate = self.stats['total_vectors_created'] / total_elapsed if total_elapsed > 0 else 0

            logger.info("")
            logger.info(f"📊 Cycle #{run_num} Summary:")
            logger.info(f"   Duration: {elapsed:.2f}s")
            logger.info(f"   Vectors created this cycle: {len(embeddings_map)}")
            logger.info(f"   Total vectors created: {self.stats['total_vectors_created']:,}")
            logger.info(f"   Average rate: {avg_rate:.2f} vectors/sec")
            logger.info(f"   Total errors: {self.stats['total_errors']}")
            logger.info("=" * 80)

        except Exception as e:
            logger.error(f"❌ Enrichment cycle failed: {e}")
            import traceback
            traceback.print_exc()
            self.stats['total_errors'] += 1

    def print_final_stats(self):
        """Print final statistics before shutdown"""
        elapsed = time.time() - self.stats['start_time']

        print()
        print("=" * 80)
        print("📊 Periodic Enrichment Scheduler - Final Statistics")
        print("=" * 80)
        print(f"  Total runtime: {elapsed:.1f}s ({elapsed/60:.1f} minutes)")
        print(f"  Total cycles: {self.stats['runs']}")
        print(f"  Total vectors created: {self.stats['total_vectors_created']:,}")
        print(f"  Total errors: {self.stats['total_errors']}")

        if elapsed > 0:
            print(f"  Average rate: {self.stats['total_vectors_created']/elapsed:.2f} vectors/sec")

        print("=" * 80)
        print()

    async def start(self):
        """
        Start the periodic scheduler

        Runs every 10 seconds indefinitely until interrupted.
        """
        logger.info("🚀 Starting Periodic Enrichment Scheduler")
        logger.info(f"   Schedule: Every {self.interval_seconds} seconds")
        logger.info(f"   Batch size: {self.batch_size} logs per cycle")
        logger.info(f"   Model: text-embedding-3-large")
        logger.info("")

        # Create scheduler
        scheduler = AsyncIOScheduler()

        # Add job (run every 10 seconds)
        scheduler.add_job(
            self.run_enrichment_cycle,
            trigger=IntervalTrigger(seconds=self.interval_seconds),
            id='periodic_enrichment',
            name='Periodic ERROR Log Enrichment',
            replace_existing=True
        )

        # Start scheduler
        scheduler.start()
        logger.info("✅ Scheduler started successfully")
        logger.info(f"   Next run: {scheduler.get_job('periodic_enrichment').next_run_time}")
        logger.info("")

        # Run first cycle immediately
        logger.info("⚡ Running first cycle immediately...")
        await self.run_enrichment_cycle()

        try:
            # Keep running
            while True:
                await asyncio.sleep(60)  # Check every minute

        except KeyboardInterrupt:
            logger.info("\n⚠️  Received shutdown signal")
            scheduler.shutdown(wait=False)
            self.print_final_stats()
            logger.info("👋 Shutdown complete")


async def main():
    """Entry point"""
    try:
        scheduler = PeriodicEnrichmentScheduler()
        await scheduler.start()
    except Exception as e:
        logger.error(f"❌ Fatal error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    asyncio.run(main())
