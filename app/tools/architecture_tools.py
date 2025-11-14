"""
아키텍처 분석 도구 (Architecture Analysis Tools)
- 레이어별 에러 분석, 컴포넌트 호출 그래프, 핫스팟 메서드
"""

from typing import Optional
from datetime import datetime, timedelta
from langchain_core.tools import tool

from app.core.opensearch import opensearch_client
from app.tools.common_fields import BASE_FIELDS, LOG_DETAILS_FIELDS


@tool
async def analyze_error_by_layer(
    project_uuid: str,
    time_hours: int = 24,
    min_error_count: int = 1
) -> str:
    """
    애플리케이션 레이어별 에러 분포를 분석합니다.

    이 도구는 다음을 수행합니다:
    - ✅ layer 필드로 그룹핑 (Controller/Service/Repository/Filter/Util)
    - ✅ 레이어별 에러 건수, 에러율 계산
    - ✅ class_name과 method_name으로 핫스팟 식별
    - ✅ 레이어 간 에러 비율 비교
    - ❌ 레이어 간 호출 관계는 추적 안 함 (trace_component_calls 사용)

    사용 시나리오:
    1. "Controller/Service/Repository 중 어디서 에러가 많아?"
    2. "레이어별 에러 통계"
    3. "어느 계층이 불안정해?"
    4. "아키텍처 문제 진단"

    ⚠️ 중요한 제약사항:
    - 1회 호출로 충분합니다
    - layer 필드가 없는 로그는 "Other"로 분류됩니다
    - 기본 분석 기간은 24시간입니다

    입력 파라미터 (JSON 형식):
        time_hours: 분석 기간 (기본 24시간)
        min_error_count: 표시할 최소 에러 수 (기본 1건)

    관련 도구:
    - get_service_health_status: 서비스별 헬스 상태
    - trace_component_calls: 컴포넌트 호출 그래프
    - get_error_frequency_ranking: 에러 타입별 빈도

    Returns:
        레이어별 에러 분포, 핫스팟 클래스/메서드
    """
    # 인덱스 패턴
    index_pattern = f"{project_uuid.replace('-', '_')}_*"

    # 시간 범위 계산
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)

    try:
        # OpenSearch 집계 쿼리
        results = opensearch_client.search(
            index=index_pattern,
            body={
                "size": 0,  # 집계만 필요
                "query": {
                    "range": {
                        "timestamp": {
                            "gte": start_time.isoformat() + "Z",
                            "lte": end_time.isoformat() + "Z"
                        }
                    }
                },
                "aggs": {
                    "by_layer": {
                        "terms": {
                            "field": "layer",
                            "size": 10,
                            "missing": "Other"  # layer 없는 로그
                        },
                        "aggs": {
                            "total_logs": {
                                "value_count": {"field": "log_id"}
                            },
                            "error_logs": {
                                "filter": {"term": {"level": "ERROR"}}
                            },
                            "error_rate": {
                                "bucket_script": {
                                    "buckets_path": {
                                        "errors": "error_logs>_count",
                                        "total": "total_logs"
                                    },
                                    "script": "params.total > 0 ? (params.errors / params.total * 100) : 0"
                                }
                            },
                            "top_classes": {
                                "terms": {
                                    "field": "class_name",
                                    "size": 5
                                },
                                "aggs": {
                                    "error_count": {
                                        "filter": {"term": {"level": "ERROR"}}
                                    }
                                }
                            }
                        }
                    },
                    "total_errors": {
                        "filter": {"term": {"level": "ERROR"}}
                    }
                }
            }
        )

        aggs = results.get("aggregations", {})
        layer_buckets = aggs.get("by_layer", {}).get("buckets", [])
        total_errors = aggs.get("total_errors", {}).get("doc_count", 0)

        if total_errors == 0:
            return (
                f"최근 {time_hours}시간 동안 에러 로그가 없습니다.\n\n"
                f"🟢 시스템이 안정적으로 운영되고 있습니다."
            )

        # 결과 포맷팅
        lines = [
            f"## 📊 레이어별 에러 분포 분석",
            f"**기간:** 최근 {time_hours}시간",
            f"**총 에러:** {total_errors}건",
            ""
        ]

        # 레이어별 통계 테이블
        lines.append("### 🔍 레이어별 에러 현황")
        lines.append("")
        lines.append("| 레이어 | 총 로그 | 에러 수 | 에러율 | 주요 클래스 |")
        lines.append("|--------|---------|---------|--------|-------------|")

        layer_stats = []  # 나중에 정렬용

        for bucket in layer_buckets:
            layer_name = bucket.get("key", "Other")
            total_count = bucket.get("total_logs", {}).get("value", 0)
            error_count = bucket.get("error_logs", {}).get("doc_count", 0)
            error_rate = bucket.get("error_rate", {}).get("value", 0)

            # 필터링
            if error_count < min_error_count:
                continue

            # 주요 클래스
            top_classes = bucket.get("top_classes", {}).get("buckets", [])
            class_str = ", ".join([
                f"{cls['key']}({cls.get('error_count', {}).get('doc_count', 0)})"
                for cls in top_classes[:3]
            ])

            # 에러율 등급
            if error_rate > 5:
                rate_badge = f"🔴 **{error_rate:.1f}%**"
            elif error_rate > 2:
                rate_badge = f"🟡 {error_rate:.1f}%"
            else:
                rate_badge = f"🟢 {error_rate:.1f}%"

            lines.append(
                f"| {layer_name} | {total_count} | **{error_count}건** | {rate_badge} | {class_str} |"
            )

            layer_stats.append({
                "layer": layer_name,
                "error_count": error_count,
                "error_rate": error_rate,
                "classes": top_classes
            })

        lines.append("")

        # 레이어별 에러 비율 (파이 차트 대신 텍스트)
        if layer_stats:
            lines.append("### 📈 레이어별 에러 비중")
            lines.append("")

            # 에러 수 기준 정렬
            sorted_layers = sorted(layer_stats, key=lambda x: x["error_count"], reverse=True)

            for i, layer in enumerate(sorted_layers[:5], 1):
                percentage = (layer["error_count"] / total_errors * 100) if total_errors > 0 else 0
                bar = "█" * int(percentage / 5)  # 5%당 █ 1개
                lines.append(f"{i}. **{layer['layer']}**: {layer['error_count']}건 ({percentage:.1f}%) {bar}")

            lines.append("")

        # 핫스팟 클래스/메서드 분석
        lines.append("### 🔥 핫스팟 클래스 (에러 많은 순)")
        lines.append("")

        # 모든 레이어의 클래스를 통합하여 정렬
        all_classes = []
        for layer_stat in layer_stats:
            for cls in layer_stat["classes"]:
                all_classes.append({
                    "layer": layer_stat["layer"],
                    "class_name": cls.get("key", ""),
                    "error_count": cls.get("error_count", {}).get("doc_count", 0)
                })

        # 에러 수 기준 정렬
        sorted_classes = sorted(all_classes, key=lambda x: x["error_count"], reverse=True)

        if sorted_classes:
            for i, cls in enumerate(sorted_classes[:10], 1):
                lines.append(
                    f"{i}. **{cls['class_name']}** ({cls['layer']}) - {cls['error_count']}건"
                )
        else:
            lines.append("클래스 정보가 없습니다.")

        lines.append("")

        # 분석 요약
        lines.append("### 💡 아키텍처 진단")
        lines.append("")

        # 가장 문제 많은 레이어
        if sorted_layers:
            worst_layer = sorted_layers[0]
            worst_layer_percentage = (worst_layer["error_count"] / total_errors * 100)

            if worst_layer_percentage > 60:
                lines.append(f"- 🔴 **{worst_layer['layer']} 레이어에 에러 집중** ({worst_layer_percentage:.1f}%)")
                lines.append(f"  → 이 레이어의 코드 품질 개선이 시급합니다.")
            elif worst_layer_percentage > 40:
                lines.append(f"- 🟡 **{worst_layer['layer']} 레이어 에러 비중 높음** ({worst_layer_percentage:.1f}%)")
            else:
                lines.append(f"- 🟢 **에러가 여러 레이어에 분산**되어 특정 계층의 문제는 아닙니다.")

        # 에러율 평가
        high_error_rate_layers = [l for l in layer_stats if l["error_rate"] > 5]
        if high_error_rate_layers:
            layer_names = ", ".join([l["layer"] for l in high_error_rate_layers])
            lines.append(f"- 🔴 **에러율 높은 레이어**: {layer_names} (5% 초과)")

        # 권장 조치
        lines.append("")
        lines.append("### ✅ 권장 조치")
        lines.append("")

        if sorted_layers:
            top_layer = sorted_layers[0]
            lines.append(f"1. **{top_layer['layer']} 레이어 집중 점검** ({top_layer['error_count']}건 발생)")

        if sorted_classes:
            top_class = sorted_classes[0]
            lines.append(f"2. **{top_class['class_name']} 클래스 우선 수정** ({top_class['error_count']}건)")

        lines.append(f"3. **레이어 간 에러 전파 확인**: trace_component_calls 도구 활용")
        lines.append(f"4. **단위 테스트 강화**: 에러 다발 클래스 집중 테스트")

        return "\n".join(lines)

    except Exception as e:
        return f"레이어별 에러 분석 중 오류 발생: {str(e)}"


