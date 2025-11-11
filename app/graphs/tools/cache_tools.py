"""
Cache Tools for Log Analysis Agent

3개의 캐시 체크 도구:
1. check_direct_cache: 직접 캐시 (로그가 이미 분석됨)
2. check_trace_cache: 트레이스 캐시 (같은 trace의 다른 로그 분석 활용)
3. check_similarity_cache: 유사도 캐시 (유사한 로그의 분석 활용)
"""

from typing import Dict, Any, Optional
from langchain_core.tools import tool

from app.core.opensearch import opensearch_client
from app.services.similarity_service import similarity_service
from app.services.embedding_service import embedding_service


def create_check_direct_cache_tool(project_uuid: str):
    """
    Direct Cache 체크 도구 생성 (project_uuid 바인딩)
    """

    @tool
    async def check_direct_cache(log_id: int) -> str:
        """
        로그가 이미 분석되었는지 확인합니다.

        Args:
            log_id: 로그 ID (정수)

        Returns:
            캐시된 분석 결과 JSON 문자열 또는 "CACHE_MISS"
        """
        import json

        # 인덱스 패턴
        index_pattern = f"{project_uuid.replace('-', '_')}_*"

        try:
            # OpenSearch에서 로그 조회
            results = opensearch_client.search(
                index=index_pattern,
                body={
                    "query": {"term": {"log_id": log_id}},
                    "size": 1,
                    "_source": ["ai_analysis"]
                }
            )

            hits = results.get("hits", {}).get("hits", [])
            if not hits:
                return "CACHE_MISS: 로그를 찾을 수 없습니다."

            ai_analysis = hits[0]["_source"].get("ai_analysis")

            # ai_analysis가 있고 summary가 있으면 캐시 hit
            if ai_analysis and ai_analysis.get("summary"):
                return json.dumps({
                    "cache_hit": True,
                    "cache_type": "direct",
                    "analysis": ai_analysis
                }, ensure_ascii=False)

            return "CACHE_MISS: 분석 결과가 없습니다."

        except Exception as e:
            return f"ERROR: {str(e)}"

    return check_direct_cache


def create_check_trace_cache_tool(project_uuid: str):
    """
    Trace Cache 체크 도구 생성 (project_uuid 바인딩)
    """

    @tool
    async def check_trace_cache(trace_id: str) -> str:
        """
        동일한 trace_id를 가진 다른 로그의 분석 결과를 확인합니다.

        Args:
            trace_id: Trace ID (문자열)

        Returns:
            캐시된 분석 결과 JSON 문자열 또는 "CACHE_MISS"
        """
        import json

        if not trace_id:
            return "CACHE_MISS: trace_id가 없습니다."

        # 인덱스 패턴
        index_pattern = f"{project_uuid.replace('-', '_')}_*"

        try:
            # Trace에 속한 로그 중 분석된 로그 찾기
            results = opensearch_client.search(
                index=index_pattern,
                body={
                    "query": {
                        "bool": {
                            "must": [
                                {"term": {"trace_id": trace_id}},
                                {"exists": {"field": "ai_analysis.summary"}}
                            ]
                        }
                    },
                    "size": 1,
                    "_source": ["ai_analysis", "log_id"]
                }
            )

            hits = results.get("hits", {}).get("hits", [])
            if not hits:
                return "CACHE_MISS: trace에 분석된 로그가 없습니다."

            source = hits[0]["_source"]
            source_log_id = source.get("log_id")
            ai_analysis = source.get("ai_analysis")

            if ai_analysis and ai_analysis.get("summary"):
                return json.dumps({
                    "cache_hit": True,
                    "cache_type": "trace",
                    "source_log_id": source_log_id,
                    "analysis": ai_analysis
                }, ensure_ascii=False)

            return "CACHE_MISS: trace에 유효한 분석 결과가 없습니다."

        except Exception as e:
            return f"ERROR: {str(e)}"

    return check_trace_cache


def create_check_similarity_cache_tool(project_uuid: str):
    """
    Similarity Cache 체크 도구 생성 (project_uuid 바인딩)
    """

    @tool
    async def check_similarity_cache(
        log_message: str,
        threshold: float = 0.8
    ) -> str:
        """
        유사한 로그의 분석 결과를 찾습니다.

        Args:
            log_message: 로그 메시지 (문자열)
            threshold: 유사도 임계값 (0.0-1.0, 기본 0.8)

        Returns:
            유사한 로그의 분석 결과 JSON 문자열 또는 "CACHE_MISS"
        """
        import json

        if not log_message:
            return "CACHE_MISS: 로그 메시지가 없습니다."

        try:
            # 임베딩 생성
            log_vector = await embedding_service.embed_query(log_message)

            # 유사한 로그 검색 (분석 결과가 있는 로그만)
            filters = {}  # 레벨 필터 등을 추가 가능

            # 인덱스 패턴
            index_pattern = f"{project_uuid.replace('-', '_')}_*"

            # OpenSearch KNN 검색
            results = opensearch_client.search(
                index=index_pattern,
                body={
                    "size": 5,
                    "query": {
                        "bool": {
                            "must": [
                                {
                                    "knn": {
                                        "message_embedding": {
                                            "vector": log_vector,
                                            "k": 5
                                        }
                                    }
                                },
                                {"exists": {"field": "ai_analysis.summary"}}
                            ]
                        }
                    },
                    "_source": ["log_id", "message", "ai_analysis"]
                }
            )

            hits = results.get("hits", {}).get("hits", [])

            for hit in hits:
                score = hit.get("_score", 0)
                # OpenSearch KNN score를 cosine similarity로 변환
                # (score는 이미 0-1 범위의 유사도)
                similarity = score

                if similarity >= threshold:
                    source = hit["_source"]
                    similar_log_id = source.get("log_id")
                    ai_analysis = source.get("ai_analysis")

                    if ai_analysis and ai_analysis.get("summary"):
                        return json.dumps({
                            "cache_hit": True,
                            "cache_type": "similarity",
                            "similar_log_id": similar_log_id,
                            "similarity_score": similarity,
                            "analysis": ai_analysis
                        }, ensure_ascii=False)

            return "CACHE_MISS: 유사한 로그의 분석 결과가 없습니다."

        except Exception as e:
            return f"ERROR: {str(e)}"

    return check_similarity_cache
