"""
Similarity search service using OpenSearch KNN
"""

from typing import List, Dict, Any, Optional
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
    ) -> List[Dict[str, Any]]:
        """
        Find similar logs using KNN search

        Args:
            log_vector: Query vector
            k: Number of results to return
            filters: Optional filters (e.g., level, service_name)

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
        if filters:
            filter_clauses = []
            for field, value in filters.items():
                filter_clauses.append({"term": {field: value}})
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


# Global instance
similarity_service = SimilarityService()
