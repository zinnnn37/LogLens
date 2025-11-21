#!/usr/bin/env python3
"""
Seed test data to OpenSearch for local testing
"""

import os
import sys
from datetime import datetime, timedelta
from typing import List, Dict
import random

# Add parent directory to path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# Load environment variables (.env.test first for local testing)
from dotenv import load_dotenv

project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
env_test_path = os.path.join(project_root, '.env.test')
env_path = os.path.join(project_root, '.env')

if os.path.exists(env_test_path):
    load_dotenv(env_test_path)
    print("🔧 Using .env.test for local testing")
else:
    load_dotenv(env_path)

from app.core.opensearch import opensearch_client
from app.services.embedding_service import embedding_service


# Sample log data
SAMPLE_LOGS = [
    # ERROR logs
    {
        "log_level": "ERROR",
        "service_name": "user-service",
        "logger": "com.example.user.UserService",
        "layer": "Service",
        "method_name": "getUser",
        "class_name": "UserService",
        "message": "Failed to fetch user data: NullPointerException",
        "stack_trace": """java.lang.NullPointerException: User object is null
    at com.example.user.UserService.getUser(UserService.java:42)
    at com.example.user.UserController.getUserById(UserController.java:28)
    at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)""",
        "log_details": {
            "exception_type": "NullPointerException",
            "http_method": "GET",
            "request_uri": "/api/users/12345",
            "response_status": 500
        }
    },
    {
        "log_level": "ERROR",
        "service_name": "database-service",
        "logger": "com.example.db.ConnectionPool",
        "layer": "Repository",
        "method_name": "getConnection",
        "class_name": "ConnectionPool",
        "message": "Database connection timeout after 30 seconds",
        "stack_trace": """java.sql.SQLException: Connection is not available, request timed out after 30000ms
    at com.zaxxer.hikari.pool.HikariPool.createTimeoutException(HikariPool.java:696)
    at com.zaxxer.hikari.pool.HikariPool.getConnection(HikariPool.java:197)
    at com.example.db.ConnectionPool.getConnection(ConnectionPool.java:54)""",
        "log_details": {
            "exception_type": "SQLException",
            "execution_time": 30000
        }
    },
    {
        "log_level": "ERROR",
        "service_name": "payment-service",
        "logger": "com.example.payment.PaymentProcessor",
        "layer": "Service",
        "method_name": "processPayment",
        "class_name": "PaymentProcessor",
        "message": "Payment processing failed: Insufficient funds",
        "stack_trace": """com.example.payment.InsufficientFundsException: Account balance is insufficient
    at com.example.payment.PaymentProcessor.validateBalance(PaymentProcessor.java:78)
    at com.example.payment.PaymentProcessor.processPayment(PaymentProcessor.java:45)""",
        "log_details": {
            "exception_type": "InsufficientFundsException",
            "http_method": "POST",
            "request_uri": "/api/payments/process",
            "response_status": 400
        }
    },
    {
        "log_level": "ERROR",
        "service_name": "file-service",
        "logger": "com.example.file.FileHandler",
        "layer": "Service",
        "method_name": "readFile",
        "class_name": "FileHandler",
        "message": "File not found: /data/uploads/document.pdf",
        "stack_trace": """java.io.FileNotFoundException: /data/uploads/document.pdf (No such file or directory)
    at java.base/java.io.FileInputStream.open0(Native Method)
    at java.base/java.io.FileInputStream.open(FileInputStream.java:219)
    at com.example.file.FileHandler.readFile(FileHandler.java:34)""",
        "log_details": {
            "exception_type": "FileNotFoundException",
            "http_method": "GET",
            "request_uri": "/api/files/document.pdf",
            "response_status": 404
        }
    },
    {
        "log_level": "ERROR",
        "service_name": "auth-service",
        "logger": "com.example.auth.AuthenticationService",
        "layer": "Service",
        "method_name": "authenticate",
        "class_name": "AuthenticationService",
        "message": "Authentication failed: Invalid credentials",
        "log_details": {
            "http_method": "POST",
            "request_uri": "/api/auth/login",
            "response_status": 401
        }
    },

    # WARN logs
    {
        "log_level": "WARN",
        "service_name": "memory-service",
        "logger": "com.example.memory.MemoryMonitor",
        "layer": "Service",
        "method_name": "checkMemory",
        "class_name": "MemoryMonitor",
        "message": "Memory usage exceeded 85%: Current usage 87.5%",
        "log_details": {
            "additional_info": {"memory_usage": 0.875, "threshold": 0.85}
        }
    },
    {
        "log_level": "WARN",
        "service_name": "api-gateway",
        "logger": "com.example.gateway.RateLimiter",
        "layer": "Controller",
        "method_name": "checkRateLimit",
        "class_name": "RateLimiter",
        "message": "API rate limit approaching: 95 of 100 requests used",
        "log_details": {
            "http_method": "GET",
            "request_uri": "/api/data/list",
            "additional_info": {"requests_used": 95, "limit": 100}
        }
    },
    {
        "log_level": "WARN",
        "service_name": "cache-service",
        "logger": "com.example.cache.CacheManager",
        "layer": "Service",
        "method_name": "evictExpired",
        "class_name": "CacheManager",
        "message": "Cache size exceeded 90%: Evicting old entries",
        "log_details": {
            "additional_info": {"cache_size": 0.92, "evicted_entries": 1500}
        }
    },
    {
        "log_level": "WARN",
        "service_name": "queue-service",
        "logger": "com.example.queue.MessageQueue",
        "layer": "Service",
        "method_name": "processQueue",
        "class_name": "MessageQueue",
        "message": "Message queue backlog detected: 5000 pending messages",
        "log_details": {
            "additional_info": {"pending_messages": 5000, "threshold": 1000}
        }
    },

    # INFO logs
    {
        "log_level": "INFO",
        "service_name": "user-service",
        "logger": "com.example.user.UserController",
        "layer": "Controller",
        "method_name": "createUser",
        "class_name": "UserController",
        "message": "User created successfully",
        "log_details": {
            "http_method": "POST",
            "request_uri": "/api/users",
            "response_status": 201,
            "execution_time": 145
        }
    },
    {
        "log_level": "INFO",
        "service_name": "order-service",
        "logger": "com.example.order.OrderService",
        "layer": "Service",
        "method_name": "createOrder",
        "class_name": "OrderService",
        "message": "Order created successfully: Order #12345",
        "log_details": {
            "http_method": "POST",
            "request_uri": "/api/orders",
            "response_status": 200,
            "execution_time": 234
        }
    },
    {
        "log_level": "INFO",
        "service_name": "notification-service",
        "logger": "com.example.notification.EmailService",
        "layer": "Service",
        "method_name": "sendEmail",
        "class_name": "EmailService",
        "message": "Email sent successfully to user@example.com",
        "log_details": {
            "execution_time": 523
        }
    },
]


