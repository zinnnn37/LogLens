"""
분석 도구 (Analysis Tools)
- 로그 통계, 최근 에러 분석
"""

import re
from typing import Optional, Dict, Any
from datetime import datetime, timedelta
from langchain_core.tools import tool

from app.core.opensearch import opensearch_client
from app.tools.common_fields import ALL_FIELDS


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
    최근 에러 로그를 조회하고 심각도 분석을 수행합니다.

    이 도구는 다음을 수행합니다:
    - ✅ 최근 N시간 동안의 ERROR 레벨 로그 조회
    - ✅ 에러 타입 자동 추출 (Exception, Error, Timeout 등)
    - ✅ 심각도 자동 평가 (CRITICAL/HIGH/MEDIUM/LOW/MINIMAL)
    - ✅ 에러 타입별 분포 통계 제공
    - ✅ 심각도순으로 정렬된 에러 목록 반환
    - ✅ 특정 서비스 필터링 지원
    - ❌ AI 기반 근본 원인 분석은 하지 않음 (analyze_single_log 또는 analyze_errors_unified 사용)
    - ❌ 스택 트레이스 심층 분석은 하지 않음 (analyze_single_log 사용)
    - ❌ 해결책 제시는 하지 않음 (analyze_single_log 사용)

    사용 시나리오:
    1. "최근 에러가 뭐야?" - 전체 에러 현황 파악 (service_name 없이 호출)
    2. "user-service 에러 보여줘" - 특정 서비스 에러만 조회
    3. "지난 1시간 에러 확인" - time_hours=1로 설정
    4. "심각한 에러부터 보여줘" - 자동으로 심각도순 정렬됨

    ⚠️ 중요한 제약사항:
    - 이 도구는 **1회 호출로 충분**합니다. 같은 조건으로 반복 호출하지 마세요.
    - "ERROR 레벨 로그가 없습니다" 응답은 **정상 결과**입니다. 다른 도구로 재시도하지 마세요.
    - 심각도 분석이 **이미 포함**되어 있습니다. 별도 분석 도구를 추가로 호출할 필요 없습니다.
    - 전체 현황 파악 시 service_name 필터 없이 먼저 조회하세요.

    입력 파라미터 (JSON 형식):
        limit: 조회할 개수 (기본 10개, 최대 100개)
        service_name: 특정 서비스만 조회 (선택, 예: "user-service")
        time_hours: 검색 시간 범위 (기본 24시간)

    관련 도구:
    - analyze_single_log: 특정 log_id의 AI 기반 심층 분석 (근본 원인, 해결책)
    - analyze_errors_unified: 여러 에러의 통합 분석 (트렌드, 패턴)
    - get_log_detail: 단순 로그 원본 조회 (분석 없음)

    Returns:
        에러 타입별 분포, 심각도순 에러 목록, 각 에러의 상세 정보
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
                "_source": ALL_FIELDS  # 공통 필드 사용 (trace_id, request_id 포함)
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
            f"상위 {len(hits)}건 표시",  # "총 X건" 제거 - LLM 혼란 방지
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
            msg = source.get("message", "")[:200]  # 400 → 200자로 축소 (컨텍스트 부담 감소)
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

            # 추적 정보 (trace_id, request_id)
            trace_id = source.get("trace_id", "")
            request_id = source.get("request_id", "")

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

            # AI 분석 결과 (있는 경우) - 축소하여 컨텍스트 부담 감소
            if ai_summary:
                summary_lines.append(f"   🤖 AI: {ai_summary[:100]}")  # 200 → 100자
            if ai_cause:
                summary_lines.append(f"   📌 원인: {ai_cause[:80]}")  # 150 → 80자
            if ai_solution:
                summary_lines.append(f"   💡 해결: {ai_solution[:80]}")  # 150 → 80자

            # 스택 트레이스 여부
            if has_stack:
                summary_lines.append(f"   📚 (스택 트레이스 있음)")

            # 추적 ID들 (trace_id, request_id, log_id)
            tracking_ids = []
            if log_id:
                tracking_ids.append(f"log_id: {log_id}")
            if trace_id:
                tracking_ids.append(f"trace_id: {trace_id}")
            if request_id:
                tracking_ids.append(f"request_id: {request_id}")

            if tracking_ids:
                summary_lines.append(f"   ({', '.join(tracking_ids)})")

            summary_lines.append("")

        return "\n".join(summary_lines)

    except Exception as e:
        return f"에러 로그 조회 중 오류 발생: {str(e)}"


