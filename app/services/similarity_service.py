"""
Similarity search service using OpenSearch KNN
"""

from typing import List, Dict, Any, Optional
from datetime import datetime, timedelta
from app.core.opensearch import opensearch_client
from app.core.config import settings


class SimilarityService:
    """Service for vector similarity search"""

    def __init__(self):
        self.client = opensearch_client
        self.index_pattern = "logs-*"

    async def find_similar_logs(
        self,
        log_vector: List[float],
        k: int = 5,
        filters: Optional[Dict[str, Any]] = None,
        project_id: Optional[str] = None,
    ) -> List[Dict[str, Any]]:
        """
        Find similar logs using KNN search

        Args:
            log_vector: Query vector
            k: Number of results to return
            filters: Optional filters (e.g., level, service_name)
            project_id: Project ID for multi-tenancy filtering

        Returns:
            List of similar logs with scores
        """
        # Build query
        query = {
            "size": k,
            "query": {
                "bool": {
                    "must": [
                        {
                            "knn": {
                                "log_vector": {
                                    "vector": log_vector,
                                    "k": k,
                                }
                            }
                        }
                    ]
                }
            },
            "_source": {
                "excludes": ["log_vector"]  # Don't return the vector
            },
        }

        # Add filters if provided
        filter_clauses = []
        if filters:
            for field, value in filters.items():
                filter_clauses.append({"term": {field: value}})

        # Always filter by project_id if provided
        if project_id:
            filter_clauses.append({"term": {"project_id": project_id}})

        if filter_clauses:
            query["query"]["bool"]["filter"] = filter_clauses

        # Execute search
        response = self.client.search(index=self.index_pattern, body=query)

        # Format results
        results = []
        for hit in response["hits"]["hits"]:
            result = {
                "log_id": hit["_source"]["log_id"],
                "score": hit["_score"],
                "data": hit["_source"],
            }
            results.append(result)

        return results

    async def find_similar_questions(
        self, question_vector: List[float], k: int = 3
    ) -> List[Dict[str, Any]]:
        """
        Find similar questions in QA cache

        Args:
            question_vector: Question embedding vector
            k: Number of results to return

        Returns:
            List of similar QA pairs with scores
        """
        query = {
            "size": k,
            "query": {
                "knn": {
                    "question_vector": {
                        "vector": question_vector,
                        "k": k,
                    }
                }
            },
        }

        response = self.client.search(index="qa-cache", body=query)

        results = []
        for hit in response["hits"]["hits"]:
            result = {
                "score": hit["_score"],
                "question": hit["_source"]["question"],
                "answer": hit["_source"]["answer"],
                "related_log_ids": hit["_source"].get("related_log_ids", []),
            }
            results.append(result)

        return results

    async def find_logs_by_trace_id(
        self,
        trace_id: str,
        center_timestamp: str,
        project_id: str,
        max_logs: int = 100,
        time_window_seconds: int = 3,
    ) -> List[Dict[str, Any]]:
        """
        Find logs with the same trace_id within a time window

        Args:
            trace_id: The trace ID to search for
            center_timestamp: Center timestamp (ISO format string)
            project_id: Project ID for multi-tenancy filtering
            max_logs: Maximum number of logs to return (default: 100)
            time_window_seconds: Time window in seconds (±3 seconds by default)

        Returns:
            List of logs sorted by timestamp
        """
        # Parse center timestamp
        try:
            center_time = datetime.fromisoformat(
                center_timestamp.replace("Z", "+00:00")
            )
        except Exception:
            # Fallback: try parsing without timezone
            center_time = datetime.strptime(
                center_timestamp[:19], "%Y-%m-%dT%H:%M:%S"
            )

        # Calculate time range (± 3 seconds)
        start_time = center_time - timedelta(seconds=time_window_seconds)
        end_time = center_time + timedelta(seconds=time_window_seconds)

        # Build OpenSearch query
        query = {
            "size": max_logs,
            "query": {
                "bool": {
                    "must": [
                        {"term": {"trace_id": trace_id}},
                        {"term": {"project_id": project_id}},
                    ],
                    "filter": [
                        {
                            "range": {
                                "timestamp": {
                                    "gte": start_time.isoformat(),
                                    "lte": end_time.isoformat(),
                                }
                            }
                        }
                    ],
                }
            },
            "sort": [{"timestamp": {"order": "asc"}}],
            "_source": {"excludes": ["log_vector"]},  # Exclude vector for efficiency
        }

        # Execute search
        response = self.client.search(index=self.index_pattern, body=query)

        # Format results
        results = []
        for hit in response["hits"]["hits"]:
            results.append(hit["_source"])

        return results


# Global instance
similarity_service = SimilarityService()
