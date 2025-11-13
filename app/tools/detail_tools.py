"""
상세 조회 도구 (Detail Tools)
- 특정 로그 상세 정보, trace_id로 연관 로그 조회
"""

from typing import Optional
from langchain_core.tools import tool

from app.core.opensearch import opensearch_client
from app.tools.common_fields import ALL_FIELDS


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

    입력 파라미터 (JSON 형식):
        log_id: 로그 ID (정수, 필수)

    참고: project_uuid는 자동으로 주입되므로 전달하지 마세요.

    Returns:
        로그 상세 정보 (메시지, 스택 트레이스, 메타데이터 전체)
    """
    # 인덱스 패턴 (UUID의 하이픈을 언더스코어로 변환)
    index_pattern = f"{project_uuid.replace('-', '_')}_*"

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

        # 레이어/컴포넌트
        layer = log.get("layer")
        component = log.get("component_name")
        if layer:
            summary_lines.append(f"🏗️  레이어: {layer}")
        if component:
            summary_lines.append(f"🧩 컴포넌트: {component}")

        # Trace ID
        trace_id = log.get("trace_id")
        if trace_id:
            summary_lines.append(f"🔗 Trace ID: {trace_id}")

        summary_lines.append("")

        # log_details 상세 정보
        log_details = log.get("log_details", {})
        if log_details:
            summary_lines.append("🔍 상세 정보:")

            # 클래스/메서드
            class_name = log_details.get("class_name")
            method_name = log_details.get("method_name")
            if class_name:
                summary_lines.append(f"  📍 클래스: {class_name}")
            if method_name:
                summary_lines.append(f"  📍 메서드: {method_name}")

            # 예외 타입
            exception_type = log_details.get("exception_type")
            if exception_type:
                summary_lines.append(f"  ❌ 예외: {exception_type}")

            # 실행 시간
            execution_time = log_details.get("execution_time")
            if execution_time:
                summary_lines.append(f"  ⏱️  실행 시간: {execution_time}ms")

            # HTTP 정보
            http_method = log_details.get("http_method")
            request_uri = log_details.get("request_uri")
            response_status = log_details.get("response_status")
            if http_method or request_uri:
                http_info = f"{http_method or ''} {request_uri or ''}".strip()
                if response_status:
                    http_info += f" → {response_status}"
                summary_lines.append(f"  🌐 HTTP: {http_info}")

            summary_lines.append("")

        # 메시지
        message = log.get("message", "")
        summary_lines.append(f"💬 메시지:")
        summary_lines.append(f"{message}")
        summary_lines.append("")

        # AI 분석 결과
        ai_analysis = log.get("ai_analysis", {})
        if ai_analysis and (ai_analysis.get("summary") or ai_analysis.get("error_cause")):
            summary_lines.append("🤖 AI 분석:")

            ai_summary = ai_analysis.get("summary")
            if ai_summary:
                summary_lines.append(f"  요약: {ai_summary}")

            ai_cause = ai_analysis.get("error_cause")
            if ai_cause:
                summary_lines.append(f"  📌 원인: {ai_cause}")

            ai_solution = ai_analysis.get("solution")
            if ai_solution:
                summary_lines.append(f"  💡 해결책: {ai_solution}")

            ai_tags = ai_analysis.get("tags")
            if ai_tags:
                summary_lines.append(f"  🏷️  태그: {', '.join(ai_tags)}")

            analysis_type = ai_analysis.get("analysis_type")
            if analysis_type:
                summary_lines.append(f"  분석 타입: {analysis_type}")

            analyzed_at = ai_analysis.get("analyzed_at")
            if analyzed_at:
                summary_lines.append(f"  분석 시간: {analyzed_at}")

            summary_lines.append("")

        # 스택 트레이스 (두 곳 확인: 최상위 또는 log_details)
        stack_trace = log.get("stacktrace") or log_details.get("stacktrace")
        if stack_trace:
            summary_lines.append("📚 스택 트레이스:")
            summary_lines.append(stack_trace[:2000])  # 최대 2000자
            if len(stack_trace) > 2000:
                summary_lines.append("... (생략)")
            summary_lines.append("")

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

    입력 파라미터 (JSON 형식):
        trace_id: Trace ID (문자열, 필수)
        limit: 최대 조회 개수 (기본 50개)

    참고: project_uuid는 자동으로 주입되므로 전달하지 마세요.

    Returns:
        연관된 로그 목록 (시간순)
    """
    # 인덱스 패턴 (UUID의 하이픈을 언더스코어로 변환)
    index_pattern = f"{project_uuid.replace('-', '_')}_*"

    # Query 구성 (EC2: trace_id는 text + keyword 멀티필드이므로 .keyword 사용)
    query = {
        "term": {"trace_id.keyword": trace_id}
    }

    try:
        # OpenSearch 검색
        results = opensearch_client.search(
            index=index_pattern,
            body={
                "query": query,
                "size": limit,
                "sort": [{"timestamp": "asc"}],  # 시간순 (오름차순)
                "_source": ALL_FIELDS  # 공통 필드 사용
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
            msg = source.get("message", "")[:300]
            log_id = source.get("log_id", "")
            layer = source.get("layer", "")
            component = source.get("component_name", "")

            # log_details 접근
            log_details = source.get("log_details", {})
            exc_type = log_details.get("exception_type")
            class_name = log_details.get("class_name")
            method_name = log_details.get("method_name")
            http_method = log_details.get("http_method")
            request_uri = log_details.get("request_uri")
            response_status = log_details.get("response_status")

            # AI 분석
            ai_summary = source.get("ai_analysis", {}).get("summary", "")

            # 기본 정보
            summary_lines.append(f"{i}. {timestamp_str} | [{level}] {service}")

            # 레이어/컴포넌트
            if layer:
                summary_lines.append(f"   Layer: {layer}")
            if component:
                summary_lines.append(f"   Component: {component}")

            # 클래스/메서드
            if class_name and method_name:
                summary_lines.append(f"   📍 {class_name}.{method_name}")

            # HTTP 정보
            if http_method and request_uri:
                status_info = f" → {response_status}" if response_status else ""
                summary_lines.append(f"   🌐 {http_method} {request_uri}{status_info}")
            elif response_status:
                summary_lines.append(f"   📊 HTTP {response_status}")

            # 예외
            if exc_type:
                summary_lines.append(f"   ❌ 예외: {exc_type}")

            # 메시지
            summary_lines.append(f"   메시지: {msg}...")

            # AI 분석 (있는 경우)
            if ai_summary:
                summary_lines.append(f"   🤖 {ai_summary[:150]}")

            # log_id
            if log_id:
                summary_lines.append(f"   (log_id: {log_id})")

            summary_lines.append("")

        return "\n".join(summary_lines)

    except Exception as e:
        return f"trace_id 검색 중 오류 발생: {str(e)}"
