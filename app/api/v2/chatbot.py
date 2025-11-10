"""
Chatbot V2 endpoints - ReAct Agent 기반

LLM이 자율적으로 도구를 선택하여 로그 분석
"""

from fastapi import APIRouter, HTTPException, BackgroundTasks
from fastapi.responses import StreamingResponse
import logging
import re
import json

from app.services.chatbot_service_v2 import chatbot_service_v2
from app.models.chat import ChatRequest, ChatResponse
from app.agents.chatbot_agent import create_log_analysis_agent

router = APIRouter()
logger = logging.getLogger(__name__)


@router.post(
    "/chatbot/ask",
    response_model=ChatResponse,
    summary="챗봇 질문 (V2 - Agent 기반)",
    description="""
    **ReAct Agent 기반 로그 분석 챗봇 (V2)**

    LLM이 **자율적으로 판단**하여 필요한 도구를 선택하고 로그를 분석합니다.

    ## 🆕 V2의 주요 개선점

    ### 1. 자율적 도구 선택 (Autonomous Tool Use)
    - LLM이 질문을 분석하고 **스스로 필요한 도구를 결정**
    - 사용자가 필터를 명시하지 않아도 OK
    - 예: "시스템 상태 요약" → 자동으로 통계 도구 선택

    ### 2. 다양한 검색 전략
    V1은 Vector 검색만 가능했지만, V2는 6가지 도구 사용:

    **검색 도구:**
    - `search_logs_by_keyword`: 키워드 검색
    - `search_logs_by_similarity`: 의미 유사도 검색

    **분석 도구:**
    - `get_log_statistics`: 로그 통계 (레벨별/서비스별 집계)
    - `get_recent_errors`: 최근 에러 목록

    **상세 조회:**
    - `get_log_detail`: 특정 로그 상세 정보
    - `get_logs_by_trace_id`: trace_id로 연관 로그 추적

    ### 3. 반복적 추론 (Multi-step Reasoning)
    - LLM이 필요하면 여러 도구를 **연속으로 사용** (최대 5회)
    - 예: 통계 확인 → 에러 목록 조회 → 특정 에러 상세 분석

    ### 4. 불필요한 검색 방지
    - 인사말이나 간단한 질문에는 **DB 조회 없이 바로 답변**
    - 비용 절감 및 응답 속도 향상

    ## 예시 시나리오

    ### 시나리오 1: "시스템 전체 상태 요약해줘"
    ```
    Thought: 전체 통계가 필요하다
    Action: get_log_statistics(time_hours=24)
    Observation: ERROR 15건, WARN 32건, INFO 1523건
    Thought: 최근 에러를 확인해야겠다
    Action: get_recent_errors(limit=5)
    Observation: NullPointerException 8건, DatabaseTimeout 7건
    Final Answer: 최근 24시간 동안 ERROR 15건 발생. 주요 에러는 NPE(8건)...
    ```

    ### 시나리오 2: "NullPointerException 어디서 발생했어?"
    ```
    Thought: 키워드로 검색해야겠다
    Action: search_logs_by_keyword(keyword="NullPointerException", level="ERROR")
    Observation: 8건 발견, user-service에서 6건, payment-api에서 2건
    Thought: 가장 최근 것의 상세를 봐야겠다
    Action: get_log_detail(log_id=12345)
    Final Answer: user-service의 UserController.java:45에서 발생...
    ```

    ### 시나리오 3: "안녕" (불필요한 검색 방지)
    ```
    Thought: 인사말이므로 도구 불필요
    Final Answer: 안녕하세요! 로그 분석이 필요하시면 질문해주세요.
    ```

    ## V1 vs V2 비교

    | 기능 | V1 (Simple Chain) | V2 (ReAct Agent) |
    |------|------------------|------------------|
    | 실행 흐름 | 고정 (항상 Vector 검색) | 동적 (LLM 판단) |
    | 도구 선택 | 수동 (코드 결정) | 자율 (LLM 선택) |
    | 검색 전략 | Vector 검색만 | 6가지 도구 |
    | 반복 추론 | 불가능 | 가능 (최대 5회) |
    | 통계 조회 | 불가능 | 가능 |
    | 적합한 질문 | 특정 로그 검색 | 모든 종류의 질문 |

    ## 요청 형식

    ```json
    {
      "question": "시스템 전체 상태 요약해줘",
      "project_uuid": "9911573f-8a1d-3b96-98b4-5a0def93513b",
      "chat_history": []
    }
    ```

    **참고:** V2에서는 `filters`, `time_range`를 전달할 필요 없음 (Agent가 자동 처리)

    ## 응답 형식

    ```json
    {
      "answer": "최근 24시간 동안 ERROR 15건, WARN 32건 발생. 주요 에러는 NullPointerException(8건)과 DatabaseTimeout(7건)입니다...",
      "related_logs": [],
      "chat_history": [...],
      "answered_at": "2025-11-10T10:00:00Z"
    }
    ```

    **참고:** `related_logs`는 빈 배열 (Agent가 내부적으로 로그 처리)

    ## 언제 V1 대신 V2를 사용해야 하나요?

    ✅ **V2 사용 권장:**
    - "시스템 상태 요약", "전체 에러 보여줘" 같은 **광범위한 질문**
    - "가장 심각한 에러가 뭐야?" 같은 **분석이 필요한 질문**
    - 특정 로그의 **상세 정보나 trace 추적**이 필요한 경우

    ✅ **V1 사용 권장:**
    - "이 에러는 왜 발생했어?" 같은 **특정 로그에 대한 질문** (Vector 검색이 효율적)
    - 대화 히스토리가 **매우 중요한 경우** (V1이 더 나은 히스토리 지원)
    - QA 캐싱으로 **빠른 응답**이 필요한 경우

    ## 주의사항

    - Agent는 최대 5회까지 도구를 호출하므로 V1보다 **응답 시간이 길 수 있음**
    - 대화 히스토리 지원은 제한적 (V1 권장)
    - 현재 캐싱 미지원 (매번 새로 분석)

    ## 디버깅

    Agent 실행 로그는 `AGENT_VERBOSE=True` 설정 시 서버 로그에 출력됩니다.
    """,
    responses={
        200: {
            "description": "답변 성공",
            "content": {
                "application/json": {
                    "examples": {
                        "system_summary": {
                            "summary": "시스템 상태 요약 (Agent 자동 분석)",
                            "description": "Agent가 통계 도구와 에러 목록 도구를 조합하여 답변",
                            "value": {
                                "answer": "최근 24시간 동안 총 1,570건의 로그가 발생했습니다.\n\n레벨별 분포:\n- ERROR: 15건 (0.96%)\n- WARN: 32건 (2.04%)\n- INFO: 1,523건 (96.99%)\n\n주요 에러:\n1. NullPointerException: 8건 (user-service)\n2. DatabaseTimeout: 7건 (payment-api)\n\n가장 최근 에러는 10분 전 user-service의 UserController.java:45에서 발생한 NullPointerException입니다.",
                                "related_logs": [],
                                "chat_history": [],
                                "answered_at": "2025-11-10T10:00:00Z"
                            }
                        },
                        "error_analysis": {
                            "summary": "에러 분석 (다단계 추론)",
                            "description": "Agent가 키워드 검색 → 상세 조회를 연속으로 수행",
                            "value": {
                                "answer": "NullPointerException은 user-service에서 6건, payment-api에서 2건 발생했습니다.\n\n가장 최근 에러 상세:\n- 시간: 2025-11-10 09:50:23\n- 위치: UserController.java:45\n- 원인: getUser() 메서드에서 userId가 null\n- 스택 트레이스: com.example.UserController.getUser(UserController.java:45)...\n\n권장 조치: userId null 체크 추가",
                                "related_logs": [],
                                "chat_history": [],
                                "answered_at": "2025-11-10T10:01:00Z"
                            }
                        },
                        "simple_greeting": {
                            "summary": "간단한 인사 (도구 미사용)",
                            "description": "Agent가 도구 없이 바로 답변",
                            "value": {
                                "answer": "안녕하세요! 저는 로그 분석 전문 AI입니다. 시스템 로그에 대해 궁금한 점이 있으시면 자유롭게 질문해주세요.",
                                "related_logs": [],
                                "chat_history": [],
                                "answered_at": "2025-11-10T10:02:00Z"
                            }
                        }
                    }
                }
            }
        },
        400: {
            "description": "잘못된 요청 - question이 비어있거나 project_uuid 형식이 잘못됨",
            "content": {
                "application/json": {
                    "example": {
                        "detail": "question field is required"
                    }
                }
            }
        },
        500: {
            "description": "처리 오류 - Agent 실행 실패, Tool 호출 오류 등",
            "content": {
                "application/json": {
                    "example": {
                        "detail": "Chatbot V2 error: Tool execution failed"
                    }
                }
            }
        }
    }
)
async def ask_chatbot_v2(request: ChatRequest):
    """ReAct Agent 기반 로그 질문 응답"""
    try:
        # project_uuid 형식 검증 (인덱스 이름 형식 거부)
        if re.match(r'^.+_\d{4}_\d{2}$', request.project_uuid):
            raise HTTPException(
                status_code=400,
                detail=(
                    f"잘못된 project_uuid 형식입니다. "
                    f"인덱스 이름(_YYYY_MM 포함)이 아닌 프로젝트 UUID만 전달하세요. "
                    f"예: '9f8c4c75-a936-3ab6-92a5-d1309cd9f87e' (올바름), "
                    f"'9f8c4c75-a936-3ab6-92a5-d1309cd9f87e_2025_11' (잘못됨)"
                )
            )

        result = await chatbot_service_v2.ask(
            question=request.question,
            project_uuid=request.project_uuid,
            chat_history=request.chat_history,
        )
        return result
    except Exception as e:
        logger.error(
            f"Chatbot V2 error for project {request.project_uuid}: {str(e)}",
            extra={
                "project_uuid": request.project_uuid,
                "question": request.question[:100],
                "error": str(e),
            },
            exc_info=True
        )
        raise HTTPException(status_code=500, detail=f"Chatbot V2 error: {str(e)}")


