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


@router.post("/chatbot/ask", response_model=ChatResponse)
async def ask_chatbot(request: ChatRequest):
    """
    Ask a question about logs using RAG-based chatbot with context-aware caching and history support

    This endpoint:
    1. Generates question embedding
    2. Checks QA cache with 2-stage validation:
       a. Semantic similarity (vector search, similarity >= 0.8)
       b. Metadata matching (project_id, filters, time_range) + TTL validation
    3. Returns cached answer if found (saves ~40-60% cost)
    4. Otherwise:
       - Searches relevant logs using vector similarity (filtered by project_id)
       - Generates answer using GPT-4o mini with RAG and chat history
       - Caches the QA pair with metadata and dynamic TTL

    Args:
        request: ChatRequest with question, project_id, chat_history, and optional filters

    Returns:
        ChatResponse with answer and related logs

    Raises:
        HTTPException: 500 if chatbot processing fails
    """
    try:
        result = await chatbot_service.ask(
            question=request.question,
            project_id=request.project_id,  # Multi-tenancy support
            chat_history=request.chat_history,  # Chat history support
            filters=request.filters,
            time_range=request.time_range,
        )
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Chatbot error: {str(e)}")


@router.post("/chatbot/ask/stream")
async def ask_chatbot_stream(request: ChatRequest, background_tasks: BackgroundTasks):
    """
    Stream-based chatbot endpoint with real-time typing effect and history support

    This endpoint streams the answer in SSE (Server-Sent Events) format:
    - Each chunk is sent as "data: {content}\\n\\n"
    - Completion is signaled with "data: [DONE]\\n\\n"
    - Errors are sent as "data: {\"error\": \"...\"}\\n\\n"

    Args:
        request: ChatRequest with question, project_id, chat_history, and optional filters

    Returns:
        StreamingResponse with text/event-stream

    Raises:
        Errors are sent through the stream
    """
    async def generate():
        try:
            # 1. Generate question embedding
            question_vector = await embedding_service.embed_query(request.question)

            # 2. Check cache (skip if chat_history present - history-dependent answers)
            if not request.chat_history:
                cache_candidates = await similarity_service.find_similar_questions(
                    question_vector=question_vector,
                    k=settings.CACHE_CANDIDATE_SIZE,
                    project_id=request.project_id
                )

                # 캐시 히트 시 전체 답변 전송
                for candidate in cache_candidates:
                    if candidate["score"] >= chatbot_service.threshold:
                        if chatbot_service._is_cache_valid(candidate):
                            if chatbot_service._metadata_matches(
                                candidate.get("metadata", {}),
                                request.filters,
                                request.time_range,
                                request.project_id
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
                project_id=request.project_id,
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
                    "project_id": request.project_id,
                    "filters": request.filters,
                    "time_range": request.time_range,
                },
                ttl=ttl
            )

        except Exception as e:
            # Log the error with context
            logger.error(
                f"Stream error for project {request.project_id}: {str(e)}",
                extra={
                    "project_id": request.project_id,
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
