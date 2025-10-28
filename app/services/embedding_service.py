"""
Embedding service using OpenAI
"""

from langchain_openai import OpenAIEmbeddings
from app.core.config import settings
from typing import List


class EmbeddingService:
    """Service for generating embeddings"""

    def __init__(self):
        self.embeddings = OpenAIEmbeddings(
            model=settings.EMBEDDING_MODEL,
            api_key=settings.OPENAI_API_KEY,
            base_url=settings.OPENAI_BASE_URL,
        )

    async def embed_query(self, text: str) -> List[float]:
        """
        Generate embedding for a single text query

        Args:
            text: Text to embed

        Returns:
            Embedding vector
        """
        return await self.embeddings.aembed_query(text)

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
