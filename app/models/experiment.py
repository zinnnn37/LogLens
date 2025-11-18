"""
실험 결과 모델 (Vector AI vs OpenSearch 집계 비교)
"""

from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from datetime import datetime


class VectorAIResult(BaseModel):
    """Vector AI 추론 결과 (k 값별)"""
    k: int = Field(..., description="사용한 샘플 크기")
    estimated_total_logs: int = Field(..., description="추론한 총 로그 수")
    estimated_error_count: int = Field(..., description="추론한 ERROR 로그 수")
    estimated_warn_count: int = Field(..., description="추론한 WARN 로그 수")
    estimated_info_count: int = Field(..., description="추론한 INFO 로그 수")
    estimated_error_rate: float = Field(..., description="추론한 에러율 (%)")
    reasoning: str = Field(..., description="AI 추론 근거")
    confidence_score: int = Field(..., description="AI 신뢰도 (0-100)")
    processing_time_ms: float = Field(..., description="처리 시간 (밀리초)")


class AccuracyMetrics(BaseModel):
    """정확도 지표"""
    total_logs_accuracy: float = Field(..., description="총 로그 수 일치율 (%)")
    error_count_accuracy: float = Field(..., description="ERROR 수 일치율 (%)")
    warn_count_accuracy: float = Field(..., description="WARN 수 일치율 (%)")
    info_count_accuracy: float = Field(..., description="INFO 수 일치율 (%)")
    error_rate_accuracy: float = Field(..., description="에러율 정확도 (%)")
    overall_accuracy: float = Field(..., description="종합 정확도 (%)")


class ExperimentConclusion(BaseModel):
    """실험 결론"""
    best_k: int = Field(..., description="최고 정확도 k 값")
    best_accuracy: float = Field(..., description="최고 정확도 (%)")
    feasible: bool = Field(..., description="Vector AI가 DB 대체 가능 여부 (90% 이상)")
    grade: str = Field(..., description="등급 (매우 우수/우수/양호/보통/미흡)")
    recommendation: str = Field(..., description="권장 사항")
    performance_vs_db: Dict[str, Any] = Field(..., description="DB 대비 성능 비교")


class DBStatistics(BaseModel):
    """DB 직접 조회 통계"""
    total_logs: int = Field(..., description="총 로그 수")
    error_count: int = Field(..., description="ERROR 로그 수")
    warn_count: int = Field(..., description="WARN 로그 수")
    info_count: int = Field(..., description="INFO 로그 수")
    error_rate: float = Field(..., description="에러율 (%)")
    processing_time_ms: float = Field(..., description="처리 시간 (밀리초)")


class VectorAIExperiment(BaseModel):
    """Vector AI 실험 결과 (단일 k)"""
    k: int = Field(..., description="샘플 크기")
    ai_result: VectorAIResult = Field(..., description="AI 추론 결과")
    accuracy_metrics: AccuracyMetrics = Field(..., description="정확도 지표")
    sample_distribution: Dict[str, int] = Field(..., description="샘플의 레벨별 분포")


class ExperimentComparison(BaseModel):
    """전체 실험 비교 결과"""
    project_uuid: str = Field(..., description="프로젝트 UUID")
    experiment_time: datetime = Field(..., description="실험 시간")
    time_range_hours: int = Field(..., description="분석 기간 (시간)")

    ground_truth: DBStatistics = Field(..., description="Ground Truth (OpenSearch 집계)")
    vector_ai_experiments: List[VectorAIExperiment] = Field(..., description="Vector AI 실험 결과들")

    conclusion: ExperimentConclusion = Field(..., description="실험 결론")
    insights: List[str] = Field(..., description="주요 인사이트")
