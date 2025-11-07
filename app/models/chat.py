"""
Chatbot models
"""

from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from datetime import datetime


class ChatMessage(BaseModel):
    """Single chat message for history"""

    role: str = Field(..., description="역할 ('user': 사용자, 'assistant': AI 챗봇)")
    content: str = Field(..., description="메시지 내용")

    class Config:
        json_schema_extra = {
            "example": {
                "role": "user",
                "content": "최근 에러 알려줘"
            }
        }


class ChatRequest(BaseModel):
    """Chatbot question request with history support"""

    question: str = Field(..., description="로그에 대한 사용자 질문 (자연어)")
    project_uuid: str = Field(
        ...,
        description="프로젝트 UUID (멀티테넌시 격리, 예: test-project 또는 550e8400-e29b-41d4-a716-446655440000)"
    )
    chat_history: Optional[List[ChatMessage]] = Field(
        default=None,
        description="이전 대화 히스토리 (최대 1500 토큰까지 사용, 초과 시 자동 압축)"
    )
    filters: Optional[Dict[str, Any]] = Field(
        None,
        description="로그 검색 필터 (선택사항). 가능한 필드: level (ERROR/WARN/INFO), service_name (서비스명), class_name (클래스명), method_name (메서드명)"
    )
    time_range: Optional[Dict[str, str]] = Field(
        None,
        description="시간 범위 필터 (선택사항, ISO 8601 형식). 형식: {'start': 'YYYY-MM-DDTHH:MM:SSZ', 'end': 'YYYY-MM-DDTHH:MM:SSZ'}"
    )

    class Config:
        json_schema_extra = {
            "example": {
                "question": "그 중 가장 심각한 건?",
                "project_uuid": "test-project",
                "chat_history": [
                    {"role": "user", "content": "최근 에러 알려줘"},
                    {"role": "assistant", "content": "NPE 3건, DB 타임아웃 2건 발생했습니다"}
                ],
                "filters": {"level": "ERROR", "service_name": "user-service"},
                "time_range": {"start": "2024-01-15T00:00:00Z", "end": "2024-01-15T23:59:59Z"},
            }
        }


class RelatedLog(BaseModel):
    """Related log information"""

    log_id: int = Field(..., description="로그 ID (양의 정수)")
    timestamp: datetime = Field(..., description="로그 발생 시각 (UTC)")
    level: str = Field(..., description="로그 레벨 (ERROR, WARN, INFO 등)")
    message: str = Field(..., description="로그 메시지")
    service_name: str = Field(..., description="서비스 이름")
    similarity_score: float = Field(..., description="질문과의 유사도 점수 (0.0~1.0, 높을수록 관련성 높음)", ge=0.0, le=1.0)


class ChatResponse(BaseModel):
    """Chatbot response"""

    answer: str = Field(..., description="질문에 대한 AI 답변 (한국어)")
    from_cache: bool = Field(..., description="QA 캐시에서 가져왔는지 여부 (True: 캐시 재사용, False: 새로 생성)")
    related_logs: List[RelatedLog] = Field(
        default_factory=list, description="답변 생성에 사용된 관련 로그 목록 (최대 5개, 유사도 높은 순)"
    )
    answered_at: datetime = Field(default_factory=datetime.utcnow, description="응답 생성 시각 (UTC)")

    class Config:
        json_schema_extra = {
            "example": {
                "answer": "최근 24시간 동안 user-service에서 5건의 에러가 발생했습니다. 주요 원인은 NullPointerException(3건)과 DatabaseConnectionException(2건)입니다.",
                "from_cache": False,
                "related_logs": [
                    {
                        "log_id": 12345,
                        "timestamp": "2024-01-15T10:30:00Z",
                        "level": "ERROR",
                        "message": "NullPointerException in UserService",
                        "service_name": "user-service",
                        "similarity_score": 0.92,
                    }
                ],
                "answered_at": "2024-01-15T10:35:00.000Z",
            }
        }
