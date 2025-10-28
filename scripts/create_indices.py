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
                    "log_id": {"type": "keyword"},
                    "timestamp": {"type": "date"},
                    "service_name": {"type": "keyword"},
                    "level": {"type": "keyword"},
                    "message": {"type": "text"},
                    "method_name": {"type": "keyword"},
                    "class_name": {"type": "keyword"},
                    "thread_name": {"type": "keyword"},
                    "trace_id": {"type": "keyword"},
                    "user_id": {"type": "keyword"},
                    "duration": {"type": "integer"},
                    "stack_trace": {"type": "text"},
                    "log_vector": {
                        "type": "knn_vector",
                        "dimension": 1536,  # text-embedding-3-large
                        "method": {
                            "name": "hnsw",
                            "space_type": "l2",
                            "engine": "faiss",
                        },
                    },
                    "ai_analysis": {
                        "type": "object",
                        "properties": {
                            "summary": {"type": "text"},
                            "error_cause": {"type": "text"},
                            "solution": {"type": "text"},
                            "tags": {"type": "keyword"},
                            "analyzed_at": {"type": "date"},
                        },
                    },
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
    """Create QA cache index for chatbot"""

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
        # Delete existing index if exists
        if opensearch_client.indices.exists(index="qa-cache"):
            opensearch_client.indices.delete(index="qa-cache")
            print("🗑️  Deleted existing qa-cache index")

        # Create new index
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
        print("1. Start the FastAPI server: uvicorn app.main:app --reload")
        print("2. Send logs via Kafka to topic 'application-logs'")
        sys.exit(0)
    else:
        print("\n⚠️  Some indices failed to create. Please check errors above.")
        sys.exit(1)


if __name__ == "__main__":
    main()
