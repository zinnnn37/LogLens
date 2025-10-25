"""
Log analysis models
"""

from pydantic import BaseModel, Field
from typing import List, Optional
from datetime import datetime


class LogAnalysisResult(BaseModel):
    """Result of AI log analysis"""

    summary: str = Field(..., description="Brief summary of the log")
    error_cause: Optional[str] = Field(None, description="Root cause of error (if applicable)")
    solution: Optional[str] = Field(None, description="Suggested solution")
    tags: List[str] = Field(default_factory=list, description="Relevant tags")
    analyzed_at: datetime = Field(default_factory=datetime.utcnow, description="Analysis timestamp")

    class Config:
        json_schema_extra = {
            "example": {
                "summary": "NullPointerException in UserService.getUser() method",
                "error_cause": "User object was null when accessing getName() method",
                "solution": "Add null check before accessing user object properties",
                "tags": ["NullPointerException", "UserService", "critical"],
                "analyzed_at": "2024-01-15T10:35:00.000Z",
            }
        }


class LogAnalysisResponse(BaseModel):
    """API response for log analysis"""

    log_id: str = Field(..., description="Log ID")
    analysis: LogAnalysisResult = Field(..., description="Analysis result")
    from_cache: bool = Field(..., description="Whether result came from similar log cache")
    similar_log_id: Optional[str] = Field(None, description="ID of similar log (if from cache)")
    similarity_score: Optional[float] = Field(None, description="Similarity score (if from cache)")

    class Config:
        json_schema_extra = {
            "example": {
                "log_id": "550e8400-e29b-41d4-a716-446655440000",
                "analysis": {
                    "summary": "NullPointerException in UserService",
                    "error_cause": "User object was null",
                    "solution": "Add null check",
                    "tags": ["NullPointerException", "critical"],
                    "analyzed_at": "2024-01-15T10:35:00.000Z",
                },
                "from_cache": False,
                "similar_log_id": None,
                "similarity_score": None,
            }
        }