@tool
async def correlate_logs(
    project_uuid: str,
    log_id: int,
    correlation_type: str = "trace",
    time_window_minutes: int = 5,
    limit: int = 20
) -> str:
    """
    특정 로그와 연관된 다른 로그들을 찾아 상관관계 분석

    Args:
        project_uuid: 프로젝트 UUID
        log_id: 기준 로그 ID
        correlation_type: 상관관계 유형 (trace|error_type|time_window)
            - trace: 같은 trace_id를 가진 로그들 (분산 추적)
            - error_type: 같은 에러 타입 발생 로그들
            - time_window: 시간대 기반 (±N분 이내)
        time_window_minutes: time_window 사용 시 시간 범위 (기본 5분)
        limit: 최대 결과 수

    Returns:
        연관 로그 목록 및 분석

    Examples:
        - "이 NullPointerException의 근본 원인은?"
        - "log_id 12345와 연관된 로그 추적"
    """
    try:
        from app.db.opensearch import get_opensearch_client
        from datetime import datetime, timedelta

        client = get_opensearch_client()
        index_name = f"logs_{project_uuid}"

        # 1. 기준 로그 조회
        try:
            base_log = client.get(index=index_name, id=str(log_id))
            base_source = base_log['_source']
        except Exception:
            return f"로그 ID {log_id}를 찾을 수 없습니다."

        summary_lines = [f"=== 로그 상관관계 분석 (log_id: {log_id}) ===", ""]

        # 기준 로그 정보
        base_timestamp = base_source.get('timestamp')
        base_service = base_source.get('service_name', 'unknown')
        base_level = base_source.get('level', 'INFO')
        base_message = base_source.get('message', '')[:150]

        summary_lines.append("**기준 로그:**")
        summary_lines.append(f"- 시간: {base_timestamp}")
        summary_lines.append(f"- 서비스: {base_service}")
        summary_lines.append(f"- 레벨: {base_level}")
        summary_lines.append(f"- 메시지: {base_message}")
        summary_lines.append("")

        # 2. 상관관계 타입별 검색
        query = None

        if correlation_type == "trace":
            # trace_id 기반 (EC2: top-level field)
            trace_id = base_source.get('trace_id')
            if not trace_id:
                return f"기준 로그에 trace_id가 없습니다. 다른 correlation_type을 사용하세요."

            query = {
                "bool": {
                    "must": [
                        {"term": {"trace_id.keyword": trace_id}}
                    ],
                    "must_not": [
                        {"term": {"_id": str(log_id)}}  # 자기 자신 제외
                    ]
                }
            }
            summary_lines.append(f"**상관관계 유형:** trace_id 추적 ({trace_id})")

        elif correlation_type == "error_type":
            # 같은 에러 타입 (EC2: log_details.exception_type)
            error_type = base_source.get('log_details', {}).get('exception_type') or \
                        base_message.split(':')[0] if ':' in base_message else None

            if not error_type:
                return "기준 로그에서 에러 타입을 추출할 수 없습니다."

            query = {
                "bool": {
                    "should": [
                        {"match": {"message": error_type}},
                        {"term": {"log_details.exception_type": error_type}}
                    ],
                    "minimum_should_match": 1,
                    "must_not": [
                        {"term": {"_id": str(log_id)}}
                    ]
                }
            }
            summary_lines.append(f"**상관관계 유형:** 같은 에러 타입 ({error_type})")

        elif correlation_type == "time_window":
            # 시간대 기반 (±N분)
            if not base_timestamp:
                return "기준 로그에 timestamp가 없습니다."

            base_dt = datetime.fromisoformat(base_timestamp.replace('Z', '+00:00'))
            start_time = base_dt - timedelta(minutes=time_window_minutes)
            end_time = base_dt + timedelta(minutes=time_window_minutes)

            query = {
                "bool": {
                    "must": [
                        {
                            "range": {
                                "timestamp": {
                                    "gte": start_time.isoformat(),
                                    "lte": end_time.isoformat()
                                }
                            }
                        }
                    ],
                    "must_not": [
                        {"term": {"_id": str(log_id)}}
                    ]
                }
            }
            summary_lines.append(f"**상관관계 유형:** 시간 범위 (±{time_window_minutes}분)")

        else:
            return f"지원하지 않는 correlation_type: {correlation_type}"

        # 3. 검색 실행
        response = client.search(
            index=index_name,
            body={
                "query": query,
                "sort": [{"timestamp": "asc"}],
                "size": limit
            }
        )

        hits = response['hits']['hits']
        total = response['hits']['total']['value']

        if total == 0:
            summary_lines.append("")
            summary_lines.append("연관된 로그를 찾지 못했습니다.")
            return "\n".join(summary_lines)

        summary_lines.append(f"**발견:** 총 {total}건 중 {len(hits)}건 표시")
        summary_lines.append("")

        # 4. 연관 로그 목록
        summary_lines.append("**연관 로그 목록 (시간순):**")
        summary_lines.append("")

        for i, hit in enumerate(hits, 1):
            source = hit['_source']
            related_id = hit.get('_id')
            timestamp = source.get('timestamp', 'N/A')
            level = source.get('level', 'INFO')
            service = source.get('service_name', 'unknown')
            message = source.get('message', '')[:200]

            summary_lines.append(f"{i}. [{level}] {timestamp}")
            summary_lines.append(f"   🔧 {service}")
            summary_lines.append(f"   💬 {message}")
            summary_lines.append(f"   (log_id: {related_id})")
            summary_lines.append("")

        # 5. 인사이트
        summary_lines.append("**💡 분석 인사이트:**")
        if correlation_type == "trace":
            summary_lines.append(f"- 이 요청은 {len(hits)+1}개 서비스를 거쳐 처리되었습니다")
            summary_lines.append("- trace_id로 전체 호출 흐름을 추적할 수 있습니다")
        elif correlation_type == "error_type":
            summary_lines.append(f"- 같은 에러가 총 {total+1}건 발생했습니다")
            summary_lines.append("- 재현 가능한 버그일 가능성이 높습니다")
        elif correlation_type == "time_window":
            summary_lines.append(f"- {time_window_minutes}분 이내 {total}건의 로그가 발생했습니다")
            summary_lines.append("- 동시다발적 문제 또는 연쇄 장애 가능성이 있습니다")

        return "\n".join(summary_lines)

    except Exception as e:
        return f"상관관계 분석 중 오류 발생: {str(e)}"