@router.post(
    "/chatbot/ask/stream",
    summary="챗봇 질문 (V2 - Agent 기반, 스트리밍)",
    description="""
    **ReAct Agent 기반 로그 분석 챗봇 (V2 - 실시간 스트리밍)**

    `/api/v2/chatbot/ask`와 동일한 기능이지만 **실시간 스트리밍**으로 답변을 전송합니다.

    ## 스트리밍 방식

    - **SSE (Server-Sent Events)** 형식 사용
    - Agent의 중간 추론 과정(Thought/Action/Observation)은 숨기고 **최종 답변만** 스트리밍
    - 답변이 생성되는 즉시 실시간으로 전송

    ## 응답 형식

    ```
    data: 최근 24시간 동안 \\n\\n
    data: **ERROR 3건** 발생했습니다.\\n\\n
    data: 주요 에러:\\n
    data: 1. DatabaseTimeout (payment-service)\\n
    data: [DONE]
    ```

    - 각 청크는 `data: ` 접두사로 시작
    - 줄바꿈은 `\\n`으로 이스케이프됨 (프론트엔드에서 `\\n` → `\n` 변환)
    - 완료 시그널: `data: [DONE]`
    - 에러 발생 시: `data: [ERROR]` 후 에러 정보

    ## V1 vs V2 스트리밍 비교

    | 특징 | V1 Stream | V2 Stream |
    |------|-----------|-----------|
    | 응답 시작 | 즉시 (Vector 검색 후) | 다소 지연 (Agent 추론 중) |
    | 중간 과정 | 없음 | 숨김 (Thought/Action 미표시) |
    | 도구 사용 | Vector 검색만 | 6가지 도구 자율 선택 |
    | 캐싱 | 지원 | 미지원 |

    ## 주의사항

    - Agent가 도구를 선택하고 실행하는 동안 **최대 5-10초** 지연 가능
    - 첫 토큰이 늦게 도착할 수 있음 (Agent 추론 시간)
    - 복잡한 질문일수록 응답 시작이 늦어짐
    """,
    responses={
        200: {
            "description": "스트리밍 응답 성공 (SSE 형식)",
            "content": {
                "text/event-stream": {
                    "example": "data: 최근 24시간 동안\\n\\ndata: ERROR 3건 발생\\n\\ndata: [DONE]\\n\\n"
                }
            }
        },
        400: {
            "description": "잘못된 요청",
            "content": {
                "application/json": {
                    "example": {"detail": "question field is required"}
                }
            }
        },
        500: {
            "description": "Agent 실행 오류",
            "content": {
                "text/event-stream": {
                    "example": "data: [ERROR]\\n\\ndata: {\"error\": \"Agent execution failed\"}\\n\\n"
                }
            }
        }
    }
)
async def ask_chatbot_v2_stream(
    background_tasks: BackgroundTasks,
    request: ChatRequest
):
    """ReAct Agent 기반 로그 질문 응답 (실시간 스트리밍)"""
    async def generate():
        try:
            # 1. project_uuid 형식 검증 (인덱스 이름 형식 거부)
            if re.match(r'^.+_\d{4}_\d{2}$', request.project_uuid):
                error_msg = (
                    f"잘못된 project_uuid 형식입니다. "
                    f"인덱스 이름(_YYYY_MM 포함)이 아닌 프로젝트 UUID만 전달하세요. "
                    f"예: '9f8c4c75-a936-3ab6-92a5-d1309cd9f87e' (올바름), "
                    f"'9f8c4c75-a936-3ab6-92a5-d1309cd9f87e_2025_11' (잘못됨)"
                )
                yield "data: [ERROR]\n\n"
                yield f"data: {json.dumps({'error': error_msg})}\n\n"
                return

            # 2. Agent 생성
            agent_executor = create_log_analysis_agent(request.project_uuid)

            # 3. 대화 히스토리 포맷팅
            history_text = ""
            if request.chat_history:
                history_text = "\n\n## 이전 대화:\n"
                for msg in request.chat_history:
                    role = "User" if msg.role == "user" else "Assistant"
                    history_text += f"{role}: {msg.content}\n"

            agent_input = {
                "input": request.question,
                "chat_history": history_text
            }

            # 4. Agent 스트리밍 실행
            full_answer = ""
            is_streaming = False  # "Final Answer:" 감지 후 True
            buffer = ""  # 토큰 버퍼

            try:
                # LangChain 0.2.x: astream_events 사용
                async for event in agent_executor.astream_events(
                    agent_input,
                    version="v1"
                ):
                    kind = event["event"]

                    # LLM 토큰만 스트리밍 (최종 답변만)
                    if kind == "on_chat_model_stream":
                        chunk = event.get("data", {}).get("chunk")
                        if chunk and hasattr(chunk, "content"):
                            content = chunk.content

                            if content:
                                buffer += content

                                # "Final Answer:" 패턴 감지
                                if "Final Answer:" in buffer and not is_streaming:
                                    is_streaming = True
                                    # "Final Answer:" 이후 텍스트만 추출
                                    parts = buffer.split("Final Answer:", 1)
                                    if len(parts) > 1:
                                        answer_start = parts[1].lstrip()  # 앞 공백 제거
                                        if answer_start:
                                            full_answer += answer_start
                                            escaped = answer_start.replace("\n", "\\n")
                                            yield f"data: {escaped}\n\n"
                                    buffer = ""  # 버퍼 초기화
                                elif is_streaming:
                                    # 이미 Final Answer 시작됨, 그대로 스트리밍
                                    full_answer += content
                                    escaped_content = content.replace("\n", "\\n")
                                    yield f"data: {escaped_content}\n\n"
                                # else: Final Answer 이전이므로 스킵 (Thought, Action 등)

                # 5. Completion signal
                yield "data: [DONE]\n\n"

            except AttributeError:
                # astream_events가 없는 경우 fallback: 일반 실행 후 타이핑 효과
                logger.warning("astream_events not available, using fallback streaming")

                # 분석 중 메시지
                yield "data: 로그를 분석하고 있습니다...\\n\\n\n\n"

                # Agent 실행 (비동기)
                result = await agent_executor.ainvoke(agent_input)
                answer = result.get("output", "")

                # 답변을 단어 단위로 스트리밍 (타이핑 효과)
                import asyncio
                words = answer.split(" ")
                for i, word in enumerate(words):
                    if i == 0:
                        # 첫 단어: "분석 중..." 덮어쓰기
                        yield f"data: {word.replace(chr(10), '\\n')}\n\n"
                    else:
                        # 공백 포함
                        yield f"data:  {word.replace(chr(10), '\\n')}\n\n"
                    await asyncio.sleep(0.02)  # 20ms 딜레이

                yield "data: [DONE]\n\n"

        except Exception as e:
            logger.error(
                f"Chatbot V2 stream error for project {request.project_uuid}: {str(e)}",
                extra={
                    "project_uuid": request.project_uuid,
                    "question": request.question[:100],
                    "error": str(e),
                },
                exc_info=True
            )
            yield "data: [ERROR]\n\n"
            error_data = json.dumps({"error": str(e)})
            yield f"data: {error_data}\n\n"

    return StreamingResponse(
        generate(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",  # Nginx 버퍼링 비활성화
        }
    )
