"""
V2 LangGraph API - Statistics Comparison Endpoints

AI vs DB 통계 비교를 통한 LLM 역량 검증 API
"""

import logging
from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from datetime import datetime

from app.tools.statistics_comparison_tools import (
    _get_db_statistics,
    _get_log_samples,
    _get_stratified_log_samples,
    _llm_estimate_statistics,
    _calculate_accuracy,
    _get_db_error_statistics,
    _sample_errors_with_vector,
    _vector_estimate_error_count,
    _calculate_error_accuracy
)
from app.tools.sampling_strategies import sample_two_stage_rare_aware

logger = logging.getLogger(__name__)


router = APIRouter(prefix="/v2-langgraph", tags=["Statistics Comparison"])


# Response Models
class DBStatistics(BaseModel):
    """DB 직접 조회 통계"""
    total_logs: int = Field(..., description="총 로그 수")
    error_count: int = Field(..., description="ERROR 로그 수")
    warn_count: int = Field(..., description="WARN 로그 수")
    info_count: int = Field(..., description="INFO 로그 수")
    error_rate: float = Field(..., description="에러율 (%)")
    peak_hour: str = Field(..., description="피크 시간")
    peak_count: int = Field(..., description="피크 시간 로그 수")


class AIStatistics(BaseModel):
    """AI(LLM) 추론 통계"""
    estimated_total_logs: int = Field(..., description="추론한 총 로그 수")
    estimated_error_count: int = Field(..., description="추론한 ERROR 로그 수")
    estimated_warn_count: int = Field(..., description="추론한 WARN 로그 수")
    estimated_info_count: int = Field(..., description="추론한 INFO 로그 수")
    estimated_error_rate: float = Field(..., description="추론한 에러율 (%)")
    confidence_score: int = Field(..., description="AI 신뢰도 (0-100)")
    reasoning: str = Field(..., description="추론 근거")


class AccuracyMetrics(BaseModel):
    """정확도 지표"""
    total_logs_accuracy: float = Field(..., description="총 로그 수 일치율 (%)")
    error_count_accuracy: float = Field(..., description="ERROR 수 일치율 (%)")
    warn_count_accuracy: float = Field(..., description="WARN 수 일치율 (%)")
    info_count_accuracy: float = Field(..., description="INFO 수 일치율 (%)")
    error_rate_accuracy: float = Field(..., description="에러율 정확도 (%)")
    overall_accuracy: float = Field(..., description="종합 정확도 (%)")
    ai_confidence: int = Field(..., description="AI 자체 신뢰도")


class ComparisonVerdict(BaseModel):
    """검증 결론"""
    grade: str = Field(..., description="등급 (매우 우수/우수/양호/보통/미흡)")
    can_replace_db: bool = Field(..., description="DB 대체 가능 여부")
    explanation: str = Field(..., description="설명")
    recommendations: List[str] = Field(..., description="권장 사항")


class AIvsDBComparisonResponse(BaseModel):
    """AI vs DB 통계 비교 응답"""
    project_uuid: str = Field(..., description="프로젝트 UUID")
    analysis_period_hours: int = Field(..., description="분석 기간 (시간)")
    sample_size: int = Field(..., description="사용된 샘플 크기")
    analyzed_at: datetime = Field(..., description="분석 시점")

    db_statistics: DBStatistics = Field(..., description="DB 직접 조회 결과")
    ai_statistics: AIStatistics = Field(..., description="AI 추론 결과")
    accuracy_metrics: AccuracyMetrics = Field(..., description="정확도 지표")
    verdict: ComparisonVerdict = Field(..., description="검증 결론")

    technical_highlights: List[str] = Field(..., description="기술적 어필 포인트")


# ERROR 비교 전용 Response Models
class DBErrorStats(BaseModel):
    """DB에서 조회한 ERROR 통계"""
    total_errors: int = Field(..., description="ERROR 로그 수")
    error_rate: float = Field(..., description="ERROR 비율 (%)")
    peak_error_hour: Optional[str] = Field(None, description="ERROR 최다 발생 시간")
    peak_error_count: Optional[int] = Field(None, description="최다 시간 ERROR 수")


