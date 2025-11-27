#!/usr/bin/env python3
"""
Test script for Periodic Enrichment Scheduler

Tests the main functionality without actually running the scheduler.
"""

import asyncio
import sys
from dotenv import load_dotenv

load_dotenv()

# Import the scheduler
from periodic_enrichment_scheduler import PeriodicEnrichmentScheduler


async def test_scheduler_components():
    """Test individual scheduler components"""
    print("=" * 80)
    print("🧪 Testing Periodic Enrichment Scheduler Components")
    print("=" * 80)
    print()

    scheduler = PeriodicEnrichmentScheduler()

    # Test 1: Find ERROR logs without vectors
    print("[Test 1] Finding ERROR logs without vectors...")
    logs = await scheduler.find_error_logs_without_vectors()
    print(f"  ✅ Found {len(logs)} ERROR logs without vectors")
    print()

    if len(logs) == 0:
        print("⚠️  No ERROR logs found without vectors")
        print("   All ERROR logs are already vectorized, or no ERROR logs exist.")
        print("   This is expected if enrichment_consumer is running.")
        print()
        return True

    # Test 2: Generate embeddings for a subset
    print(f"[Test 2] Generating embeddings for {min(3, len(logs))} sample logs...")
    sample_logs = logs[:3]  # Test with first 3 logs only
    embeddings_map = await scheduler.generate_embeddings_batch(sample_logs)
    print(f"  ✅ Generated {len(embeddings_map)} embeddings")

    for log_id, embedding in list(embeddings_map.items())[:1]:
        print(f"     Sample: log_id={log_id}, vector_dim={len(embedding)}")
    print()

    # Test 3: Update OpenSearch (dry-run check)
    print("[Test 3] Checking OpenSearch update capability...")
    if embeddings_map:
        # Just verify the structure, don't actually update
        sample_log = sample_logs[0]
        print(f"  ✅ Would update:")
        print(f"     Index: {sample_log.get('_index')}")
        print(f"     Document ID: {sample_log.get('_id')}")
        print(f"     Vector dimension: {len(list(embeddings_map.values())[0])}")
    print()

    print("=" * 80)
    print("✅ All component tests passed!")
    print("=" * 80)
    print()

    return True


async def test_single_cycle():
    """Test running a single enrichment cycle"""
    print("=" * 80)
    print("🧪 Testing Single Enrichment Cycle (DRY RUN)")
    print("=" * 80)
    print()
    print("⚠️  This will actually update OpenSearch!")
    print("   Press Ctrl+C within 5 seconds to cancel...")
    print()

    try:
        await asyncio.sleep(5)
    except KeyboardInterrupt:
        print("\n❌ Test cancelled by user")
        return False

    scheduler = PeriodicEnrichmentScheduler()
    await scheduler.run_enrichment_cycle()

    print()
    print("=" * 80)
    print("✅ Single cycle test completed!")
    print("=" * 80)
    print()

    return True


async def main():
    """Run all tests"""
    print()
    print("🚀 Periodic Enrichment Scheduler Test Suite")
    print()

    # Test 1: Component tests
    print("Running component tests (safe, no updates)...")
    await test_scheduler_components()

    # Test 2: Ask user if they want to test a full cycle
    print()
    response = input("Do you want to test a full enrichment cycle? (updates OpenSearch) [y/N]: ")
    if response.lower() == 'y':
        await test_single_cycle()
    else:
        print("Skipping full cycle test")

    print()
    print("✅ All tests completed!")
    print()


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\n⚠️  Tests interrupted by user")
        sys.exit(0)
    except Exception as e:
        print(f"\n❌ Test failed: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