@tool
async def analyze_errors_unified(
    project_uuid: str,
    group_by: str = "severity",
    sort_by: str = "severity",
    time_hours: int = 24,
    limit: int = 10,
    service_name: Optional[str] = None
) -> str:
    """
    통합 에러 분석 도구 (기존 3개 도구 통합)

    하나의 도구로 다양한 에러 분석 수행 (심각도별/빈도별/API별)

    Args:
        project_uuid: 프로젝트 UUID
        group_by: 그룹핑 기준
            - severity: 심각도별 그룹핑 (CRITICAL > HIGH > MEDIUM)
            - error_type: 에러 타입별 (NullPointerException 등)
            - service: 서비스별
            - api: API 엔드포인트별
        sort_by: 정렬 기준
            - severity: 심각도순 (CRITICAL 우선)
            - frequency: 발생 빈도순
            - time: 최근 발생 시간순
        time_hours: 분석 기간 (기본 24시간)
        limit: 최대 결과 수
        service_name: 특정 서비스만 분석 (선택)

    Returns:
        그룹핑/정렬된 에러 분석 결과

    Examples:
        - "가장 심각한 에러" → group_by=severity, sort_by=severity
        - "가장 자주 발생하는 에러" → group_by=error_type, sort_by=frequency
        - "서비스별 에러 현황" → group_by=service
        - "API별 에러율" → group_by=api
    """
    # 기존 도구 재사용
    if group_by == "severity" and sort_by == "severity":
        # get_recent_errors 로직
        return await get_recent_errors.ainvoke({
            "project_uuid": project_uuid,
            "limit": limit,
            "service_name": service_name,
            "time_hours": time_hours
        })
    elif group_by == "error_type":
        # get_error_frequency_ranking 로직
        from app.tools.monitoring_tools import get_error_frequency_ranking
        return await get_error_frequency_ranking.ainvoke({
            "project_uuid": project_uuid,
            "time_hours": time_hours,
            "limit": limit,
            "service_name": service_name
        })
    elif group_by == "api":
        # get_api_error_rates 로직
        from app.tools.monitoring_tools import get_api_error_rates
        return await get_api_error_rates.ainvoke({
            "project_uuid": project_uuid,
            "time_hours": time_hours,
            "limit": limit
        })
    elif group_by == "service":
        # get_service_health_status 로직
        from app.tools.monitoring_tools import get_service_health_status
        return await get_service_health_status.ainvoke({
            "project_uuid": project_uuid,
            "time_hours": time_hours,
            "service_name": service_name
        })
    else:
        return f"⚠️ 지원하지 않는 조합: group_by={group_by}, sort_by={sort_by}\n\n지원되는 옵션:\n- group_by: severity|error_type|service|api\n- sort_by: severity|frequency|time"


