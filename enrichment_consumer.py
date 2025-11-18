#!/usr/bin/env python3
"""
AI Enrichment Consumer for ERROR Log Vectorization

This service:
1. Consumes logs from Kafka topic 'application-logs'
2. Filters ERROR logs only
3. Batches them (50 logs or 5 seconds timeout)
4. Calls AI Service batch embedding API
5. Updates OpenSearch documents with log_vector field

Architecture:
    Kafka → Logstash → OpenSearch (all logs, fast indexing)
    Kafka → This Consumer → AI Service → OpenSearch _update (ERROR only)
"""

import asyncio
import aiohttp
import json
import os
import sys
import time
import logging
from datetime import datetime
from typing import List, Dict, Any
from kafka import KafkaConsumer
from opensearchpy import OpenSearch, helpers
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler('enrichment_consumer.log')
    ]
)
logger = logging.getLogger(__name__)


class AIEnrichmentConsumer:
    """
    Kafka consumer for AI-based log enrichment

    Processes ERROR logs asynchronously with batching for optimal performance.
    """

    def __init__(self):
        # Kafka configuration
        self.kafka_servers = os.getenv('KAFKA_BOOTSTRAP_SERVERS', 'localhost:19092').split(',')
        self.kafka_topic = os.getenv('KAFKA_TOPIC', 'application-logs')
        self.consumer_group = os.getenv('CONSUMER_GROUP_ID', 'ai-enrichment-consumer')

        # OpenSearch configuration
        self.opensearch_host = os.getenv('OPENSEARCH_HOST', 'localhost')
        self.opensearch_port = int(os.getenv('OPENSEARCH_PORT', '9200'))
        self.opensearch_user = os.getenv('OPENSEARCH_USER', 'admin')
        self.opensearch_password = os.getenv('OPENSEARCH_PASSWORD', 'admin')

        # AI Service configuration
        self.ai_service_url = os.getenv('AI_SERVICE_URL', 'http://localhost:8000')
        self.ai_batch_endpoint = f"{self.ai_service_url}/api/v1/embedding/batch"

        # Batch configuration
        self.batch_size = int(os.getenv('ENRICHMENT_BATCH_SIZE', '50'))
        self.batch_timeout = float(os.getenv('ENRICHMENT_BATCH_TIMEOUT', '5.0'))

        # Performance metrics
        self.stats = {
            'logs_processed': 0,
            'logs_filtered': 0,  # ERROR logs
            'batches_sent': 0,
            'vectors_created': 0,
            'errors': 0,
            'start_time': time.time()
        }

        logger.info("Initializing AI Enrichment Consumer")
        logger.info(f"  Kafka: {self.kafka_servers} / topic: {self.kafka_topic}")
        logger.info(f"  OpenSearch: {self.opensearch_host}:{self.opensearch_port}")
        logger.info(f"  AI Service: {self.ai_service_url}")
        logger.info(f"  Batch size: {self.batch_size}, timeout: {self.batch_timeout}s")

        # Initialize Kafka consumer
        try:
            self.consumer = KafkaConsumer(
                self.kafka_topic,
                bootstrap_servers=self.kafka_servers,
                group_id=self.consumer_group,
                auto_offset_reset='latest',  # Only process new logs
                enable_auto_commit=True,
                value_deserializer=lambda m: json.loads(m.decode('utf-8')),
                consumer_timeout_ms=1000  # 1 second timeout for batching
            )
            logger.info("✅ Kafka consumer initialized successfully")
        except Exception as e:
            logger.error(f"❌ Failed to initialize Kafka consumer: {e}")
            raise

        # Initialize OpenSearch client
        try:
            self.os_client = OpenSearch(
                hosts=[{
                    'host': self.opensearch_host,
                    'port': self.opensearch_port
                }],
                http_auth=(self.opensearch_user, self.opensearch_password),
                use_ssl=False,
                verify_certs=False,
                timeout=30,
                max_retries=3,
                retry_on_timeout=True
            )
            # Test connection
            info = self.os_client.info()
            logger.info(f"✅ OpenSearch connected: {info['version']['number']}")
        except Exception as e:
            logger.error(f"❌ Failed to connect to OpenSearch: {e}")
            raise

    def should_process_log(self, log: Dict[str, Any]) -> bool:
        """
        Determine if log should be processed (ERROR level only)

        Args:
            log: Log document from Kafka

        Returns:
            True if log is ERROR level
        """
        log_level = log.get('log_level') or log.get('level', '')
        return log_level == 'ERROR'

    async def call_ai_service_batch(self, batch: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        Call AI Service batch embedding API

        Args:
            batch: List of ERROR logs

        Returns:
            List of embeddings with log_id
        """
        payload = {
            'logs': [
                {
                    'log_id': str(log.get('log_id', '')),
                    'message': log.get('message', ''),
                    'trace_id': log.get('trace_id')
                }
                for log in batch
            ]
        }

        try:
            async with aiohttp.ClientSession() as session:
                async with session.post(
                    self.ai_batch_endpoint,
                    json=payload,
                    timeout=aiohttp.ClientTimeout(total=60)
                ) as response:
                    if response.status == 200:
                        result = await response.json()
                        embeddings = result.get('embeddings', [])
                        logger.info(f"  ✅ AI Service returned {len(embeddings)} embeddings")
                        return embeddings
                    else:
                        error_text = await response.text()
                        logger.error(f"  ❌ AI Service error {response.status}: {error_text}")
                        return []
        except asyncio.TimeoutError:
            logger.error(f"  ❌ AI Service timeout after 60s")
            return []
        except Exception as e:
            logger.error(f"  ❌ AI Service call failed: {e}")
            return []

    async def update_opensearch_batch(self, batch: List[Dict[str, Any]], embeddings: List[Dict[str, Any]]):
        """
        Update OpenSearch documents with log_vector field

        Uses _update API to add vector to existing documents without re-indexing.

        Args:
            batch: Original log documents
            embeddings: Embeddings from AI Service (with log_id)
        """
        # Create embedding lookup by log_id
        embedding_map = {
            str(emb['log_id']): emb['embedding']
            for emb in embeddings
        }

        actions = []
        for log in batch:
            log_id = str(log.get('log_id', ''))
            embedding = embedding_map.get(log_id)

            if not embedding:
                logger.warning(f"  ⚠️  No embedding for log_id {log_id}")
                continue

            # Calculate index name (same logic as Logstash)
            # Format: {safe_project_uuid}_{YYYY_MM}
            project_uuid = log.get('project_uuid', '')
            safe_project_uuid = log.get('safe_project_uuid') or project_uuid.replace('-', '_')

            timestamp = log.get('timestamp', '')
            if timestamp:
                # Extract YYYY_MM from timestamp (e.g., "2025-11-18T...")
                try:
                    year_month = timestamp[:7].replace('-', '_')  # "2025-11" → "2025_11"
                    index_name = f"{safe_project_uuid}_{year_month}"
                except Exception:
                    logger.warning(f"  ⚠️  Invalid timestamp format: {timestamp}")
                    continue
            else:
                # Fallback: current month
                year_month = datetime.utcnow().strftime('%Y_%m')
                index_name = f"{safe_project_uuid}_{year_month}"

            # Prepare update action
            actions.append({
                '_op_type': 'update',
                '_index': index_name,
                '_id': log.get('[@metadata][doc_id]') or log_id,
                'doc': {
                    'log_vector': embedding,
                    'ai_enriched_at': datetime.utcnow().isoformat() + 'Z',
                    'ai_enrichment_status': 'success'
                },
                'doc_as_upsert': False  # Don't create new docs, only update existing
            })

        # Bulk update
        if actions:
            try:
                success, failed = helpers.bulk(
                    self.os_client,
                    actions,
                    raise_on_error=False,
                    raise_on_exception=False
                )

                logger.info(f"  ✅ OpenSearch updated: {success} success, {len(failed)} failed")

                if failed:
                    for fail in failed[:5]:  # Log first 5 failures
                        logger.warning(f"    ⚠️  Update failed: {fail}")

                self.stats['vectors_created'] += success

            except Exception as e:
                logger.error(f"  ❌ OpenSearch bulk update failed: {e}")
                self.stats['errors'] += 1

    async def process_batch(self, batch: List[Dict[str, Any]]):
        """
        Process a batch of ERROR logs

        1. Call AI Service for embeddings
        2. Update OpenSearch with vectors

        Args:
            batch: List of ERROR logs (max 50)
        """
        if not batch:
            return

        batch_num = self.stats['batches_sent'] + 1
        logger.info(f"🔄 Processing batch #{batch_num} ({len(batch)} ERROR logs)")

        # Step 1: Get embeddings from AI Service
        logger.info(f"  [1/2] Calling AI Service...")
        embeddings = await self.call_ai_service_batch(batch)

        if not embeddings:
            logger.warning(f"  ⚠️  No embeddings returned, skipping OpenSearch update")
            self.stats['errors'] += 1
            return

        # Step 2: Update OpenSearch
        logger.info(f"  [2/2] Updating OpenSearch...")
        await self.update_opensearch_batch(batch, embeddings)

        self.stats['batches_sent'] += 1

        # Log performance
        elapsed = time.time() - self.stats['start_time']
        rate = self.stats['vectors_created'] / elapsed if elapsed > 0 else 0
        logger.info(f"  📊 Performance: {rate:.1f} vectors/sec, {self.stats['batches_sent']} batches total")

    def print_stats(self):
        """Print performance statistics"""
        elapsed = time.time() - self.stats['start_time']

        print()
        print("=" * 80)
        print("📊 AI Enrichment Consumer Statistics")
        print("=" * 80)
        print(f"  Runtime: {elapsed:.1f}s")
        print(f"  Logs processed: {self.stats['logs_processed']:,}")
        print(f"  ERROR logs (filtered): {self.stats['logs_filtered']:,}")
        print(f"  Batches sent: {self.stats['batches_sent']}")
        print(f"  Vectors created: {self.stats['vectors_created']:,}")
        print(f"  Errors: {self.stats['errors']}")

        if elapsed > 0:
            print(f"  Rate: {self.stats['logs_processed']/elapsed:.1f} logs/sec")
            print(f"  Rate: {self.stats['vectors_created']/elapsed:.1f} vectors/sec")

        print("=" * 80)
        print()

    async def run(self):
        """
        Main consumer loop

        Consumes logs from Kafka, batches ERROR logs, and calls AI Service.
        """
        logger.info("🚀 Starting AI Enrichment Consumer")
        logger.info(f"   Listening to Kafka topic: {self.kafka_topic}")
        logger.info(f"   Filter: log_level == 'ERROR'")
        logger.info(f"   Batch: {self.batch_size} logs or {self.batch_timeout}s timeout")
        print()

        batch = []
        last_batch_time = time.time()

        try:
            while True:
                # Poll messages from Kafka
                messages = self.consumer.poll(timeout_ms=1000, max_records=100)

                for topic_partition, records in messages.items():
                    for message in records:
                        self.stats['logs_processed'] += 1

                        try:
                            log = message.value

                            # Filter ERROR logs only
                            if not self.should_process_log(log):
                                continue

                            self.stats['logs_filtered'] += 1
                            batch.append(log)

                            # Process batch if size threshold reached
                            if len(batch) >= self.batch_size:
                                await self.process_batch(batch)
                                batch = []
                                last_batch_time = time.time()

                        except Exception as e:
                            logger.error(f"❌ Error processing message: {e}")
                            self.stats['errors'] += 1

                # Process batch if timeout reached
                current_time = time.time()
                if batch and (current_time - last_batch_time) >= self.batch_timeout:
                    await self.process_batch(batch)
                    batch = []
                    last_batch_time = current_time

                # Print stats every 100 logs
                if self.stats['logs_processed'] % 100 == 0 and self.stats['logs_processed'] > 0:
                    self.print_stats()

        except KeyboardInterrupt:
            logger.info("\n⚠️  Received shutdown signal")

            # Process remaining batch
            if batch:
                logger.info(f"   Processing final batch ({len(batch)} logs)...")
                await self.process_batch(batch)

            self.print_stats()
            logger.info("👋 Shutdown complete")

        finally:
            self.consumer.close()


async def main():
    """Entry point"""
    try:
        consumer = AIEnrichmentConsumer()
        await consumer.run()
    except Exception as e:
        logger.error(f"❌ Fatal error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    asyncio.run(main())
