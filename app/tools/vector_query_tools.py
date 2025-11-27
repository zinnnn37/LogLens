"""
Vector 기반 복합 쿼리 도구

복잡한 SQL WHERE 조건을 자연어로 대체하여 Vector DB의 의미적 검색 능력을 활용
"""

from typing import Dict, Any, List, Optional
from datetime import datetime, timedelta
from langchain_core.tools import tool

from app.services.embedding_service import embedding_service
from app.services.similarity_service import similarity_service
from app.core.opensearch import opensearch_client


@tool
async def complex_log_search(
    description: str,
    project_uuid: str,
    time_range: str = "24h",
    max_results: int = 20
) -> str:
    """
    복잡한 SQL 조건 대신 자연어 설명으로 로그 검색

    전통적 방식:
    WHERE service = 'payment'
      AND level IN ('ERROR', 'FATAL')
      AND timestamp > NOW() - INTERVAL '7 days'
      AND (message LIKE '%critical%' OR message LIKE '%severe%')

    Vector 방식:
    의미적 유사도로 자동 필터링

    Args:
        description: 검색 조건 자연어 설명 (예: "지난주 payment 서비스의 심각한 에러")
        project_uuid: 프로젝트 UUID
        time_range: 검색 시간 범위 (예: "24h", "7d", "30d")
        max_results: 최대 결과 개수 (기본: 20)

    Returns:
        JSON 형식의 로그 검색 결과
    """
    # 설명을 embedding으로 변환
    query_vector = await embedding_service.embed_query(description)

    # 시간 범위 파싱
    time_filter = _parse_time_range(time_range)

    # Vector KNN 검색 (기본 필터만)
    results = await similarity_service.find_similar_logs(
        log_vector=query_vector,
        k=max_results,
        filters=time_filter,
        project_uuid=project_uuid
    )

    if not results:
        return f"검색 결과 없음: '{description}'"

    # 결과 포맷팅
    formatted_results = []
    for i, log in enumerate(results, 1):
        formatted_results.append({
            "rank": i,
            "similarity_score": round(log.get("score", 0), 4),
            "log_id": log.get("log_id"),
            "level": log.get("level"),
            "timestamp": log.get("timestamp"),
            "service": log.get("service_name", "unknown"),
            "message": log.get("message", "")[:200]
        })

    return f"""📊 자연어 검색 결과

🔍 검색 쿼리: "{description}"
📅 시간 범위: {time_range}
📋 결과 개수: {len(formatted_results)}개

{_format_results_table(formatted_results)}

✅ Vector 검색으로 복잡한 WHERE 조건 없이 의미적으로 유사한 로그를 찾았습니다.
"""


@tool
async def multi_condition_search(
    service_names: Optional[List[str]] = None,
    log_levels: Optional[List[str]] = None,
    keywords: Optional[List[str]] = None,
    project_uuid: str = None,
    time_range: str = "24h",
    max_results: int = 20
) -> str:
    """
    다중 조건 검색을 Vector로 간소화

    전통적 방식:
    WHERE service_name IN ('user-service', 'auth-service')
      AND level IN ('ERROR', 'WARN')
      AND (message LIKE '%timeout%' OR message LIKE '%connection%')
      AND timestamp > NOW() - INTERVAL '1 day'

    Vector 방식:
    조건들을 자연어로 결합하여 Vector 검색

    Args:
        service_names: 서비스 이름 리스트 (예: ["user-service", "auth-service"])
        log_levels: 로그 레벨 리스트 (예: ["ERROR", "WARN"])
        keywords: 검색 키워드 리스트 (예: ["timeout", "connection"])
        project_uuid: 프로젝트 UUID
        time_range: 검색 시간 범위
        max_results: 최대 결과 개수

    Returns:
        JSON 형식의 로그 검색 결과
    """
    # 조건들을 자연어로 결합
    conditions = []

    if service_names:
        services = ", ".join(service_names)
        conditions.append(f"{services} 서비스")

    if log_levels:
        levels = ", ".join(log_levels)
        conditions.append(f"{levels} 레벨")

    if keywords:
        words = ", ".join(keywords)
        conditions.append(f"{words} 키워드 포함")

    if not conditions:
        return "❌ 검색 조건이 없습니다."

    # 자연어 쿼리 생성
    query_text = " ".join(conditions) + " 로그"

    # complex_log_search 재사용
    return await complex_log_search(
        description=query_text,
        project_uuid=project_uuid,
        time_range=time_range,
        max_results=max_results
    )


def _parse_time_range(time_range: str) -> Dict[str, Any]:
    """
    시간 범위 문자열을 OpenSearch 필터로 변환

    Args:
        time_range: 시간 범위 (예: "24h", "7d", "30d")

    Returns:
        OpenSearch timestamp 필터
    """
    unit = time_range[-1]  # 마지막 문자 (h, d, m)
    value = int(time_range[:-1])  # 숫자 부분

    if unit == 'h':
        delta = timedelta(hours=value)
    elif unit == 'd':
        delta = timedelta(days=value)
    elif unit == 'm':
        delta = timedelta(minutes=value)
    else:
        delta = timedelta(hours=24)  # 기본값

    start_time = datetime.utcnow() - delta

    return {
        "timestamp": {
            "gte": start_time.isoformat() + "Z"
        }
    }


def _format_results_table(results: List[Dict[str, Any]]) -> str:
    """
    검색 결과를 마크다운 테이블로 포맷팅

    Args:
        results: 검색 결과 리스트

    Returns:
        마크다운 테이블 문자열
    """
    if not results:
        return "결과 없음"

    table = "| # | 유사도 | 레벨 | 시간 | 서비스 | 메시지 |\n"
    table += "|---|--------|------|------|--------|--------|\n"

    for r in results[:10]:  # 최대 10개만 표시
        rank = r.get("rank", "?")
        score = r.get("similarity_score", 0)
        level = r.get("level", "?")
        timestamp = r.get("timestamp", "")[:16] if r.get("timestamp") else "?"
        service = r.get("service", "unknown")[:20]
        message = r.get("message", "")[:50]

        table += f"| {rank} | {score:.2f} | {level} | {timestamp} | {service} | {message}... |\n"

    if len(results) > 10:
        table += f"\n... 외 {len(results) - 10}개 결과 더 있음\n"

    return table
