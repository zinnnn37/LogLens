"""
Log analysis service with similarity-based caching
"""

from typing import Optional
from app.core.opensearch import opensearch_client
from app.core.config import settings
from app.chains.log_analysis_chain import log_analysis_chain
from app.services.embedding_service import embedding_service
from app.services.similarity_service import similarity_service
from app.models.analysis import LogAnalysisResponse, LogAnalysisResult


class LogAnalysisService:
    """Service for analyzing logs with AI"""

    def __init__(self):
        self.client = opensearch_client
        self.threshold = settings.SIMILARITY_THRESHOLD

    async def analyze_log(self, log_id: str) -> LogAnalysisResponse:
        """
        Analyze a log using AI with similarity-based caching

        Strategy:
        1. Check if this log already has analysis
        2. If not, search for similar logs
        3. If similar log found (score >= threshold), reuse its analysis
        4. Otherwise, analyze with LLM and save

        Args:
            log_id: Log ID to analyze

        Returns:
            Analysis result
        """
        # Step 1: Get the log from OpenSearch
        log_data = await self._get_log(log_id)
        if not log_data:
            raise ValueError(f"Log not found: {log_id}")

        # Step 2: Check if already analyzed
        if log_data.get("ai_analysis"):
            return LogAnalysisResponse(
                log_id=log_id,
                analysis=LogAnalysisResult(**log_data["ai_analysis"]),
                from_cache=True,
                similar_log_id=log_id,
                similarity_score=1.0,
            )

        # Step 3: Get log vector (should already exist from consumer)
        log_vector = log_data.get("log_vector")
        if not log_vector:
            # Generate vector if missing
            text_to_embed = self._prepare_text_for_embedding(log_data)
            log_vector = await embedding_service.embed_query(text_to_embed)

        # Step 4: Search for similar logs that have analysis
        similar_logs = await similarity_service.find_similar_logs(
            log_vector=log_vector,
            k=5,
            filters={"ai_analysis": {"exists": True}},
        )

        # Step 5: Check if we can reuse similar log's analysis
        if similar_logs and similar_logs[0]["score"] >= self.threshold:
            similar_log = similar_logs[0]
            similar_analysis = similar_log["data"]["ai_analysis"]

            # Save the reused analysis to current log
            await self._save_analysis(log_id, similar_analysis)

            return LogAnalysisResponse(
                log_id=log_id,
                analysis=LogAnalysisResult(**similar_analysis),
                from_cache=True,
                similar_log_id=similar_log["log_id"],
                similarity_score=similar_log["score"],
            )

        # Step 6: No similar log found, analyze with LLM
        analysis_result = await self._analyze_with_llm(log_data)

        # Step 7: Save analysis
        await self._save_analysis(log_id, analysis_result.dict())

        return LogAnalysisResponse(
            log_id=log_id,
            analysis=analysis_result,
            from_cache=False,
            similar_log_id=None,
            similarity_score=None,
        )

    async def _get_log(self, log_id: str) -> Optional[dict]:
        """Get log from OpenSearch"""
        try:
            response = self.client.search(
                index="logs-*",
                body={"query": {"term": {"log_id": log_id}}},
            )
            if response["hits"]["total"]["value"] > 0:
                return response["hits"]["hits"][0]["_source"]
            return None
        except Exception as e:
            print(f"Error fetching log {log_id}: {e}")
            return None

    async def _analyze_with_llm(self, log_data: dict) -> LogAnalysisResult:
        """Analyze log using LLM"""
        result = await log_analysis_chain.ainvoke(
            {
                "service_name": log_data.get("service_name", "Unknown"),
                "level": log_data.get("level", "Unknown"),
                "message": log_data.get("message", ""),
                "method_name": log_data.get("method_name", "N/A"),
                "class_name": log_data.get("class_name", "N/A"),
                "timestamp": log_data.get("timestamp", ""),
                "duration": log_data.get("duration", "N/A"),
                "stack_trace": log_data.get("stack_trace", "N/A"),
                "additional_context": log_data.get("additional_context", {}),
            }
        )
        return result

    async def _save_analysis(self, log_id: str, analysis: dict):
        """Save analysis result to log"""
        try:
            self.client.update_by_query(
                index="logs-*",
                body={
                    "script": {
                        "source": "ctx._source.ai_analysis = params.analysis",
                        "params": {"analysis": analysis},
                    },
                    "query": {"term": {"log_id": log_id}},
                },
            )
        except Exception as e:
            print(f"Error saving analysis for log {log_id}: {e}")

    def _prepare_text_for_embedding(self, log_data: dict) -> str:
        """Prepare log text for embedding"""
        parts = [
            log_data.get("message", ""),
            log_data.get("stack_trace", ""),
            log_data.get("class_name", ""),
            log_data.get("method_name", ""),
        ]
        return " ".join(filter(None, parts))


# Global instance
log_analysis_service = LogAnalysisService()
