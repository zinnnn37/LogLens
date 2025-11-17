"""
사용자 추적 도구 (User Tracking Tools)
- IP 기반 사용자 세션 추적, 파라미터 분포, 에러 전파 경로
"""

from typing import Optional
from datetime import datetime, timedelta
from langchain_core.tools import tool

from app.core.opensearch import opensearch_client
from app.tools.common_fields import BASE_FIELDS, LOG_DETAILS_FIELDS


@tool
async def trace_user_session(
    requester_ip: str,
    project_uuid: str,
    time_hours: int = 24,
    include_info: bool = False
) -> str:
    """
    특정 사용자의 활동 로그를 IP 기반으로 추적합니다.

    이 도구는 다음을 수행합니다:
    - ✅ requester_ip 필드로 사용자 식별 및 활동 추적
    - ✅ 시간순 정렬로 사용자 행동 순서 파악
    - ✅ trace_id로 요청 그룹핑
    - ✅ 호출한 API 목록 및 빈도 집계
    - ✅ 에러 발생 패턴 분석
    - ❌ 크로스 서비스 추적은 제한적 (trace_id 범위 내)

    사용 시나리오:
    1. "IP 203.0.113.45 사용자의 활동 내역"
    2. "이 사용자가 어떤 API를 호출했어?"
    3. "특정 IP의 에러 히스토리"
    4. "사용자별 에러 발생 패턴"
    5. "악성 사용자 탐지" (비정상 요청 패턴)

    ⚠️ 중요한 제약사항:
    - 1회 호출로 충분합니다
    - requester_ip가 null인 로그는 제외됩니다
    - 개인정보 보호를 위해 IP만 사용 (사용자 ID 미포함)

    입력 파라미터 (JSON 형식):
        requester_ip: 추적할 IP 주소 (필수, 예: "203.0.113.45")
        time_hours: 추적 기간 (기본 24시간)
        include_info: INFO 로그 포함 여부 (기본 False, ERROR/WARN만)

    관련 도구:
    - get_logs_by_trace_id: 특정 요청의 전체 로그 추적
    - get_recent_errors: 최근 에러 목록
    - analyze_error_by_layer: 레이어별 에러 분석

    Returns:
        사용자 활동 내역 (시간순, API 통계, 에러 패턴)
    """
    # 인덱스 패턴
    index_pattern = f"{project_uuid.replace('-', '_')}_*"

    # 시간 범위 계산
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)

    # Query 구성
    must_clauses = [
        {"exists": {"field": "requester_ip"}},
        {"match_phrase": {"requester_ip": requester_ip}},  # IP 타입 필드 지원을 위해 match_phrase 사용
        {"range": {
            "timestamp": {
                "gte": start_time.isoformat() + "Z",
                "lte": end_time.isoformat() + "Z"
            }
        }}
    ]

    # INFO 로그 제외 옵션
    if not include_info:
        must_clauses.append({
            "bool": {
                "should": [
                    {"term": {"level": "ERROR"}},
                    {"term": {"level": "WARN"}}
                ],
                "minimum_should_match": 1
            }
        })

    try:
        # OpenSearch 검색 + 집계
        results = opensearch_client.search(
            index=index_pattern,
            body={
                "size": 50,  # 최근 50개 활동
                "query": {"bool": {"must": must_clauses}},
                "sort": [{"timestamp": "asc"}],  # 시간순 정렬
                "_source": BASE_FIELDS + LOG_DETAILS_FIELDS + ["ai_analysis.summary"],
                "aggs": {
                    "api_stats": {
                        "terms": {
                            "field": "log_details.request_uri",
                            "size": 10,
                            "missing": "__no_api__"
                        },
                        "aggs": {
                            "http_methods": {
                                "terms": {"field": "log_details.http_method", "size": 5}
                            }
                        }
                    },
                    "error_stats": {
                        "filter": {"term": {"level": "ERROR"}},
                        "aggs": {
                            "by_service": {
                                "terms": {"field": "service_name", "size": 5}
                            }
                        }
                    },
                    "total_requests": {
                        "cardinality": {"field": "trace_id"}
                    }
                }
            }
        )

        hits = results.get("hits", {}).get("hits", [])
        total_logs = results.get("hits", {}).get("total", {}).get("value", 0)
        aggs = results.get("aggregations", {})

        if total_logs == 0:
            return (
                f"IP {requester_ip}의 활동 로그가 최근 {time_hours}시간 동안 없습니다.\n\n"
                f"💡 확인 사항:\n"
                f"1. IP 주소가 정확한지 확인하세요\n"
                f"2. 시간 범위를 늘려보세요 (time_hours 증가)\n"
                f"3. include_info=True로 INFO 로그도 포함해보세요"
            )

        # 결과 포맷팅
        lines = [
            f"## 📊 IP {requester_ip} 활동 분석",
            f"**기간:** 최근 {time_hours}시간",
            f"**총 로그:** {total_logs}건",
            f"**고유 요청:** {aggs.get('total_requests', {}).get('value', 0)}건",
            ""
        ]

        # API 호출 통계
        api_buckets = aggs.get("api_stats", {}).get("buckets", [])
        if api_buckets:
            lines.append("### 🌐 호출한 API TOP 10")
            lines.append("")
            lines.append("| API | 호출 수 | 주요 메서드 |")
            lines.append("|-----|---------|-------------|")

            for bucket in api_buckets[:10]:
                api = bucket.get("key", "")
                if api == "__no_api__":
                    api = "(Non-HTTP 로그)"
                count = bucket.get("doc_count", 0)
                methods = bucket.get("http_methods", {}).get("buckets", [])
                method_str = ", ".join([m["key"] for m in methods[:3]])

                lines.append(f"| {api} | **{count}건** | {method_str} |")

            lines.append("")

        # 에러 통계
        error_count = aggs.get("error_stats", {}).get("doc_count", 0)
        if error_count > 0:
            lines.append("### 🔴 에러 발생 현황")
            lines.append(f"**총 에러:** {error_count}건")
            lines.append("")

            error_services = aggs.get("error_stats", {}).get("by_service", {}).get("buckets", [])
            if error_services:
                lines.append("**서비스별 에러:**")
                for svc in error_services:
                    service = svc.get("key", "unknown")
                    svc_count = svc.get("doc_count", 0)
                    lines.append(f"- {service}: {svc_count}건")
                lines.append("")

        # 최근 활동 내역 (시간순)
        lines.append("### 📋 최근 활동 내역 (시간순)")
        lines.append("")

        prev_timestamp = None
        for i, hit in enumerate(hits[:20], 1):  # 최근 20개만
            source = hit["_source"]
            timestamp_str = source.get("timestamp", "")[:19].replace("T", " ")
            level = source.get("level", "INFO")
            service = source.get("service_name", "unknown")
            message = source.get("message", "")[:150]

            log_details = source.get("log_details", {})
            http_method = log_details.get("http_method", "")
            request_uri = log_details.get("request_uri", "")
            response_status = log_details.get("response_status")

            trace_id = source.get("trace_id", "")

            # 시간 간격 계산 (이전 로그와의 차이)
            time_gap = ""
            if prev_timestamp:
                try:
                    prev_dt = datetime.fromisoformat(prev_timestamp.replace("Z", ""))
                    curr_dt = datetime.fromisoformat(source.get("timestamp", "").replace("Z", ""))
                    gap_seconds = (curr_dt - prev_dt).total_seconds()
                    if gap_seconds > 60:
                        time_gap = f" (+{int(gap_seconds/60)}분)"
                    elif gap_seconds > 1:
                        time_gap = f" (+{int(gap_seconds)}초)"
                except:
                    pass

            prev_timestamp = source.get("timestamp", "")

            # 레벨별 이모지
            level_emoji = {"ERROR": "🔴", "WARN": "🟡", "INFO": "🟢"}.get(level, "⚪")

            lines.append(f"{i}. [{level_emoji} {level}] {timestamp_str}{time_gap}")

            # HTTP 요청 정보
            if http_method and request_uri:
                status_str = f" → {response_status}" if response_status else ""
                lines.append(f"   🌐 {http_method} {request_uri}{status_str}")

            # 서비스
            lines.append(f"   📦 {service}")

            # trace_id (연관 요청 추적 가능)
            if trace_id:
                lines.append(f"   🔗 trace_id: {trace_id}")

            # 메시지
            if message:
                lines.append(f"   💬 {message}")

            lines.append("")

        # 분석 요약
        lines.append("### 💡 분석 요약")
        lines.append("")

        # 에러율 계산
        if total_logs > 0:
            error_rate = (error_count / total_logs * 100) if error_count else 0
            lines.append(f"- **에러율:** {error_rate:.1f}% ({error_count}/{total_logs})")

        # 활동 패턴
        if total_logs > 20:
            lines.append(f"- **활동량:** 높음 ({total_logs}건)")
        elif total_logs > 5:
            lines.append(f"- **활동량:** 보통 ({total_logs}건)")
        else:
            lines.append(f"- **활동량:** 낮음 ({total_logs}건)")

        # 권장 사항
        if error_count > 3:
            lines.append("")
            lines.append("### ✅ 권장 조치")
            lines.append(f"1. 에러 {error_count}건 발생 - 상세 분석 권장 (get_recent_errors)")
            if error_services:
                top_error_service = error_services[0].get("key", "")
                lines.append(f"2. {top_error_service} 서비스 집중 점검")

        return "\n".join(lines)

    except Exception as e:
        return f"사용자 세션 추적 중 오류 발생: {str(e)}"


