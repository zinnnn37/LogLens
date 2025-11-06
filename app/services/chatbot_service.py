"""
Chatbot service with RAG and context-aware QA caching
"""

import re
import asyncio
from typing import List, Dict, Any, Optional
from datetime import datetime, timedelta, timezone
from langchain_core.messages import HumanMessage, AIMessage
import tiktoken
from app.core.opensearch import opensearch_client
from app.core.config import settings
from app.chains.chatbot_chain import chatbot_chain
from app.services.embedding_service import embedding_service
from app.services.similarity_service import similarity_service
from app.models.chat import ChatResponse, RelatedLog, ChatMessage
from app.utils.index_naming import format_index_name


class ChatbotService:
    """Service for chatbot QA with RAG"""

    def __init__(self):
        self.client = opensearch_client
        self.threshold = settings.SIMILARITY_THRESHOLD
        self.max_context = settings.MAX_CONTEXT_LOGS
        # Initialize tiktoken encoder for token counting
        try:
            self.encoding = tiktoken.encoding_for_model("gpt-4")
        except KeyError:
            self.encoding = tiktoken.get_encoding("cl100k_base")  # Fallback

    async def ask(
        self,
        question: str,
        project_uuid: str,
        chat_history: Optional[List[ChatMessage]] = None,
        filters: Optional[Dict[str, Any]] = None,
        time_range: Optional[Dict[str, str]] = None,
    ) -> ChatResponse:
        """
        Answer a question about logs using RAG with context-aware caching and history support

        Strategy:
        1. Generate question embedding
        2. Check QA cache for similar questions (2-stage validation)
           a. Semantic similarity (vector search)
           b. Metadata matching (filters, time_range, project_uuid) + TTL validation
        3. If found, return cached answer
        4. Otherwise:
           - Search relevant logs
           - Generate answer with LLM (with chat history)
           - Cache the QA pair with metadata

        Args:
            question: User's question
            project_uuid: Project UUID for multi-tenancy isolation
            chat_history: Previous conversation history
            filters: Optional filters for log search
            time_range: Optional time range filter

        Returns:
            Chat response
        """
        # Step 1: Generate question embedding
        question_vector = await embedding_service.embed_query(question)

        # Step 2: Check QA cache (2-stage validation)
        # Skip caching when chat_history is present (history-dependent answers)
        if not chat_history:
            # 2a. Get candidates by semantic similarity
            cache_candidates = await similarity_service.find_similar_questions(
                question_vector=question_vector,
                k=settings.CACHE_CANDIDATE_SIZE,  # Get more candidates
                project_uuid=project_uuid  # Filter by project_uuid
            )

            # 2b. Validate metadata and TTL
            for candidate in cache_candidates:
                if candidate["score"] >= self.threshold:
                    # Check TTL validity
                    if not self._is_cache_valid(candidate):
                        continue  # Expired cache, skip

                    # Check metadata match
                    if self._metadata_matches(
                        candidate.get("metadata", {}),
                        filters,
                        time_range,
                        project_uuid
                    ):
                        # Cache hit!
                        cached = candidate
                        related_logs = await self._get_logs_by_ids(
                            cached.get("related_log_ids", []),
                            project_uuid
                        )

                        return ChatResponse(
                            answer=cached["answer"],
                            from_cache=True,
                            related_logs=related_logs,
                        )

        # Step 3: Cache miss - Search relevant logs using vector search
        relevant_logs_data = await similarity_service.find_similar_logs(
            log_vector=question_vector,
            k=self.max_context,
            filters=filters,
            project_uuid=project_uuid,  # Project isolation
        )

        # Step 4: Prepare context from logs
        context_logs = self._format_context_logs(relevant_logs_data)

        # Step 5: Convert chat history to LangChain messages (token-based truncation)
        history_messages = []
        if chat_history:
            # Truncate history based on token budget (1500 tokens)
            truncated_history = self._truncate_history(chat_history, max_tokens=1500)
            for msg in truncated_history:
                if msg.role == "user":
                    history_messages.append(HumanMessage(content=msg.content))
                elif msg.role == "assistant":
                    history_messages.append(AIMessage(content=msg.content))

        # Step 6: Generate answer with LLM (with history)
        response = await chatbot_chain.ainvoke(
            {
                "context_logs": context_logs,
                "question": question,
                "chat_history": history_messages,  # 히스토리 추가
            }
        )

        answer = response.content

        # Step 6: Cache the QA pair with metadata
        related_log_ids = [log["log_id"] for log in relevant_logs_data]
        ttl = self._calculate_ttl(question, time_range)

        await self._cache_qa_pair(
            question=question,
            question_vector=question_vector,
            answer=answer,
            related_log_ids=related_log_ids,
            metadata={
                "project_uuid": project_uuid,
                "filters": filters,
                "time_range": time_range,
            },
            ttl=ttl
        )

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
        related_log_ids: List[int],
        metadata: Dict[str, Any],
        ttl: int,
    ):
        """Cache a QA pair in OpenSearch with metadata and TTL"""
        try:
            now = datetime.now(timezone.utc)
            expires_at = now + timedelta(seconds=ttl)

            doc = {
                "question": question,
                "question_vector": question_vector,
                "answer": answer,
                "related_log_ids": related_log_ids,
                "metadata": metadata,  # Store filters, time_range, project_id
                "cached_at": now.isoformat(),
                "expires_at": expires_at.isoformat(),  # TTL expiration
            }
            await asyncio.to_thread(self.client.index, index="qa-cache", body=doc)
        except Exception as e:
            print(f"Error caching QA pair: {e}")

    async def _get_logs_by_ids(self, log_ids: List[int], project_uuid: str) -> List[RelatedLog]:
        """Get logs by IDs filtered by project_uuid"""
        if not log_ids:
            return []

        try:
            # Use wildcard pattern to search across all months
            uuid_formatted = project_uuid.replace('-', '_')
            index_pattern = f"{uuid_formatted}_*"

            response = await asyncio.to_thread(
                self.client.search,
                index=index_pattern,
                body={
                    "query": {
                        "bool": {
                            "must": [
                                {"terms": {"log_id": log_ids}},
                                {"term": {"project_uuid.keyword": project_uuid}},  # Project isolation
                            ]
                        }
                    },
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
                        level=source.get("log_level") or source.get("level", "INFO"),  # Logstash sends log_level
                        message=source["message"],
                        service_name=source["service_name"],
                        similarity_score=1.0,  # From cache, so perfect match
                    )
                )
            return logs
        except Exception as e:
            print(f"Error fetching logs by IDs for project {project_uuid}: {e}")
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

    def _metadata_matches(
        self,
        cached_metadata: Dict[str, Any],
        filters: Optional[Dict[str, Any]],
        time_range: Optional[Dict[str, str]],
        project_uuid: str,
    ) -> bool:
        """
        Check if cached metadata matches current request metadata

        All fields must match exactly for cache hit:
        - project_uuid (required)
        - filters (optional)
        - time_range (optional)
        """
        # Project UUID must match (required for multi-tenancy)
        if cached_metadata.get("project_uuid") != project_uuid:
            return False

        # Filters must match exactly (None == None is valid)
        if cached_metadata.get("filters") != filters:
            return False

        # Time range must match exactly (None == None is valid)
        if cached_metadata.get("time_range") != time_range:
            return False

        return True

    def _is_cache_valid(self, candidate: Dict[str, Any]) -> bool:
        """Check if cache entry is still valid (not expired)"""
        expires_at = candidate.get("expires_at")
        if not expires_at:
            # No expiration set, always valid
            return True

        try:
            expiration_time = datetime.fromisoformat(expires_at.replace("Z", "+00:00"))
            return expiration_time > datetime.now(timezone.utc)
        except Exception as e:
            print(f"Error parsing expiration time: {e}")
            return False  # Treat as expired if parsing fails

    def _calculate_ttl(
        self,
        question: str,
        time_range: Optional[Dict[str, str]],
    ) -> int:
        """
        Calculate TTL based on question content and time_range

        Strategy:
        - Relative time expressions ("방금", "지금", "1시간 전", "최근"): SHORT_CACHE_TTL (10 min)
        - Explicit time_range provided: DEFAULT_CACHE_TTL (30 min)
        - Absolute dates in question (YYYY-MM-DD): LONG_CACHE_TTL (1 day)
        - Default: DEFAULT_CACHE_TTL (30 min)
        """
        question_lower = question.lower()

        # Check for immediate/relative time expressions (shortest TTL)
        immediate_keywords = ["방금", "지금", "현재", "just now", "right now", "currently"]
        if any(keyword in question_lower for keyword in immediate_keywords):
            return settings.SHORT_CACHE_TTL

        # Check for recent/short-term expressions
        recent_keywords = ["1시간", "한시간", "2시간", "최근", "최신", "1 hour", "2 hour", "recent", "latest"]
        if any(keyword in question_lower for keyword in recent_keywords):
            return settings.SHORT_CACHE_TTL

        # Check for today/this week (medium TTL)
        today_keywords = ["오늘", "금일", "이번주", "today", "this week"]
        if any(keyword in question_lower for keyword in today_keywords):
            return settings.DEFAULT_CACHE_TTL

        # Check for absolute dates (longest TTL)
        # Matches patterns like 2024-01-15, 2024/01/15, 01-15, etc.
        date_pattern = r'\d{4}[-/]\d{1,2}[-/]\d{1,2}'
        if re.search(date_pattern, question):
            return settings.LONG_CACHE_TTL

        # If explicit time_range provided, use default TTL
        if time_range:
            return settings.DEFAULT_CACHE_TTL

        # Default TTL for general questions
        return settings.DEFAULT_CACHE_TTL

    def _format_related_logs(self, logs_data: List[Dict[str, Any]]) -> List[RelatedLog]:
        """Format logs into RelatedLog objects"""
        related_logs = []
        for log in logs_data:
            data = log["data"]
            related_logs.append(
                RelatedLog(
                    log_id=data["log_id"],
                    timestamp=data["timestamp"],
                    level=data.get("log_level") or data.get("level", "INFO"),  # Logstash sends log_level
                    message=data["message"],
                    service_name=data["service_name"],
                    similarity_score=log["score"],
                )
            )
        return related_logs

    def _truncate_history(
        self,
        chat_history: List[ChatMessage],
        max_tokens: int = 1500
    ) -> List[ChatMessage]:
        """
        Truncate chat history to fit within token budget

        Strategy:
        - Keep most recent messages first (reverse order)
        - Count tokens for each message
        - Stop when budget exhausted
        - Return in correct chronological order

        Args:
            chat_history: Full chat history
            max_tokens: Maximum tokens allowed (default 1500)

        Returns:
            Truncated chat history within token budget
        """
        if not chat_history:
            return []

        truncated = []
        total_tokens = 0

        # Process from most recent to oldest
        for msg in reversed(chat_history):
            # Count tokens in this message
            msg_tokens = len(self.encoding.encode(msg.content))

            # Check if adding this message exceeds budget
            if total_tokens + msg_tokens > max_tokens:
                break

            truncated.append(msg)
            total_tokens += msg_tokens

        # Reverse back to chronological order
        return list(reversed(truncated))


# Global instance
chatbot_service = ChatbotService()
