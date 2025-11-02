"""
Log analysis endpoints
"""

from fastapi import APIRouter, HTTPException, Query
from app.services.log_analysis_service import log_analysis_service
from app.models.analysis import LogAnalysisResponse

router = APIRouter()


@router.get("/logs/{log_id}/analysis", response_model=LogAnalysisResponse)
async def analyze_log(
    log_id: int,
    project_id: str = Query(..., description="Project ID for multi-tenancy (UUID)"),
):
    """
    Analyze a specific log using AI

    This endpoint:
    1. Filters logs by project_id (multi-tenancy)
    2. Checks if the log already has analysis
    3. If trace_id exists, collects related logs (±3 seconds, max 100)
    4. Checks for similar logs (cosine similarity >= 0.8)
    5. Reuses analysis from similar log if found (saves ~80% cost)
    6. Otherwise, analyzes with LLM and caches the result

    Args:
        log_id: Unique log ID
        project_id: Project ID for data isolation

    Returns:
        LogAnalysisResponse with analysis result
    """
    try:
        result = await log_analysis_service.analyze_log(log_id, project_id)
        return result
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Analysis failed: {str(e)}")
