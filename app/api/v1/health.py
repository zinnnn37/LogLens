"""
Health check endpoint
"""

from fastapi import APIRouter
from app.core.config import settings
from app.core.opensearch import opensearch_client

router = APIRouter()


@router.get(
    "/health",
    summary="헬스체크",
    description="""
    애플리케이션 상태 및 외부 서비스 연결 상태를 확인합니다.

    **확인 항목:**
    - 애플리케이션 기본 정보 (이름, 버전, 환경)
    - OpenSearch 클러스터 연결 상태

    **응답 예시:**
    ```json
    {
      "status": "ok",
      "app_name": "log-analysis-api",
      "version": "1.0.0",
      "environment": "development",
      "services": {
        "opensearch": "healthy"
      }
    }
    ```
    """,
    responses={
        200: {
            "description": "정상 - 애플리케이션 및 모든 서비스가 정상 작동 중",
            "content": {
                "application/json": {
                    "example": {
                        "status": "ok",
                        "app_name": "log-analysis-api",
                        "version": "1.0.0",
                        "environment": "development",
                        "services": {
                            "opensearch": "healthy"
                        }
                    }
                }
            }
        },
        500: {
            "description": "서비스 오류 - OpenSearch 연결 실패 등 외부 서비스 문제",
            "content": {
                "application/json": {
                    "example": {
                        "status": "ok",
                        "app_name": "log-analysis-api",
                        "version": "1.0.0",
                        "environment": "development",
                        "services": {
                            "opensearch": "unhealthy: Connection refused"
                        }
                    }
                }
            }
        }
    }
)
async def health_check():
    """헬스체크 엔드포인트"""
    # Check OpenSearch
    opensearch_status = "healthy"
    try:
        opensearch_client.cluster.health()
    except Exception as e:
        opensearch_status = f"unhealthy: {str(e)}"

    return {
        "status": "ok",
        "app_name": settings.APP_NAME,
        "version": settings.APP_VERSION,
        "environment": settings.ENVIRONMENT,
        "services": {
            "opensearch": opensearch_status,
        },
    }
