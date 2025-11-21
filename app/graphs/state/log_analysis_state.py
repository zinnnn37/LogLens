"""
Log Analysis State Definition for LangGraph
"""

from typing import TypedDict, Optional, List, Dict, Any
from typing_extensions import Annotated


class LogAnalysisState(TypedDict):
    """
    State for Log Analysis Agent

    이 State는 Agent가 로그 분석 과정에서 사용하는 모든 정보를 담습니다.
    """

    # ===== Input (Required) =====
    log_id: int
    project_uuid: str

    # ===== Log Data =====
    log_data: Optional[Dict[str, Any]]
    trace_id: Optional[str]
    timestamp: Optional[str]
    log_message: Optional[str]
    log_level: Optional[str]
    service_name: Optional[str]

    # ===== Cache Results =====
    direct_cache_result: Optional[Dict[str, Any]]
    trace_cache_result: Optional[Dict[str, Any]]
    similarity_cache_result: Optional[Dict[str, Any]]
    similar_log_id: Optional[int]
    similarity_score: Optional[float]
    from_cache: bool
    cache_type: Optional[str]  # "direct", "trace", "similarity", None

    # ===== Collection Results =====
    related_logs: Optional[List[Dict[str, Any]]]
    log_count: int

    # ===== Analysis Results =====
    analysis_result: Optional[Dict[str, Any]]
    analysis_method: Optional[str]  # "single", "direct", "map_reduce"
    chunk_size: Optional[int]  # Map-Reduce에서 사용된 chunk size

    # ===== Validation =====
    korean_valid: bool
    korean_retry_count: int
    quality_score: Optional[float]
    quality_suggestions: Optional[List[str]]
    validation_retry_count: int
    max_korean_retries: int  # 기본값 2
    max_validation_retries: int  # 기본값 1

    # ===== Agent State =====
    agent_iterations: int
    agent_scratchpad: Annotated[str, "Agent's intermediate thinking and actions"]

    # ===== Output =====
    final_analysis: Optional[Dict[str, Any]]
    error: Optional[str]

    # ===== Metadata (for debugging/monitoring) =====
    started_at: Optional[str]
    finished_at: Optional[str]
    total_duration_ms: Optional[int]
    llm_call_count: int
    cache_check_duration_ms: Optional[int]
    analysis_duration_ms: Optional[int]
    validation_duration_ms: Optional[int]


def create_initial_state(log_id: int, project_uuid: str) -> LogAnalysisState:
    """
    초기 State 생성

    Args:
        log_id: 로그 ID
        project_uuid: 프로젝트 UUID

    Returns:
        초기화된 LogAnalysisState
    """
    from datetime import datetime

    return LogAnalysisState(
        # Input
        log_id=log_id,
        project_uuid=project_uuid,

        # Log Data
        log_data=None,
        trace_id=None,
        timestamp=None,
        log_message=None,
        log_level=None,
        service_name=None,

        # Cache
        direct_cache_result=None,
        trace_cache_result=None,
        similarity_cache_result=None,
        similar_log_id=None,
        similarity_score=None,
        from_cache=False,
        cache_type=None,

        # Collection
        related_logs=None,
        log_count=0,

        # Analysis
        analysis_result=None,
        analysis_method=None,
        chunk_size=None,

        # Validation
        korean_valid=False,
        korean_retry_count=0,
        quality_score=None,
        quality_suggestions=None,
        validation_retry_count=0,
        max_korean_retries=2,
        max_validation_retries=1,

        # Agent
        agent_iterations=0,
        agent_scratchpad="",

        # Output
        final_analysis=None,
        error=None,

        # Metadata
        started_at=datetime.utcnow().isoformat() + "Z",
        finished_at=None,
        total_duration_ms=None,
        llm_call_count=0,
        cache_check_duration_ms=None,
        analysis_duration_ms=None,
        validation_duration_ms=None,
    )
