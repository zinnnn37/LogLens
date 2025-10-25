"""
Chatbot models
"""

from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from datetime import datetime


class ChatRequest(BaseModel):
    """Chatbot question request"""

    question: str = Field(..., description="User's question about logs")
    filters: Optional[Dict[str, Any]] = Field(None, description="Optional filters for log search")
    time_range: Optional[Dict[str, str]] = Field(
        None, description="Time range filter (start, end)"
    )

    class Config:
        json_schema_extra = {
            "example": {
                "question": "최근에 발생한 에러들을 요약해줘",
                "filters": {"level": "ERROR", "service_name": "user-service"},
                "time_range": {"start": "2024-01-15T00:00:00Z", "end": "2024-01-15T23:59:59Z"},
            }
        }


class RelatedLog(BaseModel):
    """Related log information"""

    log_id: str = Field(..., description="Log ID")
    timestamp: datetime = Field(..., description="Log timestamp")
    level: str = Field(..., description="Log level")
    message: str = Field(..., description="Log message")
    service_name: str = Field(..., description="Service name")
    similarity_score: float = Field(..., description="Similarity score to the question")


class ChatResponse(BaseModel):
    """Chatbot response"""

    answer: str = Field(..., description="Answer to the user's question")
    from_cache: bool = Field(..., description="Whether answer came from QA cache")
    related_logs: List[RelatedLog] = Field(
        default_factory=list, description="Related logs used for answer"
    )
    answered_at: datetime = Field(default_factory=datetime.utcnow, description="Response timestamp")

    class Config:
        json_schema_extra = {
            "example": {
                "answer": "최근 24시간 동안 user-service에서 5건의 에러가 발생했습니다. 주요 원인은 NullPointerException(3건)과 DatabaseConnectionException(2건)입니다.",
                "from_cache": False,
                "related_logs": [
                    {
                        "log_id": "550e8400-e29b-41d4-a716-446655440000",
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
