"""
분석 도구 (Analysis Tools)
- 로그 통계, 최근 에러 분석
"""

import re
from typing import Optional, Dict, Any
from datetime import datetime, timedelta
from langchain_core.tools import tool

from app.core.opensearch import opensearch_client


def extract_exception_type(source: Dict[str, Any]) -> str:
    """
    로그 데이터에서 예외 타입을 추출합니다.

    우선순위:
    1. log_details.exception_type (nested 필드)
    2. ai_analysis.tags (AI가 분석한 태그)
    3. message에서 정규식 추출
    4. "Unknown" 반환
    """
    # 1. log_details.exception_type 우선
    log_details = source.get("log_details", {})
    exc_type = log_details.get("exception_type")
    if exc_type and exc_type != "Unknown" and exc_type.strip():
        return exc_type

    # 2. AI 분석 태그에서 추출
    ai_analysis = source.get("ai_analysis", {})
    ai_tags = ai_analysis.get("tags", [])
    if ai_tags:
        for tag in ai_tags:
            if "Exception" in tag or "Error" in tag or "Timeout" in tag:
                return tag

    # 3. message에서 정규식 추출
    message = source.get("message", "")
    patterns = [
        r'([A-Z][a-zA-Z]*Exception)',
        r'([A-Z][a-zA-Z]*Error)',
        r'(DatabaseTimeout|ConnectionRefused|ConnectionPoolExhausted|PoolExhausted|Timeout)',
    ]
    for pattern in patterns:
        match = re.search(pattern, message)
        if match:
            return match.group(1)

    return "Unknown"


def assess_severity(source: Dict[str, Any]) -> int:
    """
    에러 심각도를 평가합니다.

    Returns:
        1: CRITICAL (최고 심각도) - 즉시 조치 필요
        2: HIGH (높음) - 긴급 조치 필요
        3: MEDIUM (중간) - 우선 조치 필요
        4: LOW (낮음) - 모니터링 필요
        5: MINIMAL (최소) - 정보성
    """
    exc_type = extract_exception_type(source)
    log_details = source.get("log_details", {})
    response_status = log_details.get("response_status", 0)

    # AI 분석이 있으면 우선 활용
    ai_analysis = source.get("ai_analysis", {})
    analysis_type = ai_analysis.get("analysis_type", "").upper()
    if analysis_type == "CRITICAL":
        return 1
    elif analysis_type == "HIGH":
        return 2
    elif analysis_type == "MEDIUM":
        return 3

    # Database/Connection 에러 = CRITICAL (모든 사용자 영향)
    critical_keywords = [
        "Database", "Connection", "Pool", "Timeout",
        "OutOfMemory", "StackOverflow", "Deadlock"
    ]
    if any(keyword in exc_type for keyword in critical_keywords):
        return 1

    # 5xx 에러 = HIGH (서버 오류)
    if 500 <= response_status < 600:
        return 2

    # Security/Auth 에러 = HIGH (보안 위험)
    security_keywords = ["Auth", "Security", "Unauthorized", "Token", "Permission"]
    if any(keyword in exc_type for keyword in security_keywords):
        return 2

    # NullPointer, Runtime = MEDIUM (특정 기능 영향)
    medium_keywords = ["NullPointer", "Runtime", "IllegalState", "IllegalArgument"]
    if any(keyword in exc_type for keyword in medium_keywords):
        return 3

    # 4xx 에러 = LOW (클라이언트 오류)
    if 400 <= response_status < 500:
        return 4

    # 기타 = MINIMAL
    return 5