@tool
async def analyze_parameter_distribution(
    project_uuid: str,
    class_name: str,
    method_name: str,
    time_hours: int = 168
) -> str:
    """
    특정 메서드의 parameters 필드 값 분포를 분석합니다.

    이 도구는 다음을 수행합니다:
    - ✅ parameters JSON 필드 파싱 및 분석
    - ✅ 파라미터별 값 범위, null 비율 집계
    - ✅ 에러 발생 파라미터 패턴 식별
    - ✅ 값 분포 통계 (최빈값, 이상치)
    - ❌ 복잡한 nested JSON 구조는 제한적 분석

    사용 시나리오:
    1. "UserService.createUser는 주로 어떤 값으로 호출돼?"
    2. "이 메서드의 파라미터 null 비율"
    3. "에러 발생 시 파라미터 패턴"

    ⚠️ 중요한 제약사항:
    - 1회 호출로 충분합니다
    - parameters 필드가 없는 로그는 제외됩니다
    - 기본 분석 기간은 7일입니다

    입력 파라미터 (JSON 형식):
        class_name: 클래스 이름 (필수, 예: "UserService")
        method_name: 메서드 이름 (필수, 예: "createUser")
        time_hours: 분석 기간 (기본 168시간=7일)

    관련 도구:
    - analyze_request_patterns: HTTP Request Body 패턴 분석
    - analyze_errors_unified: 에러 통합 분석

    Returns:
        파라미터 값 분포, null 비율, 에러 패턴
    """
    # 인덱스 패턴
    index_pattern = f"{project_uuid.replace('-', '_')}_*"

    # 시간 범위 계산
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)

    try:
        # OpenSearch 검색
        results = opensearch_client.search(
            index=index_pattern,
            body={
                "size": 100,  # 파라미터 분석용 샘플
                "query": {
                    "bool": {
                        "must": [
                            {"term": {"class_name": class_name}},
                            {"term": {"method_name": method_name}},
                            {"exists": {"field": "parameters"}},
                            {"range": {
                                "timestamp": {
                                    "gte": start_time.isoformat() + "Z",
                                    "lte": end_time.isoformat() + "Z"
                                }
                            }}
                        ]
                    }
                },
                "sort": [{"timestamp": "desc"}],
                "_source": ["parameters", "level", "timestamp", "log_id"]
            }
        )

        hits = results.get("hits", {}).get("hits", [])
        total_count = results.get("hits", {}).get("total", {}).get("value", 0)

        if total_count == 0:
            return (
                f"{class_name}.{method_name}의 파라미터 데이터가 최근 {time_hours}시간 동안 없습니다.\n\n"
                f"💡 확인 사항:\n"
                f"1. 클래스/메서드 이름이 정확한지 확인하세요\n"
                f"2. 로그에 parameters 필드가 포함되어 있는지 확인하세요\n"
                f"3. 시간 범위를 늘려보세요 (time_hours 증가)"
            )

        # 파라미터 분석
        param_stats = {}  # {param_name: {values: [], null_count: 0, error_count: 0}}
        error_params = []  # 에러 발생 시 파라미터 값들

        for hit in hits:
            source = hit["_source"]
            params = source.get("parameters")
            level = source.get("level", "INFO")

            if not isinstance(params, dict):
                continue

            # 파라미터별 통계 수집
            for key, value in params.items():
                if key not in param_stats:
                    param_stats[key] = {"values": [], "null_count": 0, "error_count": 0}

                if value is None:
                    param_stats[key]["null_count"] += 1
                else:
                    param_stats[key]["values"].append(value)

                if level == "ERROR":
                    param_stats[key]["error_count"] += 1

            # 에러 시 파라미터 저장
            if level == "ERROR":
                error_params.append(params)

        # 결과 포맷팅
        lines = [
            f"## 📊 {class_name}.{method_name} 파라미터 분석",
            f"**기간:** 최근 {time_hours}시간",
            f"**분석 샘플:** {len(hits)}건",
            ""
        ]

        # 파라미터별 통계
        if param_stats:
            lines.append("### 📋 파라미터별 통계")
            lines.append("")
            lines.append("| 파라미터 | 샘플 수 | Null 비율 | 에러 발생 | 주요 값 |")
            lines.append("|----------|---------|-----------|-----------|---------|")

            for param_name, stats in sorted(param_stats.items()):
                sample_count = len(stats["values"]) + stats["null_count"]
                null_ratio = (stats["null_count"] / sample_count * 100) if sample_count > 0 else 0
                error_count = stats["error_count"]

                # 주요 값 (최빈값 3개)
                from collections import Counter
                value_counts = Counter([str(v)[:30] for v in stats["values"]])
                top_values = value_counts.most_common(3)
                top_values_str = ", ".join([f"{v}" for v, c in top_values]) if top_values else "-"

                lines.append(
                    f"| {param_name} | {sample_count} | "
                    f"**{null_ratio:.1f}%** | {error_count}건 | {top_values_str} |"
                )

            lines.append("")

        # 에러 패턴 분석
        if error_params:
            lines.append("### 🔴 에러 발생 파라미터 패턴")
            lines.append(f"**에러 샘플:** {len(error_params)}건")
            lines.append("")

            # 공통 패턴 찾기
            common_issues = []

            for param_name, stats in param_stats.items():
                # Null 에러
                if stats["null_count"] > 0 and stats["error_count"] > 0:
                    null_error_ratio = (stats["error_count"] / stats["null_count"] * 100) if stats["null_count"] > 0 else 0
                    if null_error_ratio > 50:
                        common_issues.append(f"- **{param_name}**: null 값 시 에러 빈번 (null {stats['null_count']}건 중 에러 {stats['error_count']}건)")

            if common_issues:
                lines.append("**공통 문제점:**")
                lines.extend(common_issues)
            else:
                lines.append("특별한 패턴이 발견되지 않았습니다.")

            lines.append("")

        # 권장 조치
        lines.append("### ✅ 권장 조치")
        lines.append("")

        high_null_params = [name for name, stats in param_stats.items()
                           if (stats["null_count"] / (len(stats["values"]) + stats["null_count"]) * 100) > 30]

        if high_null_params:
            lines.append(f"1. **Null 체크 강화:** {', '.join(high_null_params[:3])}")

        if error_params:
            lines.append(f"2. **입력 검증 추가:** 에러 발생 파라미터 패턴 검토")

        lines.append(f"3. **파라미터 타입 확인:** 예상 타입과 실제 값 일치 여부")

        return "\n".join(lines)

    except Exception as e:
        return f"파라미터 분포 분석 중 오류 발생: {str(e)}"


