"""
Log analysis endpoints
"""

from fastapi import APIRouter, HTTPException
from app.services.log_analysis_service import log_analysis_service
from app.models.analysis import LogAnalysisResponse

router = APIRouter()


@router.get("/logs/{log_id}/analysis", response_model=LogAnalysisResponse)
async def analyze_log(log_id: str):
    """
    Analyze a specific log using AI

    This endpoint:
    1. Checks if the log already has analysis
    2. Searches for similar logs (cosine similarity >= 0.8)
    3. Reuses analysis from similar log if found (saves ~80% cost)
    4. Otherwise, analyzes with GPT-4o and caches the result

    Args:
        log_id: Unique log ID

    Returns:
        LogAnalysisResponse with analysis result
    """
    try:
        result = await log_analysis_service.analyze_log(log_id)
        return result
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Analysis failed: {str(e)}")
