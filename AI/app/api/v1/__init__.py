"""
API v1 routes
"""

from fastapi import APIRouter
from app.api.v1 import health, logs, chatbot, embedding

# Create v1 router
router = APIRouter()

# Include all v1 endpoints
router.include_router(health.router, tags=["health"])
router.include_router(logs.router, tags=["logs"])
router.include_router(chatbot.router, tags=["chatbot"])
router.include_router(embedding.router, tags=["embedding"])