@tool
async def trace_component_calls(
    project_uuid: str,
    trace_id: str,
    visualize: bool = True
) -> str:
    """
    trace_id로 컴포넌트 간 호출 순서를 추적합니다.

    이 도구는 다음을 수행합니다:
    - ✅ trace_id로 로그 수집 및 시간순 정렬
    - ✅ layer, class_name, method_name으로 호출 체인 구성
    - ✅ execution_time으로 병목 지점 표시
    - ✅ 에러 발생 지점 강조
    - ❌ 비동기 호출은 순서 보장 안 됨

    사용 시나리오:
    1. "이 요청이 어떤 컴포넌트를 거쳤어?"
    2. "호출 순서 보여줘"
    3. "어디서 시간이 오래 걸렸어?"
    4. "요청 흐름 시각화"

    ⚠️ 중요한 제약사항:
    - 1회 호출로 충분합니다
    - trace_id가 없는 로그는 추적 불가
    - execution_time이 없으면 성능 분석 제한적

    입력 파라미터 (JSON 형식):
        trace_id: 추적할 Trace ID (필수)
        visualize: ASCII 그래프 표시 여부 (기본 True)

    관련 도구:
    - get_logs_by_trace_id: trace_id로 로그 조회 (간단 버전)
    - trace_error_propagation: 에러 전파 경로 추적
    - get_slowest_apis: 느린 API 분석

    Returns:
        컴포넌트 호출 그래프, 성능 분석
    """
    # 인덱스 패턴
    index_pattern = f"{project_uuid.replace('-', '_')}_*"

    try:
        # trace_id로 로그 검색
        results = opensearch_client.search(
            index=index_pattern,
            body={
                "size": 100,
                "query": {"term": {"trace_id": trace_id}},
                "sort": [{"timestamp": "asc"}],
                "_source": BASE_FIELDS + LOG_DETAILS_FIELDS
            }
        )

        hits = results.get("hits", {}).get("hits", [])

        if not hits:
            return (
                f"trace_id '{trace_id}'에 해당하는 로그가 없습니다.\n\n"
                f"💡 확인 사항:\n"
                f"1. trace_id가 정확한지 확인하세요\n"
                f"2. 로그가 아직 수집되지 않았을 수 있습니다"
            )

        # 결과 포맷팅
        lines = [
            f"## 🔗 컴포넌트 호출 그래프 (trace_id: {trace_id})",
            f"**총 로그:** {len(hits)}건",
            ""
        ]

        # 호출 체인 구성
        call_chain = []
        total_time = 0
        error_point = None

        for i, hit in enumerate(hits, 1):
            source = hit["_source"]

            timestamp = source.get("timestamp", "")
            level = source.get("level", "INFO")
            service = source.get("service_name", "unknown")
            layer = source.get("layer", "")
            class_name = source.get("class_name", "")
            method_name = source.get("method_name", "")

            log_details = source.get("log_details", {})
            execution_time = log_details.get("execution_time") or source.get("duration", 0)
            exception_type = log_details.get("exception_type", "")

            # 호출 정보
            call_info = {
                "index": i,
                "timestamp": timestamp,
                "level": level,
                "service": service,
                "layer": layer,
                "class_name": class_name,
                "method_name": method_name,
                "execution_time": execution_time,
                "exception": exception_type
            }

            call_chain.append(call_info)

            if execution_time:
                total_time += execution_time

            if level == "ERROR" and not error_point:
                error_point = i

        # 시각화
        if visualize:
            lines.append("### 📍 호출 흐름 (시간순)")
            lines.append("")
            lines.append("```")

            for call in call_chain:
                # 레이어 태그
                layer_tag = f"[{call['layer']}] " if call['layer'] else ""

                # 컴포넌트 이름
                if call['class_name'] and call['method_name']:
                    component = f"{call['class_name']}.{call['method_name']}"
                elif call['class_name']:
                    component = call['class_name']
                else:
                    component = call['service']

                # 실행 시간
                time_str = f" ({call['execution_time']}ms)" if call['execution_time'] else ""

                # 병목 표시 (100ms 이상)
                bottleneck = " ⚠️ 병목" if call['execution_time'] and call['execution_time'] > 100 else ""

                # 에러 표시
                error_marker = ""
                if call['level'] == "ERROR":
                    error_marker = f" ❌ {call['exception']}" if call['exception'] else " ❌ ERROR"

                lines.append(f"{call['index']}. {layer_tag}{component}{time_str}{bottleneck}{error_marker}")

                # 다음 호출로의 화살표
                if call['index'] < len(call_chain):
                    lines.append("     ↓")

            lines.append("```")
            lines.append("")

        # 성능 분석
        if total_time > 0:
            lines.append("### ⏱️ 성능 분석")
            lines.append("")
            lines.append(f"**전체 소요 시간:** {total_time}ms")
            lines.append("")

            # 가장 느린 호출 찾기
            sorted_calls = sorted(call_chain, key=lambda x: x['execution_time'] or 0, reverse=True)
            slow_calls = [c for c in sorted_calls if c['execution_time'] and c['execution_time'] > 50]

            if slow_calls:
                lines.append("**병목 지점:**")
                for i, call in enumerate(slow_calls[:5], 1):
                    component = f"{call['class_name']}.{call['method_name']}" if call['class_name'] and call['method_name'] else call['service']
                    percentage = (call['execution_time'] / total_time * 100) if total_time > 0 else 0
                    lines.append(f"{i}. {component}: {call['execution_time']}ms ({percentage:.1f}%)")

                lines.append("")

        # 에러 분석
        if error_point:
            lines.append("### 🔴 에러 발생 지점")
            lines.append("")
            error_call = call_chain[error_point - 1]
            lines.append(f"**위치:** {error_call['index']}번째 호출")
            lines.append(f"**레이어:** {error_call['layer']}")
            lines.append(f"**컴포넌트:** {error_call['class_name']}.{error_call['method_name']}" if error_call['class_name'] else error_call['service'])

            if error_call['exception']:
                lines.append(f"**예외:** {error_call['exception']}")

            lines.append("")

        # 요약
        lines.append("### 💡 요약")
        lines.append("")
        lines.append(f"- **총 호출 수:** {len(call_chain)}건")

        if total_time > 0:
            lines.append(f"- **총 실행 시간:** {total_time}ms")

            if total_time > 1000:
                lines.append(f"- **성능 평가:** 🔴 느림 (1초 초과)")
            elif total_time > 500:
                lines.append(f"- **성능 평가:** 🟡 보통 (500ms 초과)")
            else:
                lines.append(f"- **성능 평가:** 🟢 빠름 (500ms 이하)")

        if error_point:
            lines.append(f"- **에러 발생:** {error_point}번째 호출에서 실패")

        # 권장 조치
        if slow_calls or error_point:
            lines.append("")
            lines.append("### ✅ 권장 조치")
            lines.append("")

            if slow_calls:
                slowest = slow_calls[0]
                component = f"{slowest['class_name']}.{slowest['method_name']}" if slowest['class_name'] and slowest['method_name'] else slowest['service']
                lines.append(f"1. **{component} 성능 개선** ({slowest['execution_time']}ms 소요)")

            if error_point:
                lines.append(f"2. **에러 발생 컴포넌트 수정**: {error_call['index']}번째 호출")

        return "\n".join(lines)

    except Exception as e:
        return f"컴포넌트 호출 추적 중 오류 발생: {str(e)}"


