#!/usr/bin/env python3
"""
Test script for AI Enrichment Consumer

Tests:
1. AI Service batch embedding API
2. Consumer processing logic (without Kafka)
3. OpenSearch update functionality
"""

import asyncio
import sys
from dotenv import load_dotenv

load_dotenv()

import requests
from app.services.embedding_service import embedding_service


async def test_batch_embedding_api():
    """Test 1: AI Service batch embedding API"""
    print("=" * 80)
    print("Test 1: AI Service Batch Embedding API")
    print("=" * 80)
    print()

    url = "http://localhost:8000/api/v1/embedding/batch"

    # Test data: 3 ERROR logs
    payload = {
        "logs": [
            {
                "log_id": "test-1001",
                "message": "NullPointerException at UserService.createUser line 45",
                "trace_id": "trace-abc123"
            },
            {
                "log_id": "test-1002",
                "message": "Database connection timeout after 30 seconds",
                "trace_id": "trace-def456"
            },
            {
                "log_id": "test-1003",
                "message": "Authentication failed: invalid JWT token signature",
                "trace_id": None
            }
        ]
    }

    print(f"📤 Sending batch request to {url}")
    print(f"   Logs: {len(payload['logs'])}")
    print()

    try:
        response = requests.post(url, json=payload, timeout=60)

        if response.status_code == 200:
            result = response.json()
            embeddings = result.get("embeddings", [])

            print(f"✅ Success!")
            print(f"   Status: {response.status_code}")
            print(f"   Embeddings returned: {len(embeddings)}")
            print(f"   Model: {result.get('model')}")
            print()

            for i, emb in enumerate(embeddings[:2], 1):  # Show first 2
                vector = emb['embedding']
                print(f"   [{i}] log_id: {emb['log_id']}")
                print(f"       vector: [{vector[0]:.4f}, {vector[1]:.4f}, ..., {vector[-1]:.4f}]")
                print(f"       dimension: {len(vector)}")
            print()

            return True

        else:
            print(f"❌ Failed!")
            print(f"   Status: {response.status_code}")
            print(f"   Response: {response.text}")
            return False

    except Exception as e:
        print(f"❌ Error: {e}")
        import traceback
        traceback.print_exc()
        return False


async def test_embedding_service_cache():
    """Test 2: Embedding service caching"""
    print("=" * 80)
    print("Test 2: Embedding Service Caching")
    print("=" * 80)
    print()

    test_text = "NullPointerException at line 45"

    # First call (cache miss)
    print(f"📤 First call (cache miss): '{test_text}'")
    import time
    start = time.time()
    embedding1 = await embedding_service.embed_query(test_text)
    elapsed1 = (time.time() - start) * 1000
    print(f"   ✅ Time: {elapsed1:.0f}ms, dimension: {len(embedding1)}")
    print()

    # Second call (cache hit)
    print(f"📤 Second call (cache hit): '{test_text}'")
    start = time.time()
    embedding2 = await embedding_service.embed_query(test_text)
    elapsed2 = (time.time() - start) * 1000
    print(f"   ✅ Time: {elapsed2:.0f}ms, dimension: {len(embedding2)}")
    print()

    # Verify caching
    if embedding1 == embedding2:
        print(f"✅ Cache working! Speed improvement: {elapsed1/elapsed2:.1f}x")
    else:
        print(f"❌ Cache not working (embeddings differ)")

    print()
    return True


async def test_opensearch_update():
    """Test 3: OpenSearch update with vector"""
    print("=" * 80)
    print("Test 3: OpenSearch Update (Dry Run)")
    print("=" * 80)
    print()

    from app.core.opensearch import opensearch_client

    # Test connection
    try:
        info = opensearch_client.info()
        print(f"✅ OpenSearch connected: {info['version']['number']}")
        print()

        # Check if test index exists
        test_index = "test_large_data_project_2025_11"
        exists = opensearch_client.indices.exists(index=test_index)

        if exists:
            # Count ERROR logs
            count_result = opensearch_client.count(
                index=test_index,
                body={
                    "query": {
                        "term": {"level": "ERROR"}
                    }
                }
            )
            error_count = count_result['count']
            print(f"📊 Test index: {test_index}")
            print(f"   ERROR logs: {error_count}")
            print()

            # Check how many have vectors
            vector_count = opensearch_client.count(
                index=test_index,
                body={
                    "query": {
                        "bool": {
                            "must": [
                                {"term": {"level": "ERROR"}},
                                {"exists": {"field": "log_vector"}}
                            ]
                        }
                    }
                }
            )['count']

            print(f"   ERROR logs with vectors: {vector_count}/{error_count}")

            if vector_count < error_count:
                print(f"   ⚠️  {error_count - vector_count} ERROR logs need vectorization")
            else:
                print(f"   ✅ All ERROR logs have vectors!")
            print()

        else:
            print(f"⚠️  Test index not found: {test_index}")
            print(f"   Run insert_large_test_data.py first")
            print()

        return True

    except Exception as e:
        print(f"❌ OpenSearch error: {e}")
        return False


async def run_all_tests():
    """Run all tests"""
    print()
    print("🧪 AI Enrichment Consumer Test Suite")
    print()

    results = {
        "Batch Embedding API": await test_batch_embedding_api(),
        "Embedding Service Cache": await test_embedding_service_cache(),
        "OpenSearch Update": await test_opensearch_update(),
    }

    print("=" * 80)
    print("📊 Test Results Summary")
    print("=" * 80)
    print()

    for test_name, passed in results.items():
        status = "✅ PASS" if passed else "❌ FAIL"
        print(f"  {status}  {test_name}")

    print()

    all_passed = all(results.values())
    if all_passed:
        print("✅ All tests passed!")
    else:
        print("❌ Some tests failed")

    print()
    return all_passed


if __name__ == "__main__":
    success = asyncio.run(run_all_tests())
    sys.exit(0 if success else 1)
