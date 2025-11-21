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
    특정 log_id의 원본 로그 데이터를 조회합니다 (분석 없음).

    이 도구는 다음을 수행합니다:
    - ✅ 특정 log_id의 모든 필드 조회 (메시지, 스택 트레이스, 메타데이터)
    - ✅ AI 분석 결과가 있으면 함께 표시
    - ✅ log_details 상세 정보 포함
    - ✅ trace_id 표시 (연관 로그 추적용)
    - ❌ 새로운 AI 분석은 수행하지 않음 (analyze_single_log 사용)
    - ❌ 연관 로그 자동 조회 안 함 (get_logs_by_trace_id 사용)
    - ❌ 근본 원인/해결책 제시 안 함 (analyze_single_log 사용)

    사용 시나리오:
    1. "log_id 12345의 원본 내용 보여줘"
    2. "스택 트레이스 전체를 확인하고 싶어"
    3. "이 로그의 trace_id가 뭐야?"

    ⚠️ 중요한 제약사항:
    - 이 도구는 **조회만** 합니다. AI 분석이 필요하면 analyze_single_log를 사용하세요
    - "log_id를 찾을 수 없습니다" 응답은 **정상 결과**입니다
    - 사용자가 "분석"을 요청하면 analyze_single_log를 사용하세요

    입력 파라미터 (JSON 형식):
        log_id: 로그 ID (정수, 필수, 예: 1234567890)

    관련 도구:
    - analyze_single_log: 특정 log_id AI 기반 심층 분석 (근본 원인, 해결책)
    - get_logs_by_trace_id: 동일 trace_id를 가진 연관 로그 조회
    - search_logs_by_keyword: 키워드로 로그 검색

    Returns:
        로그 상세 정보 (기본 정보, 메시지, 스택 트레이스, AI 분석 결과)
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
    동일한 trace_id를 가진 모든 연관 로그를 조회합니다 (분산 트레이싱).

    이 도구는 다음을 수행합니다:
    - ✅ 특정 trace_id의 모든 로그를 시간순 조회
    - ✅ 서비스 간 호출 흐름 파악
    - ✅ 서비스별 분포 통계 제공
    - ✅ 최대 50개 로그 반환
    - ❌ AI 기반 근본 원인 분석은 하지 않음 (analyze_single_log 사용)
    - ❌ 특정 log_id의 상세 정보만 필요하면 사용 불가 (get_log_detail 사용)
    - ❌ trace_id가 없는 로그는 조회 불가

    사용 시나리오:
    1. "이 요청의 전체 흐름을 추적해줘" (trace_id 제공 시)
    2. "trace_id abc123의 모든 로그 보여줘"
    3. "서비스 간 호출 순서 확인"

    ⚠️ 중요한 제약사항:
    - "trace_id에 해당하는 로그가 없습니다" 응답은 **정상 결과**입니다
    - 1회 호출로 충분합니다
    - trace_id는 대소문자를 구분합니다

    입력 파라미터 (JSON 형식):
        trace_id: Trace ID (문자열, 필수, 예: "abc123-def456")
        limit: 최대 조회 개수 (기본 50개)

    관련 도구:
    - get_log_detail: 특정 log_id의 상세 정보 조회
    - analyze_single_log: 특정 log_id AI 분석
    - detect_cascading_failures: 연쇄 장애 패턴 감지

    Returns:
        시간순 로그 목록, 서비스별 분포 통계
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
