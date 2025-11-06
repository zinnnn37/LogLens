#!/usr/bin/env python3
"""
Create OpenSearch indices with proper mappings
"""

import os
import sys

# Add parent directory to path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# Load environment variables (.env.test first for local testing)
from dotenv import load_dotenv

# Try loading .env.test first (for local testing), fallback to .env
project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
env_test_path = os.path.join(project_root, '.env.test')
env_path = os.path.join(project_root, '.env')

if os.path.exists(env_test_path):
    load_dotenv(env_test_path)
    print("🔧 Using .env.test for local testing")
else:
    load_dotenv(env_path)

from app.core.opensearch import opensearch_client


def create_logs_index_template():
    """Create index template for logs-* indices"""

    template_body = {
        "index_patterns": ["logs-*"],
        "settings": {
            "number_of_shards": 1,
            "number_of_replicas": 0,
            "index.knn": True,  # Enable KNN
        },
        "mappings": {
                "properties": {
                    # Core identification fields
                    "log_id": {"type": "long"},  # Integer log ID from database
                    "project_uuid": {"type": "keyword"},  # Multi-tenancy support (UUID)
                    "timestamp": {"type": "date"},

                    # Service and logging context
                    "service_name": {"type": "keyword"},
                    "logger": {"type": "keyword"},  # Logger name (e.g., com.example.UserService)
                    "source_type": {"type": "keyword"},  # FE/BE/INFRA
                    "layer": {"type": "keyword"},  # Controller/Service/Repository

                    # Log content
                    "log_level": {"type": "keyword"},  # INFO/WARN/ERROR only
                    "level": {"type": "keyword"},      # Compatibility alias for AI code
                    "message": {"type": "text"},
                    "comment": {"type": "text"},  # Additional comment

                    # Method and thread info
                    "method_name": {"type": "keyword"},
                    "class_name": {"type": "keyword"},
                    "thread_name": {"type": "keyword"},

                    # Tracing
                    "trace_id": {"type": "keyword"},

                    # Requester identification (DevSecOps)
                    "requester_ip": {"type": "ip"},  # Requester IP address for user identification

                    # Performance
                    "duration": {"type": "integer"},  # milliseconds

                    # Error info
                    "stack_trace": {"type": "text"},

                    # Log details (nested object - from log_details table)
                    "log_details": {
                        "type": "object",
                        "properties": {
                            "exception_type": {"type": "keyword"},
                            "execution_time": {"type": "long"},  # milliseconds
                            "stack_trace": {"type": "text"},

                            # HTTP request/response details
                            "http_method": {"type": "keyword"},  # GET/POST/PUT/DELETE
                            "request_uri": {"type": "text"},
                            "request_headers": {"type": "object"},  # JSON object
                            "request_body": {"type": "object"},  # JSON object
                            "response_status": {"type": "integer"},  # HTTP status code
                            "response_body": {"type": "object"},  # JSON object

                            # Code location
                            "class_name": {"type": "keyword"},
                            "method_name": {"type": "keyword"},

                            # Additional context
                            "additional_info": {"type": "object"},  # JSON object
                        }
                    },

                    # Vector for similarity search
                    "log_vector": {
                        "type": "knn_vector",
                        "dimension": 1536,  # text-embedding-3-large
                        "method": {
                            "name": "hnsw",
                            "space_type": "innerproduct",  # Equivalent to cosine for normalized OpenAI embeddings
                            "engine": "faiss",
                        },
                    },

                    # AI analysis results (integrated from ai_analysis_results table)
                    "ai_analysis": {
                        "type": "object",
                        "properties": {
                            "summary": {"type": "text"},
                            "error_cause": {"type": "text"},
                            "solution": {"type": "text"},
                            "tags": {"type": "keyword"},
                            "analysis_type": {"type": "keyword"},  # SINGLE/TRACE_BASED
                            "target_type": {"type": "keyword"},  # LOG/LOG_DETAILS
                            "analyzed_at": {"type": "date"},
                        },
                    },

                    # System fields
                    "indexed_at": {"type": "date"},
                }
        },
    }

    try:
        # Delete existing template if exists
        try:
            opensearch_client.indices.delete_template(name="logs-template")
            print("🗑️  Deleted existing logs template")
        except:
            pass

        # Create new template
        opensearch_client.indices.put_template(
            name="logs-template", body=template_body
        )
        print("✅ Created logs index template")
        return True
    except Exception as e:
        print(f"❌ Error creating logs template: {e}")
        return False


def create_qa_cache_index():
    """Create QA cache index for chatbot (preserves existing cache data)"""

    index_body = {
        "settings": {
            "number_of_shards": 1,
            "number_of_replicas": 0,
            "index.knn": True,
        },
        "mappings": {
            "properties": {
                "question": {"type": "text"},
                "question_vector": {
                    "type": "knn_vector",
                    "dimension": 1536,
                    "method": {
                        "name": "hnsw",
                        "space_type": "innerproduct",  # Equivalent to cosine for normalized OpenAI embeddings
                        "engine": "faiss",
                    },
                },
                "answer": {"type": "text"},
                "related_log_ids": {"type": "long"},  # Array of integer log IDs

                # Metadata for context-aware caching
                "metadata": {
                    "type": "object",
                    "properties": {
                        "project_id": {"type": "keyword"},  # UUID string
                        "filters": {"type": "object"},  # Dynamic filters object
                        "time_range": {"type": "object"},  # Time range object
                    }
                },

                # TTL fields
                "cached_at": {"type": "date"},
                "expires_at": {"type": "date"},  # TTL expiration time
            }
        },
    }

    try:
        # Check if index already exists
        if opensearch_client.indices.exists(index="qa-cache"):
            print("ℹ️  qa-cache index already exists")
            print("✅ Preserving existing cached QA data")
            return True

        # Create new index only if it doesn't exist
        opensearch_client.indices.create(index="qa-cache", body=index_body)
        print("✅ Created qa-cache index")
        return True
    except Exception as e:
        print(f"❌ Error creating qa-cache index: {e}")
        return False


def main():
    """Main function"""
    print("🚀 Creating OpenSearch indices...\n")

    results = []
    results.append(("Logs template", create_logs_index_template()))
    results.append(("QA cache index", create_qa_cache_index()))

    print("\n" + "=" * 50)
    print("📊 Index Creation Results:")
    print("=" * 50)

    for name, success in results:
        status = "✅ SUCCESS" if success else "❌ FAILED"
        print(f"{name:20s} {status}")

    all_success = all(success for _, success in results)

    if all_success:
        print("\n✨ All indices created successfully!")
        print("\nNext steps:")
        print("1. Ensure Logstash is sending logs to OpenSearch (logs-* indices)")
        print("2. Start the FastAPI server: uvicorn app.main:app --reload")
        print("3. Access API at: http://localhost:8000/docs")
        sys.exit(0)
    else:
        print("\n⚠️  Some indices failed to create. Please check errors above.")
        sys.exit(1)


if __name__ == "__main__":
    main()
