"""
검색 도구 (Search Tools)
- 키워드 검색, 의미 유사도 검색
"""

from typing import Optional, List
from datetime import datetime, timedelta
from langchain_core.tools import tool

from app.core.opensearch import opensearch_client
from app.services.similarity_service import similarity_service
from app.services.embedding_service import embedding_service


@tool
async def search_logs_by_keyword(
    keyword: str,
    project_uuid: str,
    level: Optional[str] = None,
    service_name: Optional[str] = None,
    time_hours: int = 24
) -> str:
    """
    키워드로 로그를 검색합니다.

    사용 시나리오:
    - 특정 에러 메시지로 로그 찾기 (예: "NullPointerException")
    - 특정 단어가 포함된 로그 찾기 (예: "user-service")

    입력 파라미터 (JSON 형식):
        keyword: 검색할 키워드 (메시지 내용, 필수)
        level: 로그 레벨 필터 (ERROR, WARN, INFO, DEBUG 중 하나, 선택)
        service_name: 서비스 이름 필터 (선택)
        time_hours: 검색할 시간 범위 (시간 단위, 기본 24시간)

    참고:
    - project_uuid는 자동으로 주입되므로 전달하지 마세요.
    - ⚠️ "검색 결과가 없습니다" 응답은 유효한 결과입니다. 다른 도구로 재시도하지 마세요.

    Returns:
        검색 결과 요약 (건수, 주요 메시지, 시간 정보)
    """
    # 시간 범위 계산
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)

    time_range = {
        "start": start_time.isoformat() + "Z",
        "end": end_time.isoformat() + "Z"
    }

    # OpenSearch Query 구성
    must_clauses = [
        {"match": {"message": keyword}}
    ]

    if level:
        must_clauses.append({"term": {"level": level.upper()}})

    if service_name:
        must_clauses.append({"term": {"service_name": service_name}})

    query = {
        "bool": {
            "must": must_clauses,
            "filter": [
                {
                    "range": {
                        "timestamp": {
                            "gte": time_range["start"],
                            "lte": time_range["end"]
                        }
                    }
                }
            ]
        }
    }

    # 인덱스 패턴 (UUID의 하이픈을 언더스코어로 변환)
    index_pattern = f"{project_uuid.replace('-', '_')}_*"

    try:
        # OpenSearch 검색
        results = opensearch_client.search(
            index=index_pattern,
            body={
                "query": query,
                "size": 20,  # 최대 20개
                "sort": [{"timestamp": "desc"}],
                "_source": ["message", "level", "service_name", "timestamp", "log_id"]
            }
        )

        hits = results.get("hits", {}).get("hits", [])
        total_count = results.get("hits", {}).get("total", {}).get("value", 0)

        if total_count == 0:
            return f"'{keyword}' 키워드로 검색한 결과 {time_hours}시간 내 로그가 없습니다."

        # 결과 포맷팅
        summary_lines = [
            f"'{keyword}' 검색 결과: 총 {total_count}건 (최근 {time_hours}시간)",
            ""
        ]

        # 레벨별 카운트
        level_counts = {}
        for hit in hits:
            log_level = hit["_source"].get("level", "UNKNOWN")
            level_counts[log_level] = level_counts.get(log_level, 0) + 1

        summary_lines.append(f"레벨별 분포: {', '.join([f'{k}: {v}건' for k, v in level_counts.items()])}")
        summary_lines.append("")

        # 상위 5개 로그
        summary_lines.append("최근 로그 5개:")
        for i, hit in enumerate(hits[:5], 1):
            source = hit["_source"]
            msg = source.get("message", "")[:300]
            level_str = source.get("level", "?")
            timestamp_str = source.get("timestamp", "")[:19]
            service = source.get("service_name", "unknown")
            log_id = source.get("log_id", "")

            summary_lines.append(f"{i}. [{level_str}] {timestamp_str} | {service}")
            summary_lines.append(f"   {msg}...")
            if log_id:
                summary_lines.append(f"   (log_id: {log_id})")

        return "\n".join(summary_lines)

    except Exception as e:
        return f"검색 중 오류 발생: {str(e)}"


@tool
async def search_logs_by_similarity(
    query: str,
    project_uuid: str,
    k: int = 5,
    level: Optional[str] = None,
    time_hours: int = 168  # 기본 7일
) -> str:
    """
    의미적 유사도로 로그를 검색합니다 (Vector Search).

    사용 시나리오:
    - 자연어 질문으로 관련 로그 찾기 (예: "사용자 인증 실패 관련 로그")
    - 특정 상황과 유사한 로그 찾기 (예: "데이터베이스 연결 문제")

    입력 파라미터 (JSON 형식):
        query: 검색 쿼리 (자연어, 필수)
        k: 반환할 로그 개수 (기본 5개)
        level: 로그 레벨 필터 (ERROR, WARN, INFO, DEBUG 중 하나, 선택)
        time_hours: 검색할 시간 범위 (시간 단위, 기본 168시간=7일)

    참고:
    - project_uuid는 자동으로 주입되므로 전달하지 마세요.
    - ⚠️ "유사한 로그를 찾을 수 없습니다" 응답은 유효한 결과입니다. 다른 도구로 재시도하지 마세요.

    Returns:
        유사한 로그 목록 (상위 k개)
    """
    try:
        # 쿼리 임베딩
        query_vector = await embedding_service.embed_query(query)

        # 시간 범위 계산
        end_time = datetime.utcnow()
        start_time = end_time - timedelta(hours=time_hours)

        time_range = {
            "start": start_time.isoformat() + "Z",
            "end": end_time.isoformat() + "Z"
        }

        # 필터 구성
        filters = {}
        if level:
            filters["level"] = level.upper()

        # 유사도 검색 (기존 similarity_service 활용)
        results = await similarity_service.find_similar_logs(
            log_vector=query_vector,
            k=k,
            filters=filters,
            project_uuid=project_uuid,
            time_range=time_range
        )

        if not results:
            return f"'{query}' 쿼리로 검색한 결과 {time_hours}시간 내 유사한 로그가 없습니다."

        # 결과 포맷팅
        summary_lines = [
            f"'{query}' 유사도 검색 결과: {len(results)}건 (최근 {time_hours}시간)",
            ""
        ]

        for i, result in enumerate(results, 1):
            log_data = result.get("data", {})
            score = result.get("score", 0.0)

            msg = log_data.get("message", "")[:100]
            level_str = log_data.get("level", "?")
            timestamp_str = log_data.get("timestamp", "")[:19]
            service = log_data.get("service_name", "unknown")
            log_id = log_data.get("log_id", "")

            summary_lines.append(f"{i}. [{level_str}] {timestamp_str} | 유사도: {score:.3f}")
            summary_lines.append(f"   서비스: {service}")
            summary_lines.append(f"   메시지: {msg}...")
            if log_id:
                summary_lines.append(f"   (log_id: {log_id})")
            summary_lines.append("")

        return "\n".join(summary_lines)

    except Exception as e:
        return f"유사도 검색 중 오류 발생: {str(e)}"
