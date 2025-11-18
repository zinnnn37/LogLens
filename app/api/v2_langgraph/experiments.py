"""
V2 LangGraph API - Vector AI vs DB Experiment Endpoints

Vector 검색 + AI 추론이 OpenSearch 집계를 대체할 수 있는지 검증
"""

import logging
import asyncio
from fastapi import APIRouter, HTTPException, Query
from typing import List
from datetime import datetime

from app.models.experiment import (
    ExperimentComparison,
    VectorAIExperiment,
    VectorAIResult,
    AccuracyMetrics,
    DBStatistics,
    ExperimentConclusion
)
from app.tools.vector_experiment_tools import (
    _get_db_statistics_for_experiment,
    _vector_search_all,
    _llm_estimate_from_vectors,
    _calculate_accuracy_for_experiment
)

logger = logging.getLogger(__name__)


router = APIRouter(prefix="/v2-langgraph", tags=["Vector AI Experiments"])


@router.get("/experiments/vector-vs-db", response_model=ExperimentComparison)
async def compare_vector_ai_vs_db(
    project_uuid: str = Query(..., description="프로젝트 UUID"),
    time_hours: int = Query(24, ge=1, le=168, description="분석 기간 (시간)"),
    k_values: str = Query("100,500,1000", description="테스트할 k 값 (콤마로 구분, 예: 100,500,1000)")
) -> ExperimentComparison:
    """
    Vector AI vs OpenSearch 집계 성능/정확도 비교 실험

    **실험 목표:**
    - Vector KNN 검색 + LLM 추론이 OpenSearch 집계를 대체할 수 있는가?
    - 어떤 k 값에서 90% 이상의 정확도를 달성하는가?

    **실험 방법:**
    1. OpenSearch 집계로 Ground Truth 획득 (정확도 100%)
    2. 각 k 값에 대해:
       - Vector KNN으로 k개 샘플 수집
       - LLM이 샘플만 보고 전체 통계 추론 (level_counts 힌트 없음)
       - 정확도 계산 및 성능 측정
    3. 최적 k 값 도출 및 실용성 평가

    **k 값 의미:**
    - k=100: 매우 적은 샘플 (빠르지만 정확도 낮음)
    - k=500: 중간 샘플 (균형)
    - k=1000: 충분한 샘플 (느리지만 정확)
    - k=5000: 대량 샘플 (DB 집계와 비교 가능한 수준)

    **실용성 평가 기준:**
    - 90% 이상: Vector AI가 DB 대체 가능
    - 80-90%: 보조 도구로 활용 가능
    - 80% 미만: 추가 개선 필요

    **시사점:**
    - 성공 시: 복잡한 집계 쿼리를 Vector 검색 + LLM으로 대체 가능
    - 실패 시: 전통적 집계 방식이 여전히 필수적
    """
    logger.info(f"🧪 Vector AI vs DB 실험 시작: project_uuid={project_uuid}, time_hours={time_hours}, k_values={k_values}")

    try:
        # k 값 파싱
        try:
            k_list = [int(k.strip()) for k in k_values.split(",")]
            if not k_list:
                raise ValueError("k 값이 비어있습니다.")
            if any(k <= 0 for k in k_list):
                raise ValueError("k 값은 양수여야 합니다.")
            if any(k > 10000 for k in k_list):
                raise ValueError("k 값은 10000 이하여야 합니다.")
        except ValueError as e:
            raise HTTPException(
                status_code=400,
                detail=f"k_values 파싱 오류: {str(e)}"
            )

        logger.info(f"📊 실험 k 값: {k_list}")

        # 1. Ground Truth: OpenSearch 집계로 정확한 통계 조회
        logger.debug(f"1단계: OpenSearch 집계로 Ground Truth 조회")
        import time
        db_start = time.time()
        db_stats = _get_db_statistics_for_experiment(project_uuid, time_hours)
        db_time_ms = (time.time() - db_start) * 1000
        logger.info(f"✅ Ground Truth 조회 완료: total_logs={db_stats['total_logs']}, time={db_time_ms:.2f}ms")

        if db_stats["total_logs"] == 0:
            logger.warning(f"⚠️ 로그 데이터 없음: project_uuid={project_uuid}")
            raise HTTPException(
                status_code=404,
                detail=f"최근 {time_hours}시간 동안 로그 데이터가 없습니다."
            )

        # 2. 각 k 값에 대해 Vector AI 실험 수행
        vector_ai_experiments = []

        for k in k_list:
            logger.info(f"🔬 k={k} 실험 시작")

            # Vector 검색으로 샘플 수집
            logger.debug(f"  - Vector KNN 검색 (k={k})")
            ai_start = time.time()
            samples = await _vector_search_all(project_uuid, k, time_hours)
            logger.info(f"  ✅ 샘플 수집 완료: {len(samples)}개")

            if not samples:
                logger.warning(f"  ⚠️ k={k}에서 샘플 수집 실패")
                continue

            # 샘플 분포 계산
            sample_distribution = {}
            for sample in samples:
                level = sample.get("level", "UNKNOWN")
                sample_distribution[level] = sample_distribution.get(level, 0) + 1

            logger.debug(f"  - 샘플 분포: {sample_distribution}")

            # LLM이 샘플만 보고 전체 통계 추론 (level_counts 힌트 없음!)
            logger.debug(f"  - LLM 통계 추론 시작")
            ai_stats = await _llm_estimate_from_vectors(
                samples,
                db_stats["total_logs"],  # 총 개수만 힌트로 제공
                time_hours
            )
            ai_time_ms = (time.time() - ai_start) * 1000
            logger.info(f"  ✅ LLM 추론 완료: estimated_error_count={ai_stats['estimated_error_count']}, time={ai_time_ms:.2f}ms")

            # 정확도 계산
            logger.debug(f"  - 정확도 계산 중")
            accuracy_metrics = _calculate_accuracy_for_experiment(db_stats, ai_stats)
            logger.info(f"  ✅ 정확도: overall={accuracy_metrics['overall_accuracy']:.2f}%")

            # 실험 결과 저장
            vector_ai_experiments.append(VectorAIExperiment(
                k=k,
                ai_result=VectorAIResult(
                    k=k,
                    estimated_total_logs=ai_stats["estimated_total_logs"],
                    estimated_error_count=ai_stats["estimated_error_count"],
                    estimated_warn_count=ai_stats["estimated_warn_count"],
                    estimated_info_count=ai_stats["estimated_info_count"],
                    estimated_error_rate=ai_stats["estimated_error_rate"],
                    reasoning=ai_stats["reasoning"],
                    confidence_score=ai_stats["confidence_score"],
                    processing_time_ms=ai_time_ms
                ),
                accuracy_metrics=AccuracyMetrics(
                    total_logs_accuracy=accuracy_metrics["total_logs_accuracy"],
                    error_count_accuracy=accuracy_metrics["error_count_accuracy"],
                    warn_count_accuracy=accuracy_metrics["warn_count_accuracy"],
                    info_count_accuracy=accuracy_metrics["info_count_accuracy"],
                    error_rate_accuracy=accuracy_metrics["error_rate_accuracy"],
                    overall_accuracy=accuracy_metrics["overall_accuracy"]
                ),
                sample_distribution=sample_distribution
            ))

        if not vector_ai_experiments:
            logger.error(f"🔴 모든 k 값에서 실험 실패")
            raise HTTPException(
                status_code=500,
                detail="Vector AI 실험에서 샘플을 수집할 수 없습니다."
            )

        # 3. 최적 k 값 찾기 및 결론 도출
        logger.debug(f"3단계: 최적 k 값 및 결론 도출")
        best_experiment = max(vector_ai_experiments, key=lambda e: e.accuracy_metrics.overall_accuracy)
        best_k = best_experiment.k
        best_accuracy = best_experiment.accuracy_metrics.overall_accuracy

        logger.info(f"🏆 최적 k 값: k={best_k}, accuracy={best_accuracy:.2f}%")

        # 등급 및 실용성 평가
        if best_accuracy >= 95:
            grade = "매우 우수"
            feasible = True
            recommendation = "Vector AI가 DB 집계를 완전히 대체할 수 있습니다. 프로덕션 환경에 즉시 적용 가능합니다."
        elif best_accuracy >= 90:
            grade = "우수"
            feasible = True
            recommendation = "Vector AI가 DB 집계를 대체할 수 있습니다. 일부 조정 후 프로덕션 적용 가능합니다."
        elif best_accuracy >= 80:
            grade = "양호"
            feasible = False
            recommendation = "Vector AI를 보조 도구로 활용할 수 있습니다. 주요 통계는 DB 집계 사용을 권장합니다."
        elif best_accuracy >= 70:
            grade = "보통"
            feasible = False
            recommendation = "Vector AI의 정확도가 부족합니다. 프롬프트 튜닝 및 샘플링 전략 개선이 필요합니다."
        else:
            grade = "미흡"
            feasible = False
            recommendation = "Vector AI가 DB 집계를 대체하기에는 정확도가 매우 부족합니다. 근본적인 개선이 필요합니다."

        # 성능 비교
        performance_vs_db = {
            "db_processing_time_ms": round(db_time_ms, 2),
            "best_ai_processing_time_ms": round(best_experiment.ai_result.processing_time_ms, 2),
            "speed_ratio": round(best_experiment.ai_result.processing_time_ms / db_time_ms, 2) if db_time_ms > 0 else 0,
            "accuracy_trade_off": round(100 - best_accuracy, 2),
            "verdict": "DB가 더 빠르고 정확함" if best_experiment.ai_result.processing_time_ms > db_time_ms else "Vector AI가 더 빠름 (단, 정확도 트레이드오프 존재)"
        }

        conclusion = ExperimentConclusion(
            best_k=best_k,
            best_accuracy=best_accuracy,
            feasible=feasible,
            grade=grade,
            recommendation=recommendation,
            performance_vs_db=performance_vs_db
        )

        # 4. 주요 인사이트 생성
        insights = []

        # k 값에 따른 정확도 변화
        if len(vector_ai_experiments) > 1:
            accuracies = [e.accuracy_metrics.overall_accuracy for e in vector_ai_experiments]
            k_vals = [e.k for e in vector_ai_experiments]
            if accuracies[-1] > accuracies[0]:
                insights.append(f"k 값이 증가할수록 정확도가 향상됨 (k={k_vals[0]}: {accuracies[0]:.1f}% → k={k_vals[-1]}: {accuracies[-1]:.1f}%)")
            else:
                insights.append(f"k={k_vals[0]}에서도 충분한 정확도 달성 (추가 샘플링 불필요)")

        # 에러 탐지 정확도
        error_accuracy = best_experiment.accuracy_metrics.error_count_accuracy
        if error_accuracy >= 90:
            insights.append(f"ERROR 로그 개수 추론 정확도 {error_accuracy:.1f}% (이상 탐지에 활용 가능)")
        else:
            insights.append(f"ERROR 로그 추론 정확도 {error_accuracy:.1f}% (개선 필요)")

        # 샘플 효율성
        sample_ratio = best_k / db_stats["total_logs"] * 100 if db_stats["total_logs"] > 0 else 0
        insights.append(f"전체 로그의 {sample_ratio:.2f}%만 조회하여 {best_accuracy:.1f}% 정확도 달성")

        # 성능 트레이드오프
        if performance_vs_db["speed_ratio"] < 1:
            insights.append(f"Vector AI가 DB 집계 대비 {(1-performance_vs_db['speed_ratio'])*100:.0f}% 더 빠름")
        else:
            insights.append(f"DB 집계가 Vector AI 대비 {(performance_vs_db['speed_ratio']-1)*100:.0f}% 더 빠름")

        # AI 신뢰도
        ai_confidence = best_experiment.ai_result.confidence_score
        insights.append(f"AI 자체 신뢰도: {ai_confidence}/100 (실제 정확도: {best_accuracy:.1f}%)")

        # 5. 응답 구성
        logger.info(f"✅ 실험 완료: best_k={best_k}, best_accuracy={best_accuracy:.2f}%, feasible={feasible}")

        return ExperimentComparison(
            project_uuid=project_uuid,
            experiment_time=datetime.utcnow(),
            time_range_hours=time_hours,
            ground_truth=DBStatistics(
                total_logs=db_stats["total_logs"],
                error_count=db_stats["error_count"],
                warn_count=db_stats["warn_count"],
                info_count=db_stats["info_count"],
                error_rate=db_stats["error_rate"],
                processing_time_ms=db_time_ms
            ),
            vector_ai_experiments=vector_ai_experiments,
            conclusion=conclusion,
            insights=insights
        )

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"🔴 Vector AI vs DB 실험 중 예외 발생: project_uuid={project_uuid}, error={str(e)}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"실험 중 오류 발생: {str(e)}"
        )
