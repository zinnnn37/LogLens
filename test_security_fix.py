#!/usr/bin/env python3
"""
Test security fixes for XSS and SQL Injection reflection
"""
import httpx
import asyncio

BASE_URL = "http://localhost:8000"
API_ENDPOINT = "/api/v2/chatbot/ask"
PROJECT_UUID = "9f8c4c75-a936-3ab6-92a5-d1309cd9f87e"


async def test_security_fix():
    """Test that XSS and SQL injection strings are sanitized"""

    tests = [
        {
            "name": "XSS Test",
            "query": "<script>alert('XSS')</script> 에러 분석해줘",
            "bad_pattern": "<script>",
            "good_pattern": "&lt;script&gt;",
        },
        {
            "name": "SQL Injection Test",
            "query": "'; DROP TABLE logs; -- 키워드로 검색해줘",
            "bad_pattern": "'; DROP TABLE",
            "good_pattern": "&#x27;",  # Escaped single quote
        },
    ]

    print("=" * 60)
    print("SECURITY FIX VERIFICATION TEST")
    print("=" * 60)

    async with httpx.AsyncClient() as client:
        for test in tests:
            print(f"\n[{test['name']}]")
            print(f"Query: {test['query'][:60]}...")

            payload = {
                "question": test["query"],
                "project_uuid": PROJECT_UUID
            }

            try:
                response = await client.post(
                    f"{BASE_URL}{API_ENDPOINT}",
                    json=payload,
                    timeout=120
                )

                if response.status_code == 200:
                    answer = response.json().get("answer", "")

                    # Check for bad pattern (should NOT appear)
                    has_bad = test["bad_pattern"] in answer
                    # Check for good pattern (should appear if sanitized)
                    has_good = test["good_pattern"] in answer

                    if has_bad:
                        print(f"  ❌ FAIL: Found unsanitized pattern '{test['bad_pattern']}'")
                        print(f"  Response preview: {answer[:200]}...")
                    elif has_good:
                        print(f"  ✅ PASS: Input properly sanitized (found '{test['good_pattern']}')")
                    else:
                        print(f"  ⚠️ WARN: Pattern not found in response (may be filtered differently)")
                        print(f"  Response preview: {answer[:200]}...")

                    # Show partial response
                    print(f"  Response length: {len(answer)} chars")
                else:
                    print(f"  ❌ HTTP Error: {response.status_code}")

            except Exception as e:
                print(f"  ❌ Exception: {str(e)}")

    print("\n" + "=" * 60)
    print("TEST COMPLETE")
    print("=" * 60)


if __name__ == "__main__":
    asyncio.run(test_security_fix())
