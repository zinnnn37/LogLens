"""
비교 분석 도구 (Comparison Tools) - P1 Priority
- 시간대 비교, 연쇄 장애 감지
"""

from typing import Optional
from datetime import datetime, timedelta
from langchain_core.tools import tool

from app.core.opensearch import opensearch_client


@tool
async def compare_time_periods(
    project_uuid: str,
    current_hours: int = 24,
    comparison_hours: int = 24,
    metric: str = "error_count",  # error_count, error_rate, total_logs
    service_name: Optional[str] = None
) -> str:
    """
    두 시간대를 비교 분석합니다.

    사용 시나리오:
    - "오늘이 어제보다 에러가 많나요?"
    - "이번 주와 지난 주 비교해줘"
    - "지난 1시간이 평소보다 비정상적인가요?"
    - "주말과 평일의 트래픽 차이는?"

    입력 파라미터 (JSON 형식):
        current_hours: 현재 기간 (시간 단위, 기본 24시간 = 오늘)
        comparison_hours: 비교 기간 (시간 단위, 기본 24시간 = 어제)
        metric: 비교 메트릭 (error_count, error_rate, total_logs 중 하나)
        service_name: 서비스 이름 필터 (선택)

    참고: project_uuid는 자동으로 주입되므로 전달하지 마세요.

    Returns:
        두 기간의 통계 비교, 변화율, 증감 방향
    """
    # 인덱스 패턴
    index_pattern = f"{project_uuid.replace('-', '_')}_*"

    # 시간 범위 계산
    end_time = datetime.utcnow()
    current_start = end_time - timedelta(hours=current_hours)
    comparison_start = current_start - timedelta(hours=comparison_hours)
    comparison_end = current_start

    # Query 구성
    must_clauses = []
    if service_name:
        must_clauses.append({"term": {"service_name": service_name}})

    try:
        # OpenSearch Multi-period Aggregation Query
        results = opensearch_client.search(
            index=index_pattern,
            body={
                "size": 0,
                "query": {"bool": {"must": must_clauses}} if must_clauses else {"match_all": {}},
                "aggs": {
                    "current_period": {
                        "filter": {
                            "range": {
                                "timestamp": {
                                    "gte": current_start.isoformat() + "Z",
                                    "lte": end_time.isoformat() + "Z"
                                }
                            }
                        },
                        "aggs": {
                            "total_logs": {
                                "value_count": {"field": "log_id"}
                            },
                            "error_logs": {
                                "filter": {"term": {"level": "ERROR"}}
                            },
                            "warn_logs": {
                                "filter": {"term": {"level": "WARN"}}
                            },
                            "by_service": {
                                "terms": {
                                    "field": "service_name",
                                    "size": 10
                                }
                            }
                        }
                    },
                    "comparison_period": {
                        "filter": {
                            "range": {
                                "timestamp": {
                                    "gte": comparison_start.isoformat() + "Z",
                                    "lt": comparison_end.isoformat() + "Z"
                                }
                            }
                        },
                        "aggs": {
                            "total_logs": {
                                "value_count": {"field": "log_id"}
                            },
                            "error_logs": {
                                "filter": {"term": {"level": "ERROR"}}
                            },
                            "warn_logs": {
                                "filter": {"term": {"level": "WARN"}}
                            },
                            "by_service": {
                                "terms": {
                                    "field": "service_name",
                                    "size": 10
                                }
                            }
                        }
                    }
                }
            }
        )

        # 집계 결과 추출
        aggs = results.get("aggregations", {})
        current = aggs.get("current_period", {})
        comparison = aggs.get("comparison_period", {})

        # 현재 기간 데이터
        current_total = current.get("total_logs", {}).get("value", 0)
        current_errors = current.get("error_logs", {}).get("doc_count", 0)
        current_warns = current.get("warn_logs", {}).get("doc_count", 0)
        current_error_rate = (current_errors / current_total * 100) if current_total > 0 else 0

        # 비교 기간 데이터
        comparison_total = comparison.get("total_logs", {}).get("value", 0)
        comparison_errors = comparison.get("error_logs", {}).get("doc_count", 0)
        comparison_warns = comparison.get("warn_logs", {}).get("doc_count", 0)
        comparison_error_rate = (comparison_errors / comparison_total * 100) if comparison_total > 0 else 0

        if current_total == 0 and comparison_total == 0:
            return f"두 기간 모두 로그 데이터가 없습니다."

        # 변화율 계산
        if metric == "error_count":
            current_value = current_errors
            comparison_value = comparison_errors
            metric_name = "에러 수"
            unit = "건"
        elif metric == "error_rate":
            current_value = current_error_rate
            comparison_value = comparison_error_rate
            metric_name = "에러율"
            unit = "%"
        else:  # total_logs
            current_value = current_total
            comparison_value = comparison_total
            metric_name = "총 로그 수"
            unit = "건"

        if comparison_value > 0:
            change_rate = ((current_value - comparison_value) / comparison_value * 100)
            absolute_change = current_value - comparison_value
        else:
            change_rate = 100 if current_value > 0 else 0
            absolute_change = current_value

        # 추세 판정
        if abs(change_rate) < 5:
            trend = "비슷함"
            trend_emoji = "🟢"
        elif change_rate > 0:
            if change_rate > 50:
                trend = "급증"
                trend_emoji = "🔴"
            elif change_rate > 20:
                trend = "크게 증가"
                trend_emoji = "🟠"
            else:
                trend = "증가"
                trend_emoji = "🟡"
        else:
            if change_rate < -50:
                trend = "급감"
                trend_emoji = "🟢"
            elif change_rate < -20:
                trend = "크게 감소"
                trend_emoji = "🟢"
            else:
                trend = "감소"
                trend_emoji = "🟢"

        # 결과 포맷팅
        summary_lines = [
            f"=== 시간대 비교 분석 ===",
            ""
        ]

        if service_name:
            summary_lines.append(f"🔧 서비스: {service_name}")
            summary_lines.append("")

        # 기간 정보
        current_label = f"최근 {current_hours}시간"
        comparison_label = f"이전 {comparison_hours}시간"

        summary_lines.append(f"📅 비교 기간:")
        summary_lines.append(f"  - {current_label}: {current_start.strftime('%Y-%m-%d %H:%M')} ~ {end_time.strftime('%Y-%m-%d %H:%M')}")
        summary_lines.append(f"  - {comparison_label}: {comparison_start.strftime('%Y-%m-%d %H:%M')} ~ {comparison_end.strftime('%Y-%m-%d %H:%M')}")
        summary_lines.append("")

        # 주요 메트릭 비교
        summary_lines.append(f"## 📊 {metric_name} 비교")
        summary_lines.append("")
        summary_lines.append(f"**{current_label}**: {current_value:.2f}{unit}")
        summary_lines.append(f"**{comparison_label}**: {comparison_value:.2f}{unit}")
        summary_lines.append(f"**변화**: {absolute_change:+.2f}{unit} ({change_rate:+.1f}%)")
        summary_lines.append(f"**추세**: {trend_emoji} {trend}")
        summary_lines.append("")

        # 상세 통계 비교표
        summary_lines.append("## 📈 상세 통계 비교")
        summary_lines.append("")
        summary_lines.append(f"| 메트릭 | {current_label} | {comparison_label} | 변화 |")
        summary_lines.append("|--------|---------|---------|------|")
        summary_lines.append(f"| 총 로그 | {int(current_total):,}건 | {int(comparison_total):,}건 | {int(current_total - comparison_total):+,}건 |")
        summary_lines.append(f"| 에러 | {current_errors}건 | {comparison_errors}건 | {current_errors - comparison_errors:+}건 |")
        summary_lines.append(f"| 경고 | {current_warns}건 | {comparison_warns}건 | {current_warns - comparison_warns:+}건 |")
        summary_lines.append(f"| 에러율 | {current_error_rate:.2f}% | {comparison_error_rate:.2f}% | {current_error_rate - comparison_error_rate:+.2f}%p |")
        summary_lines.append("")

        # 서비스별 비교 (상위 5개)
        current_services = {sb["key"]: sb["doc_count"] for sb in current.get("by_service", {}).get("buckets", [])}
        comparison_services = {sb["key"]: sb["doc_count"] for sb in comparison.get("by_service", {}).get("buckets", [])}
        all_services = set(current_services.keys()) | set(comparison_services.keys())

        if all_services and not service_name:
            summary_lines.append("## 🔧 서비스별 비교 (상위 5개)")
            summary_lines.append("")

            # 서비스별 변화율 계산 후 정렬
            service_changes = []
            for svc in all_services:
                cur_count = current_services.get(svc, 0)
                cmp_count = comparison_services.get(svc, 0)
                change = cur_count - cmp_count
                change_pct = ((cur_count - cmp_count) / cmp_count * 100) if cmp_count > 0 else (100 if cur_count > 0 else 0)
                service_changes.append({
                    "name": svc,
                    "current": cur_count,
                    "comparison": cmp_count,
                    "change": change,
                    "change_pct": change_pct
                })

            # 절대 변화량 순으로 정렬
            service_changes.sort(key=lambda s: abs(s["change"]), reverse=True)

            for sc in service_changes[:5]:
                change_emoji = "🔴" if sc["change"] > 0 else "🟢" if sc["change"] < 0 else "⚪"
                summary_lines.append(f"**{sc['name']}**")
                summary_lines.append(f"  {change_emoji} {int(sc['current']):,}건 → {int(sc['comparison']):,}건 ({sc['change']:+,}건, {sc['change_pct']:+.1f}%)")

            summary_lines.append("")

        # 종합 평가
        summary_lines.append("## 💡 종합 평가")
        summary_lines.append("")

        if trend in ["급증", "크게 증가"] and metric in ["error_count", "error_rate"]:
            summary_lines.append(f"🚨 **경고**: {metric_name}이(가) {trend}했습니다. 즉시 원인 파악이 필요합니다.")
            summary_lines.append(f"   - 변화율: {change_rate:+.1f}%")
            summary_lines.append(f"   - 절대 변화: {absolute_change:+.2f}{unit}")
        elif trend == "증가" and metric in ["error_count", "error_rate"]:
            summary_lines.append(f"⚠️ **주의**: {metric_name}이(가) 증가하고 있습니다. 모니터링을 강화하세요.")
        elif trend in ["급감", "크게 감소", "감소"] and metric in ["error_count", "error_rate"]:
            summary_lines.append(f"✅ **양호**: {metric_name}이(가) {trend}했습니다. 시스템이 개선되고 있습니다.")
        else:
            summary_lines.append(f"ℹ️ **안정**: {metric_name}이(가) 비슷한 수준을 유지하고 있습니다.")

        return "\n".join(summary_lines)

    except Exception as e:
        return f"시간대 비교 분석 중 오류 발생: {str(e)}"