@tool
async def analyze_single_log(
    project_uuid: str,
    log_id: int
) -> str:
    """
    단일 로그를 AI가 심층 분석합니다 (v2-langgraph API 사용).

    이 도구는 다음을 수행합니다:
    - ✅ 특정 log_id의 AI 분석 수행 (근본 원인, 해결책, 심각도)
    - ✅ 스택 트레이스 분석 (있는 경우)
    - ✅ 연관 로그 추천
    - ✅ 3-tier 캐싱 활용 (빠른 응답 + 비용 절감)
    - ❌ 여러 로그를 동시 분석하지 않음 (analyze_errors_unified 사용)
    - ❌ 로그 원본만 조회하지 않음 (get_log_detail 사용)

    사용 시나리오:
    - "log_id 12345를 분석해줘" ✅
    - "이 에러의 근본 원인은?" ✅ (log_id 필요)
    - "NullPointerException을 해결하는 방법은?" ✅ (log_id 필요)
    - "최근 에러들을 분석해줘" ❌ (get_recent_errors 또는 analyze_errors_unified 사용)
    - "log_id 12345의 상세 정보만 보여줘" ❌ (get_log_detail 사용)

    입력 파라미터 (JSON):
        log_id: 분석할 로그 ID (int, 필수)

    출력 형식:
        ## 🤖 AI 로그 분석 (log_id: {log_id})

        ### 📋 요약
        [AI가 생성한 요약]

        ### 🔍 근본 원인
        [Root Cause Analysis]

        ### 💡 해결 방법
        [Solutions]

        ### 🏷️ 태그
        [분류 태그]

    주의사항:
    - ⚠️ 이 도구는 **단일 로그만** 분석. 여러 로그는 analyze_errors_unified 사용
    - ⚠️ AI 분석은 5-10초 소요될 수 있음 (캐시 사용 시 1초 이내)
    - ⚠️ log_id가 존재하지 않으면 에러 반환
    - ⚠️ 이 도구를 호출했다면 추가 분석 도구를 호출할 필요 없음 (이미 완전한 분석 포함)

    관련 도구:
    - get_log_detail: 로그 원본만 조회 (AI 분석 없음, 단순 조회용)
    - analyze_errors_unified: 여러 로그를 한 번에 분석 (이 도구는 단일만)
    - get_recent_errors: 최근 에러 목록 조회 (이 도구는 특정 log_id 분석)

    Returns:
        AI 분석 결과 (summary, error_cause, solution, tags 포함)
    """
    try:
        # v2-langgraph 서비스 임포트
        from app.services.log_analysis_service_v2 import log_analysis_service_v2

        # AI 분석 실행
        result = await log_analysis_service_v2.analyze_log(
            log_id=log_id,
            project_uuid=project_uuid
        )

        # 결과 포맷팅
        analysis = result.analysis
        lines = [
            f"## 🤖 AI 로그 분석 (log_id: {log_id})",
            ""
        ]

        # 캐시 정보
        if result.from_cache:
            cache_info = "✅ 캐시 사용"
            if result.similar_log_id:
                cache_info += f" (유사 로그: {result.similar_log_id}, 유사도: {result.similarity_score:.2f})"
            lines.append(f"**{cache_info}**")
            lines.append("")

        # 요약
        if analysis.summary:
            lines.append("### 📋 요약")
            lines.append(analysis.summary)
            lines.append("")

        # 근본 원인
        if analysis.error_cause:
            lines.append("### 🔍 근본 원인 (Root Cause)")
            lines.append(analysis.error_cause)
            lines.append("")

        # 해결 방법
        if analysis.solution:
            lines.append("### 💡 해결 방법 (Solutions)")
            lines.append(analysis.solution)
            lines.append("")

        # 태그
        if analysis.tags:
            lines.append("### 🏷️ 태그")
            lines.append(", ".join(analysis.tags))
            lines.append("")

        # 분석 타입
        if hasattr(analysis, 'analysis_type') and analysis.analysis_type:
            lines.append(f"**분석 타입:** {analysis.analysis_type.value}")
        if hasattr(analysis, 'target_type') and analysis.target_type:
            lines.append(f"**대상 타입:** {analysis.target_type.value}")
        if hasattr(analysis, 'analyzed_at') and analysis.analyzed_at:
            lines.append(f"**분석 완료:** {analysis.analyzed_at}")

        return "\n".join(lines)

    except ValueError as e:
        # 로그를 찾을 수 없거나 분석 실패
        error_msg = str(e)
        if "찾을 수 없" in error_msg or "not found" in error_msg.lower():
            return f"❌ log_id '{log_id}'를 찾을 수 없습니다.\n\n다음을 확인해주세요:\n1. log_id가 올바른지 확인\n2. 해당 로그가 프로젝트에 속해 있는지 확인\n\n💡 로그를 먼저 검색하려면 search_logs_by_keyword 또는 get_recent_errors를 사용하세요."
        else:
            return f"❌ AI 분석 중 오류 발생: {error_msg}"

    except Exception as e:
        return f"❌ 예상치 못한 오류 발생: {str(e)}\n\n로그 분석 서비스에 일시적인 문제가 발생했을 수 있습니다. 잠시 후 다시 시도해주세요."


