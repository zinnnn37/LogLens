"""
Vector AI vs DB 실험 테스트 (localhost OpenSearch 사용)

OpenSearch 호스트를 localhost로 override하여 테스트
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

# OpenSearch 호스트를 localhost로 override
os.environ['OPENSEARCH_HOST'] = 'localhost'

from app.tools.vector_experiment_tools import (
    _get_db_statistics_for_experiment,
    _vector_search_all,
    _llm_estimate_from_vectors,
    _calculate_accuracy_for_experiment
)


async def test_experiment():
    """실험 실행"""

    print("=" * 80)
    print("🧪 Vector AI vs OpenSearch 집계 비교 실험")
    print("=" * 80)
    print()

    # 실제 존재하는 프로젝트 UUID 사용
    test_project_uuid = "9f8c4c75-a936-3ab6-92a5-d1309cd9f87e"
    time_hours = 24  # 최근 24시간 (방금 삽입한 로그)
    k_values = [5, 8]  # 작은 데이터셋에 맞게 조정 (총 8개 벡터 로그)

    print(f"📋 실험 설정:")
    print(f"  - 프로젝트 UUID: {test_project_uuid}")
    print(f"  - 분석 기간: {time_hours}시간")
    print(f"  - k 값: {k_values}")
    print()

    try:
        # 1. Ground Truth
        print("=" * 80)
        print("1️⃣  Ground Truth: OpenSearch 집계")
        print("=" * 80)

        db_start = time.time()
        db_stats = _get_db_statistics_for_experiment(test_project_uuid, time_hours)
        db_time_ms = (time.time() - db_start) * 1000

        print(f"✅ 조회 완료 ({db_time_ms:.2f}ms)")
        print()
        print(f"📊 집계 결과:")
        print(f"  • 총 로그 수: {db_stats['total_logs']:,}개")
        print(f"  • ERROR: {db_stats['error_count']:,}개 ({db_stats['error_count']/db_stats['total_logs']*100:.1f}%)" if db_stats['total_logs'] > 0 else "  • ERROR: 0개")
        print(f"  • WARN: {db_stats['warn_count']:,}개 ({db_stats['warn_count']/db_stats['total_logs']*100:.1f}%)" if db_stats['total_logs'] > 0 else "  • WARN: 0개")
        print(f"  • INFO: {db_stats['info_count']:,}개 ({db_stats['info_count']/db_stats['total_logs']*100:.1f}%)" if db_stats['total_logs'] > 0 else "  • INFO: 0개")
        print(f"  • 에러율: {db_stats['error_rate']:.2f}%")
        print()

        if db_stats["total_logs"] == 0:
            print("⚠️  로그 데이터가 없습니다. 테스트를 종료합니다.")
            return

        # 2. Vector AI 실험
        print("=" * 80)
        print("2️⃣  Vector AI 실험 (k 값별)")
        print("=" * 80)
        print()

        results = []

        for i, k in enumerate(k_values, 1):
            print(f"🔬 실험 {i}/{len(k_values)}: k = {k}")
            print("-" * 80)

            # Vector 검색
            print(f"  [1/5] Vector KNN 검색 중...", end="", flush=True)
            ai_start = time.time()
            samples = await _vector_search_all(test_project_uuid, k, time_hours)
            print(f" ✅ {len(samples)}개 수집")

            if not samples:
                print(f"  ⚠️  샘플 수집 실패 - 다음 k로 진행")
                print()
                continue

            # 샘플 분포
            sample_dist = {}
            for sample in samples:
                level = sample.get("level", "UNKNOWN")
                sample_dist[level] = sample_dist.get(level, 0) + 1

            print(f"  [2/5] 샘플 분포:")
            for level in ["ERROR", "WARN", "INFO"]:
                count = sample_dist.get(level, 0)
                ratio = count / len(samples) * 100 if len(samples) > 0 else 0
                print(f"        • {level}: {count}개 ({ratio:.1f}%)")

            # LLM 추론
            print(f"  [3/5] LLM 통계 추론 중...", end="", flush=True)
            ai_stats = await _llm_estimate_from_vectors(
                samples,
                db_stats["total_logs"],
                time_hours
            )
            ai_time_ms = (time.time() - ai_start) * 1000
            print(f" ✅ 완료 ({ai_time_ms:.2f}ms)")

            print(f"  [4/5] AI 추론 결과:")
            print(f"        • 추정 총 로그: {ai_stats['estimated_total_logs']:,}개")
            print(f"        • 추정 ERROR: {ai_stats['estimated_error_count']:,}개 (실제: {db_stats['error_count']:,}개)")
            print(f"        • 추정 WARN: {ai_stats['estimated_warn_count']:,}개 (실제: {db_stats['warn_count']:,}개)")
            print(f"        • 추정 INFO: {ai_stats['estimated_info_count']:,}개 (실제: {db_stats['info_count']:,}개)")
            print(f"        • 추정 에러율: {ai_stats['estimated_error_rate']:.2f}% (실제: {db_stats['error_rate']:.2f}%)")
            print(f"        • AI 신뢰도: {ai_stats['confidence_score']}/100")

            # 정확도 계산
            print(f"  [5/5] 정확도 계산 중...", end="", flush=True)
            accuracy = _calculate_accuracy_for_experiment(db_stats, ai_stats)
            print(f" ✅ 완료")

            print()
            print(f"  📈 정확도 지표:")
            print(f"        • 총 로그 수: {accuracy['total_logs_accuracy']:.1f}%")
            print(f"        • ERROR 수: {accuracy['error_count_accuracy']:.1f}%")
            print(f"        • WARN 수: {accuracy['warn_count_accuracy']:.1f}%")
            print(f"        • INFO 수: {accuracy['info_count_accuracy']:.1f}%")
            print(f"        • 에러율: {accuracy['error_rate_accuracy']:.1f}%")
            print(f"        • ⭐ 종합 정확도: {accuracy['overall_accuracy']:.1f}%")
            print()

            # 성능 비교
            speed_ratio = ai_time_ms / db_time_ms if db_time_ms > 0 else 0
            print(f"  ⚡ 성능:")
            print(f"        • DB 집계: {db_time_ms:.0f}ms")
            print(f"        • Vector AI: {ai_time_ms:.0f}ms ({speed_ratio:.1f}x)")
            print()

            # 평가
            if accuracy['overall_accuracy'] >= 95:
                grade_emoji = "🏆"
                grade = "매우 우수 (DB 대체 가능)"
            elif accuracy['overall_accuracy'] >= 90:
                grade_emoji = "✅"
                grade = "우수 (DB 대체 가능)"
            elif accuracy['overall_accuracy'] >= 80:
                grade_emoji = "⚠️"
                grade = "양호 (보조 도구 활용)"
            else:
                grade_emoji = "❌"
                grade = "미흡 (개선 필요)"

            print(f"  {grade_emoji} 평가: {grade}")
            print()

            results.append({
                'k': k,
                'accuracy': accuracy['overall_accuracy'],
                'ai_time_ms': ai_time_ms,
                'grade': grade
            })

        # 3. 최종 요약
        if results:
            print("=" * 80)
            print("3️⃣  실험 요약")
            print("=" * 80)
            print()

            best = max(results, key=lambda r: r['accuracy'])

            print(f"🎯 최적 결과:")
            print(f"  • 최고 k 값: {best['k']}")
            print(f"  • 최고 정확도: {best['accuracy']:.1f}%")
            print(f"  • 등급: {best['grade']}")
            print()

            print(f"📊 k 값별 비교:")
            print(f"  {'k':>6} | {'정확도':>8} | {'처리시간':>10}")
            print(f"  {'-'*6}-+-{'-'*8}-+-{'-'*10}")
            for r in results:
                print(f"  {r['k']:>6} | {r['accuracy']:>7.1f}% | {r['ai_time_ms']:>8.0f}ms")
            print()

            print("💡 주요 발견:")
            print(f"  • Vector 검색으로 전체 데이터의 일부만 조회하여 통계 추론")
            print(f"  • k={best['k']}에서 최고 정확도 {best['accuracy']:.1f}% 달성")
            if best['accuracy'] >= 90:
                print(f"  • ✅ Vector AI가 DB 집계를 대체할 수 있는 수준")
            else:
                print(f"  • ⚠️ 90% 기준 미달 - k 값 증가 또는 프롬프트 튜닝 필요")
            print()

            print("🎯 시사점:")
            if best['accuracy'] >= 90:
                print("  ✅ 성공: Vector AI로 복잡한 집계 쿼리 대체 가능!")
                print("     - DB 부하 감소 (전체 스캔 → k개 샘플)")
                print("     - 자연어 통계 질의 지원 가능")
                print("     - AI 기반 대시보드 구축 기반 마련")
            else:
                print("  ⚠️ 부분 성공: 보조 도구로 활용 가능")
                print("     - k 값 증가 (500, 1000, 5000 테스트)")
                print("     - 층화 샘플링 개선")
                print("     - 프롬프트 엔지니어링 필요")

        else:
            print("❌ 모든 k 값에서 샘플 수집 실패")

        print()
        print("=" * 80)

    except Exception as e:
        print(f"\n❌ 오류 발생: {e}")
        import traceback
        traceback.print_exc()


if __name__ == "__main__":
    asyncio.run(test_experiment())