@tool
async def detect_cascading_failures(
    project_uuid: str,
    time_hours: int = 1,  # 기본 1시간 (연쇄 장애는 짧은 시간 내 발생)
    window_minutes: int = 5,  # 동시 발생 판단 윈도우 (5분)
    min_services: int = 2  # 최소 영향 서비스 수
) -> str:
    """
    연쇄 장애(Cascading Failure)를 감지합니다.

    사용 시나리오:
    - "database-service 장애가 다른 서비스에 영향을 주나요?"
    - "서비스 간 의존성 문제가 있나요?"
    - "동시에 여러 서비스에서 발생한 에러는?"
    - "연쇄 장애 패턴이 있나요?"

    입력 파라미터 (JSON 형식):
        time_hours: 분석 시간 범위 (시간 단위, 기본 1시간)
        window_minutes: 동시 발생 판단 시간 윈도우 (분 단위, 기본 5분)
        min_services: 연쇄 장애로 판단할 최소 서비스 수 (기본 2개)

    참고: project_uuid는 자동으로 주입되므로 전달하지 마세요.

    Returns:
        연쇄 장애 패턴, 영향받은 서비스 목록, 발생 시간대
    """
    # 인덱스 패턴
    index_pattern = f"{project_uuid.replace('-', '_')}_*"

    # 시간 범위 계산
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)

    # Query 구성
    must_clauses = [
        {"term": {"level": "ERROR"}},
        {"range": {
            "timestamp": {
                "gte": start_time.isoformat() + "Z",
                "lte": end_time.isoformat() + "Z"
            }
        }}
    ]

    try:
        # OpenSearch Aggregation Query (시간 윈도우별 다중 서비스 에러 감지)
        results = opensearch_client.search(
            index=index_pattern,
            body={
                "size": 0,
                "query": {"bool": {"must": must_clauses}},
                "aggs": {
                    "time_windows": {
                        "date_histogram": {
                            "field": "timestamp",
                            "fixed_interval": f"{window_minutes}m",
                            "time_zone": "Asia/Seoul"
                        },
                        "aggs": {
                            "services_with_errors": {
                                "terms": {
                                    "field": "service_name",
                                    "size": 20
                                },
                                "aggs": {
                                    "error_count": {
                                        "value_count": {"field": "log_id"}
                                    },
                                    "error_types": {
                                        "terms": {
                                            "field": "log_details.exception_type",
                                            "size": 5
                                        }
                                    }
                                }
                            },
                            "service_count": {
                                "cardinality": {"field": "service_name"}
                            }
                        }
                    }
                }
            }
        )

        # 집계 결과 추출
        buckets = results.get("aggregations", {}).get("time_windows", {}).get("buckets", [])
        total_errors = results.get("hits", {}).get("total", {}).get("value", 0)

        if total_errors == 0:
            return f"최근 {time_hours}시간 동안 ERROR 로그가 없습니다."

        # 연쇄 장애 패턴 감지 (min_services개 이상의 서비스에서 동시 에러)
        cascading_patterns = []
        for bucket in buckets:
            time_str = bucket.get("key_as_string", "")
            service_count = bucket.get("service_count", {}).get("value", 0)
            total_errors_in_window = bucket.get("doc_count", 0)

            if service_count >= min_services and total_errors_in_window > 0:
                service_buckets = bucket.get("services_with_errors", {}).get("buckets", [])
                services = []
                for sb in service_buckets:
                    service_name = sb.get("key", "")
                    error_cnt = sb.get("error_count", {}).get("value", 0)
                    error_type_buckets = sb.get("error_types", {}).get("buckets", [])
                    error_types = [etb["key"] for etb in error_type_buckets]

                    services.append({
                        "name": service_name,
                        "error_count": int(error_cnt),
                        "error_types": error_types
                    })

                cascading_patterns.append({
                    "time": time_str,
                    "service_count": service_count,
                    "total_errors": total_errors_in_window,
                    "services": services
                })

        if not cascading_patterns:
            return f"최근 {time_hours}시간 동안 연쇄 장애 패턴이 감지되지 않았습니다. (동시 {min_services}개 이상 서비스 에러 없음)"

        # 결과 포맷팅
        summary_lines = [
            f"=== 연쇄 장애 감지 (최근 {time_hours}시간) ===",
            f"감지 조건: {window_minutes}분 내 {min_services}개 이상 서비스 동시 에러",
            ""
        ]

        summary_lines.append(f"🚨 **{len(cascading_patterns)}개의 연쇄 장애 패턴 감지**")
        summary_lines.append("")

        # 연쇄 장애 패턴별 상세
        for i, pattern in enumerate(cascading_patterns, 1):
            time_display = pattern["time"][:16].replace("T", " ")
            service_count = pattern["service_count"]
            total_errors = pattern["total_errors"]
            services = pattern["services"]

            # 심각도 판정
            if service_count >= 5:
                severity = "🔴 매우 심각"
            elif service_count >= 3:
                severity = "🟠 심각"
            else:
                severity = "🟡 주의"

            summary_lines.append(f"## {i}. {time_display} | {severity}")
            summary_lines.append(f"   영향 서비스: {service_count}개 | 총 에러: {total_errors}건")
            summary_lines.append("")

            # 서비스별 상세
            summary_lines.append("   **영향받은 서비스:**")
            for svc in services:
                error_types_str = ", ".join(svc["error_types"][:2])
                if len(svc["error_types"]) > 2:
                    error_types_str += f" 외 {len(svc['error_types']) - 2}개"

                summary_lines.append(f"   - {svc['name']}: {svc['error_count']}건")
                if error_types_str:
                    summary_lines.append(f"     에러 타입: {error_types_str}")

            summary_lines.append("")

        # 연쇄 장애 분석
        summary_lines.append("## 📊 연쇄 장애 분석")
        summary_lines.append("")

        # 가장 자주 영향받은 서비스 찾기
        service_impact_count = {}
        for pattern in cascading_patterns:
            for svc in pattern["services"]:
                service_impact_count[svc["name"]] = service_impact_count.get(svc["name"], 0) + 1

        if service_impact_count:
            summary_lines.append("가장 자주 영향받은 서비스:")
            sorted_services = sorted(service_impact_count.items(), key=lambda x: x[1], reverse=True)
            for svc_name, count in sorted_services[:5]:
                summary_lines.append(f"  - {svc_name}: {count}회 영향")
            summary_lines.append("")

        # 공통 에러 타입 분석
        all_error_types = {}
        for pattern in cascading_patterns:
            for svc in pattern["services"]:
                for error_type in svc["error_types"]:
                    all_error_types[error_type] = all_error_types.get(error_type, 0) + 1

        if all_error_types:
            summary_lines.append("공통 에러 타입:")
            sorted_errors = sorted(all_error_types.items(), key=lambda x: x[1], reverse=True)
            for error_type, count in sorted_errors[:5]:
                summary_lines.append(f"  - {error_type}: {count}회 발생")
            summary_lines.append("")

        # 권장 조치
        summary_lines.append("## 💡 권장 조치")
        summary_lines.append("")

        if len(cascading_patterns) >= 3:
            summary_lines.append("🚨 **긴급**: 반복적인 연쇄 장애가 감지되었습니다.")
            summary_lines.append("   1. 가장 자주 영향받은 서비스부터 점검")
            summary_lines.append("   2. 서비스 간 의존성 검토")
            summary_lines.append("   3. Circuit Breaker 패턴 적용 검토")
        elif sorted_services[0][1] >= len(cascading_patterns) * 0.8:
            # 특정 서비스가 80% 이상 연쇄 장애에 포함
            root_service = sorted_services[0][0]
            summary_lines.append(f"🔍 **분석**: '{root_service}' 서비스가 대부분의 연쇄 장애에 관여합니다.")
            summary_lines.append(f"   → '{root_service}'가 근본 원인(Root Cause)일 가능성이 높습니다.")
            summary_lines.append(f"   → '{root_service}' 서비스를 우선 점검하세요.")
        else:
            summary_lines.append("⚠️ **주의**: 여러 서비스가 동시에 에러를 발생시키고 있습니다.")
            summary_lines.append("   1. 공통 인프라(DB, Cache 등) 점검")
            summary_lines.append("   2. 네트워크 상태 확인")
            summary_lines.append("   3. 외부 API 장애 여부 확인")

        return "\n".join(summary_lines)

    except Exception as e:
        return f"연쇄 장애 감지 중 오류 발생: {str(e)}"