@tool
async def analyze_request_patterns(
    project_uuid: str,
    api_path: str,
    time_hours: int = 24,
    only_errors: bool = True
) -> str:
    """
    요청 바디 패턴을 분석하여 공통 에러 원인을 찾습니다.

    사용 시나리오: "이 API는 어떤 요청에서 에러나?", "특정 파라미터 값이 문제인가?", "null 값 때문에 에러나는 필드"
    ⚠️ 1회 호출로 충분합니다. request_body가 없으면 분석 불가합니다.

    Args:
        api_path: API 경로 (필수, 예: "/api/users")
        time_hours: 분석 기간 (기본 24시간)
        only_errors: 에러만 분석 (기본 True)
    """
    index_pattern = f"{project_uuid.replace('-', '_')}_*"
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)

    must_clauses = [
        {"term": {"log_details.request_uri": api_path}},
        {"exists": {"field": "log_details.request_body"}},
        {"range": {"timestamp": {"gte": start_time.isoformat() + "Z", "lte": end_time.isoformat() + "Z"}}}
    ]

    if only_errors:
        must_clauses.append({"term": {"level": "ERROR"}})

    try:
        results = opensearch_client.search(
            index=index_pattern,
            body={"size": 100, "query": {"bool": {"must": must_clauses}}, "sort": [{"timestamp": "desc"}],
                  "_source": ["log_details.request_body", "log_details.http_method", "level", "message"]}
        )

        hits = results.get("hits", {}).get("hits", [])
        if not hits:
            return f"{api_path} API의 요청 데이터가 최근 {time_hours}시간 동안 없습니다.\n\n💡 확인: API 경로 정확성, request_body 로그 포함 여부"

        # 필드별 null/값 분석
        field_stats = {}
        for hit in hits:
            req_body = hit["_source"].get("log_details", {}).get("request_body")
            if isinstance(req_body, dict):
                for key, value in req_body.items():
                    if key not in field_stats:
                        field_stats[key] = {"total": 0, "null": 0, "values": []}
                    field_stats[key]["total"] += 1
                    if value is None or value == "":
                        field_stats[key]["null"] += 1
                    else:
                        field_stats[key]["values"].append(str(value)[:50])

        lines = [f"## 📋 {api_path} 요청 패턴 분석", f"**분석 샘플:** {len(hits)}건", ""]
        if field_stats:
            lines.extend(["### 필드별 통계", "", "| 필드 | 샘플 수 | Null 비율 |", "|------|---------|-----------|"])
            for field, stats in sorted(field_stats.items()):
                null_pct = (stats["null"] / stats["total"] * 100) if stats["total"] > 0 else 0
                lines.append(f"| {field} | {stats['total']} | **{null_pct:.1f}%** |")
            lines.append("")

        lines.append("### ✅ 권장 조치\n")
        high_null = [f for f, s in field_stats.items() if (s["null"]/s["total"]*100) > 30]
        if high_null:
            lines.append(f"1. **Null 검증 추가:** {', '.join(high_null[:3])}")
        return "\n".join(lines)

    except Exception as e:
        return f"요청 패턴 분석 중 오류: {str(e)}"


