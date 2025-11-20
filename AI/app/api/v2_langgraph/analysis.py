"""
V2 LangGraph API - HTML Document Analysis Endpoints

HTML 문서 생성 API (프로젝트 분석, 에러 분석)
"""

from fastapi import APIRouter, HTTPException, Body
from typing import Dict, Any

from app.services.html_document_service import html_document_service
from app.models.document import (
    AiHtmlDocumentRequest,
    AiHtmlDocumentResponse,
)


router = APIRouter(prefix="/v2-langgraph/analysis", tags=["Analysis Documents V2"])


@router.post("/projects/html-document", response_model=AiHtmlDocumentResponse)
async def generate_project_analysis_html(
    request: AiHtmlDocumentRequest = Body(
        ...,
        example={
            "project_uuid": "550e8400-e29b-41d4-a716-446655440000",
            "document_type": "PROJECT_ANALYSIS",
            "format": "HTML",
            "data": {
                "projectInfo": {
                    "name": "LogLens",
                    "uuid": "550e8400-e29b-41d4-a716-446655440000",
                    "description": "실시간 로그 분석 플랫폼"
                },
                "timeRange": {
                    "startTime": "2024-01-01T00:00:00",
                    "endTime": "2024-01-31T23:59:59"
                },
                "metrics": {
                    "totalLogs": 15000,
                    "errorCount": 250,
                    "warnCount": 1200,
                    "infoCount": 13550,
                    "avgResponseTime": 125.5
                },
                "topErrors": [
                    {
                        "logId": 1,
                        "message": "NullPointerException in UserService.getUser()",
                        "timestamp": "2024-01-15T10:30:00",
                        "componentName": "UserService",
                        "logLevel": "ERROR",
                        "traceId": "abc123"
                    },
                    {
                        "logId": 2,
                        "message": "Database connection timeout",
                        "timestamp": "2024-01-16T14:20:00",
                        "componentName": "DatabasePool",
                        "logLevel": "ERROR",
                        "traceId": "def456"
                    }
                ]
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
    ),
) -> AiHtmlDocumentResponse:
    """
    프로젝트 분석 HTML 문서 생성

    **주요 기능:**
    - Jinja2 템플릿 기반 HTML 생성
    - 대시보드 통계 시각화
    - 최근 에러 로그 분석
    - 건강 점수 계산
    - Chart.js를 사용한 차트 렌더링

    **Args:**
    - request: HTML 문서 생성 요청
      - project_uuid: 프로젝트 UUID
      - document_type: PROJECT_ANALYSIS
      - format: HTML
      - data: 프로젝트 분석 데이터
      - options: 생성 옵션 (includeCharts, darkMode 등)
      - style_preferences: 스타일 선호도 (cssFramework, chartLibrary, colorScheme)

    **Returns:**
    - AiHtmlDocumentResponse: 생성된 HTML 문서 및 메타데이터

    **Example Request:**
    ```json
    {
      "project_uuid": "550e8400-e29b-41d4-a716-446655440000",
      "document_type": "PROJECT_ANALYSIS",
      "format": "HTML",
      "data": {
        "projectInfo": {
          "name": "LogLens",
          "uuid": "550e8400-e29b-41d4-a716-446655440000",
          "description": "Log analysis platform"
        },
        "timeRange": {
          "startTime": "2024-01-01T00:00:00",
          "endTime": "2024-01-31T23:59:59"
        },
        "metrics": {
          "totalLogs": 15000,
          "errorCount": 250,
          "warnCount": 1200,
          "infoCount": 13550,
          "avgResponseTime": 125.5
        },
        "topErrors": [...]
      },
      "options": {
        "includeCharts": true,
        "darkMode": false
      },
      "style_preferences": {
        "css_framework": "tailwind",
        "chart_library": "chartjs",
        "color_scheme": "blue"
      }
    }
    ```

    **Example Response:**
    ```json
    {
      "html_content": "<!DOCTYPE html><html>...",
      "metadata": {
        "word_count": 1500,
        "estimated_reading_time": "7 minutes",
        "sections_generated": ["overview", "metrics", "errors"],
        "charts_included": ["error_trend", "level_distribution"],
        "css_libraries_used": ["tailwindcss"],
        "js_libraries_used": ["chart.js"],
        "generation_time": 2.5,
        "health_score": 75,
        "critical_issues": 250,
        "total_issues": 1450,
        "recommendations": 8
      },
      "validation_status": {
        "is_valid_html": true,
        "has_required_sections": true,
        "warnings": []
      }
    }
    ```

    **Error Responses:**
    - 400: Invalid request data
    - 500: HTML generation failed
    """
    try:
        # Validate document type
        if request.document_type.value != "PROJECT_ANALYSIS":
            raise HTTPException(
                status_code=400,
                detail=f"Invalid document type for this endpoint: {request.document_type}. Expected PROJECT_ANALYSIS.",
            )

        # Generate HTML document
        result = await html_document_service.generate_html_document(request)
        return result

    except HTTPException:
        raise

    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Failed to generate project analysis HTML: {str(e)}",
        )


@router.post("/errors/html-document", response_model=AiHtmlDocumentResponse)
async def generate_error_analysis_html(
    request: AiHtmlDocumentRequest = Body(
        ...,
        example={
            "log_id": 12345,
            "project_uuid": "550e8400-e29b-41d4-a716-446655440000",
            "document_type": "ERROR_ANALYSIS",
            "format": "HTML",
            "data": {
                "errorLog": {
                    "logId": 12345,
                    "level": "ERROR",
                    "message": "NullPointerException in UserService.getUser()",
                    "stackTrace": "java.lang.NullPointerException: Cannot invoke method getEmail() on null object\\n\\tat com.example.service.UserService.getUser(UserService.java:45)\\n\\tat com.example.controller.UserController.getUserInfo(UserController.java:28)",
                    "timestamp": "2024-01-15T10:35:00.123Z",
                    "componentName": "UserService",
                    "traceId": "abc123-def456-ghi789"
                },
                "existingAnalysis": {
                    "summary": "UserService의 getUser() 메서드에서 NullPointerException이 발생했습니다.",
                    "errorCause": "user_id=12345에 해당하는 User 객체가 null입니다.",
                    "solution": "[즉시] null 체크 추가\\n[단기] Optional 사용\\n[장기] 검증 로직 추가",
                    "tags": ["SEVERITY_HIGH", "NullPointerException", "UserService"]
                },
                "relatedLogs": [
                    {
                        "logId": 12346,
                        "message": "User not found in database",
                        "timestamp": "2024-01-15T10:34:59.998Z",
                        "componentName": "UserRepository",
                        "logLevel": "WARN"
                    },
                    {
                        "logId": 12347,
                        "message": "Cache miss for user_id=12345",
                        "timestamp": "2024-01-15T10:34:59.500Z",
                        "componentName": "UserCache",
                        "logLevel": "INFO"
                    }
                ]
            },
            "options": {
                "includeRelatedLogs": True,
                "includeImpactAnalysis": True
            },
            "style_preferences": {
                "css_framework": "tailwind",
                "chart_library": "chartjs",
                "color_scheme": "blue"
            }
        }
    ),
) -> AiHtmlDocumentResponse:
    """
    에러 분석 HTML 문서 생성

    **주요 기능:**
    - Jinja2 템플릿 기반 HTML 생성
    - AI 분석 결과 시각화
    - 스택 트레이스 포맷팅
    - 관련 로그 표시
    - 영향 분석 및 권장사항

    **Args:**
    - request: HTML 문서 생성 요청
      - log_id: 로그 ID
      - project_uuid: 프로젝트 UUID
      - document_type: ERROR_ANALYSIS
      - format: HTML
      - data: 에러 분석 데이터
      - options: 생성 옵션 (includeRelatedLogs, includeImpactAnalysis 등)
      - style_preferences: 스타일 선호도

    **Returns:**
    - AiHtmlDocumentResponse: 생성된 HTML 문서 및 메타데이터

    **Example Request:**
    ```json
    {
      "log_id": 12345,
      "project_uuid": "550e8400-e29b-41d4-a716-446655440000",
      "document_type": "ERROR_ANALYSIS",
      "format": "HTML",
      "data": {
        "errorLog": {
          "logId": 12345,
          "level": "ERROR",
          "message": "NullPointerException in UserService",
          "stackTrace": "...",
          "timestamp": "2024-01-15T10:35:00",
          "componentName": "UserService",
          "traceId": "abc123"
        },
        "existingAnalysis": {
          "summary": "UserService의 getUser() 메서드에서 NPE 발생",
          "errorCause": "user_id에 해당하는 User 객체가 null",
          "solution": "[즉시] null 체크 추가...",
          "tags": ["SEVERITY_HIGH", "NullPointerException"]
        },
        "relatedLogs": [...]
      },
      "options": {
        "includeRelatedLogs": true,
        "includeImpactAnalysis": true
      }
    }
    ```

    **Example Response:**
    ```json
    {
      "html_content": "<!DOCTYPE html><html>...",
      "metadata": {
        "word_count": 800,
        "estimated_reading_time": "4 minutes",
        "sections_generated": ["overview", "analysis", "impact"],
        "charts_included": [],
        "css_libraries_used": ["tailwindcss"],
        "js_libraries_used": [],
        "generation_time": 1.2,
        "severity": "HIGH",
        "root_cause": "user_id에 해당하는 User 객체가 null",
        "affected_users": 5
      },
      "validation_status": {
        "is_valid_html": true,
        "has_required_sections": true,
        "warnings": []
      }
    }
    ```

    **Error Responses:**
    - 400: Invalid request data
    - 500: HTML generation failed
    """
    try:
        # Validate document type
        if request.document_type.value != "ERROR_ANALYSIS":
            raise HTTPException(
                status_code=400,
                detail=f"Invalid document type for this endpoint: {request.document_type}. Expected ERROR_ANALYSIS.",
            )

        # Generate HTML document
        result = await html_document_service.generate_html_document(request)
        return result

    except HTTPException:
        raise

    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=f"Failed to generate error analysis HTML: {str(e)}",
        )
