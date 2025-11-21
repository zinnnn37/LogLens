"""
API v2 routes - ReAct Agent 기반 챗봇
"""

from fastapi import APIRouter
from app.api.v2 import chatbot

# Create v2 router
router = APIRouter()

# Include all v2 endpoints
router.include_router(chatbot.router, tags=["Chatbot V2 (Agent)"])
