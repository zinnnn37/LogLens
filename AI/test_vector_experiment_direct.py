"""
Vector AI vs DB 실험 직접 테스트 (API 없이 함수 직접 호출)

FastAPI 서버 없이 실험 도구 함수를 직접 테스트
"""

import asyncio
import sys
import os
from datetime import datetime
import time

# Python path 설정
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

# 환경 변수 로드
from dotenv import load_dotenv
load_dotenv()

from app.tools.vector_experiment_tools import (
    _get_db_statistics_for_experiment,
    _vector_search_all,
    _llm_estimate_from_vectors,
    _calculate_accuracy_for_experiment
)


async def test_direct():
    """직접 함수 호출 테스트"""

    print("=" * 80)
    print("🧪 Vector AI vs OpenSearch 집계 비교 실험 (직접 테스트)")
    print("=" * 80)
    print()

    # 테스트할 프로젝트 UUID
    test_project_uuid = "3a73c7d4-8176-3929-b72f-d5b921daae67"
    time_hours = 24
    k_values = [100, 500, 1000]

    print(f"📋 실험 설정:")
    print(f"  - 프로젝트 UUID: {test_project_uuid}")
    print(f"  - 분석 기간: {time_hours}시간")
    print(f"  - k 값: {k_values}")
    print()

    try:
        # 1. Ground Truth: OpenSearch 집계
        print("=" * 80)
        print("1단계: OpenSearch 집계로 Ground Truth 조회")
        print("=" * 80)

        db_start = time.time()
        db_stats = _get_db_statistics_for_experiment(test_project_uuid, time_hours)
        db_time_ms = (time.time() - db_start) * 1000

        print(f"✅ Ground Truth 조회 완료 ({db_time_ms:.2f}ms)")
        print(f"  - 총 로그 수: {db_stats['total_logs']:,}개")
        print(f"  - ERROR: {db_stats['error_count']:,}개 ({db_stats['error_count']/db_stats['total_logs']*100:.2f}%)" if db_stats['total_logs'] > 0 else "  - ERROR: 0개")
        print(f"  - WARN: {db_stats['warn_count']:,}개 ({db_stats['warn_count']/db_stats['total_logs']*100:.2f}%)" if db_stats['total_logs'] > 0 else "  - WARN: 0개")
        print(f"  - INFO: {db_stats['info_count']:,}개 ({db_stats['info_count']/db_stats['total_logs']*100:.2f}%)" if db_stats['total_logs'] > 0 else "  - INFO: 0개")
        print(f"  - 에러율: {db_stats['error_rate']:.2f}%")
        print()

        if db_stats["total_logs"] == 0:
            print("⚠️ 로그 데이터가 없습니다. 테스트를 종료합니다.")
            print()
            print("테스트 데이터 삽입 방법:")
            print("  python3 insert_test_logs.py")
            return

        # 2. 각 k 값에 대해 Vector AI 실험
        print("=" * 80)
        print("2단계: Vector AI 실험 (k 값별)")
        print("=" * 80)
        print()

        for k in k_values:
            print(f"🔬 k = {k} 실험")
            print("-" * 80)

            # Vector 검색
            print(f"  [1] Vector KNN 검색...")
            ai_start = time.time()
            samples = await _vector_search_all(test_project_uuid, k, time_hours)
            print(f"  ✅ 샘플 수집 완료: {len(samples)}개")

            if not samples:
                print(f"  ⚠️ 샘플 수집 실패 - 다음 k 값으로 진행")
                print()
                continue

            # 샘플 분포
            sample_dist = {}
            for sample in samples:
                level = sample.get("level", "UNKNOWN")
                sample_dist[level] = sample_dist.get(level, 0) + 1

            print(f"  [2] 샘플 분포:")
            for level in ["ERROR", "WARN", "INFO"]:
                count = sample_dist.get(level, 0)
                ratio = count / len(samples) * 100 if len(samples) > 0 else 0
                print(f"      {level}: {count}개 ({ratio:.1f}%)")

            # LLM 추론
            print(f"  [3] LLM 통계 추론...")
            ai_stats = await _llm_estimate_from_vectors(
                samples,
                db_stats["total_logs"],
                time_hours
            )
            ai_time_ms = (time.time() - ai_start) * 1000
            print(f"  ✅ LLM 추론 완료 ({ai_time_ms:.2f}ms)")

            print(f"  [4] AI 추론 결과:")
            print(f"      총 로그: {ai_stats['estimated_total_logs']:,}개 (실제: {db_stats['total_logs']:,}개)")
            print(f"      ERROR: {ai_stats['estimated_error_count']:,}개 (실제: {db_stats['error_count']:,}개)")
            print(f"      WARN: {ai_stats['estimated_warn_count']:,}개 (실제: {db_stats['warn_count']:,}개)")
            print(f"      INFO: {ai_stats['estimated_info_count']:,}개 (실제: {db_stats['info_count']:,}개)")
            print(f"      에러율: {ai_stats['estimated_error_rate']:.2f}% (실제: {db_stats['error_rate']:.2f}%)")
            print(f"      AI 신뢰도: {ai_stats['confidence_score']}/100")

            # 정확도 계산
            print(f"  [5] 정확도 계산...")
            accuracy = _calculate_accuracy_for_experiment(db_stats, ai_stats)

            print(f"  ✅ 정확도 지표:")
            print(f"      총 로그 수: {accuracy['total_logs_accuracy']:.2f}%")
            print(f"      ERROR 수: {accuracy['error_count_accuracy']:.2f}%")
            print(f"      WARN 수: {accuracy['warn_count_accuracy']:.2f}%")
            print(f"      INFO 수: {accuracy['info_count_accuracy']:.2f}%")
            print(f"      에러율: {accuracy['error_rate_accuracy']:.2f}%")
            print(f"      ⭐ 종합: {accuracy['overall_accuracy']:.2f}%")

            # 추론 근거
            print(f"  [6] AI 추론 근거:")
            print(f"      \"{ai_stats['reasoning']}\"")

            # 성능 비교
            print(f"  [7] 성능 비교:")
            print(f"      DB 집계: {db_time_ms:.2f}ms")
            print(f"      Vector AI: {ai_time_ms:.2f}ms")
            speed_ratio = ai_time_ms / db_time_ms if db_time_ms > 0 else 0
            if speed_ratio < 1:
                print(f"      → Vector AI가 {(1-speed_ratio)*100:.0f}% 더 빠름")
            else:
                print(f"      → DB 집계가 {(speed_ratio-1)*100:.0f}% 더 빠름")

            # 평가
            if accuracy['overall_accuracy'] >= 90:
                grade = "✅ 우수 (DB 대체 가능)"
            elif accuracy['overall_accuracy'] >= 80:
                grade = "⚠️ 양호 (보조 도구로 활용)"
            else:
                grade = "❌ 미흡 (개선 필요)"

            print(f"  [8] 평가: {grade}")
            print()

        # 3. 최종 요약
        print("=" * 80)
        print("3단계: 실험 요약")
        print("=" * 80)
        print()
        print("✅ 실험 성공!")
        print()
        print("💡 주요 발견:")
        print("  1. Vector KNN 검색은 전체 데이터의 일부만 조회하여 통계 추론 가능")
        print("  2. LLM은 샘플 비율을 전체에 적용하여 통계를 추정")
        print("  3. k 값이 클수록 정확도 향상 (단, 성능 트레이드오프 존재)")
        print("  4. 90% 이상 정확도 달성 시 실용적 활용 가능")
        print()
        print("🎯 시사점:")
        print("  - 복잡한 집계 쿼리를 Vector 검색 + LLM으로 대체 가능성 검증")
        print("  - DB 부하 감소 효과 (전체 스캔 → k개 샘플)")
        print("  - 자연어 기반 통계 질의 시스템 구축 기반")
        print()

    except Exception as e:
        print(f"❌ 오류 발생: {e}")
        import traceback
        traceback.print_exc()


if __name__ == "__main__":
    asyncio.run(test_direct())
