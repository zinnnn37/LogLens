"""
실험 결과 모델 - ERROR 패턴 분석 (Vector AI vs OpenSearch 집계 비교)
"""

from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from datetime import datetime


# ============================================================================
# ERROR 패턴 분석 모델 (새로운 실험)
# ============================================================================

class ErrorCluster(BaseModel):
    """AI가 식별한 ERROR 클러스터"""
    type: str = Field(..., description="에러 타입 (NullPointerException, Timeout 등)")
    estimated_count: int = Field(..., description="추정 개수")
    estimated_percentage: float = Field(..., description="추정 비율 (%)")
    severity: str = Field(..., description="심각도 (high/medium/low)")
    description: str = Field(..., description="패턴 설명")


class ErrorPatternAnalysisResult(BaseModel):
    """Vector AI ERROR 패턴 분석 결과"""
    k: int = Field(..., description="사용한 샘플 크기")
    error_clusters: List[ErrorCluster] = Field(..., description="식별된 ERROR 클러스터")
    anomalies: List[str] = Field(default_factory=list, description="이상 패턴")
    confidence: int = Field(..., description="분석 신뢰도 (0-100)")
    reasoning: str = Field(..., description="분석 근거")
    processing_time_ms: float = Field(..., description="처리 시간 (밀리초)")


class ErrorTypeStatistics(BaseModel):
    """DB에서 집계한 ERROR 타입 통계"""
    pattern: str = Field(..., description="에러 패턴")
    count: int = Field(..., description="발생 횟수")
    percentage: float = Field(..., description="비율 (%)")


class DBErrorStatistics(BaseModel):
    """DB 직접 조회 ERROR 통계"""
    total_errors: int = Field(..., description="총 ERROR 로그 수")
    error_types: Dict[str, int] = Field(..., description="에러 타입별 개수")
    top_errors: List[ErrorTypeStatistics] = Field(..., description="상위 에러 목록")
    analysis_period_hours: int = Field(..., description="분석 기간 (시간)")
    processing_time_ms: float = Field(..., description="처리 시간 (밀리초)")


class ClusterAccuracyMetrics(BaseModel):
    """클러스터링 정확도 지표"""
    total_error_accuracy: float = Field(..., description="전체 ERROR 수 일치율 (%)")
    cluster_type_match_score: float = Field(..., description="에러 타입 매칭 점수 (%)")
    cluster_count_accuracy: float = Field(..., description="클러스터별 개수 정확도 (%)")
    overall_accuracy: float = Field(..., description="종합 정확도 (%)")
    matched_types: List[str] = Field(..., description="매칭된 에러 타입")
    missed_types: List[str] = Field(..., description="놓친 에러 타입")
    false_positive_types: List[str] = Field(..., description="오탐 에러 타입")


class ErrorPatternExperiment(BaseModel):
    """ERROR 패턴 분석 실험 결과 (단일 k)"""
    k: int = Field(..., description="샘플 크기")
    ai_result: ErrorPatternAnalysisResult = Field(..., description="AI 패턴 분석 결과")
    accuracy_metrics: ClusterAccuracyMetrics = Field(..., description="정확도 지표")
    sample_distribution: Dict[str, int] = Field(..., description="샘플의 에러 타입 분포")


class ErrorPatternExperimentConclusion(BaseModel):
    """ERROR 패턴 분석 실험 결론"""
    best_k: int = Field(..., description="최고 정확도 k 값")
    best_accuracy: float = Field(..., description="최고 정확도 (%)")
    feasible: bool = Field(..., description="Vector AI가 DB 대체 가능 여부 (85% 이상)")
    grade: str = Field(..., description="등급 (매우 우수/우수/양호/보통/미흡)")
    recommendation: str = Field(..., description="권장 사항")
    performance_vs_db: Dict[str, Any] = Field(..., description="DB 대비 성능 비교")
    key_findings: List[str] = Field(..., description="주요 발견 사항")


class ErrorPatternExperimentComparison(BaseModel):
    """전체 ERROR 패턴 분석 실험 비교 결과"""
    project_uuid: str = Field(..., description="프로젝트 UUID")
    experiment_time: datetime = Field(..., description="실험 시간")
    time_range_hours: int = Field(..., description="분석 기간 (시간)")

    ground_truth: DBErrorStatistics = Field(..., description="Ground Truth (OpenSearch 집계)")
    vector_ai_experiments: List[ErrorPatternExperiment] = Field(..., description="Vector AI 실험 결과들")

    conclusion: ErrorPatternExperimentConclusion = Field(..., description="실험 결론")
    insights: List[str] = Field(..., description="주요 인사이트")
    limitations: List[str] = Field(
        default_factory=lambda: ["Vector 검색은 ERROR 로그만 지원", "WARN/INFO 로그는 벡터화되지 않음"],
        description="기능 제약사항"
    )


# ============================================================================
# 공통 검증 모델 (Chatbot, Log 분석, 문서 생성 등에 사용)
# ============================================================================