@tool
async def get_log_statistics(
    project_uuid: str,
    time_hours: int = 24,
    group_by: str = "level"
) -> str:
    """
    로그 통계를 조회합니다.

    사용 시나리오:
    - 시스템 전체 상태 확인 (예: "시스템 상태 요약해줘")
    - 특정 기간의 로그 분포 확인 (예: "오늘 에러가 몇 개야?")
    - 서비스별 로그 현황 파악

    입력 파라미터 (JSON 형식):
        time_hours: 통계 기간 (시간 단위, 기본 24시간)
        group_by: 집계 기준 (level, service_name, source_type 중 하나, 기본 level)

    참고:
    - project_uuid는 자동으로 주입되므로 전달하지 마세요.
    - ⚠️ "로그가 없습니다" 응답은 유효한 결과입니다. 다른 도구로 재시도하지 마세요.

    Returns:
        통계 정보 (레벨별/서비스별 로그 개수, 에러율 등)
    """
    # 시간 범위 계산
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)

    time_range = {
        "start": start_time.isoformat() + "Z",
        "end": end_time.isoformat() + "Z"
    }

    # 인덱스 패턴 (UUID의 하이픈을 언더스코어로 변환)
    index_pattern = f"{project_uuid.replace('-', '_')}_*"

    # Aggregation 쿼리
    query_body = {
        "query": {
            "range": {
                "timestamp": {
                    "gte": time_range["start"],
                    "lte": time_range["end"]
                }
            }
        },
        "size": 0,  # 통계만 필요 (문서는 불필요)
        "aggs": {
            "by_level": {
                "terms": {
                    "field": "level",
                    "size": 10
                }
            },
            "by_service": {
                "terms": {
                    "field": "service_name",
                    "size": 10
                }
            },
            "by_source": {
                "terms": {
                    "field": "source_type",
                    "size": 10
                }
            },
            "timeline": {
                "date_histogram": {
                    "field": "timestamp",
                    "fixed_interval": f"{max(1, time_hours // 24)}h"  # 시간대별
                }
            }
        }
    }

    try:
        # OpenSearch Aggregation 실행 (sync client)
        results = opensearch_client.search(
            index=index_pattern,
            body=query_body
        )

        # 전체 로그 개수
        total_count = results.get("hits", {}).get("total", {}).get("value", 0)

        if total_count == 0:
            return f"최근 {time_hours}시간 동안 로그가 없습니다."

        aggs = results.get("aggregations", {})

        # 레벨별 통계
        level_buckets = aggs.get("by_level", {}).get("buckets", [])
        level_stats = {bucket["key"]: bucket["doc_count"] for bucket in level_buckets}

        # 서비스별 통계
        service_buckets = aggs.get("by_service", {}).get("buckets", [])
        service_stats = {bucket["key"]: bucket["doc_count"] for bucket in service_buckets}

        # 소스별 통계
        source_buckets = aggs.get("by_source", {}).get("buckets", [])
        source_stats = {bucket["key"]: bucket["doc_count"] for bucket in source_buckets}

        # 결과 포맷팅
        summary_lines = [
            f"=== 로그 통계 (최근 {time_hours}시간) ===",
            f"총 로그 개수: {total_count}건",
            ""
        ]

        # 레벨별
        if level_stats:
            summary_lines.append("📊 레벨별 분포:")
            for level, count in sorted(level_stats.items(), key=lambda x: x[1], reverse=True):
                percentage = (count / total_count) * 100
                summary_lines.append(f"  - {level}: {count}건 ({percentage:.1f}%)")
            summary_lines.append("")

        # 에러율
        error_count = level_stats.get("ERROR", 0)
        if error_count > 0:
            error_rate = (error_count / total_count) * 100
            summary_lines.append(f"⚠️  에러율: {error_rate:.2f}% ({error_count}건)")
            summary_lines.append("")

        # 서비스별
        if service_stats and group_by in ["service_name", "all"]:
            summary_lines.append("🔧 서비스별 로그:")
            for service, count in sorted(service_stats.items(), key=lambda x: x[1], reverse=True)[:5]:
                percentage = (count / total_count) * 100
                summary_lines.append(f"  - {service}: {count}건 ({percentage:.1f}%)")
            summary_lines.append("")

        # 소스별
        if source_stats and group_by in ["source_type", "all"]:
            summary_lines.append("📝 소스별 로그:")
            for source, count in sorted(source_stats.items(), key=lambda x: x[1], reverse=True):
                summary_lines.append(f"  - {source}: {count}건")
            summary_lines.append("")

        return "\n".join(summary_lines)

    except Exception as e:
        return f"통계 조회 중 오류 발생: {str(e)}"


