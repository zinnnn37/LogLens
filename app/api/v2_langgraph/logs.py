"""
V2 LangGraph API - Log Analysis Endpoints

LangGraph 기반 로그 분석 API (V2)
"""

from fastapi import APIRouter, HTTPException, Query
from typing import Optional

from app.services.log_analysis_service_v2 import log_analysis_service_v2
from app.models.analysis import LogAnalysisResponse


router = APIRouter(prefix="/v2-langgraph", tags=["Log Analysis V2 (LangGraph)"])


@router.get("/logs/{log_id}/analysis", response_model=LogAnalysisResponse)
async def analyze_log_v2(
    log_id: int,
    project_uuid: str = Query(
        ...,
        description="Project UUID for multi-tenancy"
    ),
) -> LogAnalysisResponse:
    """
    로그 분석 V2 (LangGraph Edition)

    **주요 기능:**
    - 3-tier 캐시 전략 (Direct → Trace → Similarity)
    - 동적 분석 전략 선택 (Single / Direct / Map-Reduce)
    - 한국어 출력 검증
    - 품질 검증 (ValidationPipeline)
    - Trace 전파 (같은 trace의 모든 로그에 분석 결과 저장)

    **V1과의 차이점:**
    - LangGraph StateGraph를 사용한 구조화된 워크플로우
    - 더 명확한 캐시 체크 순서 (3단계)
    - 분석 전략 자동 선택 (로그 개수에 따라)
    - 검증 로직 분리 및 재시도 메커니즘
    - 성능 메트릭 추적 (duration, llm_call_count 등)

    **Args:**
    - log_id: 로그 ID (정수)
    - project_uuid: 프로젝트 UUID (쿼리 파라미터)

    **Returns:**
    - LogAnalysisResponse: 분석 결과

    **Example Response:**
    ```json
    {
      "log_id": 12345,
      "analysis": {
        "summary": "**NullPointerException**이 `UserService.getUser()` 메서드에서 발생",
        "error_cause": "`user_id`에 해당하는 User 객체가 DB에서 조회되지 않아...",
        "solution": "### 즉시 조치 (완료 예상: 1시간)\\n- [ ] null 체크 추가...",
        "tags": ["SEVERITY_HIGH", "NullPointerException", "UserService"]
      },
      "from_cache": false,
      "similar_log_id": null,
      "similarity_score": null
    }
    ```

    **Error Responses:**
    - 404: 로그를 찾을 수 없음
    - 500: 분석 중 오류 발생
    """
    try:
        result = await log_analysis_service_v2.analyze_log(
            log_id=log_id,
            project_uuid=project_uuid
        )
        return result

    except ValueError as e:
        # 로그 없음 또는 분석 실패
        error_message = str(e)
        if "not found" in error_message.lower():
            raise HTTPException(status_code=404, detail=error_message)
        else:
            raise HTTPException(status_code=500, detail=error_message)

    except Exception as e:
        # 예상치 못한 에러
        raise HTTPException(
            status_code=500,
            detail=f"Internal server error during log analysis: {str(e)}"
        )
