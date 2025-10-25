"""
Chatbot endpoints
"""

from fastapi import APIRouter, HTTPException
from app.services.chatbot_service import chatbot_service
from app.models.chat import ChatRequest, ChatResponse

router = APIRouter()


@router.post("/chatbot/ask", response_model=ChatResponse)
async def ask_chatbot(request: ChatRequest):
    """
    Ask a question about logs using RAG-based chatbot

    This endpoint:
    1. Generates question embedding
    2. Checks QA cache for similar questions (similarity >= 0.8)
    3. Returns cached answer if found (saves ~80% cost)
    4. Otherwise:
       - Searches relevant logs using vector similarity
       - Generates answer using GPT-4o with RAG
       - Caches the QA pair for future use

    Args:
        request: ChatRequest with question and optional filters

    Returns:
        ChatResponse with answer and related logs
    """
    try:
        result = await chatbot_service.ask(
            question=request.question,
            filters=request.filters,
            time_range=request.time_range,
        )
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Chatbot error: {str(e)}")