@tool
async def get_recent_errors(
    project_uuid: str,
    limit: int = 10,
    service_name: Optional[str] = None,
    time_hours: int = 24
) -> str:
    """
    최근 에러 로그를 시간순으로 조회합니다.

    사용 시나리오:
    - 최근 발생한 에러 확인 (예: "최근 에러가 뭐야?")
    - 특정 서비스의 에러만 조회 (예: "user-service 에러 보여줘")
    - 에러 발생 빈도 파악

    입력 파라미터 (JSON 형식):
        limit: 조회할 개수 (기본 10개)
        service_name: 특정 서비스만 조회 (선택)
        time_hours: 검색할 시간 범위 (시간 단위, 기본 24시간)

    참고:
    - project_uuid는 자동으로 주입되므로 전달하지 마세요.
    - ⚠️ "ERROR 레벨 로그가 없습니다" 응답은 유효한 결과입니다. 다른 도구로 재시도하지 마세요.
    - ⚠️ 전체 에러 현황을 파악하려면 service_name 필터 없이 먼저 조회하세요. 서비스별 상세 분석은 그 다음에 필요시에만 수행하세요.

    Returns:
        최근 에러 목록 (시간 역순)
    """
    # 시간 범위 계산
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)

    time_range = {
        "start": start_time.isoformat() + "Z",
        "end": end_time.isoformat() + "Z"
    }

    # 인덱스 패턴 (UUID의 하이픈을 언더스코어로 변환)
    index_pattern = f"{project_uuid.replace('-', '_')}_*"

    # Query 구성
    must_clauses = [
        {"term": {"level": "ERROR"}}
    ]

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

    try:
        # OpenSearch 검색 (sync client)
        results = opensearch_client.search(
            index=index_pattern,
            body={
                "query": query,
                "size": limit,
                "sort": [{"timestamp": "desc"}],
                "_source": [
                    "message", "level", "service_name", "timestamp", "log_id",
                    "stacktrace",  # 필드명 수정 (stack_trace -> stacktrace)
                    "layer", "component_name",
                    # Nested fields (log_details)
                    "log_details.exception_type",
                    "log_details.class_name",
                    "log_details.method_name",
                    "log_details.http_method",
                    "log_details.request_uri",
                    "log_details.response_status",
                    "log_details.execution_time",
                    "log_details.stacktrace",
                    # AI analysis fields
                    "ai_analysis.summary",
                    "ai_analysis.error_cause",
                    "ai_analysis.solution",
                    "ai_analysis.tags",
                    "ai_analysis.analysis_type"
                ]
            }
        )

        hits = results.get("hits", {}).get("hits", [])
        total_count = results.get("hits", {}).get("total", {}).get("value", 0)

        if total_count == 0:
            service_filter = f" (서비스: {service_name})" if service_name else ""
            return f"최근 {time_hours}시간 동안 ERROR 레벨 로그가 없습니다{service_filter}."

        # 에러 타입별 카운트 (헬퍼 함수 사용)
        error_types = {}
        errors_with_severity = []  # (hit, severity) 튜플 리스트
        for hit in hits:
            source = hit["_source"]
            exc_type = extract_exception_type(source)
            severity = assess_severity(source)
            error_types[exc_type] = error_types.get(exc_type, 0) + 1
            errors_with_severity.append((hit, severity))

        # 심각도순 정렬 (낮은 숫자 = 높은 심각도)
        errors_with_severity.sort(key=lambda x: (x[1], x[0]["_source"].get("timestamp", "")), reverse=True)

        # 결과 포맷팅
        service_filter_str = f" (서비스: {service_name})" if service_name else ""
        summary_lines = [
            f"=== 최근 에러 로그 (최근 {time_hours}시간){service_filter_str} ===",
            f"총 {total_count}건의 에러 발생, 상위 {len(hits)}건 표시",
            ""
        ]

        # 에러 타입별 분포
        if error_types:
            summary_lines.append("에러 타입별 분포:")
            for exc_type, count in sorted(error_types.items(), key=lambda x: x[1], reverse=True)[:5]:
                summary_lines.append(f"  - {exc_type}: {count}건")
            summary_lines.append("")

        # 상위 에러 목록 (심각도순)
        summary_lines.append("최근 에러 목록 (심각도순):")
        severity_labels = {1: "CRITICAL", 2: "HIGH", 3: "MEDIUM", 4: "LOW", 5: "MINIMAL"}

        for i, (hit, severity) in enumerate(errors_with_severity, 1):
            source = hit["_source"]
            msg = source.get("message", "")[:400]
            timestamp_str = source.get("timestamp", "")[:19]
            service = source.get("service_name", "unknown")
            log_id = source.get("log_id", "")
            layer = source.get("layer", "")
            component = source.get("component_name", "")

            # 에러 타입 추출
            exc_type = extract_exception_type(source)

            # log_details 접근
            log_details = source.get("log_details", {})
            class_name = log_details.get("class_name", "")
            method_name = log_details.get("method_name", "")
            http_method = log_details.get("http_method", "")
            request_uri = log_details.get("request_uri", "")
            response_status = log_details.get("response_status")
            execution_time = log_details.get("execution_time")

            # 스택 트레이스 존재 여부
            has_stack = bool(source.get("stacktrace") or log_details.get("stacktrace"))

            # AI 분석 결과
            ai_analysis = source.get("ai_analysis", {})
            ai_summary = ai_analysis.get("summary", "")
            ai_cause = ai_analysis.get("error_cause", "")
            ai_solution = ai_analysis.get("solution", "")

            # 기본 정보 출력
            severity_label = severity_labels.get(severity, "UNKNOWN")
            summary_lines.append(f"{i}. [{exc_type}] {timestamp_str} | 심각도: {severity_label}")
            summary_lines.append(f"   서비스: {service}")

            # 레이어/컴포넌트
            if layer or component:
                loc_info = []
                if layer:
                    loc_info.append(f"Layer: {layer}")
                if component:
                    loc_info.append(f"Component: {component}")
                summary_lines.append(f"   위치: {', '.join(loc_info)}")

            # 클래스/메서드
            if class_name and method_name:
                summary_lines.append(f"   📍 {class_name}.{method_name}")
            elif class_name:
                summary_lines.append(f"   📍 {class_name}")

            # HTTP 정보
            if http_method and request_uri:
                status_info = f" → {response_status}" if response_status else ""
                summary_lines.append(f"   🌐 {http_method} {request_uri}{status_info}")
            elif response_status:
                summary_lines.append(f"   📊 HTTP {response_status}")

            # 실행 시간
            if execution_time:
                summary_lines.append(f"   ⏱️  {execution_time}ms")

            # 메시지
            summary_lines.append(f"   💬 {msg}...")

            # AI 분석 결과 (있는 경우)
            if ai_summary:
                summary_lines.append(f"   🤖 AI 분석: {ai_summary[:200]}")
            if ai_cause:
                summary_lines.append(f"   📌 원인: {ai_cause[:150]}")
            if ai_solution:
                summary_lines.append(f"   💡 해결책: {ai_solution[:150]}")

            # 스택 트레이스 여부
            if has_stack:
                summary_lines.append(f"   📚 (스택 트레이스 있음)")

            # log_id
            if log_id:
                summary_lines.append(f"   (log_id: {log_id})")

            summary_lines.append("")

        return "\n".join(summary_lines)

    except Exception as e:
        return f"에러 로그 조회 중 오류 발생: {str(e)}"