class AIErrorStats(BaseModel):
    """AI 추정 ERROR 통계"""
    estimated_total_errors: int = Field(..., description="추정 ERROR 로그 수")
    estimated_error_rate: float = Field(..., description="추정 ERROR 비율 (%)")
    confidence_score: int = Field(..., description="AI 신뢰도 (0-100)")
    reasoning: str = Field(..., description="추론 근거")


class ErrorAccuracyMetrics(BaseModel):
    """ERROR 정확도 메트릭"""
    error_count_accuracy: float = Field(..., description="ERROR 수 일치율 (%)")
    error_rate_accuracy: float = Field(..., description="ERROR 비율 정확도 (%)")
    overall_accuracy: float = Field(..., description="종합 정확도 (%)")


class VectorGroupingInfo(BaseModel):
    """Vector KNN 그룹핑 정보"""
    vectorized_error_count: int = Field(..., description="log_vector 있는 ERROR 개수")
    vectorization_rate: float = Field(..., description="벡터화율 (%)")
    sampling_method: str = Field(..., description="샘플링 방법 (vector_knn 또는 random_fallback)")
    sample_distribution: str = Field(..., description="샘플 분포 설명")


class ErrorComparisonResponse(BaseModel):
    """ERROR 로그 비교 결과"""
    project_uuid: str = Field(..., description="프로젝트 UUID")
    analysis_period_hours: int = Field(..., description="분석 기간 (시간)")
    sample_size: int = Field(..., description="사용된 샘플 크기")
    analyzed_at: datetime = Field(..., description="분석 시점")
    db_error_stats: DBErrorStats = Field(..., description="DB ERROR 통계")
    ai_error_stats: AIErrorStats = Field(..., description="AI ERROR 추정")
    accuracy_metrics: ErrorAccuracyMetrics = Field(..., description="정확도 지표")
    vector_analysis: Optional[VectorGroupingInfo] = Field(None, description="Vector 샘플링 정보")


class HourlyDataPoint(BaseModel):
    """시간대별 데이터 포인트"""
    hour: str = Field(..., description="시간 (ISO format)")
    total: int = Field(..., description="총 로그 수")
    error: int = Field(..., description="ERROR 수")
    warn: int = Field(..., description="WARN 수")
    info: int = Field(..., description="INFO 수")


class HourlyStatisticsResponse(BaseModel):
    """시간대별 통계 응답"""
    project_uuid: str = Field(..., description="프로젝트 UUID")
    analysis_period_hours: int = Field(..., description="분석 기간")
    total_logs: int = Field(..., description="총 로그 수")
    error_rate: float = Field(..., description="에러율 (%)")
    peak_hour: str = Field(..., description="피크 시간")
    peak_count: int = Field(..., description="피크 시간 로그 수")
    hourly_data: List[HourlyDataPoint] = Field(..., description="시간대별 데이터")