@tool
async def get_hottest_methods(
    project_uuid: str,
    time_hours: int = 24,
    limit: int = 20,
    only_errors: bool = False
) -> str:
    """
    가장 많이 호출/실행된 메서드 순위를 보여줍니다.

    이 도구는 다음을 수행합니다:
    - ✅ class_name + method_name 조합으로 호출 빈도 집계
    - ✅ 에러 발생 빈도 함께 표시
    - ✅ 레이어 및 서비스 정보 포함
    - ✅ 실행 시간 평균 계산
    - ❌ 특정 파라미터별 호출은 분석 안 함

    사용 시나리오:
    1. "어떤 메서드가 가장 많이 실행돼?"
    2. "핫스팟 메서드 찾기"
    3. "에러 많이 발생하는 메서드"
    4. "성능 최적화 대상 선정"

    ⚠️ 중요한 제약사항:
    - 1회 호출로 충분합니다
    - method_name이 없는 로그는 제외됩니다
    - 기본 분석 기간은 24시간입니다

    입력 파라미터 (JSON 형식):
        time_hours: 분석 기간 (기본 24시간)
        limit: 조회할 메서드 개수 (기본 20개)
        only_errors: 에러 발생 메서드만 조회 (기본 False)

    관련 도구:
    - analyze_error_by_layer: 레이어별 에러 분포
    - get_slowest_apis: 느린 API 분석
    - get_error_frequency_ranking: 에러 타입별 빈도

    Returns:
        메서드 호출 빈도 TOP-N, 에러 발생 통계
    """
    # 인덱스 패턴
    index_pattern = f"{project_uuid.replace('-', '_')}_*"

    # 시간 범위 계산
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)

    # Query 구성
    must_clauses = [
        {"exists": {"field": "method_name"}},
        {"range": {
            "timestamp": {
                "gte": start_time.isoformat() + "Z",
                "lte": end_time.isoformat() + "Z"
            }
        }}
    ]

    if only_errors:
        must_clauses.append({"term": {"level": "ERROR"}})

    try:
        # OpenSearch 집계 쿼리
        results = opensearch_client.search(
            index=index_pattern,
            body={
                "size": 0,
                "query": {"bool": {"must": must_clauses}},
                "aggs": {
                    "by_method": {
                        "terms": {
                            "script": {
                                "source": "doc['class_name'].value + '.' + doc['method_name'].value",
                                "lang": "painless"
                            },
                            "size": limit
                        },
                        "aggs": {
                            "error_count": {
                                "filter": {"term": {"level": "ERROR"}}
                            },
                            "avg_execution_time": {
                                "avg": {"field": "log_details.execution_time"}
                            },
                            "sample": {
                                "top_hits": {
                                    "size": 1,
                                    "_source": ["service_name", "layer", "class_name", "method_name"]
                                }
                            }
                        }
                    }
                }
            }
        )

        aggs = results.get("aggregations", {})
        method_buckets = aggs.get("by_method", {}).get("buckets", [])

        if not method_buckets:
            filter_msg = "에러 발생 " if only_errors else ""
            return (
                f"최근 {time_hours}시간 동안 {filter_msg}메서드 호출 데이터가 없습니다.\n\n"
                f"💡 확인 사항:\n"
                f"1. 로그에 method_name 필드가 포함되어 있는지 확인하세요\n"
                f"2. 시간 범위를 늘려보세요 (time_hours 증가)\n"
                f"3. only_errors=False로 전체 메서드를 조회해보세요"
            )

        # 결과 포맷팅
        title_suffix = " (에러 발생만)" if only_errors else ""
        lines = [
            f"## 🔥 핫스팟 메서드 TOP {limit}{title_suffix}",
            f"**기간:** 최근 {time_hours}시간",
            f"**메서드 수:** {len(method_buckets)}개",
            ""
        ]

        # 메서드별 통계 테이블
        lines.append("| 순위 | 메서드 | 호출 수 | 에러 | 평균 시간 | 서비스 | 레이어 |")
        lines.append("|------|--------|---------|------|-----------|---------|--------|")

        for i, bucket in enumerate(method_buckets, 1):
            method_full = bucket.get("key", "Unknown")
            call_count = bucket.get("doc_count", 0)
            error_count = bucket.get("error_count", {}).get("doc_count", 0)
            avg_time = bucket.get("avg_execution_time", {}).get("value")

            # 샘플 정보
            sample_hits = bucket.get("sample", {}).get("hits", {}).get("hits", [])
            if sample_hits:
                sample = sample_hits[0]["_source"]
                service = sample.get("service_name", "unknown")
                layer = sample.get("layer", "-")
            else:
                service = "unknown"
                layer = "-"

            # 에러율
            error_rate = (error_count / call_count * 100) if call_count > 0 else 0

            # 평균 시간 포맷팅
            if avg_time:
                time_str = f"{avg_time:.0f}ms"
                if avg_time > 500:
                    time_str = f"🔴 {time_str}"
                elif avg_time > 200:
                    time_str = f"🟡 {time_str}"
            else:
                time_str = "-"

            # 에러 표시
            if error_count > 0:
                error_str = f"🔴 **{error_count}건** ({error_rate:.1f}%)"
            else:
                error_str = "🟢 0"

            lines.append(
                f"| {i} | {method_full} | **{call_count}** | {error_str} | {time_str} | {service} | {layer} |"
            )

        lines.append("")

        # 통계 요약
        total_calls = sum([b.get("doc_count", 0) for b in method_buckets])
        total_errors = sum([b.get("error_count", {}).get("doc_count", 0) for b in method_buckets])

        lines.append("### 📊 통계 요약")
        lines.append("")
        lines.append(f"- **총 호출 수:** {total_calls:,}건 (상위 {len(method_buckets)}개 메서드)")
        lines.append(f"- **총 에러 수:** {total_errors}건")

        if total_calls > 0:
            overall_error_rate = (total_errors / total_calls * 100)
            lines.append(f"- **전체 에러율:** {overall_error_rate:.2f}%")

        # 가장 문제 많은 메서드
        error_sorted = sorted(method_buckets, key=lambda x: x.get("error_count", {}).get("doc_count", 0), reverse=True)
        if error_sorted[0].get("error_count", {}).get("doc_count", 0) > 0:
            lines.append("")
            lines.append("### 🚨 에러 많은 메서드 TOP 5")
            lines.append("")

            for i, bucket in enumerate(error_sorted[:5], 1):
                method = bucket.get("key", "")
                err_count = bucket.get("error_count", {}).get("doc_count", 0)
                if err_count == 0:
                    break
                lines.append(f"{i}. **{method}** - {err_count}건")

        # 권장 조치
        lines.append("")
        lines.append("### ✅ 권장 조치")
        lines.append("")

        # 에러 많은 메서드
        if total_errors > 0:
            worst_method = error_sorted[0]
            worst_error_count = worst_method.get("error_count", {}).get("doc_count", 0)
            if worst_error_count > 5:
                lines.append(f"1. **{worst_method.get('key', '')} 메서드 우선 수정** ({worst_error_count}건 에러)")

        # 호출 빈도 높은 메서드
        hottest = method_buckets[0]
        hottest_count = hottest.get("doc_count", 0)
        if hottest_count > 100:
            lines.append(f"2. **{hottest.get('key', '')} 성능 최적화** (호출 빈도 높음: {hottest_count}건)")

        # 평균 시간 느린 메서드
        time_sorted = sorted(method_buckets, key=lambda x: x.get("avg_execution_time", {}).get("value") or 0, reverse=True)
        if time_sorted[0].get("avg_execution_time", {}).get("value", 0) > 500:
            slowest = time_sorted[0]
            lines.append(f"3. **{slowest.get('key', '')} 응답 시간 개선** (평균 {slowest.get('avg_execution_time', {}).get('value', 0):.0f}ms)")

        return "\n".join(lines)

    except Exception as e:
        return f"핫스팟 메서드 분석 중 오류 발생: {str(e)}"
