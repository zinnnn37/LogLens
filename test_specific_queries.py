#!/usr/bin/env python3
"""
Test specific problem queries after fixes
"""
import httpx
import asyncio
import json
from datetime import datetime

BASE_URL = "http://localhost:8000"
API_ENDPOINT = "/api/v2/chatbot/ask"
PROJECT_UUID = "9f8c4c75-a936-3ab6-92a5-d1309cd9f87e"
TIMEOUT_SECONDS = 120

# Problem queries to test
PROBLEM_QUERIES = [
    # Iteration limit issues
    {"id": 39, "query": "4xx vs 5xx 에러 비율 분석", "issue": "iteration_limit"},
    {"id": 43, "query": "API별 에러율 비교", "issue": "iteration_limit"},
    {"id": 90, "query": "주기적으로 재발하는 에러 패턴 탐지", "issue": "iteration_limit"},
    # Text field aggregation issues
    {"id": 70, "query": "IP 192.168.1.100의 세션 활동 추적", "issue": "text_field"},
    {"id": 76, "query": "의심스러운 IP 활동 패턴 분석", "issue": "text_field"},
]


async def test_query(client: httpx.AsyncClient, query_info: dict):
    """Test a single query"""
    payload = {
        "question": query_info["query"],
        "project_uuid": PROJECT_UUID
    }

    print(f"\n[Query #{query_info['id']}] {query_info['issue']}")
    print(f"Query: {query_info['query']}")

    try:
        start_time = asyncio.get_event_loop().time()
        response = await client.post(
            f"{BASE_URL}{API_ENDPOINT}",
            json=payload,
            timeout=TIMEOUT_SECONDS
        )
        elapsed = asyncio.get_event_loop().time() - start_time

        if response.status_code == 200:
            data = response.json()
            answer = data.get("answer", "")

            # Check for specific issues
            has_iteration_limit = "agent stopped due to iteration limit" in answer.lower()
            has_module_error = "no module named" in answer.lower()
            has_text_field_error = "텍스트 필드가 집계" in answer or "text field" in answer.lower()

            if has_iteration_limit:
                print(f"❌ ITERATION LIMIT ({elapsed:.2f}s)")
            elif has_module_error:
                print(f"❌ MODULE ERROR ({elapsed:.2f}s)")
            elif has_text_field_error:
                print(f"❌ TEXT FIELD ERROR ({elapsed:.2f}s)")
            else:
                print(f"✅ SUCCESS ({elapsed:.2f}s)")

            print(f"Answer preview: {answer[:150]}...")

            return {
                "id": query_info["id"],
                "issue": query_info["issue"],
                "success": not (has_iteration_limit or has_module_error or has_text_field_error),
                "has_iteration_limit": has_iteration_limit,
                "has_text_field_error": has_text_field_error,
                "time": elapsed,
                "answer": answer[:500]
            }
        else:
            print(f"❌ HTTP ERROR: {response.status_code}")
            return {"id": query_info["id"], "issue": query_info["issue"], "success": False, "error": str(response.status_code)}

    except Exception as e:
        print(f"❌ EXCEPTION: {str(e)}")
        return {"id": query_info["id"], "issue": query_info["issue"], "success": False, "error": str(e)}


async def main():
    print("=" * 60)
    print("Testing Problem Queries After Fixes")
    print(f"Time: {datetime.now().isoformat()}")
    print("=" * 60)

    results = []

    async with httpx.AsyncClient() as client:
        for query_info in PROBLEM_QUERIES:
            result = await test_query(client, query_info)
            results.append(result)
            await asyncio.sleep(2)  # Give agent time to reset

    # Summary
    print("\n" + "=" * 60)
    print("SUMMARY")
    print("=" * 60)

    iteration_limit_fixed = sum(1 for r in results if r["issue"] == "iteration_limit" and r.get("success", False))
    text_field_fixed = sum(1 for r in results if r["issue"] == "text_field" and r.get("success", False))

    print(f"Iteration Limit Issues: {iteration_limit_fixed}/3 fixed")
    print(f"Text Field Issues: {text_field_fixed}/2 fixed")
    print(f"Total: {sum(1 for r in results if r.get('success', False))}/5 fixed")

    # Save results
    filename = f"problem_queries_test_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
    with open(filename, "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2, ensure_ascii=False)
    print(f"\nResults saved to: {filename}")


if __name__ == "__main__":
    asyncio.run(main())
