"""
샘플링 문제 진단 스크립트

사용법:
python3 diagnose_sampling.py 831776ac-2d47-3e23-83b9-7619972f0cbf
"""

import asyncio
import sys
import os
from datetime import datetime, timedelta

# 환경 변수 설정
os.environ.setdefault("OPENAI_API_KEY", "dummy-key-for-diagnosis")
os.environ.setdefault("OPENSEARCH_HOST", "localhost")

from app.core.opensearch import opensearch_client
from app.tools.sampling_strategies import _get_level_distribution


async def diagnose(project_uuid: str):
    """프로젝트의 로그 상태 진단"""
    print(f"🔍 프로젝트 진단 시작: {project_uuid}\n")

    # 1. 인덱스 패턴 확인
    index_pattern = f"{project_uuid.replace('-', '_')}_*"
    print(f"1️⃣ 인덱스 패턴: {index_pattern}")

    # 2. 최근 24시간 로그 개수 확인
    try:
        end_time = datetime.utcnow()
        start_time = end_time - timedelta(hours=24)

        query_body = {
            "size": 0,
            "query": {
                "range": {
                    "timestamp": {
                        "gte": start_time.isoformat() + "Z",
                        "lte": end_time.isoformat() + "Z"
                    }
                }
            },
            "aggs": {
                "by_level": {
                    "terms": {"field": "level", "size": 10}
                }
            }
        }

        results = await asyncio.to_thread(
            opensearch_client.search,
            index=index_pattern,
            body=query_body
        )

        total = results.get("hits", {}).get("total", {}).get("value", 0)
        print(f"2️⃣ 최근 24시간 로그 총 개수: {total:,}개")

        buckets = results.get("aggregations", {}).get("by_level", {}).get("buckets", [])
        if buckets:
            print("\n   레벨별 분포:")
            for bucket in buckets:
                print(f"   - {bucket['key']}: {bucket['doc_count']:,}개")
        else:
            print("   ⚠️ 레벨별 분포를 가져올 수 없습니다.")

    except Exception as e:
        print(f"   ❌ 오류: {e}")
        return

    # 3. log_vector 필드가 있는 로그 개수 확인
    try:
        query_body = {
            "size": 0,
            "query": {
                "bool": {
                    "must": [
                        {
                            "exists": {"field": "log_vector"}
                        },
                        {
                            "range": {
                                "timestamp": {
                                    "gte": start_time.isoformat() + "Z",
                                    "lte": end_time.isoformat() + "Z"
                                }
                            }
                        }
                    ]
                }
            },
            "aggs": {
                "by_level": {
                    "terms": {"field": "level", "size": 10}
                }
            }
        }

        results = await asyncio.to_thread(
            opensearch_client.search,
            index=index_pattern,
            body=query_body
        )

        total_vectorized = results.get("hits", {}).get("total", {}).get("value", 0)
        print(f"\n3️⃣ log_vector 필드가 있는 로그: {total_vectorized:,}개")

        if total > 0:
            vectorization_rate = (total_vectorized / total) * 100
            print(f"   벡터화율: {vectorization_rate:.1f}%")

        buckets = results.get("aggregations", {}).get("by_level", {}).get("buckets", [])
        if buckets:
            print("\n   벡터화된 로그의 레벨별 분포:")
            for bucket in buckets:
                print(f"   - {bucket['key']}: {bucket['doc_count']:,}개")
        else:
            print("   ⚠️ 벡터화된 로그가 없습니다.")

    except Exception as e:
        print(f"   ❌ 오류: {e}")

    # 4. 진단 결과 요약
    print("\n" + "="*60)
    print("📊 진단 결과 요약")
    print("="*60)

    if total == 0:
        print("❌ 문제: 최근 24시간 동안 로그가 없습니다.")
        print("\n해결 방법:")
        print("1. 로그가 OpenSearch에 수집되고 있는지 확인")
        print("2. 인덱스 패턴이 올바른지 확인")
        print("3. timestamp 필드가 올바른 형식인지 확인")

    elif total_vectorized == 0:
        print("❌ 문제: 로그는 있지만 벡터화되지 않았습니다.")
        print("\n해결 방법:")
        print("1. 벡터화 스케줄러 상태 확인:")
        print("   - 스케줄러가 실행 중인지 확인")
        print("   - periodic_enrichment_scheduler.py 로그 확인")
        print("2. ERROR 로그가 있는지 확인 (스케줄러는 ERROR만 벡터화)")
        print("3. OpenAI API 키가 올바른지 확인")

    elif vectorization_rate < 10:
        print(f"⚠️ 주의: 벡터화율이 낮습니다 ({vectorization_rate:.1f}%)")
        print("\n권장 사항:")
        print("1. 스케줄러가 ERROR 로그를 벡터화하고 있는지 확인")
        print("2. 충분한 시간이 지났는지 확인 (스케줄러는 10초마다 실행)")

    else:
        print(f"✅ 로그 상태 양호: 총 {total:,}개, 벡터화 {total_vectorized:,}개 ({vectorization_rate:.1f}%)")
        print("\nVector 샘플링이 가능한 상태입니다.")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("사용법: python3 diagnose_sampling.py <project_uuid>")
        sys.exit(1)

    project_uuid = sys.argv[1]
    asyncio.run(diagnose(project_uuid))
