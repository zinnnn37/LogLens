"""
Log analysis models
"""

from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime
from enum import Enum


class AnalysisType(str, Enum):
    """Analysis type enumeration"""

    SINGLE = "SINGLE"
    TRACE_BASED = "TRACE_BASED"


class TargetType(str, Enum):
    """Analysis target type enumeration"""

    LOG = "LOG"
    LOG_DETAILS = "LOG_DETAILS"


class LogAnalysisResult(BaseModel):
    """Result of AI log analysis"""

    summary: str = Field(..., description="로그 요약 (1-2문장, 한국어)")
    error_cause: Optional[str] = Field(None, description="근본 원인 분석 (RCA, 한국어)")
    solution: Optional[str] = Field(None, description="해결 방안 ([즉시], [단기], [장기] 우선순위 포함, 한국어)")
    tags: List[str] = Field(default_factory=list, description="분류 태그 (심각도, 에러 타입, 컴포넌트)")
    analysis_type: AnalysisType = Field(default=AnalysisType.SINGLE, description="분석 유형 (SINGLE: 단일 로그, TRACE_BASED: Trace 기반)")
    target_type: TargetType = Field(default=TargetType.LOG, description="분석 대상 타입 (LOG: 로그, LOG_DETAILS: 로그 상세)")
    analyzed_at: datetime = Field(default_factory=datetime.utcnow, description="분석 시각 (UTC)")

    class Config:
        json_schema_extra = {
            "example": {
                "summary": "UserService의 getUser() 메서드에서 NullPointerException 발생",
                "error_cause": "user_id=12345에 해당하는 User 객체가 DB에서 조회되지 않아 null이 반환되었으며, 이를 검증 없이 사용하려다 예외 발생. 근본 원인은 사용자 존재 여부에 대한 사전 검증 로직 누락.",
                "solution": "[즉시] UserService.getUser() 호출 전 null 체크 추가 또는 Optional<User> 반환 타입으로 변경\n[단기] 존재하지 않는 user_id 요청 시 명시적인 UserNotFoundException 발생시키도록 리팩토링\n[장기] API 레이어에서 user_id 유효성 검증 로직 추가",
                "tags": ["SEVERITY_HIGH", "NullPointerException", "UserService", "DataIntegrity"],
                "analysis_type": "TRACE_BASED",
                "target_type": "LOG",
                "analyzed_at": "2024-01-15T10:35:00.000Z",
            }
        }


class LogAnalysisResponse(BaseModel):
    """API response for log analysis"""

    log_id: int = Field(..., description="로그 ID (양의 정수)")
    analysis: LogAnalysisResult = Field(..., description="AI 분석 결과")
    from_cache: bool = Field(..., description="캐시에서 가져왔는지 여부 (True: 캐시 재사용, False: 새로 분석)")
    similar_log_id: Optional[int] = Field(None, description="유사 로그 ID (캐시 재사용 시에만 존재)")
    similarity_score: Optional[float] = Field(None, description="유사도 점수 (0.0 이상, 높을수록 관련성 높음. 캐시 재사용 시에만 존재, 0.8 이상일 때 재사용)", ge=0.0)

    class Config:
        json_schema_extra = {
            "example": {
                "log_id": 12345,
                "analysis": {
                    "summary": "UserService의 getUser() 메서드에서 NullPointerException 발생",
                    "error_cause": "user_id=12345에 해당하는 User 객체가 DB에서 조회되지 않아 null이 반환되었으며, 이를 검증 없이 사용하려다 예외 발생",
                    "solution": "[즉시] UserService.getUser() 호출 전 null 체크 추가\n[단기] Optional<User> 반환 타입으로 변경\n[장기] API 레이어에서 유효성 검증 추가",
                    "tags": ["SEVERITY_HIGH", "NullPointerException", "UserService", "DataIntegrity"],
                    "analysis_type": "TRACE_BASED",
                    "target_type": "LOG",
                    "analyzed_at": "2024-01-15T10:35:00.000Z",
                },
                "from_cache": False,
                "similar_log_id": None,
                "similarity_score": None,
            }
        }
