"""
ERROR 패턴 분석 실험 테스트 (localhost OpenSearch 사용)

Vector AI ERROR 패턴 클러스터링 vs OpenSearch 집계 비교
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
    _get_db_error_statistics,
    _vector_search_error_patterns,
    _llm_analyze_error_patterns,
    _calculate_cluster_accuracy
)


async def test_error_pattern_experiment():
    """ERROR 패턴 분석 실험 실행"""

    print("=" * 80)
    print("🧪 Vector AI ERROR 패턴 클러스터링 vs OpenSearch 집계 비교 실험")
    print("=" * 80)
    print()

    # 실제 존재하는 프로젝트 UUID 사용
    test_project_uuid = "test-large-data-project"
    time_hours = 24  # 최근 24시간
    k_values = [10, 20, 30, 40]  # ERROR 샘플 크기 (현재 ERROR 50개)

    print(f"📋 실험 설정:")
    print(f"  - 프로젝트 UUID: {test_project_uuid}")
    print(f"  - 분석 기간: {time_hours}시간")
    print(f"  - k 값 (ERROR 샘플 크기): {k_values}")
    print()

    print("ℹ️  실험 목적:")
    print("  Vector AI가 ERROR 로그 샘플에서 패턴을 파악하고 클러스터링한 결과가")
    print("  OpenSearch 집계(SQL GROUP BY 유사)와 얼마나 일치하는지 검증")
    print()

    try:
        # 1. Ground Truth
        print("=" * 80)
        print("1️⃣  Ground Truth: OpenSearch ERROR 통계 집계")
        print("=" * 80)

        db_start = time.time()
        db_stats = _get_db_error_statistics(test_project_uuid, time_hours)
        db_time_ms = (time.time() - db_start) * 1000

        print(f"✅ 조회 완료 ({db_time_ms:.2f}ms)")
        print()
        print(f"📊 ERROR 통계:")
        print(f"  • 총 ERROR 로그: {db_stats['total_errors']:,}개")
        print()
        print(f"  • 상위 에러 타입:")

        if db_stats['top_errors']:
            for i, err in enumerate(db_stats['top_errors'][:5], 1):
                print(f"    {i}. {err['pattern']}: {err['count']:,}개 ({err['percentage']:.1f}%)")
        else:
            print("    (에러 데이터 없음)")
        print()

        if db_stats["total_errors"] == 0:
            print("⚠️  ERROR 로그가 없습니다. 테스트를 종료합니다.")
            return

        # 2. Vector AI 실험
        print("=" * 80)
        print("2️⃣  Vector AI ERROR 패턴 분석 (k 값별)")
        print("=" * 80)
        print()

        results = []

        for i, k in enumerate(k_values, 1):
            print(f"🔬 실험 {i}/{len(k_values)}: k = {k}")
            print("-" * 80)

            # Vector 검색 (ERROR만)
            print(f"  [1/4] Vector KNN 검색 (ERROR만)...", end="", flush=True)
            ai_start = time.time()
            samples = await _vector_search_error_patterns(test_project_uuid, k, time_hours)
            print(f" ✅ {len(samples)}개 수집")

            if not samples:
                print(f"  ⚠️  ERROR 샘플 수집 실패 - 다음 k로 진행")
                print()
                continue

            # 샘플 분포 확인
            sample_error_types = {}
            for sample in samples:
                from app.tools.vector_experiment_tools import _extract_error_type
                error_type = _extract_error_type(sample.get("message", ""))
                sample_error_types[error_type] = sample_error_types.get(error_type, 0) + 1

            print(f"  [2/4] 샘플 에러 타입 분포:")
            for error_type, count in sorted(sample_error_types.items(), key=lambda x: x[1], reverse=True)[:5]:
                ratio = count / len(samples) * 100 if len(samples) > 0 else 0
                print(f"        • {error_type}: {count}개 ({ratio:.1f}%)")

            # LLM 패턴 분석
            print(f"  [3/4] LLM ERROR 패턴 클러스터링...", end="", flush=True)
            ai_result = await _llm_analyze_error_patterns(
                samples,
                db_stats["total_errors"],
                time_hours
            )
            ai_time_ms = (time.time() - ai_start) * 1000
            print(f" ✅ 완료 ({ai_time_ms:.2f}ms)")

            print(f"  [4/4] AI 클러스터링 결과:")
            print(f"        • 신뢰도: {ai_result['confidence']}/100")
            print(f"        • 식별된 ERROR 클러스터:")

            total_estimated = 0
            for cluster in ai_result.get('error_clusters', [])[:5]:
                print(f"          - {cluster['type']}: {cluster['estimated_count']:,}개 "
                      f"({cluster['estimated_percentage']:.1f}%, {cluster['severity']})")
                total_estimated += cluster['estimated_count']

            print(f"        • 추정 총 ERROR: {total_estimated:,}개 (실제: {db_stats['total_errors']:,}개)")

            if ai_result.get('anomalies'):
                print(f"        • 이상 패턴: {', '.join(ai_result['anomalies'][:3])}")

            # 정확도 계산
            print()
            print(f"  📈 정확도 계산 중...", end="", flush=True)
            accuracy = _calculate_cluster_accuracy(db_stats, ai_result)
            print(f" ✅ 완료")
            print()

            print(f"  📊 정확도 지표:")
            print(f"        • 전체 ERROR 수 일치율: {accuracy['total_error_accuracy']:.1f}%")
            print(f"        • 에러 타입 매칭 점수: {accuracy['cluster_type_match_score']:.1f}%")
            print(f"        • 클러스터 개수 정확도: {accuracy['cluster_count_accuracy']:.1f}%")
            print(f"        • ⭐ 종합 정확도: {accuracy['overall_accuracy']:.1f}%")
            print()

            print(f"  🔍 타입 매칭 결과:")
            if accuracy['matched_types']:
                print(f"        • ✅ 매칭된 타입 ({len(accuracy['matched_types'])}개): {', '.join(accuracy['matched_types'][:5])}")
            if accuracy['missed_types']:
                print(f"        • ❌ 놓친 타입 ({len(accuracy['missed_types'])}개): {', '.join(accuracy['missed_types'][:5])}")
            if accuracy['false_positive_types']:
                print(f"        • ⚠️  오탐 타입 ({len(accuracy['false_positive_types'])}개): {', '.join(accuracy['false_positive_types'][:5])}")
            print()

            # 성능 비교
            speed_ratio = ai_time_ms / db_time_ms if db_time_ms > 0 else 0
            print(f"  ⚡ 성능:")
            print(f"        • OpenSearch 집계: {db_time_ms:.0f}ms")
            print(f"        • Vector AI 클러스터링: {ai_time_ms:.0f}ms ({speed_ratio:.1f}x)")
            print()

            # 평가
            if accuracy['overall_accuracy'] >= 90:
                grade_emoji = "🏆"
                grade = "매우 우수 (DB 집계 대체 가능)"
                feasible = True
            elif accuracy['overall_accuracy'] >= 85:
                grade_emoji = "✅"
                grade = "우수 (DB 집계 대체 가능)"
                feasible = True
            elif accuracy['overall_accuracy'] >= 75:
                grade_emoji = "⚠️"
                grade = "양호 (보조 도구 활용)"
                feasible = False
            elif accuracy['overall_accuracy'] >= 65:
                grade_emoji = "🔶"
                grade = "보통 (개선 필요)"
                feasible = False
            else:
                grade_emoji = "❌"
                grade = "미흡 (대폭 개선 필요)"
                feasible = False

            print(f"  {grade_emoji} 평가: {grade}")
            print()

            results.append({
                'k': k,
                'accuracy': accuracy['overall_accuracy'],
                'ai_time_ms': ai_time_ms,
                'grade': grade,
                'feasible': feasible,
                'ai_result': ai_result,
                'accuracy_details': accuracy,
                'sample_distribution': sample_error_types
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
            print(f"  • Vector AI로 DB 대체 가능: {'✅ 예' if best['feasible'] else '❌ 아니오 (85% 미만)'}")
            print()

            print(f"📊 k 값별 비교:")
            print(f"  {'k':>6} | {'종합 정확도':>12} | {'타입 매칭':>10} | {'처리시간':>10}")
            print(f"  {'-'*6}-+-{'-'*12}-+-{'-'*10}-+-{'-'*10}")
            for r in results:
                type_score = r['accuracy_details']['cluster_type_match_score']
                print(f"  {r['k']:>6} | {r['accuracy']:>11.1f}% | {type_score:>9.1f}% | {r['ai_time_ms']:>8.0f}ms")
            print()

            print("💡 주요 발견:")
            print(f"  • Vector KNN으로 ERROR 로그 {best['k']}개 샘플링")
            print(f"  • LLM이 의미적으로 유사한 ERROR를 클러스터링")
            print(f"  • k={best['k']}에서 최고 정확도 {best['accuracy']:.1f}% 달성")

            if best['feasible']:
                print(f"  • ✅ Vector AI가 OpenSearch 집계를 대체할 수 있는 수준 (85% 이상)")
            else:
                print(f"  • ⚠️ 85% 기준 미달 - k 값 증가 또는 프롬프트 튜닝 필요")
            print()

            print("🎯 시사점:")
            if best['feasible']:
                print("  ✅ 성공: Vector AI ERROR 패턴 분석 실용화 가능!")
                print("     - DB 집계 쿼리 대신 Vector 샘플링 + LLM 분석")
                print("     - 의미적 클러스터링으로 더 나은 인사이트 제공")
                print("     - Chatbot, 상세 분석, 문서 생성에 활용 가능")
                print()
                print("  📌 다음 단계:")
                print("     1. Chatbot V2 - ERROR 패턴 자동 탐지")
                print("     2. Log 분석 V2 - 유사 ERROR 클러스터링")
                print("     3. 문서 생성 V2 - ERROR 패턴 섹션 추가")
            else:
                print("  ⚠️ 부분 성공: 개선 필요")
                print("     - k 값 증가 (50, 100, 200 테스트)")
                print("     - 에러 타입 추출 로직 개선 (_extract_error_type)")
                print("     - LLM 프롬프트 엔지니어링 필요")
                print()
                print("  📌 권장 사항:")
                print("     1. 더 큰 k 값으로 재실험")
                print("     2. 에러 메시지 패턴 매칭 규칙 추가")
                print("     3. LLM 분석 프롬프트 개선")

        else:
            print("❌ 모든 k 값에서 ERROR 샘플 수집 실패")
            print()
            print("📌 확인 사항:")
            print("  1. OpenSearch에 ERROR 로그가 존재하는지 확인")
            print("  2. ERROR 로그에 log_vector 필드가 있는지 확인")
            print("  3. Enrichment Consumer가 실행 중인지 확인")

        print()
        print("=" * 80)

        # 실험 결과 반환 (추후 자동화용)
        return {
            'project_uuid': test_project_uuid,
            'time_range_hours': time_hours,
            'ground_truth': db_stats,
            'vector_ai_experiments': results,
            'best_k': best['k'] if results else None,
            'best_accuracy': best['accuracy'] if results else None,
            'feasible': best['feasible'] if results else False
        }

    except Exception as e:
        print(f"\n❌ 오류 발생: {e}")
        import traceback
        traceback.print_exc()
        return None


if __name__ == "__main__":
    result = asyncio.run(test_error_pattern_experiment())
    sys.exit(0 if result and result.get('feasible', False) else 1)