def generate_test_logs() -> List[Dict]:
    """Generate test log data with realistic timestamps"""
    test_logs = []
    base_time = datetime.now()

    for i, sample in enumerate(SAMPLE_LOGS):
        # Generate timestamp (spread over last 24 hours)
        timestamp = base_time - timedelta(
            hours=random.randint(0, 23),
            minutes=random.randint(0, 59),
            seconds=random.randint(0, 59)
        )

        log = {
            "log_id": i + 1,
            "project_uuid": "test-project-uuid",
            "timestamp": timestamp.isoformat(),
            "service_name": sample["service_name"],
            "logger": sample["logger"],
            "source_type": "BE",
            "layer": sample.get("layer", "Service"),
            "log_level": sample["log_level"],
            "level": sample["log_level"],  # Compatibility alias
            "message": sample["message"],
            "method_name": sample.get("method_name", "unknown"),
            "class_name": sample.get("class_name", "Unknown"),
            "thread_name": f"http-nio-8080-exec-{random.randint(1, 10)}",
            "trace_id": f"trace-{random.randint(1000, 9999)}",
            "requester_ip": f"192.168.1.{random.randint(1, 254)}",
            "indexed_at": datetime.now().isoformat(),
        }

        # Add optional fields
        if "stack_trace" in sample:
            log["stack_trace"] = sample["stack_trace"]

        if "log_details" in sample:
            log["log_details"] = sample["log_details"]

        if "duration" in sample:
            log["duration"] = sample["duration"]

        test_logs.append(log)

    return test_logs


async def seed_data():
    """Seed test data to OpenSearch"""
    print("🚀 Starting test data seeding...\n")

    # Generate test logs
    print("1️⃣  Generating test log data...")
    test_logs = generate_test_logs()
    print(f"   ✅ Generated {len(test_logs)} test logs")

    # Generate embeddings and index
    print("\n2️⃣  Generating embeddings and indexing to OpenSearch...")
    success_count = 0
    error_count = 0

    for i, log in enumerate(test_logs, 1):
        try:
            # Generate embedding for log message
            print(f"   [{i}/{len(test_logs)}] Processing log_id={log['log_id']} ({log['log_level']})...", end=" ")

            embedding = await embedding_service.embed_query(log["message"])
            log["log_vector"] = embedding

            # Index to OpenSearch
            index_name = f"logs-{datetime.now().year}"
            response = opensearch_client.index(
                index=index_name,
                body=log,
                refresh=True  # Make immediately searchable
            )

            print(f"✅ Indexed (doc_id: {response['_id']})")
            success_count += 1

        except Exception as e:
            print(f"❌ Error: {e}")
            error_count += 1

    # Print summary
    print("\n" + "=" * 60)
    print("📊 Seeding Results:")
    print("=" * 60)
    print(f"✅ Successfully indexed: {success_count}")
    print(f"❌ Failed: {error_count}")
    print(f"📁 Index: logs-{datetime.now().year}")
    print(f"🔑 Project UUID: test-project-uuid")

    # Show log level distribution
    print("\n📈 Log Level Distribution:")
    level_counts = {}
    for log in test_logs[:success_count]:
        level = log["log_level"]
        level_counts[level] = level_counts.get(level, 0) + 1

    for level, count in sorted(level_counts.items()):
        print(f"   {level}: {count}")

    print("\n✨ Test data seeding complete!")
    print("\n📌 Next steps:")
    print("1. Start FastAPI server: uvicorn app.main:app --reload --env-file .env.test")
    print("2. Access Swagger UI: http://localhost:8000/docs")
    print("3. Test APIs with project_uuid='test-project-uuid'")
    print("\n💡 Verify data:")
    print(f"   curl 'http://localhost:9200/logs-{datetime.now().year}/_search?pretty&size=5'")


if __name__ == "__main__":
    import asyncio
    asyncio.run(seed_data())
