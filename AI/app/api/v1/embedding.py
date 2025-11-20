"""
Embedding API endpoints for vectorization
"""

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List
from app.services.embedding_service import embedding_service
import logging

router = APIRouter()
logger = logging.getLogger(__name__)


class EmbeddingRequest(BaseModel):
    """Single embedding request"""
    text: str


class BatchEmbeddingItem(BaseModel):
    """Single item in batch embedding request"""
    log_id: str
    message: str
    trace_id: str | None = None


class BatchEmbeddingRequest(BaseModel):
    """Batch embedding request for Kafka consumer"""
    logs: List[BatchEmbeddingItem]


class EmbeddingResponse(BaseModel):
    """Single embedding response"""
    embedding: List[float]
    dimension: int


class BatchEmbeddingItemResponse(BaseModel):
    """Single item in batch embedding response"""
    log_id: str
    embedding: List[float]


class BatchEmbeddingResponse(BaseModel):
    """Batch embedding response"""
    embeddings: List[BatchEmbeddingItemResponse]
    count: int
    model: str


@router.post("/embedding", response_model=EmbeddingResponse)
async def create_embedding(request: EmbeddingRequest):
    """
    Create embedding for a single text

    Args:
        request: Text to embed

    Returns:
        Embedding vector (1536 dimensions)
    """
    try:
        embedding = await embedding_service.embed_query(request.text)

        return EmbeddingResponse(
            embedding=embedding,
            dimension=len(embedding)
        )

    except Exception as e:
        logger.error(f"Failed to create embedding: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/embedding/batch", response_model=BatchEmbeddingResponse)
async def create_batch_embeddings(request: BatchEmbeddingRequest):
    """
    Create embeddings for multiple logs in batch (for Kafka consumer)

    This endpoint is optimized for the enrichment consumer service.
    It processes 50-100 ERROR logs at once to reduce API calls.

    Args:
        request: List of logs to embed

    Returns:
        List of embeddings in the same order

    Example:
        ```json
        {
            "logs": [
                {
                    "log_id": "1001",
                    "message": "NullPointerException at line 45",
                    "trace_id": "trace-123"
                },
                {
                    "log_id": "1002",
                    "message": "Database connection timeout",
                    "trace_id": "trace-124"
                }
            ]
        }
        ```

    Response:
        ```json
        {
            "embeddings": [
                {
                    "log_id": "1001",
                    "embedding": [0.123, -0.456, ...]
                },
                {
                    "log_id": "1002",
                    "embedding": [0.789, -0.012, ...]
                }
            ],
            "count": 2,
            "model": "text-embedding-3-large"
        }
        ```
    """
    try:
        logger.info(f"Batch embedding request: {len(request.logs)} logs")

        embeddings = []

        for log in request.logs:
            # Prepare text for embedding (same logic as log_analysis_service)
            text_to_embed = log.message

            # Add trace_id if available for better context
            if log.trace_id:
                text_to_embed = f"{text_to_embed} [trace: {log.trace_id}]"

            # Generate embedding
            embedding = await embedding_service.embed_query(text_to_embed)

            embeddings.append(
                BatchEmbeddingItemResponse(
                    log_id=log.log_id,
                    embedding=embedding
                )
            )

        logger.info(f"Batch embedding completed: {len(embeddings)} vectors generated")

        return BatchEmbeddingResponse(
            embeddings=embeddings,
            count=len(embeddings),
            model="text-embedding-3-large"
        )

    except Exception as e:
        logger.error(f"Failed to create batch embeddings: {e}")
        raise HTTPException(status_code=500, detail=str(e))