@router.get("/statistics/compare", response_model=AIvsDBComparisonResponse)
async def compare_ai_vs_db(
    project_uuid: str = Query(..., description="프로젝트 UUID"),
    time_hours: int = Query(24, ge=1, le=168, description="분석 기간 (시간, 기본 24시간)"),
    sample_size: int = Query(100, ge=10, le=500, description="AI 분석용 샘플 크기")
) -> AIvsDBComparisonResponse:
    """
    AI vs DB 통계 비교를 수행하여 LLM의 DB 대체 역량을 검증합니다.

    **검증 목표:**
    - LLM이 로그 샘플만으로 전체 통계를 정확히 추론할 수 있는가?
    - AI가 사람(또는 DB 쿼리)을 대체할 수 있는 역량을 보유했는가?

    **기술적 어필 포인트:**
    - Temperature 0.1로 일관된 추론 보장
    - Structured Output으로 형식 오류 방지
    - 자동화된 정확도 측정으로 신뢰성 검증

    **반환 데이터:**
    - DB 직접 조회 결과 (Ground Truth)
    - AI 추론 결과
    - 정확도 지표 (오차율)
    - 검증 결론 (DB 대체 가능 여부)

    **정확도 기준:**
    - 95% 이상: 매우 우수 (프로덕션 사용 가능)
    - 90% 이상: 우수 (대부분 업무에 활용 가능)
    - 80% 이상: 양호 (보조 도구로 유용)
    - 70% 이상: 보통 (개선 필요)
    - 70% 미만: 미흡 (재검토 필요)
    """
    logger.info(f"🤖 AI vs DB 통계 비교 시작: project_uuid={project_uuid}, time_hours={time_hours}, sample_size={sample_size}")

    try:
        # 1. DB에서 직접 통계 조회
        logger.debug(f"1단계: DB 통계 조회 시작")
        db_stats = _get_db_statistics(project_uuid, time_hours)
        logger.info(f"✅ DB 통계 조회 완료: total_logs={db_stats.get('total_logs', 0)}")

        if db_stats["total_logs"] == 0:
            logger.warning(f"⚠️ 로그 데이터 없음: project_uuid={project_uuid}, time_hours={time_hours}")
            raise HTTPException(
                status_code=404,
                detail=f"최근 {time_hours}시간 동안 로그 데이터가 없습니다."
            )

        # 2. 로그 샘플 추출 (2단계 희소 이벤트 인식 샘플링 + IPW 가중치)
        logger.debug(f"2단계: 2단계 희소 이벤트 인식 샘플링 시작")
        sample_metadata = None
        try:
            log_samples, sample_metadata = await sample_two_stage_rare_aware(
                project_uuid=project_uuid,
                total_k=sample_size,
                time_hours=time_hours,
                rare_threshold=100  # 100개 미만이면 희소 이벤트로 간주
            )
            sampling_method = sample_metadata.get('sampling_method', 'unknown')
            logger.info(
                f"✅ 2단계 샘플링 완료: sample_count={len(log_samples)}, "
                f"method={sampling_method}, "
                f"weights={sample_metadata.get('weights', {})}, "
                f"rare_levels={sample_metadata.get('rare_levels', [])}"
            )
        except Exception as e:
            logger.warning(f"⚠️ 2단계 샘플링 실패, 기존 방식으로 폴백: {type(e).__name__}: {str(e)}")
            # 폴백: 기존 샘플링 방식 사용
            level_counts = {
                "ERROR": db_stats["error_count"],
                "WARN": db_stats["warn_count"],
                "INFO": db_stats["info_count"]
            }
            log_samples = await _get_stratified_log_samples(project_uuid, time_hours, sample_size, level_counts)
            sample_metadata = None  # 기존 방식은 메타데이터 없음
            logger.info(f"✅ 폴백 샘플링 완료: sample_count={len(log_samples)}")

        if not log_samples:
            error_msg = f"로그 샘플 추출 실패: project_uuid={project_uuid}"
            if sample_metadata:
                error_detail = sample_metadata.get("error", "Unknown error")
                level_counts = sample_metadata.get("level_counts", {})
                logger.error(
                    f"🔴 {error_msg}, error={error_detail}, level_counts={level_counts}"
                )

                # 진단 메시지 생성
                if not level_counts or sum(level_counts.values()) == 0:
                    detail = f"최근 {time_hours}시간 동안 로그가 없습니다."
                else:
                    detail = f"로그는 존재하지만 (ERROR: {level_counts.get('ERROR', 0)}, " \
                             f"WARN: {level_counts.get('WARN', 0)}, INFO: {level_counts.get('INFO', 0)}), " \
                             f"Vector 샘플링이 실패했습니다."
            else:
                logger.error(f"🔴 {error_msg}")
                detail = f"최근 {time_hours}시간 동안 로그를 찾을 수 없습니다."

            raise HTTPException(
                status_code=500,
                detail=detail
            )

        # 3. LLM 기반 통계 추론 (샘플 + IPW 가중치 메타데이터)
        logger.debug(f"3단계: LLM 통계 추론 시작 (IPW 가중치 포함)")
        ai_stats = _llm_estimate_statistics(
            log_samples,
            db_stats["total_logs"],
            time_hours,
            None,  # Inference Mode: DB 힌트 없음
            sample_metadata  # IPW 가중치 메타데이터 전달
        )
        logger.info(f"✅ LLM 추론 완료: estimated_total={ai_stats.get('estimated_total_logs', 0)}, confidence={ai_stats.get('confidence_score', 0)}")

        # 4. 정확도 계산
        logger.debug(f"4단계: 정확도 계산 시작")
        accuracy_metrics = _calculate_accuracy(db_stats, ai_stats)
        logger.info(f"✅ 정확도 계산 완료: overall_accuracy={accuracy_metrics.get('overall_accuracy', 0)}%")

        # 5. 검증 결론 생성
        overall = accuracy_metrics["overall_accuracy"]
        if overall >= 95:
            grade = "매우 우수"
            can_replace = True
            explanation = "오차율 5% 미만으로 프로덕션 환경에서 신뢰성 있게 사용 가능합니다."
            recommendations = [
                "프로덕션 환경 적용 권장",
                "실시간 대시보드 AI 기반 분석 도입 가능",
                "DB 부하 감소를 위한 AI 캐싱 레이어 구축"
            ]
        elif overall >= 90:
            grade = "우수"
            can_replace = True
            explanation = "오차율 10% 미만으로 대부분의 분석 업무에 활용 가능합니다."
            recommendations = [
                "보조 분석 도구로 즉시 활용 가능",
                "비실시간 리포트 생성에 AI 활용",
                "추가 프롬프트 튜닝으로 정확도 향상 가능"
            ]
        elif overall >= 80:
            grade = "양호"
            can_replace = False
            explanation = "오차율 20% 미만으로 트렌드 분석과 이상 탐지에 활용 가능합니다."
            recommendations = [
                "트렌드 분석 보조 도구로 활용",
                "이상 탐지 알림에 AI 추론 결합",
                "샘플 크기 증가로 정확도 향상"
            ]
        elif overall >= 70:
            grade = "보통"
            can_replace = False
            explanation = "오차율이 높아 추가 튜닝이 필요합니다."
            recommendations = [
                "프롬프트 엔지니어링 개선 필요",
                "샘플링 전략 재검토",
                "Few-shot 예제 추가 고려"
            ]
        else:
            grade = "미흡"
            can_replace = False
            explanation = "정확도가 낮아 근본적인 개선이 필요합니다."
            recommendations = [
                "LLM 모델 업그레이드 고려",
                "데이터 전처리 로직 개선",
                "RAG 패턴 도입 검토"
            ]

        verdict = ComparisonVerdict(
            grade=grade,
            can_replace_db=can_replace,
            explanation=explanation,
            recommendations=recommendations
        )

        # 6. 기술적 어필 포인트
        technical_highlights = [
            f"Temperature 0.1로 일관된 추론 (기본값 대비 7배 낮음)",
            f"층화 샘플링으로 희소 이벤트(ERROR/WARN) 포착 보장",
            f"샘플 {len(log_samples)}개로 {db_stats['total_logs']:,}개 통계 추론",
            f"종합 정확도 {overall:.1f}% 달성",
            "Structured Output으로 JSON 스키마 강제",
            "자동화된 다차원 정확도 검증",
            "MCP/멀티모달 없이 단일 LLM으로 구현"
        ]

        # 7. 응답 구성
        return AIvsDBComparisonResponse(
            project_uuid=project_uuid,
            analysis_period_hours=time_hours,
            sample_size=len(log_samples),
            analyzed_at=datetime.utcnow(),
            db_statistics=DBStatistics(
                total_logs=db_stats["total_logs"],
                error_count=db_stats["error_count"],
                warn_count=db_stats["warn_count"],
                info_count=db_stats["info_count"],
                error_rate=db_stats["error_rate"],
                peak_hour=db_stats["peak_hour"],
                peak_count=db_stats["peak_count"]
            ),
            ai_statistics=AIStatistics(
                estimated_total_logs=ai_stats.get("estimated_total_logs", 0),
                estimated_error_count=ai_stats.get("estimated_error_count", 0),
                estimated_warn_count=ai_stats.get("estimated_warn_count", 0),
                estimated_info_count=ai_stats.get("estimated_info_count", 0),
                estimated_error_rate=ai_stats.get("estimated_error_rate", 0.0),
                confidence_score=ai_stats.get("confidence_score", 0),
                reasoning=ai_stats.get("reasoning", "N/A")
            ),
            accuracy_metrics=AccuracyMetrics(
                total_logs_accuracy=accuracy_metrics["total_logs_accuracy"],
                error_count_accuracy=accuracy_metrics["error_count_accuracy"],
                warn_count_accuracy=accuracy_metrics["warn_count_accuracy"],
                info_count_accuracy=accuracy_metrics["info_count_accuracy"],
                error_rate_accuracy=accuracy_metrics["error_rate_accuracy"],
                overall_accuracy=accuracy_metrics["overall_accuracy"],
                ai_confidence=accuracy_metrics["ai_confidence"]
            ),
            verdict=verdict,
            technical_highlights=technical_highlights
        )

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"🔴 AI vs DB 통계 비교 중 예외 발생: project_uuid={project_uuid}, error={str(e)}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"AI vs DB 통계 비교 중 오류 발생: {str(e)}"
        )


