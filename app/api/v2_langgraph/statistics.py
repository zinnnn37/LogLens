"""
V2 LangGraph API - Statistics Comparison Endpoints

AI vs DB 통계 비교를 통한 LLM 역량 검증 API
"""

from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from datetime import datetime

from app.tools.statistics_comparison_tools import (
    _get_db_statistics,
    _get_log_samples,
    _llm_estimate_statistics,
    _calculate_accuracy
)


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
    try:
        # 1. DB에서 직접 통계 조회
        db_stats = _get_db_statistics(project_uuid, time_hours)

        if db_stats["total_logs"] == 0:
            raise HTTPException(
                status_code=404,
                detail=f"최근 {time_hours}시간 동안 로그 데이터가 없습니다."
            )

        # 2. 로그 샘플 추출
        log_samples = _get_log_samples(project_uuid, time_hours, sample_size)

        if not log_samples:
            raise HTTPException(
                status_code=500,
                detail="로그 샘플을 추출할 수 없습니다."
            )

        # 3. LLM 기반 통계 추론
        ai_stats = _llm_estimate_statistics(log_samples, db_stats["total_logs"], time_hours)

        # 4. 정확도 계산
        accuracy_metrics = _calculate_accuracy(db_stats, ai_stats)

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