@tool
async def trace_error_propagation(
    project_uuid: str,
    log_id: int,
    time_window: int = 60
) -> str:
    """
    특정 에러가 다른 컴포넌트로 전파되는 경로를 추적합니다.

    이 도구는 다음을 수행합니다:
    - ✅ log_id의 timestamp 기준 ±time_window초 범위 로그 수집
    - ✅ trace_id 또는 requester_ip가 같은 연관 로그 추출
    - ✅ timestamp 순으로 정렬하여 연쇄 반응 파악
    - ✅ 레이어 간 에러 전파 경로 시각화
    - ❌ 비동기 작업 전파는 추적 제한적

    사용 시나리오:
    1. "이 에러가 어떻게 전파됐어?"
    2. "log_id 12345의 연쇄 에러 찾기"
    3. "에러가 다른 서비스로 퍼졌어?"

    ⚠️ 중요한 제약사항:
    - 1회 호출로 충분합니다
    - log_id가 존재하지 않으면 분석 불가
    - trace_id가 없으면 IP 기반 추적 (정확도 낮음)

    입력 파라미터 (JSON 형식):
        log_id: 분석할 로그 ID (필수)
        time_window: 전후 시간 범위 (초 단위, 기본 60초)

    관련 도구:
    - get_logs_by_trace_id: trace_id로 연관 로그 조회
    - correlate_logs: 시간 기반 로그 상관관계
    - detect_cascading_failures: 연쇄 장애 탐지

    Returns:
        에러 전파 경로 (시간순, 레이어별)
    """
    # 인덱스 패턴
    index_pattern = f"{project_uuid.replace('-', '_')}_*"

    try:
        # 1. 원본 로그 조회
        origin_result = opensearch_client.search(
            index=index_pattern,
            body={
                "size": 1,
                "query": {"term": {"log_id": log_id}},
                "_source": BASE_FIELDS + LOG_DETAILS_FIELDS
            }
        )

        origin_hits = origin_result.get("hits", {}).get("hits", [])

        if not origin_hits:
            return (
                f"log_id {log_id}를 찾을 수 없습니다.\n\n"
                f"💡 확인 사항:\n"
                f"1. log_id가 정확한지 확인하세요\n"
                f"2. get_log_detail 도구로 로그 존재 여부 확인"
            )

        origin_source = origin_hits[0]["_source"]
        origin_timestamp = origin_source.get("timestamp", "")
        origin_trace_id = origin_source.get("trace_id")
        origin_ip = origin_source.get("requester_ip")

        # 2. 시간 범위 계산
        try:
            origin_dt = datetime.fromisoformat(origin_timestamp.replace("Z", ""))
            start_time = origin_dt - timedelta(seconds=time_window)
            end_time = origin_dt + timedelta(seconds=time_window)
        except:
            return f"log_id {log_id}의 timestamp 파싱 실패"

        # 3. 연관 로그 검색
        must_clauses = [
            {"range": {
                "timestamp": {
                    "gte": start_time.isoformat() + "Z",
                    "lte": end_time.isoformat() + "Z"
                }
            }}
        ]

        # trace_id 우선, 없으면 IP
        if origin_trace_id:
            must_clauses.append({"term": {"trace_id": origin_trace_id}})
            correlation_type = "trace_id"
        elif origin_ip:
            must_clauses.append({"match_phrase": {"requester_ip": origin_ip}})  # IP 타입 필드 지원
            correlation_type = "requester_ip"
        else:
            return (
                f"log_id {log_id}에 trace_id와 requester_ip가 모두 없어 전파 경로를 추적할 수 없습니다.\n\n"
                f"💡 이 로그는 독립적인 에러일 가능성이 높습니다."
            )

        related_result = opensearch_client.search(
            index=index_pattern,
            body={
                "size": 50,
                "query": {"bool": {"must": must_clauses}},
                "sort": [{"timestamp": "asc"}],
                "_source": BASE_FIELDS + LOG_DETAILS_FIELDS + ["ai_analysis.summary"]
            }
        )

        related_hits = related_result.get("hits", {}).get("hits", [])

        if len(related_hits) <= 1:
            return (
                f"log_id {log_id} 전후 {time_window}초 범위에서 연관 로그가 없습니다.\n\n"
                f"💡 이 에러는 독립적으로 발생했거나, 시간 범위를 늘려야 할 수 있습니다."
            )

        # 결과 포맷팅
        lines = [
            f"## 🔗 에러 전파 경로 분석 (log_id: {log_id})",
            f"**연관 기준:** {correlation_type}",
            f"**시간 범위:** ±{time_window}초",
            f"**발견 로그:** {len(related_hits)}건",
            ""
        ]

        # 전파 경로 시각화
        lines.append("### 📍 시간순 전파 경로")
        lines.append("")

        origin_found = False
        error_chain = []

        for i, hit in enumerate(related_hits, 1):
            source = hit["_source"]
            is_origin = source.get("log_id") == log_id

            timestamp_str = source.get("timestamp", "")[:19].replace("T", " ")
            level = source.get("level", "INFO")
            service = source.get("service_name", "unknown")
            layer = source.get("layer", "")
            class_name = source.get("class_name", "")
            method_name = source.get("method_name", "")
            message = source.get("message", "")[:100]

            log_details = source.get("log_details", {})
            http_method = log_details.get("http_method", "")
            request_uri = log_details.get("request_uri", "")
            response_status = log_details.get("response_status")
            exception_type = log_details.get("exception_type", "")

            # 레벨별 이모지
            level_emoji = {"ERROR": "🔴", "WARN": "🟡", "INFO": "🟢"}.get(level, "⚪")

            # 원본 표시
            origin_marker = " ⬅️ **원본 에러**" if is_origin else ""

            # 레이어 표시
            layer_str = f"[{layer}] " if layer else ""

            lines.append(f"{i}. {level_emoji} **{timestamp_str}**{origin_marker}")
            lines.append(f"   {layer_str}{service}")

            if class_name and method_name:
                lines.append(f"   📦 {class_name}.{method_name}")

            if http_method and request_uri:
                status_str = f" → {response_status}" if response_status else ""
                lines.append(f"   🌐 {http_method} {request_uri}{status_str}")

            if exception_type:
                lines.append(f"   ❌ {exception_type}")

            if message:
                lines.append(f"   💬 {message}")

            # 에러 체인 구성
            if level == "ERROR":
                error_chain.append({
                    "service": service,
                    "layer": layer,
                    "exception": exception_type or "Unknown"
                })

            lines.append("")

        # 에러 체인 요약
        if len(error_chain) > 1:
            lines.append("### 🚨 에러 연쇄 반응")
            lines.append("")
            lines.append("```")
            for j, err in enumerate(error_chain, 1):
                arrow = " ↓" if j < len(error_chain) else ""
                layer_tag = f"[{err['layer']}] " if err['layer'] else ""
                lines.append(f"{j}. {layer_tag}{err['service']} - {err['exception']}{arrow}")
            lines.append("```")
            lines.append("")

        # 분석 요약
        lines.append("### 💡 전파 분석")
        lines.append("")

        if len(error_chain) > 1:
            lines.append(f"- **연쇄 에러:** {len(error_chain)}개 컴포넌트에서 에러 발생")
            first_service = error_chain[0]['service']
            last_service = error_chain[-1]['service']
            if first_service != last_service:
                lines.append(f"- **전파 경로:** {first_service} → {last_service}")
        else:
            lines.append("- **독립 에러:** 연쇄 반응 없음")

        lines.append(f"- **연관 기준:** {correlation_type} 매칭")

        # 권장 조치
        if len(error_chain) > 2:
            lines.append("")
            lines.append("### ✅ 권장 조치")
            lines.append(f"1. 최초 에러 원인 수정: {error_chain[0]['service']} 서비스")
            lines.append(f"2. 에러 전파 차단: 중간 레이어 에러 핸들링 강화")
            lines.append(f"3. 연쇄 장애 모니터링: detect_cascading_failures 도구 활용")

        return "\n".join(lines)

    except Exception as e:
        return f"에러 전파 경로 추적 중 오류 발생: {str(e)}"
