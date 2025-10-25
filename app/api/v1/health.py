"""
Health check endpoint
"""

from fastapi import APIRouter
from app.core.config import settings
from app.core.opensearch import opensearch_client

router = APIRouter()


@router.get("/health")
async def health_check():
    """
    Health check endpoint

    Returns application status and service connectivity
    """
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
