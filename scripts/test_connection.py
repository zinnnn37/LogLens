#!/usr/bin/env python3
"""
Test connections to all external services
"""

import os
import sys

# Add parent directory to path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app.core.config import settings
from app.core.opensearch import opensearch_client
from confluent_kafka.admin import AdminClient
from openai import OpenAI


def test_opensearch():
    """Test OpenSearch connection"""
    print("🔍 Testing OpenSearch connection...")
    try:
        info = opensearch_client.info()
        health = opensearch_client.cluster.health()

        print(f"✅ OpenSearch connected")
        print(f"   Version: {info['version']['number']}")
        print(f"   Cluster: {health['cluster_name']}")
        print(f"   Status: {health['status']}")
        return True

    except Exception as e:
        print(f"❌ OpenSearch connection failed: {e}")
        return False


def test_kafka():
    """Test Kafka connection"""
    print("\n🔍 Testing Kafka connection...")
    try:
        admin_client = AdminClient(
            {"bootstrap.servers": settings.KAFKA_BOOTSTRAP_SERVERS}
        )

        metadata = admin_client.list_topics(timeout=5)

        print(f"✅ Kafka connected")
        print(f"   Bootstrap servers: {settings.KAFKA_BOOTSTRAP_SERVERS}")
        print(f"   Topics: {', '.join(metadata.topics.keys())}")
        return True

    except Exception as e:
        print(f"❌ Kafka connection failed: {e}")
        return False


def test_openai():
    """Test OpenAI API connection"""
    print("\n🔍 Testing OpenAI API...")
    try:
        client = OpenAI(api_key=settings.OPENAI_API_KEY)

        # Simple embedding test
        response = client.embeddings.create(
            model=settings.EMBEDDING_MODEL, input="test"
        )

        print(f"✅ OpenAI API connected")
        print(f"   Embedding model: {settings.EMBEDDING_MODEL}")
        print(f"   LLM model: {settings.LLM_MODEL}")
        print(f"   Vector dimension: {len(response.data[0].embedding)}")
        return True

    except Exception as e:
        print(f"❌ OpenAI API connection failed: {e}")
        return False


def main():
    """Main function"""
    print("🚀 Testing connections...\n")
    print(f"Environment: {settings.ENVIRONMENT}")
    print(f"App: {settings.APP_NAME} v{settings.APP_VERSION}\n")

    results = []
    results.append(("OpenSearch", test_opensearch()))
    results.append(("Kafka", test_kafka()))
    results.append(("OpenAI", test_openai()))

    print("\n" + "=" * 50)
    print("📊 Connection Test Results:")
    print("=" * 50)

    for service, success in results:
        status = "✅ PASS" if success else "❌ FAIL"
        print(f"{service:20s} {status}")

    all_success = all(success for _, success in results)

    if all_success:
        print("\n✨ All connections successful!")
        print("\nNext steps:")
        print("1. Run: python scripts/create_indices.py")
        print("2. Start the server: uvicorn app.main:app --reload")
        sys.exit(0)
    else:
        print("\n⚠️  Some connections failed. Please check configuration.")
        print("\nTroubleshooting:")
        print("- Check .env file has correct values")
        print("- Ensure Docker containers are running (infrastructure)")
        print("- Verify OpenAI API key is valid")
        sys.exit(1)


if __name__ == "__main__":
    main()
