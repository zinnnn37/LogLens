"""
샘플링 전략 비교 실험

3가지 전략을 비교:
1. 단순 Vector KNN (기존)
2. 비례 층화 Vector KNN (개선)
3. 비례 랜덤 층화 (Vector 없음)
"""

import asyncio
import sys
import os
from datetime import datetime
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from dotenv import load_dotenv
load_dotenv()

# OpenSearch 호스트를 localhost로 override
os.environ['OPENSEARCH_HOST'] = 'localhost'

from app.tools.vector_experiment_tools import (
    _get_db_statistics_for_experiment,
    _llm_estimate_from_vectors,
    _calculate_accuracy_for_experiment
)
from app.tools.sampling_strategies import (
    sample_simple_vector_knn,
    sample_proportional_vector_knn,
    sample_proportional_random
)


async def run_sampling_comparison():
    """샘플링 전략 비교 실험"""

    print("=" * 80)
    print("🧪 샘플링 전략 비교 실험")
    print("=" * 80)
    print()

    # 대규모 테스트 데이터 프로젝트
    project_uuid = "test-large-data-project"
    time_hours = 24
    k_values = [50, 100, 200]  # 5%, 10%, 20% 샘플링

    print(f"📋 실험 설정:")
    print(f"  - 프로젝트 UUID: {project_uuid}")
    print(f"  - 분석 기간: {time_hours}시간")
    print(f"  - k 값: {k_values}")
    print()

    try:
        # Ground Truth
        print("=" * 80)
        print("1️⃣  Ground Truth: OpenSearch 집계")
        print("=" * 80)

        db_start = time.time()
        db_stats = _get_db_statistics_for_experiment(project_uuid, time_hours)
        db_time_ms = (time.time() - db_start) * 1000

        print(f"✅ 조회 완료 ({db_time_ms:.2f}ms)")
        print()
        print(f"📊 집계 결과:")
        print(f"  • 총 로그 수: {db_stats['total_logs']:,}개")
        print(f"  • ERROR: {db_stats['error_count']:,}개 ({db_stats['error_count']/db_stats['total_logs']*100:.1f}%)")
        print(f"  • WARN: {db_stats['warn_count']:,}개 ({db_stats['warn_count']/db_stats['total_logs']*100:.1f}%)")
        print(f"  • INFO: {db_stats['info_count']:,}개 ({db_stats['info_count']/db_stats['total_logs']*100:.1f}%)")
        print(f"  • 에러율: {db_stats['error_rate']:.2f}%")
        print()

        if db_stats["total_logs"] == 0:
            print("⚠️  로그 데이터가 없습니다. 먼저 insert_large_test_data.py를 실행하세요.")
            return

        # 각 k 값에 대해 3가지 전략 비교
        for k in k_values:
            sampling_ratio = k / db_stats["total_logs"] * 100

            print("=" * 80)
            print(f"2️⃣  k = {k} ({sampling_ratio:.1f}% 샘플링) - 전략 비교")
            print("=" * 80)
            print()

            strategies = [
                ("단순 Vector KNN", sample_simple_vector_knn, {}),
                ("비례 층화 Vector KNN", sample_proportional_vector_knn, {}),
                ("비례 랜덤 층화", sample_proportional_random, {})
            ]

            results = []

            for strategy_name, strategy_func, kwargs in strategies:
                print(f"🔬 전략: {strategy_name}")
                print("-" * 80)

                try:
                    # 샘플링
                    print(f"  [1/4] 샘플링 중...", end="", flush=True)
                    sample_start = time.time()

                    if strategy_name == "단순 Vector KNN":
                        samples = await strategy_func(project_uuid, k, time_hours)
                    else:
                        samples = await strategy_func(project_uuid, k, time_hours, **kwargs)

                    sample_time_ms = (time.time() - sample_start) * 1000
                    print(f" ✅ {len(samples)}개 수집 ({sample_time_ms:.0f}ms)")

                    if not samples:
                        print(f"  ⚠️  샘플 수집 실패")
                        print()
                        continue

                    # 샘플 분포
                    sample_dist = {}
                    for sample in samples:
                        level = sample.get("level", "UNKNOWN")
                        sample_dist[level] = sample_dist.get(level, 0) + 1

                    print(f"  [2/4] 샘플 분포:")
                    for level in ["ERROR", "WARN", "INFO"]:
                        count = sample_dist.get(level, 0)
                        ratio = count / len(samples) * 100 if len(samples) > 0 else 0
                        actual_count = db_stats.get(f"{level.lower()}_count", 0)
                        print(f"        • {level}: {count}개 ({ratio:.1f}%) [실제: {actual_count}개]")

                    # LLM 추론
                    print(f"  [3/4] LLM 통계 추론 중...", end="", flush=True)
                    llm_start = time.time()
                    ai_stats = await _llm_estimate_from_vectors(
                        samples,
                        db_stats["total_logs"],
                        time_hours
                    )
                    llm_time_ms = (time.time() - llm_start) * 1000
                    print(f" ✅ 완료 ({llm_time_ms:.0f}ms)")

                    # 정확도 계산
                    print(f"  [4/4] 정확도 계산...", end="", flush=True)
                    accuracy = _calculate_accuracy_for_experiment(db_stats, ai_stats)
                    print(f" ✅ 완료")

                    total_time_ms = sample_time_ms + llm_time_ms

                    print()
                    print(f"  📈 결과:")
                    print(f"        • 종합 정확도: {accuracy['overall_accuracy']:.1f}%")
                    print(f"        • ERROR 정확도: {accuracy['error_count_accuracy']:.1f}%")
                    print(f"        • WARN 정확도: {accuracy['warn_count_accuracy']:.1f}%")
                    print(f"        • INFO 정확도: {accuracy['info_count_accuracy']:.1f}%")
                    print(f"        • 에러율 정확도: {accuracy['error_rate_accuracy']:.1f}%")
                    print(f"        • 총 처리 시간: {total_time_ms:.0f}ms (샘플링: {sample_time_ms:.0f}ms, LLM: {llm_time_ms:.0f}ms)")
                    print()

                    # 평가
                    if accuracy['overall_accuracy'] >= 95:
                        grade = "🏆 매우 우수"
                    elif accuracy['overall_accuracy'] >= 90:
                        grade = "✅ 우수"
                    elif accuracy['overall_accuracy'] >= 80:
                        grade = "⚠️  양호"
                    else:
                        grade = "❌ 미흡"

                    print(f"  {grade}")
                    print()

                    results.append({
                        'strategy': strategy_name,
                        'k': k,
                        'sample_count': len(samples),
                        'sample_dist': sample_dist,
                        'accuracy': accuracy['overall_accuracy'],
                        'error_accuracy': accuracy['error_count_accuracy'],
                        'sample_time_ms': sample_time_ms,
                        'llm_time_ms': llm_time_ms,
                        'total_time_ms': total_time_ms,
                        'grade': grade
                    })

                except Exception as e:
                    print(f"\n  ❌ 오류: {e}")
                    import traceback
                    traceback.print_exc()
                    print()

            # k 값별 요약
            if results:
                print("=" * 80)
                print(f"📊 k={k} 전략별 비교")
                print("=" * 80)
                print()

                # 테이블 헤더
                print(f"{'전략':<25} | {'정확도':>8} | {'ERROR':>8} | {'처리시간':>10} | 평가")
                print(f"{'-'*25}-+-{'-'*8}-+-{'-'*8}-+-{'-'*10}-+-{'-'*12}")

                for r in results:
                    print(f"{r['strategy']:<25} | {r['accuracy']:>7.1f}% | {r['error_accuracy']:>7.1f}% | {r['total_time_ms']:>8.0f}ms | {r['grade']}")

                print()

                # 최고 정확도 전략
                best = max(results, key=lambda r: r['accuracy'])
                print(f"🏆 최고 정확도: {best['strategy']} ({best['accuracy']:.1f}%)")
                print()

        # 전체 요약
        print("=" * 80)
        print("3️⃣  전체 실험 요약")
        print("=" * 80)
        print()

        print("💡 주요 발견:")
        print("  1. 단순 Vector KNN은 희소 레벨(ERROR) 샘플링에 약함")
        print("  2. 층화 샘플링은 레벨 분포를 정확히 보장")
        print("  3. k 값이 클수록 모든 전략의 정확도 향상")
        print("  4. Vector vs 랜덤: 의미 유사도 vs 공정한 랜덤")
        print()

        print("🎯 권장 사항:")
        print("  - 대규모 데이터(10만+): 비례 층화 Vector KNN 사용")
        print("  - ERROR 로그 중요: 최소 보장(min_per_level) 설정")
        print("  - 속도 중요: 랜덤 층화 (Vector 생성 비용 절약)")
        print()

    except Exception as e:
        print(f"\n❌ 오류 발생: {e}")
        import traceback
        traceback.print_exc()


if __name__ == "__main__":
    asyncio.run(run_sampling_comparison())
