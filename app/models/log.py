"""
Log data models
"""

from pydantic import BaseModel, Field
from typing import Optional, Dict, Any
from datetime import datetime
from enum import Enum


class LogLevel(str, Enum):
    """Log level enumeration (INFO/WARN/ERROR only)"""

    INFO = "INFO"
    WARN = "WARN"
    ERROR = "ERROR"


class SourceType(str, Enum):
    """Source type enumeration"""

    FE = "FE"
    BE = "BE"
    INFRA = "INFRA"


class LayerType(str, Enum):
    """Application layer enumeration"""

    CONTROLLER = "Controller"
    SERVICE = "Service"
    REPOSITORY = "Repository"
    FILTER = "Filter"
    UTIL = "Util"
    OTHER = "Other"


class LogDetails(BaseModel):
    """Detailed log information (from log_details table)"""

    exception_type: Optional[str] = Field(None, description="Exception class name")
    execution_time: Optional[int] = Field(None, description="Execution time in milliseconds")
    stack_trace: Optional[str] = Field(None, description="Stack trace")

    # HTTP request/response details
    http_method: Optional[str] = Field(None, description="HTTP method (GET/POST/PUT/DELETE)")
    request_uri: Optional[str] = Field(None, description="Request URI")
    request_headers: Optional[Dict[str, Any]] = Field(None, description="Request headers (JSON)")
    request_body: Optional[Dict[str, Any]] = Field(None, description="Request body (JSON)")
    response_status: Optional[int] = Field(None, description="HTTP response status code")
    response_body: Optional[Dict[str, Any]] = Field(None, description="Response body (JSON)")

    # Code location
    class_name: Optional[str] = Field(None, description="Class name")
    method_name: Optional[str] = Field(None, description="Method name")

    # Additional context
    additional_info: Optional[Dict[str, Any]] = Field(None, description="Additional information (JSON)")

    class Config:
        json_schema_extra = {
            "example": {
                "exception_type": "NullPointerException",
                "execution_time": 150,
                "http_method": "POST",
                "request_uri": "/api/users",
                "response_status": 500,
                "class_name": "UserService",
                "method_name": "createUser",
            }
        }


class ApplicationLog(BaseModel):
    """Application log model (unified logs + log_details)"""

    # Core identification
    log_id: int = Field(..., description="Unique log ID (integer from database)")
    project_uuid: str = Field(..., description="Project UUID for multi-tenancy")
    timestamp: datetime = Field(default_factory=datetime.utcnow, description="Log timestamp")

    # Service and logging context
    service_name: str = Field(..., description="Service name")
    logger: Optional[str] = Field(None, description="Logger name (e.g., com.example.UserService)")
    source_type: Optional[SourceType] = Field(None, description="Source type (FE/BE/INFRA)")
    layer: Optional[LayerType] = Field(None, description="Application layer (Controller/Service/Repository)")

    # Log content
    level: LogLevel = Field(..., description="Log level")
    message: str = Field(..., description="Log message")
    comment: Optional[str] = Field(None, description="Additional comment")

    # Method and thread info (backward compatibility - kept at top level)
    method_name: Optional[str] = Field(None, description="Method name")
    class_name: Optional[str] = Field(None, description="Class name")
    thread_name: Optional[str] = Field(None, description="Thread name")

    # Tracing
    trace_id: Optional[str] = Field(None, description="Trace ID for distributed tracing")

    # Requester identification (DevSecOps)
    requester_ip: Optional[str] = Field(None, description="Requester IP address for user identification")

    # Performance
    duration: Optional[int] = Field(None, description="Execution duration in ms")

    # Error info (backward compatibility)
    stack_trace: Optional[str] = Field(None, description="Stack trace for errors")

    # Detailed log information (nested object from log_details table)
    log_details: Optional[LogDetails] = Field(None, description="Detailed log information")

    # Additional context (backward compatibility)
    parameters: Optional[Dict[str, Any]] = Field(None, description="Method parameters")
    result: Optional[Any] = Field(None, description="Method result")
    additional_context: Optional[Dict[str, Any]] = Field(None, description="Additional context")

    class Config:
        json_schema_extra = {
            "example": {
                "log_id": 12345,
                "project_uuid": "550e8400-e29b-41d4-a716-446655440000",
                "timestamp": "2024-01-15T10:30:00.000Z",
                "service_name": "user-service",
                "logger": "com.example.user.UserService",
                "source_type": "BE",
                "layer": "Service",
                "level": "ERROR",
                "message": "Failed to process user request",
                "comment": "Null pointer exception occurred",
                "method_name": "processUser",
                "class_name": "UserService",
                "thread_name": "http-nio-8080-exec-1",
                "trace_id": "abc123",
                "requester_ip": "203.0.113.45",
                "duration": 150,
                "stack_trace": "java.lang.NullPointerException: ...",
                "log_details": {
                    "exception_type": "NullPointerException",
                    "execution_time": 150,
                    "http_method": "POST",
                    "request_uri": "/api/users",
                    "response_status": 500,
                    "class_name": "UserService",
                    "method_name": "processUser"
                }
            }
        }
