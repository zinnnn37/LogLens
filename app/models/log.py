"""
Log data models
"""

from pydantic import BaseModel, Field
from typing import Optional, Dict, Any
from datetime import datetime
from enum import Enum


class LogLevel(str, Enum):
    """Log level enumeration"""

    TRACE = "TRACE"
    DEBUG = "DEBUG"
    INFO = "INFO"
    WARN = "WARN"
    ERROR = "ERROR"
    FATAL = "FATAL"


class ApplicationLog(BaseModel):
    """Application log model"""

    log_id: str = Field(..., description="Unique log ID")
    timestamp: datetime = Field(default_factory=datetime.utcnow, description="Log timestamp")
    service_name: str = Field(..., description="Service name")
    level: LogLevel = Field(..., description="Log level")
    message: str = Field(..., description="Log message")
    method_name: Optional[str] = Field(None, description="Method name")
    class_name: Optional[str] = Field(None, description="Class name")
    thread_name: Optional[str] = Field(None, description="Thread name")
    trace_id: Optional[str] = Field(None, description="Trace ID for distributed tracing")
    user_id: Optional[str] = Field(None, description="User ID if available")
    duration: Optional[int] = Field(None, description="Execution duration in ms")
    parameters: Optional[Dict[str, Any]] = Field(None, description="Method parameters")
    result: Optional[Any] = Field(None, description="Method result")
    stack_trace: Optional[str] = Field(None, description="Stack trace for errors")
    additional_context: Optional[Dict[str, Any]] = Field(None, description="Additional context")

    class Config:
        json_schema_extra = {
            "example": {
                "log_id": "550e8400-e29b-41d4-a716-446655440000",
                "timestamp": "2024-01-15T10:30:00.000Z",
                "service_name": "user-service",
                "level": "ERROR",
                "message": "Failed to process user request",
                "method_name": "processUser",
                "class_name": "UserService",
                "thread_name": "http-nio-8080-exec-1",
                "trace_id": "abc123",
                "user_id": "user_456",
                "duration": 150,
                "stack_trace": "java.lang.RuntimeException: ...",
            }
        }
