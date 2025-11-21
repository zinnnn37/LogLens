"""
Vector AI vs DB 실험 테스트 스크립트

실제 데이터로 Vector KNN 검색 + LLM 추론이 OpenSearch 집계를 대체할 수 있는지 테스트
"""

import asyncio
import sys
import os
import httpx
from datetime import datetime

# Python path 설정
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

# 환경 변수 로드
from dotenv import load_dotenv
load_dotenv()

# API 엔드포인트
BASE_URL = "http://localhost:8000"
EXPERIMENT_URL = f"{BASE_URL}/api/v2-langgraph/experiments/vector-vs-db"


async def test_vector_experiment():
    """Vector AI vs DB 실험 테스트"""

    print("=" * 80)
    print("🧪 Vector AI vs OpenSearch 집계 비교 실험")
    print("=" * 80)
    print()

    # 테스트할 프로젝트 UUID (실제 데이터가 있는 프로젝트로 변경 필요)
    test_project_uuid = "3a73c7d4-8176-3929-b72f-d5b921daae67"

    # 실험 파라미터
    time_hours = 24
    k_values = "100,500,1000"  # 3개의 k 값으로 테스트

    print(f"📋 실험 설정:")
    print(f"  - 프로젝트 UUID: {test_project_uuid}")
    print(f"  - 분석 기간: {time_hours}시간")
    print(f"  - k 값: {k_values}")
    print()

    # API 호출
    print("🚀 실험 시작...")
    print()

    params = {
        "project_uuid": test_project_uuid,
        "time_hours": time_hours,
        "k_values": k_values
    }

    try:
        async with httpx.AsyncClient(timeout=300.0) as client:  # 5분 타임아웃
            response = await client.get(EXPERIMENT_URL, params=params)

            if response.status_code != 200:
                print(f"❌ API 호출 실패: {response.status_code}")
                print(f"응답: {response.text}")
                return

            result = response.json()

            # 결과 출력
            print("✅ 실험 완료!")
            print()

            # Ground Truth (OpenSearch 집계)
            print("=" * 80)
            print("📊 Ground Truth (OpenSearch 집계)")
            print("=" * 80)
            gt = result["ground_truth"]
            print(f"총 로그 수: {gt['total_logs']:,}개")
            print(f"ERROR: {gt['error_count']:,}개")
            print(f"WARN: {gt['warn_count']:,}개")
            print(f"INFO: {gt['info_count']:,}개")
            print(f"에러율: {gt['error_rate']:.2f}%")
            print(f"처리 시간: {gt['processing_time_ms']:.2f}ms")
            print()

            # Vector AI 실험 결과
            print("=" * 80)
            print("🤖 Vector AI 실험 결과")
            print("=" * 80)

            for exp in result["vector_ai_experiments"]:
                k = exp["k"]
                ai_result = exp["ai_result"]
                accuracy = exp["accuracy_metrics"]
                sample_dist = exp["sample_distribution"]

                print(f"\n📌 k = {k}")
                print("-" * 80)

                # AI 추론 결과
                print(f"[AI 추론]")
                print(f"  추정 총 로그: {ai_result['estimated_total_logs']:,}개 (실제: {gt['total_logs']:,}개)")
                print(f"  추정 ERROR: {ai_result['estimated_error_count']:,}개 (실제: {gt['error_count']:,}개)")
                print(f"  추정 WARN: {ai_result['estimated_warn_count']:,}개 (실제: {gt['warn_count']:,}개)")
                print(f"  추정 INFO: {ai_result['estimated_info_count']:,}개 (실제: {gt['info_count']:,}개)")
                print(f"  추정 에러율: {ai_result['estimated_error_rate']:.2f}% (실제: {gt['error_rate']:.2f}%)")
                print(f"  AI 신뢰도: {ai_result['confidence_score']}/100")
                print(f"  처리 시간: {ai_result['processing_time_ms']:.2f}ms")
                print()

                # 샘플 분포
                print(f"[샘플 분포 (총 {k}개)]")
                for level, count in sample_dist.items():
                    print(f"  {level}: {count}개 ({count/k*100:.1f}%)")
                print()

                # 정확도 지표
                print(f"[정확도 지표]")
                print(f"  총 로그 수 정확도: {accuracy['total_logs_accuracy']:.2f}%")
                print(f"  ERROR 수 정확도: {accuracy['error_count_accuracy']:.2f}%")
                print(f"  WARN 수 정확도: {accuracy['warn_count_accuracy']:.2f}%")
                print(f"  INFO 수 정확도: {accuracy['info_count_accuracy']:.2f}%")
                print(f"  에러율 정확도: {accuracy['error_rate_accuracy']:.2f}%")
                print(f"  ⭐ 종합 정확도: {accuracy['overall_accuracy']:.2f}%")
                print()

                # AI 추론 근거
                print(f"[AI 추론 근거]")
                print(f"  {ai_result['reasoning']}")

            # 실험 결론
            print()
            print("=" * 80)
            print("🎯 실험 결론")
            print("=" * 80)

            conclusion = result["conclusion"]
            print(f"최적 k 값: {conclusion['best_k']}")
            print(f"최고 정확도: {conclusion['best_accuracy']:.2f}%")
            print(f"등급: {conclusion['grade']}")
            print(f"DB 대체 가능 여부: {'✅ 가능' if conclusion['feasible'] else '❌ 불가능'}")
            print()
            print(f"권장 사항:")
            print(f"  {conclusion['recommendation']}")
            print()

            # 성능 비교
            print("=" * 80)
            print("⚡ 성능 비교 (DB vs Vector AI)")
            print("=" * 80)
            perf = conclusion["performance_vs_db"]
            print(f"DB 처리 시간: {perf['db_processing_time_ms']:.2f}ms")
            print(f"Vector AI 처리 시간 (최적 k={conclusion['best_k']}): {perf['best_ai_processing_time_ms']:.2f}ms")
            print(f"속도 비율: {perf['speed_ratio']:.2f}x")
            print(f"정확도 트레이드오프: {perf['accuracy_trade_off']:.2f}%")
            print(f"평가: {perf['verdict']}")
            print()

            # 주요 인사이트
            print("=" * 80)
            print("💡 주요 인사이트")
            print("=" * 80)
            for i, insight in enumerate(result["insights"], 1):
                print(f"{i}. {insight}")
            print()

            # 최종 요약
            print("=" * 80)
            print("📝 최종 요약")
            print("=" * 80)

            if conclusion["feasible"]:
                print("✅ Vector AI가 OpenSearch 집계를 대체할 수 있는 것으로 판명되었습니다!")
                print()
                print("시사점:")
                print("  - 복잡한 집계 쿼리를 Vector 검색 + LLM으로 대체 가능")
                print("  - DB 부하 감소 (전체 스캔 대신 k개만 조회)")
                print("  - 유연한 통계 추론 (새로운 지표도 LLM이 자동 계산)")
                print("  - 자연어 질의 지원 가능 (예: '오늘 에러가 어제보다 많아?')")
            else:
                print("⚠️ Vector AI가 아직 OpenSearch 집계를 완전히 대체하기는 어렵습니다.")
                print()
                print("원인 분석:")
                if conclusion["best_accuracy"] < 80:
                    print("  - 샘플링 정확도 부족 (k 값 증가 또는 층화 샘플링 개선 필요)")
                    print("  - LLM 추론 정확도 부족 (프롬프트 튜닝 필요)")
                else:
                    print("  - 정확도는 양호하지만 90% 기준 미달")
                    print("  - 추가 튜닝으로 개선 가능성 있음")

            print()
            print("=" * 80)

    except httpx.TimeoutException:
        print("❌ API 호출 타임아웃 (5분 초과)")
    except httpx.RequestError as e:
        print(f"❌ API 호출 오류: {e}")
    except Exception as e:
        print(f"❌ 예외 발생: {e}")
        import traceback
        traceback.print_exc()


async def check_server():
    """서버 상태 확인"""
    print("🔍 서버 상태 확인 중...")

    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.get(f"{BASE_URL}/")
            if response.status_code == 200:
                data = response.json()
                print(f"✅ 서버 접속 성공: {data['app']} v{data['version']}")
                return True
            else:
                print(f"❌ 서버 응답 오류: {response.status_code}")
                return False
    except Exception as e:
        print(f"❌ 서버 접속 실패: {e}")
        print()
        print("서버를 시작하려면:")
        print("  cd /mnt/c/SSAFY/third_project/AI/S13P31A306")
        print("  source venv/bin/activate")
        print("  uvicorn app.main:app --reload")
        return False


async def main():
    """메인 함수"""
    # 서버 상태 확인
    if not await check_server():
        return

    print()

    # 실험 실행
    await test_vector_experiment()


if __name__ == "__main__":
    asyncio.run(main())
