"""
Chatbot service with RAG and QA caching
"""

from typing import List, Dict, Any, Optional
from datetime import datetime
from app.core.opensearch import opensearch_client
from app.core.config import settings
from app.chains.chatbot_chain import chatbot_chain
from app.services.embedding_service import embedding_service
from app.services.similarity_service import similarity_service
from app.models.chat import ChatResponse, RelatedLog


class ChatbotService:
    """Service for chatbot QA with RAG"""

    def __init__(self):
        self.client = opensearch_client
        self.threshold = settings.SIMILARITY_THRESHOLD
        self.max_context = settings.MAX_CONTEXT_LOGS

    async def ask(
        self,
        question: str,
        filters: Optional[Dict[str, Any]] = None,
        time_range: Optional[Dict[str, str]] = None,
    ) -> ChatResponse:
        """
        Answer a question about logs using RAG

        Strategy:
        1. Generate question embedding
        2. Check QA cache for similar questions
        3. If found (score >= threshold), return cached answer
        4. Otherwise:
           - Search relevant logs
           - Generate answer with LLM
           - Cache the QA pair

        Args:
            question: User's question
            filters: Optional filters for log search
            time_range: Optional time range filter

        Returns:
            Chat response
        """
        # Step 1: Generate question embedding
        question_vector = await embedding_service.embed_query(question)

        # Step 2: Check QA cache
        similar_questions = await similarity_service.find_similar_questions(
            question_vector=question_vector, k=3
        )

        if similar_questions and similar_questions[0]["score"] >= self.threshold:
            cached = similar_questions[0]
            # Get related logs
            related_logs = await self._get_logs_by_ids(cached.get("related_log_ids", []))

            return ChatResponse(
                answer=cached["answer"],
                from_cache=True,
                related_logs=related_logs,
            )

        # Step 3: Search relevant logs using vector search
        relevant_logs_data = await similarity_service.find_similar_logs(
            log_vector=question_vector,
            k=self.max_context,
            filters=filters,
        )

        # Step 4: Prepare context from logs
        context_logs = self._format_context_logs(relevant_logs_data)

        # Step 5: Generate answer with LLM
        response = await chatbot_chain.ainvoke(
            {
                "context_logs": context_logs,
                "question": question,
            }
        )

        answer = response.content

        # Step 6: Cache the QA pair
        related_log_ids = [log["log_id"] for log in relevant_logs_data]
        await self._cache_qa_pair(question, question_vector, answer, related_log_ids)

        # Step 7: Format related logs
        related_logs = self._format_related_logs(relevant_logs_data)

        return ChatResponse(
            answer=answer,
            from_cache=False,
            related_logs=related_logs,
        )

    async def _cache_qa_pair(
        self,
        question: str,
        question_vector: List[float],
        answer: str,
        related_log_ids: List[str],
    ):
        """Cache a QA pair in OpenSearch"""
        try:
            doc = {
                "question": question,
                "question_vector": question_vector,
                "answer": answer,
                "related_log_ids": related_log_ids,
                "cached_at": datetime.utcnow().isoformat(),
            }
            self.client.index(index="qa-cache", body=doc)
        except Exception as e:
            print(f"Error caching QA pair: {e}")

    async def _get_logs_by_ids(self, log_ids: List[str]) -> List[RelatedLog]:
        """Get logs by IDs"""
        if not log_ids:
            return []

        try:
            response = self.client.search(
                index="logs-*",
                body={
                    "query": {"terms": {"log_id": log_ids}},
                    "size": len(log_ids),
                },
            )

            logs = []
            for hit in response["hits"]["hits"]:
                source = hit["_source"]
                logs.append(
                    RelatedLog(
                        log_id=source["log_id"],
                        timestamp=source["timestamp"],
                        level=source["level"],
                        message=source["message"],
                        service_name=source["service_name"],
                        similarity_score=1.0,  # From cache, so perfect match
                    )
                )
            return logs
        except Exception as e:
            print(f"Error fetching logs by IDs: {e}")
            return []

    def _format_context_logs(self, logs_data: List[Dict[str, Any]]) -> str:
        """Format logs into context string for LLM"""
        if not logs_data:
            return "No relevant logs found."

        context_parts = []
        for i, log in enumerate(logs_data, 1):
            data = log["data"]
            context = f"""
Log {i}:
- ID: {data.get('log_id')}
- Timestamp: {data.get('timestamp')}
- Service: {data.get('service_name')}
- Level: {data.get('level')}
- Message: {data.get('message')}
- Method: {data.get('class_name', '')}.{data.get('method_name', '')}
- Stack Trace: {data.get('stack_trace', 'N/A')[:200]}
"""
            context_parts.append(context.strip())

        return "\n\n".join(context_parts)

    def _format_related_logs(self, logs_data: List[Dict[str, Any]]) -> List[RelatedLog]:
        """Format logs into RelatedLog objects"""
        related_logs = []
        for log in logs_data:
            data = log["data"]
            related_logs.append(
                RelatedLog(
                    log_id=data["log_id"],
                    timestamp=data["timestamp"],
                    level=data["level"],
                    message=data["message"],
                    service_name=data["service_name"],
                    similarity_score=log["score"],
                )
            )
        return related_logs


# Global instance
chatbot_service = ChatbotService()
