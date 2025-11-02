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

    summary: str = Field(..., description="Brief summary of the log")
    error_cause: Optional[str] = Field(None, description="Root cause of error (if applicable)")
    solution: Optional[str] = Field(None, description="Suggested solution")
    tags: List[str] = Field(default_factory=list, description="Relevant tags")
    analysis_type: AnalysisType = Field(default=AnalysisType.SINGLE, description="Analysis type (SINGLE/TRACE_BASED)")
    target_type: TargetType = Field(default=TargetType.LOG, description="Target type (LOG/LOG_DETAILS)")
    analyzed_at: datetime = Field(default_factory=datetime.utcnow, description="Analysis timestamp")

    class Config:
        json_schema_extra = {
            "example": {
                "summary": "NullPointerException in UserService.getUser() method",
                "error_cause": "User object was null when accessing getName() method",
                "solution": "Add null check before accessing user object properties",
                "tags": ["NullPointerException", "UserService", "critical"],
                "analysis_type": "TRACE_BASED",
                "target_type": "LOG",
                "analyzed_at": "2024-01-15T10:35:00.000Z",
            }
        }


class LogAnalysisResponse(BaseModel):
    """API response for log analysis"""

    log_id: int = Field(..., description="Log ID (integer)")
    analysis: LogAnalysisResult = Field(..., description="Analysis result")
    from_cache: bool = Field(..., description="Whether result came from similar log cache")
    similar_log_id: Optional[int] = Field(None, description="ID of similar log (if from cache)")
    similarity_score: Optional[float] = Field(None, description="Similarity score (if from cache)")

    class Config:
        json_schema_extra = {
            "example": {
                "log_id": 12345,
                "analysis": {
                    "summary": "NullPointerException in UserService",
                    "error_cause": "User object was null",
                    "solution": "Add null check",
                    "tags": ["NullPointerException", "critical"],
                    "analysis_type": "TRACE_BASED",
                    "target_type": "LOG",
                    "analyzed_at": "2024-01-15T10:35:00.000Z",
                },
                "from_cache": False,
                "similar_log_id": None,
                "similarity_score": None,
            }
        }
