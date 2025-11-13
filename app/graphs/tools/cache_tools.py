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


def create_check_similarity_cache_tool(project_uuid: str, requesting_log_id: Optional[int] = None, log_metadata: Optional[Dict[str, Any]] = None):
    """
    Similarity Cache 체크 도구 생성 (project_uuid 바인딩)

    Args:
        project_uuid: 프로젝트 UUID
        requesting_log_id: 요청한 로그 ID (자기 자신 제외용)
        log_metadata: 로그 메타데이터 (level, service_name, source_type 등)
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

            # 인덱스 패턴
            index_pattern = f"{project_uuid.replace('-', '_')}_*"

            # Query 구성: must 조건
            must_clauses = [
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

            # 메타데이터 필터링 추가 (level, service_name, source_type 매칭)
            if log_metadata:
                if log_metadata.get("level"):
                    must_clauses.append({"term": {"level": log_metadata["level"]}})
                if log_metadata.get("service_name"):
                    must_clauses.append({"term": {"service_name.keyword": log_metadata["service_name"]}})
                if log_metadata.get("source_type"):
                    must_clauses.append({"term": {"source_type.keyword": log_metadata["source_type"]}})

            # Query 구성: must_not 조건 (자기 자신 제외)
            must_not_clauses = []
            if requesting_log_id:
                must_not_clauses.append({"term": {"log_id": requesting_log_id}})

            # OpenSearch KNN 검색
            query_body = {
                "size": 5,
                "query": {
                    "bool": {
                        "must": must_clauses
                    }
                },
                "_source": ["log_id", "message", "ai_analysis", "level", "service_name", "source_type"]
            }

            if must_not_clauses:
                query_body["query"]["bool"]["must_not"] = must_not_clauses

            results = opensearch_client.search(
                index=index_pattern,
                body=query_body
            )

            hits = results.get("hits", {}).get("hits", [])

            # 로깅: 검색된 후보 개수
            print(f"[Similarity Cache] Searched for log_id={requesting_log_id}, found {len(hits)} candidates")

            for hit in hits:
                score = hit.get("_score", 0)
                # OpenSearch KNN score를 cosine similarity로 변환
                # (score는 이미 0-1 범위의 유사도)
                similarity = score

                if similarity >= threshold:
                    source = hit["_source"]
                    similar_log_id = source.get("log_id")

                    # 자기 자신 매칭 방지 (Post-filter)
                    if similar_log_id == requesting_log_id:
                        print(f"[Similarity Cache] ⚠️ Skipping self-match: log_id={similar_log_id}")
                        continue

                    ai_analysis = source.get("ai_analysis")

                    if ai_analysis and ai_analysis.get("summary"):
                        # 로깅: 캐시 히트
                        print(f"[Similarity Cache] ✅ CACHE HIT - Requested: {requesting_log_id}, Matched: {similar_log_id}, Score: {similarity:.4f}")

                        return json.dumps({
                            "cache_hit": True,
                            "cache_type": "similarity",
                            "similar_log_id": similar_log_id,
                            "similarity_score": similarity,
                            "analysis": ai_analysis
                        }, ensure_ascii=False)

            # 로깅: 캐시 미스
            print(f"[Similarity Cache] ❌ CACHE MISS - Requested: {requesting_log_id}, No match above threshold {threshold}")
            return "CACHE_MISS: 유사한 로그의 분석 결과가 없습니다."

        except Exception as e:
            print(f"[Similarity Cache] ⚠️ ERROR - {str(e)}")
            return f"ERROR: {str(e)}"

    return check_similarity_cache