@router.get("/statistics/hourly", response_model=HourlyStatisticsResponse)
async def get_hourly_statistics(
    project_uuid: str = Query(..., description="프로젝트 UUID"),
    time_hours: int = Query(24, ge=1, le=168, description="분석 기간 (시간)")
) -> HourlyStatisticsResponse:
    """
    시간대별 로그 통계를 조회합니다.

    1시간 단위로 로그 분포를 확인하여 트렌드와 패턴을 분석합니다.
    """
    try:
        db_stats = _get_db_statistics(project_uuid, time_hours)

        if db_stats["total_logs"] == 0:
            raise HTTPException(
                status_code=404,
                detail=f"최근 {time_hours}시간 동안 로그 데이터가 없습니다."
            )

        hourly_data = [
            HourlyDataPoint(
                hour=h["hour"],
                total=h["total"],
                error=h["error"],
                warn=h["warn"],
                info=h["info"]
            )
            for h in db_stats["hourly_data"]
        ]

        return HourlyStatisticsResponse(
            project_uuid=project_uuid,
            analysis_period_hours=time_hours,
            total_logs=db_stats["total_logs"],
            error_rate=db_stats["error_rate"],
            peak_hour=db_stats["peak_hour"],
            peak_count=db_stats["peak_count"],
            hourly_data=hourly_data
        )

    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"시간대별 통계 조회 중 오류 발생: {str(e)}"
        )