class LogSource(BaseModel):
    """로그 출처 정보"""
    log_id: str = Field(..., description="로그 ID")
    timestamp: str = Field(..., description="타임스탬프")
    level: str = Field(..., description="로그 레벨 (ERROR/WARN/INFO)")
    message: str = Field(..., description="로그 메시지")
    service_name: str = Field(..., description="서비스 이름")
    relevance_score: Optional[float] = Field(None, description="관련성 점수 (0-1)")
    class_name: Optional[str] = Field(None, description="클래스 이름")
    method_name: Optional[str] = Field(None, description="메서드 이름")


class ValidationInfo(BaseModel):
    """유효성 검증 정보"""
    confidence: int = Field(..., description="신뢰도 (0-100)", ge=0, le=100)
    sample_count: int = Field(..., description="사용된 샘플 수")
    sampling_strategy: str = Field(..., description="샘플링 전략 (proportional_vector_knn/random/etc)")
    coverage: str = Field(..., description="데이터 커버리지 설명")
    data_quality: str = Field(default="high", description="데이터 품질 (high/medium/low)")
    limitation: Optional[str] = Field(
        default="Vector 검색은 ERROR 로그만 지원. WARN/INFO는 기본 필터 사용",
        description="기능 제약사항"
    )
    note: Optional[str] = Field(None, description="추가 메모")


class AnalysisMetadata(BaseModel):
    """분석 메타데이터 (문서 생성 등에 사용)"""
    generated_at: datetime = Field(..., description="생성 시간")
    data_range: str = Field(..., description="데이터 범위 (예: 2025-11-17 ~ 2025-11-18)")
    total_logs_analyzed: int = Field(..., description="분석된 총 로그 수")
    error_logs: Optional[int] = Field(None, description="ERROR 로그 수")
    warn_logs: Optional[int] = Field(None, description="WARN 로그 수")
    info_logs: Optional[int] = Field(None, description="INFO 로그 수")
    sample_strategy: Dict[str, Any] = Field(..., description="샘플링 전략 상세")
    limitations: List[str] = Field(default_factory=list, description="제약사항")


# ============================================================================
# 기존 모델 (하위 호환성 유지)
# ============================================================================

class VectorAIResult(BaseModel):
    """Vector AI 추론 결과 (k 값별) - DEPRECATED"""
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
    """정확도 지표 - DEPRECATED"""
    total_logs_accuracy: float = Field(..., description="총 로그 수 일치율 (%)")
    error_count_accuracy: float = Field(..., description="ERROR 수 일치율 (%)")
    warn_count_accuracy: float = Field(..., description="WARN 수 일치율 (%)")
    info_count_accuracy: float = Field(..., description="INFO 수 일치율 (%)")
    error_rate_accuracy: float = Field(..., description="에러율 정확도 (%)")
    overall_accuracy: float = Field(..., description="종합 정확도 (%)")


class ExperimentConclusion(BaseModel):
    """실험 결론 - DEPRECATED"""
    best_k: int = Field(..., description="최고 정확도 k 값")
    best_accuracy: float = Field(..., description="최고 정확도 (%)")
    feasible: bool = Field(..., description="Vector AI가 DB 대체 가능 여부 (90% 이상)")
    grade: str = Field(..., description="등급 (매우 우수/우수/양호/보통/미흡)")
    recommendation: str = Field(..., description="권장 사항")
    performance_vs_db: Dict[str, Any] = Field(..., description="DB 대비 성능 비교")


class DBStatistics(BaseModel):
    """DB 직접 조회 통계 - DEPRECATED"""
    total_logs: int = Field(..., description="총 로그 수")
    error_count: int = Field(..., description="ERROR 로그 수")
    warn_count: int = Field(..., description="WARN 로그 수")
    info_count: int = Field(..., description="INFO 로그 수")
    error_rate: float = Field(..., description="에러율 (%)")
    processing_time_ms: float = Field(..., description="처리 시간 (밀리초)")


class VectorAIExperiment(BaseModel):
    """Vector AI 실험 결과 (단일 k) - DEPRECATED"""
    k: int = Field(..., description="샘플 크기")
    ai_result: VectorAIResult = Field(..., description="AI 추론 결과")
    accuracy_metrics: AccuracyMetrics = Field(..., description="정확도 지표")
    sample_distribution: Dict[str, int] = Field(..., description="샘플의 레벨별 분포")


class ExperimentComparison(BaseModel):
    """전체 실험 비교 결과 - DEPRECATED"""
    project_uuid: str = Field(..., description="프로젝트 UUID")
    experiment_time: datetime = Field(..., description="실험 시간")
    time_range_hours: int = Field(..., description="분석 기간 (시간)")

    ground_truth: DBStatistics = Field(..., description="Ground Truth (OpenSearch 집계)")
    vector_ai_experiments: List[VectorAIExperiment] = Field(..., description="Vector AI 실험 결과들")

    conclusion: ExperimentConclusion = Field(..., description="실험 결론")
    insights: List[str] = Field(..., description="주요 인사이트")
