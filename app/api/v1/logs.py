"""
Log analysis endpoints
"""

from fastapi import APIRouter, HTTPException, Query, Path
from app.services.log_analysis_service import log_analysis_service
from app.models.analysis import LogAnalysisResponse

router = APIRouter()


@router.get(
    "/logs/{log_id}/analysis",
    response_model=LogAnalysisResponse,
    summary="로그 분석",
    description="""
    AI를 활용하여 특정 로그를 분석하고 근본 원인(RCA)을 파악합니다.

    ## 분석 프로세스

    1. **멀티테넌시 필터링**: project_uuid로 로그 격리
    2. **기존 분석 확인**: 이미 분석된 로그인지 확인 (캐시 재사용)
    3. **Trace 기반 분석**: trace_id가 있으면 관련 로그 수집 (±3초, 최대 100개)
    4. **유사도 검색**: Vector 검색으로 유사 로그 확인 (코사인 유사도 ≥ 0.8)
    5. **캐시 재사용**: 유사 로그의 분석 결과 재사용 (약 80% 비용 절감)
    6. **새 분석**: 캐시가 없으면 LLM으로 분석 후 저장

    ## 분석 결과 (한국어)

    - **요약**: 무엇이 발생했는지 1-2문장 설명
    - **근본 원인**: 에러가 발생한 근본적인 이유
    - **해결 방안**: 우선순위별 해결책 ([즉시], [단기], [장기])
    - **태그**: 심각도, 에러 타입, 컴포넌트

    ## 캐싱 전략

    - **Trace 캐싱**: 같은 trace 내 로그는 한 번만 분석 (97-99% 절감)
    - **유사도 캐싱**: 유사한 로그의 분석 재사용 (80% 절감)
    """,
    responses={
        200: {
            "description": "분석 성공",
            "content": {
                "application/json": {
                    "example": {
                        "log_id": 12345,
                        "analysis": {
                            "summary": "UserService의 getUser() 메서드에서 NullPointerException 발생",
                            "error_cause": "user_id=12345에 해당하는 User 객체가 DB에서 조회되지 않아 null이 반환되었으며, 이를 검증 없이 사용하려다 예외 발생",
                            "solution": "[즉시] UserService.getUser() 호출 전 null 체크 추가\n[단기] Optional<User> 반환 타입으로 변경\n[장기] API 레이어에서 유효성 검증 추가",
                            "tags": ["SEVERITY_HIGH", "NullPointerException", "UserService", "DataIntegrity"],
                            "analysis_type": "TRACE_BASED",
                            "target_type": "LOG",
                            "analyzed_at": "2024-01-15T10:30:00Z"
                        },
                        "from_cache": False,
                        "similar_log_id": None,
                        "similarity_score": None
                    }
                }
            }
        },
        404: {
            "description": "로그를 찾을 수 없음 - log_id 또는 project_uuid가 잘못되었거나 해당 로그가 존재하지 않음",
            "content": {
                "application/json": {
                    "example": {
                        "detail": "Log not found: 12345 for project: 550e8400-e29b-41d4-a716-446655440000"
                    }
                }
            }
        },
        500: {
            "description": "분석 실패 - LLM 호출 오류, OpenSearch 연결 문제 등",
            "content": {
                "application/json": {
                    "example": {
                        "detail": "Analysis failed: OpenAI API error"
                    }
                }
            }
        }
    }
)
async def analyze_log(
    log_id: int = Path(..., description="분석할 로그 ID (양의 정수)", ge=1),
    project_uuid: str = Query(
        ...,
        description="프로젝트 UUID (멀티테넌시 격리용, 예: test-project 또는 550e8400-e29b-41d4-a716-446655440000)"
    ),
):
    """로그 AI 분석"""
    try:
        result = await log_analysis_service.analyze_log(log_id, project_uuid)
        return result
    except ValueError as e:
        raise HTTPException(status_code=404, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Analysis failed: {str(e)}")
