#!/usr/bin/env python3
"""
Test new queries for chatbot v2 API
"""
import httpx
import asyncio
import json
from datetime import datetime

BASE_URL = "http://localhost:8000"
API_ENDPOINT = "/api/v2/chatbot/ask"
PROJECT_UUID = "9f8c4c75-a936-3ab6-92a5-d1309cd9f87e"
TIMEOUT_SECONDS = 120

# New queries to test
NEW_QUERIES = [
    # Error Analysis
    {"id": 1, "query": "최근 발생한 에러를 알려줘", "category": "error_analysis"},
    {"id": 2, "query": "가장 심각한 에러는 무엇인가요?", "category": "error_analysis"},
    {"id": 3, "query": "NullPointerException 에러 분석해줘", "category": "error_analysis"},
    {"id": 4, "query": "서비스별 에러 현황", "category": "error_analysis"},
    {"id": 5, "query": "에러 타입별 분포를 보여줘", "category": "error_analysis"},

    # Performance Analysis
    {"id": 6, "query": "응답 시간이 느린 API를 찾아줘", "category": "performance"},
    {"id": 7, "query": "성능 병목 지점이 어디인가요?", "category": "performance"},
    {"id": 8, "query": "평균 응답 시간 통계", "category": "performance"},
    {"id": 9, "query": "API 성능 순위", "category": "performance"},

    # Monitoring
    {"id": 10, "query": "서비스 헬스 체크 해줘", "category": "monitoring"},
    {"id": 11, "query": "에러율 추이를 분석해줘", "category": "monitoring"},
    {"id": 12, "query": "시간대별 트래픽 패턴", "category": "monitoring"},
    {"id": 13, "query": "시스템 전체 상태 요약", "category": "monitoring"},
    {"id": 14, "query": "비정상적인 패턴이 있나요?", "category": "monitoring"},

    # Search
    {"id": 15, "query": "user-service 로그를 검색해줘", "category": "search"},
    {"id": 16, "query": "지난 1시간 로그 조회", "category": "search"},
    {"id": 17, "query": "ERROR 레벨 로그만 보여줘", "category": "search"},
    {"id": 18, "query": "authentication 키워드로 검색", "category": "search"},

    # Comparison
    {"id": 19, "query": "오늘과 어제 에러율 비교", "category": "comparison"},
    {"id": 20, "query": "서비스별 성능 비교", "category": "comparison"},

    # Complex/Specific
    {"id": 21, "query": "5xx 에러가 가장 많은 API는?", "category": "specific"},
    {"id": 22, "query": "연속으로 실패한 요청 패턴", "category": "specific"},
    {"id": 23, "query": "에러 발생 후 자동 복구된 케이스", "category": "specific"},
    {"id": 24, "query": "트래픽 급증 시간대", "category": "specific"},
    {"id": 25, "query": "가장 자주 발생하는 에러 TOP 5", "category": "specific"},
]


async def test_query(client: httpx.AsyncClient, query_info: dict):
    """Test a single query"""
    payload = {
        "question": query_info["query"],
        "project_uuid": PROJECT_UUID
    }

    print(f"\n[Query #{query_info['id']}] {query_info['category']}")
    print(f"Q: {query_info['query']}")

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

            # Check for issues
            has_iteration_limit = "agent stopped due to iteration limit" in answer.lower()
            has_error = "오류" in answer and "발생" in answer and len(answer) < 100
            has_no_data = "조건을 만족하는 로그가 없습니다" in answer or "로그가 없습니다" in answer
            has_valid_response = "##" in answer or "**" in answer or len(answer) > 100

            status = "✅" if has_valid_response and not has_iteration_limit else "⚠️" if has_no_data else "❌"

            print(f"A: {answer[:200]}..." if len(answer) > 200 else f"A: {answer}")
            print(f"Status: {status} | Time: {elapsed:.2f}s | Length: {len(answer)} chars")

            return {
                "id": query_info["id"],
                "category": query_info["category"],
                "query": query_info["query"],
                "success": has_valid_response and not has_iteration_limit,
                "has_no_data": has_no_data,
                "has_iteration_limit": has_iteration_limit,
                "time": elapsed,
                "answer_length": len(answer),
                "answer_preview": answer[:500]
            }
        else:
            print(f"❌ HTTP ERROR: {response.status_code}")
            return {"id": query_info["id"], "success": False, "error": str(response.status_code)}

    except Exception as e:
        print(f"❌ EXCEPTION: {str(e)}")
        return {"id": query_info["id"], "success": False, "error": str(e)}


async def main():
    print("=" * 70)
    print("Testing New Queries for Chatbot V2 API")
    print(f"Time: {datetime.now().isoformat()}")
    print(f"Total Queries: {len(NEW_QUERIES)}")
    print("=" * 70)

    results = []

    async with httpx.AsyncClient() as client:
        for query_info in NEW_QUERIES:
            result = await test_query(client, query_info)
            results.append(result)
            await asyncio.sleep(1)  # Small delay between requests

    # Summary
    print("\n" + "=" * 70)
    print("SUMMARY")
    print("=" * 70)

    total = len(results)
    successes = sum(1 for r in results if r.get("success", False))
    no_data = sum(1 for r in results if r.get("has_no_data", False))
    iteration_limits = sum(1 for r in results if r.get("has_iteration_limit", False))
    errors = sum(1 for r in results if r.get("error"))

    print(f"Total Queries: {total}")
    print(f"✅ Successful Responses: {successes}/{total} ({100*successes/total:.1f}%)")
    print(f"⚠️ No Data Found: {no_data}/{total}")
    print(f"❌ Iteration Limits: {iteration_limits}/{total}")
    print(f"❌ Errors: {errors}/{total}")

    # By category
    print("\nBy Category:")
    categories = {}
    for r in results:
        cat = r.get("category", "unknown")
        if cat not in categories:
            categories[cat] = {"total": 0, "success": 0}
        categories[cat]["total"] += 1
        if r.get("success"):
            categories[cat]["success"] += 1

    for cat, stats in categories.items():
        print(f"  {cat}: {stats['success']}/{stats['total']} success")

    # Average time
    times = [r.get("time", 0) for r in results if r.get("time")]
    if times:
        print(f"\nAverage Response Time: {sum(times)/len(times):.2f}s")
        print(f"Min: {min(times):.2f}s | Max: {max(times):.2f}s")

    # Save results
    filename = f"new_queries_test_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
    with open(filename, "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2, ensure_ascii=False)
    print(f"\nResults saved to: {filename}")


if __name__ == "__main__":
    asyncio.run(main())
