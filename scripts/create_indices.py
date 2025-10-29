#!/usr/bin/env python3
"""
Create OpenSearch indices with proper mappings
"""

import os
import sys

# Add parent directory to path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.core.opensearch import opensearch_client


def create_logs_index_template():
    """Create index template for logs-* indices"""

    template_body = {
        "index_patterns": ["logs-*"],
        "template": {
            "settings": {
                "number_of_shards": 1,
                "number_of_replicas": 0,
                "index.knn": True,  # Enable KNN
            },
            "mappings": {
                "properties": {
                    # Core identification fields
                    "log_id": {"type": "keyword"},
                    "project_id": {"type": "integer"},  # Multi-tenancy support (Logstash sends integer)
                    "timestamp": {"type": "date"},

                    # Service and logging context
                    "service_name": {"type": "keyword"},
                    "logger": {"type": "keyword"},  # Logger name (e.g., com.example.UserService)
                    "source_type": {"type": "keyword"},  # FE/BE/INFRA
                    "layer": {"type": "keyword"},  # Controller/Service/Repository

                    # Log content
                    "log_level": {"type": "keyword"},  # Logstash sends this field (ERROR/WARN/INFO)
                    "level": {"type": "keyword"},      # Compatibility alias for AI code
                    "message": {"type": "text"},
                    "comment": {"type": "text"},  # Additional comment

                    # Method and thread info
                    "method_name": {"type": "keyword"},
                    "class_name": {"type": "keyword"},
                    "thread_name": {"type": "keyword"},

                    # Tracing
                    "trace_id": {"type": "keyword"},
                    "user_id": {"type": "keyword"},

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
                            "request_headers": {"type": "flattened"},  # JSON object
                            "request_body": {"type": "flattened"},  # JSON object
                            "response_status": {"type": "integer"},  # HTTP status code
                            "response_body": {"type": "flattened"},  # JSON object

                            # Code location
                            "class_name": {"type": "keyword"},
                            "method_name": {"type": "keyword"},

                            # Additional context
                            "additional_info": {"type": "flattened"},  # JSON object
                        }
                    },

                    # Vector for similarity search
                    "log_vector": {
                        "type": "knn_vector",
                        "dimension": 1536,  # text-embedding-3-large
                        "method": {
                            "name": "hnsw",
                            "space_type": "cosinesimil",  # Cosine similarity for text embeddings
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
        },
    }

    try:
        # Delete existing template if exists
        try:
            opensearch_client.indices.delete_index_template(name="logs-template")
            print("🗑️  Deleted existing logs template")
        except:
            pass

        # Create new template
        opensearch_client.indices.put_index_template(
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
                        "space_type": "l2",
                        "engine": "faiss",
                    },
                },
                "answer": {"type": "text"},
                "related_log_ids": {"type": "keyword"},
                "cached_at": {"type": "date"},
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
