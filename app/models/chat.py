"""
Chatbot models
"""

from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from datetime import datetime


class ChatMessage(BaseModel):
    """Single chat message for history"""

    role: str = Field(..., description="역할 ('user': 사용자, 'assistant': AI 챗봇)")
    content: str = Field(..., description="메시지 내용")

    class Config:
        json_schema_extra = {
            "example": {
                "role": "user",
                "content": "최근 에러 알려줘"
            }
        }


class ChatRequest(BaseModel):
    """Chatbot question request with history support"""

    question: str = Field(..., description="로그에 대한 사용자 질문 (자연어)")
    project_uuid: str = Field(
        ...,
        description="프로젝트 UUID (멀티테넌시 격리, 예: test-project 또는 550e8400-e29b-41d4-a716-446655440000)"
    )
    chat_history: Optional[List[ChatMessage]] = Field(
        default=None,
        description="이전 대화 히스토리 (최대 1500 토큰까지 사용, 초과 시 자동 압축)"
    )
    filters: Optional[Dict[str, Any]] = Field(
        None,
        description="""🆕 **자동 추출됨!** (선택사항, 일반적으로 전달 불필요)

question에서 자동으로 필터 조건을 추출합니다. 명시적으로 전달하면 자동 추출을 건너뜁니다.

**자동 추출되는 필터**:
- level: ERROR, WARN, INFO (예: "에러 로그" → {level: "ERROR"})
- service_name: 서비스명 (예: "user-service 로그" → {service_name: "user-service"})
- source_type: FE, BE (예: "프론트엔드 에러" → {source_type: "FE"})
- ip: IP 주소 (예: "192.168.1.100 로그" → {ip: "192.168.1.100"})

**직접 지정 시 가능한 필드**: level, service_name, class_name, method_name, source_type, layer, ip"""
    )
    time_range: Optional[Dict[str, str]] = Field(
        None,
        description="""🆕 **자동 추출됨!** (선택사항, 기본값: 최근 7일)

question에서 시간 표현을 자동으로 파싱합니다. 명시하지 않으면 최근 7일이 기본값입니다.

**자동 추출 예시**:
- "최근 1시간" → 현재부터 1시간 전
- "오늘" → 오늘 00:00 ~ 현재
- "어제" → 어제 00:00 ~ 23:59
- "2024-01-15" → 해당 날짜 전체

**직접 지정 시 형식**: ISO 8601 (YYYY-MM-DDTHH:MM:SSZ)
- 예: {"start": "2024-01-15T00:00:00Z", "end": "2024-01-15T23:59:59Z"}"""
    )

    class Config:
        json_schema_extra = {
            "examples": [
                {
                    "summary": "간단한 질문 (권장) - 자동 필터 추출",
                    "description": "question만 입력하면 필터와 시간 범위가 자동으로 추출됩니다",
                    "value": {
                        "question": "최근 1시간 동안 user-service에서 발생한 ERROR 로그 알려줘",
                        "project_uuid": "test-project"
                        # filters와 time_range는 자동으로 추출됨:
                        # - filters: {"level": "ERROR", "service_name": "user-service"}
                        # - time_range: 최근 1시간
                    }
                },
                {
                    "summary": "대화 히스토리 포함",
                    "description": "이전 대화를 참조하여 후속 질문에 답변",
                    "value": {
                        "question": "그 중 가장 심각한 건?",
                        "project_uuid": "test-project",
                        "chat_history": [
                            {"role": "user", "content": "최근 에러 알려줘"},
                            {"role": "assistant", "content": "NPE 3건, DB 타임아웃 2건 발생했습니다"}
                        ]
                    }
                },
                {
                    "summary": "고급 사용 - 필터 직접 지정 (선택사항)",
                    "description": "자동 추출 대신 필터와 시간 범위를 직접 지정할 수 있습니다",
                    "value": {
                        "question": "이 로그들의 패턴을 분석해줘",
                        "project_uuid": "test-project",
                        "filters": {"level": "ERROR", "service_name": "payment-api"},
                        "time_range": {"start": "2024-01-15T00:00:00Z", "end": "2024-01-15T23:59:59Z"}
                    }
                }
            ]
        }


class RelatedLog(BaseModel):
    """Related log information"""

    log_id: int = Field(..., description="로그 ID (양의 정수)")
    timestamp: datetime = Field(..., description="로그 발생 시각 (UTC)")
    level: str = Field(..., description="로그 레벨 (ERROR, WARN, INFO 등)")
    message: str = Field(..., description="로그 메시지")
    service_name: str = Field(..., description="서비스 이름")
    similarity_score: float = Field(..., description="질문과의 유사도 점수 (0.0~1.0, 높을수록 관련성 높음)", ge=0.0, le=1.0)


class ChatResponse(BaseModel):
    """Chatbot response"""

    answer: str = Field(..., description="질문에 대한 AI 답변 (한국어)")
    from_cache: bool = Field(..., description="QA 캐시에서 가져왔는지 여부 (True: 캐시 재사용, False: 새로 생성)")
    related_logs: List[RelatedLog] = Field(
        default_factory=list, description="답변 생성에 사용된 관련 로그 목록 (최대 5개, 유사도 높은 순)"
    )
    answered_at: datetime = Field(default_factory=datetime.utcnow, description="응답 생성 시각 (UTC)")

    class Config:
        json_schema_extra = {
            "example": {
                "answer": "최근 24시간 동안 user-service에서 5건의 에러가 발생했습니다. 주요 원인은 NullPointerException(3건)과 DatabaseConnectionException(2건)입니다.",
                "from_cache": False,
                "related_logs": [
                    {
                        "log_id": 12345,
                        "timestamp": "2024-01-15T10:30:00Z",
                        "level": "ERROR",
                        "message": "NullPointerException in UserService",
                        "service_name": "user-service",
                        "similarity_score": 0.92,
                    }
                ],
                "answered_at": "2024-01-15T10:35:00.000Z",
            }
        }