@router.get("/statistics/compare-errors", response_model=ErrorComparisonResponse)
async def compare_error_statistics(
    project_uuid: str = Query(..., description="프로젝트 UUID"),
    time_hours: int = Query(24, ge=1, le=168, description="분석 기간 (시간, 기본 24시간)"),
    sample_size: int = Query(100, ge=10, le=500, description="AI 분석용 샘플 크기")
) -> ErrorComparisonResponse:
    """
    ERROR 로그 전용 DB vs AI 통계 비교

    Vector KNN 샘플링을 활용하여 유사한 ERROR 패턴을 그룹핑하고,
    LLM으로 ERROR 통계를 추정하여 정확도를 검증합니다.

    **특징:**
    - ERROR 로그만 집중 분석
    - Vector KNN으로 유사 ERROR 그룹핑
    - log_vector 필드 활용 (벡터화된 ERROR만)
    - 벡터화율 50% 미만 시 자동 랜덤 샘플링 폴백

    **정확도 지표:**
    - ERROR 개수 정확도
    - ERROR 비율 정확도
    - 종합 정확도 (가중 평균)
    """
    logger.info(f"ERROR 비교 시작: project={project_uuid}, hours={time_hours}, sample_size={sample_size}")

    try:
        # Step 1: DB에서 ERROR 통계 조회
        db_stats = await _get_db_error_statistics(project_uuid, time_hours)
        logger.info(f"✅ DB ERROR 통계: total_errors={db_stats['total_errors']}, rate={db_stats['error_rate']}%")

        if db_stats["total_errors"] == 0:
            logger.warning(f"⚠️ ERROR 로그 없음: project_uuid={project_uuid}")
            raise HTTPException(
                status_code=404,
                detail=f"최근 {time_hours}시간 동안 ERROR 로그가 없습니다."
            )

        # Step 2: Vector 샘플링 (ERROR만)
        error_samples, vector_info = await _sample_errors_with_vector(
            project_uuid, time_hours, sample_size
        )
        logger.info(
            f"✅ ERROR 샘플링 완료: count={len(error_samples)}, "
            f"method={vector_info['sampling_method']}, "
            f"vectorization_rate={vector_info['vectorization_rate']}%"
        )

        if not error_samples:
            raise HTTPException(
                status_code=500,
                detail="ERROR 로그 샘플링 실패"
            )

        # Step 3: Vector 유사도로 ERROR 추정 (동적 threshold)
        ai_stats = await _vector_estimate_error_count(
            error_samples,
            project_uuid,
            time_hours,
            db_stats["total_logs"]
            # similarity_threshold=None -> 동적 계산
        )
        logger.info(
            f"✅ AI ERROR 추정 (Vector): estimated_errors={ai_stats['estimated_total_errors']}, "
            f"confidence={ai_stats['confidence_score']}"
        )

        # Step 4: 정확도 계산
        accuracy = _calculate_error_accuracy(db_stats, ai_stats)
        logger.info(f"✅ 정확도 계산: overall={accuracy['overall_accuracy']:.1f}%")

        # Step 5: 응답 구성
        response = ErrorComparisonResponse(
            project_uuid=project_uuid,
            analysis_period_hours=time_hours,
            sample_size=len(error_samples),
            analyzed_at=datetime.utcnow(),
            db_error_stats=DBErrorStats(
                total_errors=db_stats["total_errors"],
                error_rate=db_stats["error_rate"],
                peak_error_hour=db_stats["peak_error_hour"],
                peak_error_count=db_stats["peak_error_count"]
            ),
            ai_error_stats=AIErrorStats(
                estimated_total_errors=ai_stats["estimated_total_errors"],
                estimated_error_rate=ai_stats["estimated_error_rate"],
                confidence_score=ai_stats["confidence_score"],
                reasoning=ai_stats["reasoning"]
            ),
            accuracy_metrics=ErrorAccuracyMetrics(
                error_count_accuracy=accuracy["error_count_accuracy"],
                error_rate_accuracy=accuracy["error_rate_accuracy"],
                overall_accuracy=accuracy["overall_accuracy"]
            ),
            vector_analysis=VectorGroupingInfo(
                vectorized_error_count=vector_info["vectorized_error_count"],
                vectorization_rate=vector_info["vectorization_rate"],
                sampling_method=vector_info["sampling_method"],
                sample_distribution=vector_info["sample_distribution"]
            ) if vector_info else None
        )

        logger.info(f"✅ ERROR 비교 완료: accuracy={accuracy['overall_accuracy']:.1f}%")
        return response

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"🔴 ERROR 비교 실패: {e}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"ERROR 비교 중 오류 발생: {str(e)}"
        )
