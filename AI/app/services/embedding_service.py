"""
Embedding service using OpenAI
"""

from langchain_openai import OpenAIEmbeddings
from app.core.config import settings
from typing import List
from cachetools import TTLCache


class EmbeddingService:
    """Service for generating embeddings"""

    def __init__(self):
        self.embeddings = OpenAIEmbeddings(
            model=settings.EMBEDDING_MODEL,
            api_key=settings.OPENAI_API_KEY,
            base_url=settings.OPENAI_BASE_URL,
            dimensions=1536,  # Reduce from 3072 to 1536 for efficiency
        )
        # TTL Cache: 1000 items, 1 hour TTL (3600 seconds)
        # Key: (text, model), Value: embedding vector
        self.cache = TTLCache(maxsize=1000, ttl=3600)

    async def embed_query(self, text: str) -> List[float]:
        """
        Generate embedding for a single text query with caching

        Args:
            text: Text to embed

        Returns:
            Embedding vector (from cache or newly generated)
        """
        # Create cache key (text + model name)
        cache_key = (text, settings.EMBEDDING_MODEL)

        # Check cache first
        if cache_key in self.cache:
            return self.cache[cache_key]

        # Cache miss - generate embedding
        embedding = await self.embeddings.aembed_query(text)

        # Store in cache
        self.cache[cache_key] = embedding

        return embedding

    async def embed_documents(self, texts: List[str]) -> List[List[float]]:
        """
        Generate embeddings for multiple documents

        Args:
            texts: List of texts to embed

        Returns:
            List of embedding vectors
        """
        return await self.embeddings.aembed_documents(texts)


# Global instance
embedding_service = EmbeddingService()
