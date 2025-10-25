"""
Kafka consumer for processing logs
"""

import json
import asyncio
from datetime import datetime
from confluent_kafka import Consumer, KafkaError
from app.core.config import settings
from app.core.opensearch import opensearch_client
from app.services.embedding_service import embedding_service


class LogConsumer:
    """Kafka consumer for application logs"""

    def __init__(self):
        self.consumer = Consumer(
            {
                "bootstrap.servers": settings.KAFKA_BOOTSTRAP_SERVERS,
                "group.id": settings.KAFKA_GROUP_ID,
                "auto.offset.reset": "earliest",
                "enable.auto.commit": True,
            }
        )
        self.running = False
        self.topic = settings.KAFKA_TOPIC

    async def start(self):
        """Start consuming messages"""
        self.running = True
        self.consumer.subscribe([self.topic])

        print(f"📨 Kafka consumer started: topic={self.topic}")

        try:
            while self.running:
                msg = self.consumer.poll(timeout=1.0)

                if msg is None:
                    await asyncio.sleep(0.1)
                    continue

                if msg.error():
                    if msg.error().code() == KafkaError._PARTITION_EOF:
                        continue
                    else:
                        print(f"❌ Consumer error: {msg.error()}")
                        continue

                # Process message
                await self._process_message(msg)

        except Exception as e:
            print(f"❌ Fatal consumer error: {e}")
        finally:
            self.consumer.close()

    def stop(self):
        """Stop consuming messages"""
        self.running = False
        print("🛑 Kafka consumer stopped")

    async def _process_message(self, msg):
        """
        Process a single log message

        Steps:
        1. Parse JSON message
        2. Generate embedding for log
        3. Store in OpenSearch with vector
        """
        try:
            # Parse message
            log_data = json.loads(msg.value().decode("utf-8"))
            log_id = log_data.get("log_id", "unknown")

            print(f"📨 Processing log: {log_id}")

            # Generate embedding
            text_to_embed = self._prepare_text_for_embedding(log_data)
            log_vector = await embedding_service.embed_query(text_to_embed)

            # Add vector to log data
            log_data["log_vector"] = log_vector
            log_data["indexed_at"] = datetime.utcnow().isoformat()

            # Store in OpenSearch
            index_name = self._get_index_name(log_data.get("timestamp"))
            opensearch_client.index(index=index_name, body=log_data, id=log_id)

            print(f"✅ Log saved to OpenSearch: {log_id} -> {index_name}")

        except Exception as e:
            print(f"❌ Error processing message: {e}")

    def _prepare_text_for_embedding(self, log_data: dict) -> str:
        """
        Prepare log text for embedding

        Combines relevant fields to create semantic representation
        """
        parts = [
            log_data.get("level", ""),
            log_data.get("message", ""),
            log_data.get("class_name", ""),
            log_data.get("method_name", ""),
            log_data.get("stack_trace", "")[:500],  # Limit stack trace length
        ]
        return " ".join(filter(None, parts))

    def _get_index_name(self, timestamp: str) -> str:
        """
        Generate time-based index name

        Format: logs-YYYY-MM
        """
        try:
            dt = datetime.fromisoformat(timestamp.replace("Z", "+00:00"))
            return f"logs-{dt.strftime('%Y-%m')}"
        except:
            return f"logs-{datetime.utcnow().strftime('%Y-%m')}"


# Global instance
log_consumer = LogConsumer()
