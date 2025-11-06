"""
Chatbot endpoints
"""

from fastapi import APIRouter, HTTPException, BackgroundTasks
from fastapi.responses import StreamingResponse
from langchain_core.messages import HumanMessage, AIMessage
import json
import logging

from app.services.chatbot_service import chatbot_service
from app.services.embedding_service import embedding_service
from app.services.similarity_service import similarity_service
from app.chains.chatbot_chain import chatbot_chain
from app.models.chat import ChatRequest, ChatResponse
from app.core.config import settings

router = APIRouter()
logger = logging.getLogger(__name__)


@router.post(
    "/chatbot/ask",
    response_model=ChatResponse,
    summary="챗봇 질문",
    description="""
    자연어로 로그에 대해 질문하고 AI 기반 답변을 받습니다. RAG(Retrieval-Augmented Generation) 방식으로 관련 로그를 검색하여 컨텍스트로 활용합니다.

    ## 처리 흐름

    1. **질문 임베딩 생성**: 질문을 1536차원 벡터로 변환
    2. **QA 캐시 확인** (2단계 검증):
       - a. 의미적 유사도 검색 (코사인 유사도 ≥ 0.8)
       - b. 메타데이터 매칭 (project_uuid, filters, time_range) + TTL 검증
    3. **캐시 히트**: 저장된 답변 반환 (40-60% 비용 절감)
    4. **캐시 미스**:
       - Vector 검색으로 관련 로그 탐색 (project_uuid 필터링)
       - GPT-4o mini로 RAG 답변 생성 (대화 히스토리 포함)
       - QA 페어 캐싱 (메타데이터 + 동적 TTL)

    ## 대화 히스토리 지원

    - 이전 대화 맥락을 유지하여 후속 질문에 답변 가능
    - 토큰 기반 히스토리 압축 (최대 1500 토큰)
    - 예: "그 에러는 언제 발생했어?" → 이전 대화의 에러를 참조

    ## 필터링 옵션

    - **level**: ERROR, WARN, INFO 등
    - **service_name**: 특정 서비스만 검색
    - **time_range**: 시간 범위 지정 (ISO 8601)

    ## 답변 형식

    - 한국어로 답변
    - 관련 로그 최대 5개 포함 (유사도 점수와 함께)
    - 캐시 여부 표시
    """,
    responses={
        200: {
            "description": "답변 성공",
            "content": {
                "application/json": {
                    "example": {
                        "answer": "최근 24시간 동안 user-service에서 3건의 NullPointerException이 발생했습니다. 모두 UserService.getUser() 메서드에서 발생했으며, user_id=12345에 해당하는 데이터가 DB에 없어서 발생한 것으로 보입니다.",
                        "from_cache": False,
                        "related_logs": [
                            {
                                "log_id": 12345,
                                "timestamp": "2024-01-15T10:30:00Z",
                                "level": "ERROR",
                                "message": "NullPointerException in UserService.getUser()",
                                "service_name": "user-service",
                                "similarity_score": 0.92
                            }
                        ],
                        "answered_at": "2024-01-15T10:35:00Z"
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
            "description": "처리 오류 - LLM 호출 실패, OpenSearch 연결 문제 등",
            "content": {
                "application/json": {
                    "example": {
                        "detail": "Chatbot error: OpenAI API timeout"
                    }
                }
            }
        }
    }
)
async def ask_chatbot(request: ChatRequest):
    """RAG 기반 로그 질문 응답"""
    try:
        result = await chatbot_service.ask(
            question=request.question,
            project_uuid=request.project_uuid,  # Multi-tenancy support
            chat_history=request.chat_history,  # Chat history support
            filters=request.filters,
            time_range=request.time_range,
        )
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Chatbot error: {str(e)}")


@router.post(
    "/chatbot/ask/stream",
    summary="챗봇 스트리밍 질문",
    description="""
    자연어로 로그에 대해 질문하고 실시간 타이핑 효과로 답변을 받습니다. SSE(Server-Sent Events) 형식으로 응답이 스트리밍됩니다.

    ## SSE 응답 형식

    ```
    data: 최근 24시간 동안\n\n
    data: user-service에서\n\n
    data: 3건의 에러가\n\n
    data: 발생했습니다.\n\n
    data: [DONE]\n\n
    ```

    ## 응답 시그널

    - **텍스트 청크**: `data: {content}\\n\\n`
    - **완료**: `data: [DONE]\\n\\n`
    - **에러**: `data: [ERROR]\\n\\n` 다음 `data: {"error": "..."}\\n\\n`

    ## 처리 흐름

    1. 질문 임베딩 생성
    2. QA 캐시 확인 (히스토리 없을 때만)
    3. 캐시 히트: 전체 답변 한 번에 전송 후 [DONE]
    4. 캐시 미스:
       - 관련 로그 검색
       - LLM 스트리밍 응답 (청크 단위 전송)
       - 백그라운드에서 QA 캐싱 (비차단)

    ## 대화 히스토리

    - 이전 대화를 참조하여 답변 생성
    - 토큰 기반 압축 (최대 1500 토큰)

    ## 클라이언트 사용 예시 (JavaScript)

    ```javascript
    const eventSource = new EventSource('/api/v1/chatbot/ask/stream');
    eventSource.onmessage = (event) => {
      if (event.data === '[DONE]') {
        eventSource.close();
      } else if (event.data === '[ERROR]') {
        // 다음 메시지에서 에러 내용 수신
      } else {
        console.log(event.data); // 텍스트 청크 출력
      }
    };
    ```

    ## 주의사항

    - Content-Type: `text/event-stream`
    - 캐시 비활성화 (Cache-Control: no-cache)
    - Nginx 버퍼링 비활성화 (X-Accel-Buffering: no)
    """,
    responses={
        200: {
            "description": "스트리밍 시작 - SSE 형식으로 답변 전송",
            "content": {
                "text/event-stream": {
                    "example": "data: 최근 24시간 동안\n\ndata: user-service에서\n\ndata: 3건의 에러가\n\ndata: 발생했습니다.\n\ndata: [DONE]\n\n"
                }
            }
        },
        500: {
            "description": "처리 오류 - 에러 시그널과 함께 스트림으로 전송됨",
            "content": {
                "text/event-stream": {
                    "example": "data: [ERROR]\n\ndata: {\"error\": \"OpenSearch connection failed\"}\n\n"
                }
            }
        }
    }
)
async def ask_chatbot_stream(request: ChatRequest, background_tasks: BackgroundTasks):
    """RAG 기반 로그 질문 응답 (실시간 스트리밍)"""
    async def generate():
        try:
            # 1. Generate question embedding
            question_vector = await embedding_service.embed_query(request.question)

            # 2. Check cache (skip if chat_history present - history-dependent answers)
            if not request.chat_history:
                cache_candidates = await similarity_service.find_similar_questions(
                    question_vector=question_vector,
                    k=settings.CACHE_CANDIDATE_SIZE,
                    project_uuid=request.project_uuid
                )

                # 캐시 히트 시 전체 답변 전송
                for candidate in cache_candidates:
                    if candidate["score"] >= chatbot_service.threshold:
                        if chatbot_service._is_cache_valid(candidate):
                            if chatbot_service._metadata_matches(
                                candidate.get("metadata", {}),
                                request.filters,
                                request.time_range,
                                request.project_uuid
                            ):
                                cached_answer = candidate["answer"]
                                yield f"data: {cached_answer}\n\n"
                                yield "data: [DONE]\n\n"
                                return

            # 3. Search relevant logs
            relevant_logs_data = await similarity_service.find_similar_logs(
                log_vector=question_vector,
                k=chatbot_service.max_context,
                filters=request.filters,
                project_uuid=request.project_uuid,
            )

            # 4. Prepare context
            context_logs = chatbot_service._format_context_logs(relevant_logs_data)

            # 5. Convert chat history to LangChain messages (token-based truncation)
            history_messages = []
            if request.chat_history:
                # Truncate history based on token budget (1500 tokens)
                truncated_history = chatbot_service._truncate_history(request.chat_history, max_tokens=1500)
                for msg in truncated_history:
                    if msg.role == "user":
                        history_messages.append(HumanMessage(content=msg.content))
                    elif msg.role == "assistant":
                        history_messages.append(AIMessage(content=msg.content))

            # 6. Stream LLM response
            full_answer = ""
            async for chunk in chatbot_chain.astream({
                "context_logs": context_logs,
                "question": request.question,
                "chat_history": history_messages,
            }):
                content = chunk.content
                full_answer += content

                # SSE 형식으로 전송
                yield f"data: {content}\n\n"

            # 7. Completion signal
            yield "data: [DONE]\n\n"

            # 8. Cache QA pair (백그라운드에서 비동기)
            related_log_ids = [log["log_id"] for log in relevant_logs_data]
            ttl = chatbot_service._calculate_ttl(request.question, request.time_range)

            background_tasks.add_task(
                chatbot_service._cache_qa_pair,
                question=request.question,
                question_vector=question_vector,
                answer=full_answer,
                related_log_ids=related_log_ids,
                metadata={
                    "project_uuid": request.project_uuid,
                    "filters": request.filters,
                    "time_range": request.time_range,
                },
                ttl=ttl
            )

        except Exception as e:
            # Log the error with context
            logger.error(
                f"Stream error for project {request.project_uuid}: {str(e)}",
                extra={
                    "project_uuid": request.project_uuid,
                    "question": request.question[:100],  # Truncate for logging
                    "error": str(e),
                },
                exc_info=True
            )

            # Send error signal and data to client
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
