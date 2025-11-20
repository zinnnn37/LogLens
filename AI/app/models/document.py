"""
HTML Document generation models
"""

from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from datetime import datetime
from enum import Enum
from app.models.experiment import AnalysisMetadata  # Import common metadata model


class DocumentType(str, Enum):
    """Document type enumeration"""

    PROJECT_ANALYSIS = "PROJECT_ANALYSIS"
    ERROR_ANALYSIS = "ERROR_ANALYSIS"


class DocumentFormat(str, Enum):
    """Document format enumeration"""

    HTML = "HTML"
    PDF = "PDF"


class StylePreferences(BaseModel):
    """AI document generation style preferences"""

    css_framework: str = Field(default="tailwind", description="CSS framework (tailwind, bootstrap)")
    chart_library: str = Field(default="chartjs", description="Chart library (chartjs, d3)")
    color_scheme: str = Field(default="blue", description="Color theme (blue, green, red)")


class AiHtmlDocumentRequest(BaseModel):
    """AI HTML document generation request"""

    project_uuid: Optional[str] = Field(None, description="Project UUID")
    log_id: Optional[int] = Field(None, description="Log ID (for error analysis)")
    document_type: DocumentType = Field(..., description="Document type")
    format: DocumentFormat = Field(..., description="Output format")
    data: Dict[str, Any] = Field(..., description="Analysis data (project or error data)")
    options: Optional[Dict[str, Any]] = Field(None, description="Generation options")
    style_preferences: Optional[StylePreferences] = Field(default_factory=StylePreferences, description="Style preferences")
    regeneration_feedback: Optional[List[str]] = Field(None, description="Regeneration feedback (on validation failure)")

    class Config:
        json_schema_extra = {
            "example": {
                "project_uuid": "550e8400-e29b-41d4-a716-446655440000",
                "document_type": "PROJECT_ANALYSIS",
                "format": "HTML",
                "data": {
                    "projectInfo": {
                        "name": "LogLens",
                        "uuid": "550e8400-e29b-41d4-a716-446655440000",
                        "description": "Log analysis platform"
                    },
                    "metrics": {
                        "totalLogs": 15000,
                        "errorCount": 250,
                        "warnCount": 1200
                    }
                },
                "options": {
                    "includeCharts": True,
                    "darkMode": False
                },
                "style_preferences": {
                    "css_framework": "tailwind",
                    "chart_library": "chartjs",
                    "color_scheme": "blue"
                }
            }
        }


class AiDocumentMetadata(BaseModel):
    """AI generated document metadata"""

    word_count: Optional[int] = Field(None, description="Word count")
    estimated_reading_time: Optional[str] = Field(None, description="Estimated reading time")
    sections_generated: Optional[List[str]] = Field(None, description="List of generated sections")
    charts_included: Optional[List[str]] = Field(None, description="List of included charts")
    css_libraries_used: Optional[List[str]] = Field(None, description="CSS libraries used")
    js_libraries_used: Optional[List[str]] = Field(None, description="JS libraries used")
    generation_time: Optional[float] = Field(None, description="Generation time (seconds)")

    # Project analysis specific
    health_score: Optional[int] = Field(None, description="Health score (project analysis)")
    critical_issues: Optional[int] = Field(None, description="Critical issues count")
    total_issues: Optional[int] = Field(None, description="Total issues count")
    recommendations: Optional[int] = Field(None, description="Recommendations count")

    # Error analysis specific
    severity: Optional[str] = Field(None, description="Error severity (error analysis)")
    root_cause: Optional[str] = Field(None, description="Root cause (error analysis)")
    affected_users: Optional[int] = Field(None, description="Affected users count (error analysis)")

    # AI-generated insights
    ai_insights: Optional[Dict[str, str]] = Field(None, description="AI-generated insights and analysis")

    # V2 추가: 분석 메타데이터 (RAG 검증용)
    analysis_metadata: Optional[AnalysisMetadata] = Field(
        None,
        description="""문서 생성에 사용된 데이터 출처 및 샘플링 정보 (V2 전용)
        - 생성 시간, 데이터 범위
        - 분석된 로그 수 (ERROR/WARN/INFO별)
        - 샘플링 전략 상세
        - 기능 제약사항 (Vector 검색은 ERROR만 지원)"""
    )


class AiValidationStatus(BaseModel):
    """AI self-validation status"""

    is_valid_html: bool = Field(default=True, description="Whether HTML is valid")
    has_required_sections: bool = Field(default=True, description="Whether required sections are included")
    warnings: List[str] = Field(default_factory=list, description="Warning messages")


class AiHtmlDocumentResponse(BaseModel):
    """AI HTML document generation response"""

    html_content: str = Field(..., description="Generated HTML content")
    metadata: AiDocumentMetadata = Field(..., description="Document metadata")
    validation_status: AiValidationStatus = Field(..., description="Validation status")

    class Config:
        json_schema_extra = {
            "example": {
                "html_content": "<!DOCTYPE html><html><head>...</head><body>...</body></html>",
                "metadata": {
                    "word_count": 1500,
                    "estimated_reading_time": "7 minutes",
                    "sections_generated": ["overview", "metrics", "errors", "recommendations"],
                    "charts_included": ["error_trend", "level_distribution"],
                    "css_libraries_used": ["tailwindcss"],
                    "js_libraries_used": ["chart.js"],
                    "generation_time": 2.5,
                    "health_score": 75,
                    "critical_issues": 3,
                    "total_issues": 25,
                    "recommendations": 8
                },
                "validation_status": {
                    "is_valid_html": True,
                    "has_required_sections": True,
                    "warnings": []
                }
            }
        }