@tool
async def analyze_response_failures(
    project_uuid: str,
    api_path: str,
    time_hours: int = 24,
    status_code_filter: Optional[int] = None
) -> str:
    """
    응답 바디의 에러 메시지 패턴을 분석합니다.

    사용 시나리오: "이 API는 주로 어떤 에러 메시지를 반환해?", "validation 에러 종류", "가장 많은 실패 원인"
    ⚠️ 1회 호출로 충분합니다. response_body가 없으면 분석 불가합니다.

    Args:
        api_path: API 경로 (필수)
        time_hours: 분석 기간 (기본 24시간)
        status_code_filter: HTTP 상태 코드 필터 (선택)
    """
    index_pattern = f"{project_uuid.replace('-', '_')}_*"
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)

    must_clauses = [
        {"term": {"log_details.request_uri": api_path}},
        {"exists": {"field": "log_details.response_body"}},
        {"range": {"log_details.response_status": {"gte": 400}}},  # 에러 응답만
        {"range": {"timestamp": {"gte": start_time.isoformat() + "Z", "lte": end_time.isoformat() + "Z"}}}
    ]

    if status_code_filter:
        must_clauses.append({"term": {"log_details.response_status": status_code_filter}})

    try:
        results = opensearch_client.search(
            index=index_pattern,
            body={"size": 100, "query": {"bool": {"must": must_clauses}}, "sort": [{"timestamp": "desc"}],
                  "_source": ["log_details.response_body", "log_details.response_status", "timestamp"]}
        )

        hits = results.get("hits", {}).get("hits", [])
        if not hits:
            return f"{api_path} API의 에러 응답 데이터가 최근 {time_hours}시간 동안 없습니다.\n\n💡 확인: 4xx/5xx 에러 발생 여부"

        # 에러 메시지 집계
        error_messages = {}
        for hit in hits:
            res_body = hit["_source"].get("log_details", {}).get("response_body")
            status = hit["_source"].get("log_details", {}).get("response_status", 0)

            error_msg = "Unknown"
            if isinstance(res_body, dict):
                # 다양한 에러 메시지 필드 탐색
                for key in ["error", "message", "error_message", "detail", "errorMessage"]:
                    if key in res_body:
                        error_msg = str(res_body[key])[:200]
                        break

            key = f"{status}: {error_msg}"
            error_messages[key] = error_messages.get(key, 0) + 1

        lines = [f"## 🔴 {api_path} 실패 응답 분석", f"**분석 샘플:** {len(hits)}건", ""]
        if error_messages:
            lines.extend(["### 에러 메시지 빈도", "", "| 에러 메시지 | 빈도 |", "|-------------|------|"])
            for msg, count in sorted(error_messages.items(), key=lambda x: x[1], reverse=True)[:15]:
                lines.append(f"| {msg[:80]} | **{count}건** |")
            lines.append("")

        lines.append("### ✅ 권장 조치\n")
        if error_messages:
            top_error = max(error_messages.items(), key=lambda x: x[1])
            lines.append(f"1. **가장 많은 에러 수정:** {top_error[0][:100]} ({top_error[1]}건)")
        return "\n".join(lines)

    except Exception as e:
        return f"응답 실패 분석 중 오류: {str(e)}"
