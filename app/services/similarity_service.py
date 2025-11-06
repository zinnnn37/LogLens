"""
Similarity search service using OpenSearch KNN
"""

from typing import List, Dict, Any, Optional
from datetime import datetime, timedelta
from app.core.opensearch import opensearch_client
from app.core.config import settings
from app.utils.index_naming import format_index_name


class SimilarityService:
    """Service for vector similarity search"""

    def __init__(self):
        self.client = opensearch_client

    async def find_similar_logs(
        self,
        log_vector: List[float],
        k: int = 5,
        filters: Optional[Dict[str, Any]] = None,
        project_uuid: Optional[str] = None,
    ) -> List[Dict[str, Any]]:
        """
        Find similar logs using KNN search

        Args:
            log_vector: Query vector
            k: Number of results to return
            filters: Optional filters (e.g., level, service_name)
            project_uuid: Project UUID for multi-tenancy filtering

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
                # Handle exists filter
                if isinstance(value, dict) and "exists" in value:
                    if value["exists"]:
                        filter_clauses.append({"exists": {"field": field}})
                    else:
                        filter_clauses.append({"bool": {"must_not": {"exists": {"field": field}}}})
                else:
                    filter_clauses.append({"term": {field: value}})

        # Always filter by project_uuid if provided
        if project_uuid:
            filter_clauses.append({"term": {"project_uuid": project_uuid}})

        if filter_clauses:
            query["query"]["bool"]["filter"] = filter_clauses

        # Generate index name using new format
        if not project_uuid:
            raise ValueError("project_uuid is required for log search")

        index_name = format_index_name(project_uuid)

        # Execute search
        response = self.client.search(index=index_name, body=query)

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
        self,
        question_vector: List[float],
        k: int = 3,
        project_uuid: Optional[str] = None,
    ) -> List[Dict[str, Any]]:
        """
        Find similar questions in QA cache

        Args:
            question_vector: Question embedding vector
            k: Number of results to return
            project_uuid: Project UUID for multi-tenancy filtering

        Returns:
            List of similar QA pairs with scores
        """
        query = {
            "size": k,
            "query": {
                "bool": {
                    "must": [
                        {
                            "knn": {
                                "question_vector": {
                                    "vector": question_vector,
                                    "k": k,
                                }
                            }
                        }
                    ]
                }
            },
        }

        # Add project_uuid filter if provided
        if project_uuid:
            query["query"]["bool"]["filter"] = [
                {"term": {"metadata.project_uuid": project_uuid}}
            ]

        response = self.client.search(index="qa-cache", body=query)

        results = []
        for hit in response["hits"]["hits"]:
            result = {
                "score": hit["_score"],
                "question": hit["_source"]["question"],
                "answer": hit["_source"]["answer"],
                "related_log_ids": hit["_source"].get("related_log_ids", []),
                "metadata": hit["_source"].get("metadata", {}),  # Include metadata
                "expires_at": hit["_source"].get("expires_at"),  # Include TTL
            }
            results.append(result)

        return results

    async def find_logs_by_trace_id(
        self,
        trace_id: str,
        center_timestamp: str,
        project_uuid: str,
        max_logs: int = 100,
        time_window_seconds: int = 3,
    ) -> List[Dict[str, Any]]:
        """
        Find logs with the same trace_id within a time window

        Args:
            trace_id: The trace ID to search for
            center_timestamp: Center timestamp (ISO format string)
            project_uuid: Project UUID for multi-tenancy filtering
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
                        {"term": {"project_uuid": project_uuid}},
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

        # Generate index name using new format
        index_name = format_index_name(project_uuid)

        # Execute search
        response = self.client.search(index=index_name, body=query)

        # Format results
        results = []
        for hit in response["hits"]["hits"]:
            results.append(hit["_source"])

        return results


# Global instance
similarity_service = SimilarityService()
