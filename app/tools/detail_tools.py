"""
상세 조회 도구 (Detail Tools)
- 특정 로그 상세 정보, trace_id로 연관 로그 조회
"""

from typing import Optional
from langchain_core.tools import tool

from app.core.opensearch import opensearch_client


@tool
async def get_log_detail(
    log_id: int,
    project_uuid: str
) -> str:
    """
    특정 로그의 상세 정보를 조회합니다.

    사용 시나리오:
    - 특정 로그의 전체 내용 확인 (스택 트레이스 포함)
    - 에러 원인 분석을 위한 상세 정보 조회

    Args:
        log_id: 로그 ID (ULID)
        project_uuid: 프로젝트 UUID (언더스코어 형식)

    Returns:
        로그 상세 정보 (메시지, 스택 트레이스, 메타데이터 전체)
    """
    # 인덱스 패턴
    index_pattern = f"{project_uuid}_*"

    # Query 구성
    query = {
        "term": {"log_id": log_id}
    }

    try:
        # OpenSearch 검색
        results = opensearch_client.search(
            index=index_pattern,
            body={
                "query": query,
                "size": 1
            }
        )

        hits = results.get("hits", {}).get("hits", [])

        if not hits:
            return f"log_id '{log_id}'를 찾을 수 없습니다."

        # 로그 데이터
        log = hits[0]["_source"]

        # 결과 포맷팅
        summary_lines = [
            f"=== 로그 상세 정보 (log_id: {log_id}) ===",
            ""
        ]

        # 기본 정보
        summary_lines.append(f"⏰ 시간: {log.get('timestamp', 'N/A')}")
        summary_lines.append(f"📊 레벨: {log.get('level', 'N/A')}")
        summary_lines.append(f"🔧 서비스: {log.get('service_name', 'N/A')}")
        summary_lines.append(f"📝 소스: {log.get('source_type', 'N/A')}")

        # Trace ID
        trace_id = log.get("trace_id")
        if trace_id:
            summary_lines.append(f"🔗 Trace ID: {trace_id}")

        summary_lines.append("")

        # 메시지
        message = log.get("message", "")
        summary_lines.append(f"💬 메시지:")
        summary_lines.append(f"{message}")
        summary_lines.append("")

        # 예외 정보
        exception_type = log.get("exception_type")
        if exception_type:
            summary_lines.append(f"❌ 예외 타입: {exception_type}")

        error_code = log.get("error_code")
        if error_code:
            summary_lines.append(f"🔢 에러 코드: {error_code}")

        # 스택 트레이스
        stack_trace = log.get("stack_trace")
        if stack_trace:
            summary_lines.append("")
            summary_lines.append("📚 스택 트레이스:")
            summary_lines.append(stack_trace[:500])  # 최대 500자
            if len(stack_trace) > 500:
                summary_lines.append("... (생략)")

        # 기타 메타데이터
        summary_lines.append("")
        summary_lines.append("📋 메타데이터:")

        metadata_fields = ["http_method", "http_status", "http_path", "user_id", "session_id"]
        for field in metadata_fields:
            value = log.get(field)
            if value:
                summary_lines.append(f"  - {field}: {value}")

        return "\n".join(summary_lines)

    except Exception as e:
        return f"로그 상세 조회 중 오류 발생: {str(e)}"


@tool
async def get_logs_by_trace_id(
    trace_id: str,
    project_uuid: str,
    limit: int = 50
) -> str:
    """
    동일한 trace_id를 가진 모든 로그를 조회합니다.

    사용 시나리오:
    - 특정 요청의 전체 흐름 추적 (분산 트레이싱)
    - 연관된 로그를 시간순으로 확인하여 문제 원인 파악

    Args:
        trace_id: Trace ID
        project_uuid: 프로젝트 UUID (언더스코어 형식)
        limit: 최대 조회 개수 (기본 50개)

    Returns:
        연관된 로그 목록 (시간순)
    """
    # 인덱스 패턴
    index_pattern = f"{project_uuid}_*"

    # Query 구성
    query = {
        "term": {"trace_id": trace_id}
    }

    try:
        # OpenSearch 검색
        results = opensearch_client.search(
            index=index_pattern,
            body={
                "query": query,
                "size": limit,
                "sort": [{"timestamp": "asc"}],  # 시간순 (오름차순)
                "_source": [
                    "log_id", "timestamp", "level", "service_name",
                    "message", "exception_type", "http_status"
                ]
            }
        )

        hits = results.get("hits", {}).get("hits", [])
        total_count = results.get("hits", {}).get("total", {}).get("value", 0)

        if total_count == 0:
            return f"trace_id '{trace_id}'에 해당하는 로그가 없습니다."

        # 결과 포맷팅
        summary_lines = [
            f"=== Trace ID: {trace_id} ===",
            f"총 {total_count}건의 연관 로그 ({len(hits)}건 표시)",
            ""
        ]

        # 서비스별 카운트
        service_counts = {}
        for hit in hits:
            service = hit["_source"].get("service_name", "unknown")
            service_counts[service] = service_counts.get(service, 0) + 1

        if service_counts:
            summary_lines.append("서비스별 분포:")
            for service, count in service_counts.items():
                summary_lines.append(f"  - {service}: {count}건")
            summary_lines.append("")

        # 로그 목록 (시간순)
        summary_lines.append("로그 흐름 (시간순):")
        for i, hit in enumerate(hits, 1):
            source = hit["_source"]
            timestamp_str = source.get("timestamp", "")[:23]
            level = source.get("level", "?")
            service = source.get("service_name", "unknown")
            msg = source.get("message", "")[:100]
            log_id = source.get("log_id", "")
            http_status = source.get("http_status")
            exc_type = source.get("exception_type")

            summary_lines.append(f"{i}. {timestamp_str} | [{level}] {service}")

            if http_status:
                summary_lines.append(f"   HTTP {http_status}")

            if exc_type:
                summary_lines.append(f"   예외: {exc_type}")

            summary_lines.append(f"   메시지: {msg}...")

            if log_id:
                summary_lines.append(f"   (log_id: {log_id})")

            summary_lines.append("")

        return "\n".join(summary_lines)

    except Exception as e:
        return f"trace_id 검색 중 오류 발생: {str(e)}"
