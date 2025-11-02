"""
Log analysis service with similarity-based caching and Map-Reduce pattern
"""

from typing import Optional, List
from app.core.opensearch import opensearch_client
from app.core.config import settings
from app.chains.log_analysis_chain import log_analysis_chain
from app.chains.log_summarization_chain import log_summarization_chain
from app.services.embedding_service import embedding_service
from app.services.similarity_service import similarity_service
from app.models.analysis import LogAnalysisResponse, LogAnalysisResult


class LogAnalysisService:
    """Service for analyzing logs with AI"""

    def __init__(self):
        self.client = opensearch_client
        self.threshold = settings.SIMILARITY_THRESHOLD

    async def analyze_log(self, log_id: int, project_id: str) -> LogAnalysisResponse:
        """
        Analyze a log using AI with trace-based context and similarity-based caching

        Strategy:
        1. Get the log from OpenSearch (filtered by project_id)
        2. Check if already analyzed
        3. If trace_id exists, collect related logs (±3 seconds, max 100)
        4. Check for similar trace analysis (caching)
        5. Analyze with LLM (single or multiple logs)
        6. Save analysis

        Args:
            log_id: Log ID to analyze (integer)
            project_id: Project ID for multi-tenancy (UUID string)

        Returns:
            Analysis result
        """
        # Step 1: Get the log from OpenSearch (filtered by project_id)
        log_data = await self._get_log(log_id, project_id)
        if not log_data:
            raise ValueError(f"Log not found: {log_id} for project: {project_id}")

        # Step 2: Check if already analyzed
        if log_data.get("ai_analysis"):
            return LogAnalysisResponse(
                log_id=log_id,
                analysis=LogAnalysisResult(**log_data["ai_analysis"]),
                from_cache=True,
                similar_log_id=log_id,
                similarity_score=1.0,
            )

        # Step 3: Check if trace_id exists for context-based analysis
        trace_id = log_data.get("trace_id")
        timestamp = log_data.get("timestamp")

        related_logs = []
        if trace_id and timestamp:
            # Collect related logs by trace_id (±3 seconds, max 100, filtered by project_id)
            try:
                related_logs = await similarity_service.find_logs_by_trace_id(
                    trace_id=trace_id,
                    center_timestamp=timestamp,
                    project_id=project_id,
                    max_logs=100,
                )
                print(f"Found {len(related_logs)} related logs for trace_id: {trace_id}")
            except Exception as e:
                print(f"Error finding related logs: {e}")
                # Continue with single log analysis

        # Step 4: Check trace-level cache (if we have related logs)
        if related_logs:
            # Check if any log in this trace already has analysis
            for log in related_logs:
                if log.get("ai_analysis"):
                    # Reuse trace-level analysis
                    cached_analysis = log["ai_analysis"]
                    await self._save_analysis(log_id, cached_analysis)

                    return LogAnalysisResponse(
                        log_id=log_id,
                        analysis=LogAnalysisResult(**cached_analysis),
                        from_cache=True,
                        similar_log_id=log.get("log_id"),
                        similarity_score=1.0,  # Same trace
                    )

        # Step 5: No trace cache, check similarity-based cache (fallback)
        log_vector = log_data.get("log_vector")
        if not log_vector:
            # Generate vector if missing
            text_to_embed = self._prepare_text_for_embedding(log_data)
            log_vector = await embedding_service.embed_query(text_to_embed)

        similar_logs = await similarity_service.find_similar_logs(
            log_vector=log_vector,
            k=5,
            filters={"ai_analysis": {"exists": True}},
            project_id=project_id,
        )

        if similar_logs and similar_logs[0]["score"] >= self.threshold:
            similar_log = similar_logs[0]
            similar_analysis = similar_log["data"]["ai_analysis"]

            await self._save_analysis(log_id, similar_analysis)

            return LogAnalysisResponse(
                log_id=log_id,
                analysis=LogAnalysisResult(**similar_analysis),
                from_cache=True,
                similar_log_id=similar_log["log_id"],
                similarity_score=similar_log["score"],
            )

        # Step 6: Analyze with LLM
        if related_logs:
            # Analyze with full trace context (multiple logs)
            analysis_result = await self._analyze_with_llm_trace(related_logs, log_data)
        else:
            # Analyze single log (fallback)
            analysis_result = await self._analyze_with_llm(log_data)

        # Step 7: Save analysis to all logs in the trace
        analysis_dict = analysis_result.dict()
        await self._save_analysis(log_id, analysis_dict, project_id)

        if related_logs:
            # Save to all logs in the trace for future cache hits
            for log in related_logs:
                if log.get("log_id") != log_id:
                    await self._save_analysis(log["log_id"], analysis_dict, project_id)

        return LogAnalysisResponse(
            log_id=log_id,
            analysis=analysis_result,
            from_cache=False,
            similar_log_id=None,
            similarity_score=None,
        )

    async def _get_log(self, log_id: int, project_id: str) -> Optional[dict]:
        """Get log from OpenSearch filtered by project_id"""
        try:
            response = self.client.search(
                index="logs-*",
                body={
                    "query": {
                        "bool": {
                            "must": [
                                {"term": {"log_id": log_id}},
                                {"term": {"project_id": project_id}},
                            ]
                        }
                    }
                },
            )
            if response["hits"]["total"]["value"] > 0:
                return response["hits"]["hits"][0]["_source"]
            return None
        except Exception as e:
            print(f"Error fetching log {log_id} for project {project_id}: {e}")
            return None

    async def _analyze_with_llm(self, log_data: dict) -> LogAnalysisResult:
        """Analyze single log using LLM"""
        result = await log_analysis_chain.ainvoke(
            {
                "service_name": log_data.get("service_name", "Unknown"),
                "level": log_data.get("log_level") or log_data.get("level", "Unknown"),  # Logstash sends log_level
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

    async def _analyze_with_llm_trace(
        self, related_logs: list, center_log: dict
    ) -> LogAnalysisResult:
        """
        Analyze multiple logs (trace context) using LLM with Map-Reduce pattern

        Strategy:
        - If logs > MAP_REDUCE_THRESHOLD and ENABLE_MAP_REDUCE:
          1. Map: Split logs into chunks, summarize each chunk
          2. Reduce: Analyze combined summaries
        - Otherwise: Use traditional single-pass analysis
        """
        # Apply Map-Reduce for large log sets
        if (
            len(related_logs) > settings.MAP_REDUCE_THRESHOLD
            and settings.ENABLE_MAP_REDUCE
        ):
            print(
                f"🔄 Applying Map-Reduce: {len(related_logs)} logs → "
                f"{(len(related_logs) + settings.LOG_CHUNK_SIZE - 1) // settings.LOG_CHUNK_SIZE} chunks"
            )

            # Map: Summarize chunks
            summaries = await self._map_summarize_chunks(
                related_logs, chunk_size=settings.LOG_CHUNK_SIZE
            )

            # Reduce: Analyze combined summaries
            result = await self._reduce_analyze(summaries, center_log)
        else:
            # Traditional single-pass analysis for small log sets
            formatted_logs = self._format_logs_for_analysis(related_logs)
            result = await log_analysis_chain.ainvoke(
                {
                    "total_logs": len(related_logs),
                    "trace_id": center_log.get("trace_id", "Unknown"),
                    "center_log_id": center_log.get("log_id", ""),
                    "center_timestamp": center_log.get("timestamp", ""),
                    "logs_context": formatted_logs,
                    "service_name": center_log.get("service_name", "Unknown"),
                }
            )

        return result

    def _format_logs_for_analysis(self, logs: list) -> str:
        """Format multiple logs into a readable string for LLM"""
        formatted = []
        for idx, log in enumerate(logs, 1):
            log_str = f"\n--- Log {idx}/{len(logs)} ---\n"
            log_str += f"Timestamp: {log.get('timestamp', 'N/A')}\n"
            log_str += f"Level: {log.get('log_level') or log.get('level', 'N/A')}\n"  # Logstash sends log_level
            log_str += f"Service: {log.get('service_name', 'N/A')}\n"
            log_str += f"Class: {log.get('class_name', 'N/A')}\n"
            log_str += f"Method: {log.get('method_name', 'N/A')}\n"
            log_str += f"Message: {log.get('message', 'N/A')}\n"

            if log.get("stack_trace"):
                log_str += f"Stack Trace:\n{log.get('stack_trace')}\n"

            if log.get("additional_context"):
                log_str += f"Context: {log.get('additional_context')}\n"

            formatted.append(log_str)

        return "\n".join(formatted)

    async def _save_analysis(self, log_id: int, analysis: dict, project_id: str):
        """Save analysis result to log filtered by project_id"""
        try:
            self.client.update_by_query(
                index="logs-*",
                body={
                    "script": {
                        "source": "ctx._source.ai_analysis = params.analysis",
                        "params": {"analysis": analysis},
                    },
                    "query": {
                        "bool": {
                            "must": [
                                {"term": {"log_id": log_id}},
                                {"term": {"project_id": project_id}},
                            ]
                        }
                    },
                },
            )
        except Exception as e:
            print(f"Error saving analysis for log {log_id} in project {project_id}: {e}")

    async def _map_summarize_chunks(
        self, logs: List[dict], chunk_size: int
    ) -> List[str]:
        """
        Map phase: Split logs into chunks and summarize each chunk

        Args:
            logs: List of log dictionaries
            chunk_size: Number of logs per chunk

        Returns:
            List of summary strings (one per chunk)
        """
        # Split logs into chunks
        chunks = [logs[i : i + chunk_size] for i in range(0, len(logs), chunk_size)]
        summaries = []

        for idx, chunk in enumerate(chunks):
            # Format chunk for summarization (lighter than full analysis)
            formatted_chunk = self._format_logs_for_summary(chunk)

            # Summarize this chunk
            try:
                response = await log_summarization_chain.ainvoke(
                    {
                        "chunk_number": idx + 1,
                        "total_chunks": len(chunks),
                        "logs": formatted_chunk,
                    }
                )
                summary = response.content
                summaries.append(summary)
                print(f"  ✅ Chunk {idx + 1}/{len(chunks)} summarized")
            except Exception as e:
                print(f"  ❌ Error summarizing chunk {idx + 1}: {e}")
                # Fallback: use formatted chunk as-is (but truncated)
                summaries.append(formatted_chunk[:500] + "...")

        return summaries

    async def _reduce_analyze(
        self, summaries: List[str], center_log: dict
    ) -> LogAnalysisResult:
        """
        Reduce phase: Analyze combined summaries to produce final analysis

        Args:
            summaries: List of chunk summaries from Map phase
            center_log: The center log (for context)

        Returns:
            Final analysis result
        """
        # Combine summaries into a single context string
        combined_summary = "\n\n".join(
            [f"[Chunk {i + 1}] {summary}" for i, summary in enumerate(summaries)]
        )

        print(f"🔍 Reduce: Analyzing {len(summaries)} chunk summaries")

        # Use the analysis chain with combined summaries
        result = await log_analysis_chain.ainvoke(
            {
                "total_logs": len(summaries),  # Number of chunks (not logs)
                "trace_id": center_log.get("trace_id", "Unknown"),
                "center_log_id": center_log.get("log_id", ""),
                "center_timestamp": center_log.get("timestamp", ""),
                "logs_context": combined_summary,
                "service_name": center_log.get("service_name", "Unknown"),
            }
        )

        return result

    def _format_logs_for_summary(self, logs: List[dict]) -> str:
        """
        Format logs for Map phase summarization (lighter than full analysis)

        Only includes essential fields to minimize token usage
        """
        formatted = []
        for idx, log in enumerate(logs, 1):
            # Only include critical fields
            level = log.get("log_level") or log.get("level", "INFO")
            message = log.get("message", "N/A")
            stack_trace = log.get("stack_trace", "")

            log_str = f"{idx}. [{level}] {message}"

            # Include truncated stack trace only for errors
            if stack_trace and level in ["ERROR", "FATAL", "WARN"]:
                # Only first 3 lines of stack trace
                lines = stack_trace.split("\n")[:3]
                log_str += f"\n   Stack: {' | '.join(lines)}"

            formatted.append(log_str)

        return "\n".join(formatted)

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
