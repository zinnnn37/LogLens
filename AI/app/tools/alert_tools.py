"""
알림 도구 (Alert Tools) - P1 Priority
- 알림 조건 평가, 리소스 이슈 감지
"""

from typing import Optional, List
from datetime import datetime, timedelta
from langchain_core.tools import tool

from app.core.opensearch import opensearch_client


@tool
async def evaluate_alert_conditions(
    project_uuid: str,
    time_window_minutes: int = 5,  # 최근 N분
    error_rate_threshold: float = 5.0,  # 에러율 임계치 (%)
    spike_threshold: float = 50.0,  # 급증 판단 임계치 (%)
    service_name: Optional[str] = None
) -> str:
    """
    알림이 필요한 상황인지 평가합니다.

    사용 시나리오:
    - "알림을 보내야 할 심각한 상황이 있나요?"
    - "에러율이 임계치를 넘었나요?"
    - "비정상적인 트래픽 급증이 있나요?"
    - "반복되는 CRITICAL 에러가 있나요?"

    입력 파라미터 (JSON 형식):
        time_window_minutes: 평가 시간 윈도우 (분 단위, 기본 5분)
        error_rate_threshold: 에러율 임계치 (%, 기본 5.0%)
        spike_threshold: 급증 판단 임계치 (%, 기본 50%)
        service_name: 서비스 이름 필터 (선택)

    참고: project_uuid는 자동으로 주입되므로 전달하지 마세요.

    Returns:
        알림 필요 여부, 위반한 조건, 심각도
    """
    # 인덱스 패턴
    index_pattern = f"{project_uuid.replace('-', '_')}_*"

    # 시간 범위 계산
    end_time = datetime.utcnow()
    recent_start = end_time - timedelta(minutes=time_window_minutes)
    baseline_start = end_time - timedelta(minutes=time_window_minutes * 3)  # 비교 기준 (3배 기간)
    baseline_end = recent_start

    # Query 구성
    must_clauses = []
    if service_name:
        must_clauses.append({"term": {"service_name": service_name}})

    try:
        # OpenSearch Aggregation Query (다중 평가)
        results = opensearch_client.search(
            index=index_pattern,
            body={
                "size": 0,
                "query": {"bool": {"must": must_clauses}} if must_clauses else {"match_all": {}},
                "aggs": {
                    # 최근 윈도우
                    "recent_window": {
                        "filter": {
                            "range": {
                                "timestamp": {
                                    "gte": recent_start.isoformat() + "Z",
                                    "lte": end_time.isoformat() + "Z"
                                }
                            }
                        },
                        "aggs": {
                            "total_logs": {"value_count": {"field": "log_id"}},
                            "error_logs": {"filter": {"term": {"level": "ERROR"}}},
                            "critical_errors": {
                                "filter": {
                                    "bool": {
                                        "must": [
                                            {"term": {"level": "ERROR"}},
                                            {
                                                "bool": {
                                                    "should": [
                                                        {"match": {"log_details.exception_type": "Database"}},
                                                        {"match": {"log_details.exception_type": "OutOfMemory"}},
                                                        {"match": {"log_details.exception_type": "Connection"}},
                                                        {"match": {"message": "OutOfMemoryError"}},
                                                        {"match": {"message": "StackOverflowError"}},
                                                        {"range": {"log_details.response_status": {"gte": 500, "lt": 600}}}
                                                    ]
                                                }
                                            }
                                        ]
                                    }
                                }
                            },
                            "p95_latency": {
                                "percentiles": {
                                    "field": "log_details.execution_time",
                                    "percents": [95]
                                }
                            }
                        }
                    },
                    # 기준 윈도우 (비교용)
                    "baseline_window": {
                        "filter": {
                            "range": {
                                "timestamp": {
                                    "gte": baseline_start.isoformat() + "Z",
                                    "lt": baseline_end.isoformat() + "Z"
                                }
                            }
                        },
                        "aggs": {
                            "total_logs": {"value_count": {"field": "log_id"}},
                            "error_logs": {"filter": {"term": {"level": "ERROR"}}}
                        }
                    }
                }
            }
        )

        # 집계 결과 추출
        aggs = results.get("aggregations", {})
        recent = aggs.get("recent_window", {})
        baseline = aggs.get("baseline_window", {})

        # 최근 윈도우 데이터
        recent_total = recent.get("total_logs", {}).get("value", 0)
        recent_errors = recent.get("error_logs", {}).get("doc_count", 0)
        recent_critical = recent.get("critical_errors", {}).get("doc_count", 0)
        recent_error_rate = (recent_errors / recent_total * 100) if recent_total > 0 else 0
        recent_p95 = recent.get("p95_latency", {}).get("values", {}).get("95.0")

        # 기준 윈도우 데이터
        baseline_total = baseline.get("total_logs", {}).get("value", 0)
        baseline_errors = baseline.get("error_logs", {}).get("doc_count", 0)
        baseline_error_rate = (baseline_errors / baseline_total * 100) if baseline_total > 0 else 0

        # 트래픽 변화율
        if baseline_total > 0:
            traffic_change = ((recent_total - baseline_total) / baseline_total * 100)
        else:
            traffic_change = 100 if recent_total > 0 else 0

        # 알림 조건 평가
        alerts = []
        severity_scores = []

        # 조건 1: 에러율 임계치 초과
        if recent_error_rate > error_rate_threshold:
            severity = "🔴 CRITICAL" if recent_error_rate > error_rate_threshold * 2 else "🟠 HIGH"
            alerts.append({
                "condition": "에러율 임계치 초과",
                "severity": severity,
                "current": f"{recent_error_rate:.2f}%",
                "threshold": f"{error_rate_threshold}%",
                "message": f"에러율이 {recent_error_rate:.2f}%로 임계치 {error_rate_threshold}%를 초과했습니다."
            })
            severity_scores.append(3 if "CRITICAL" in severity else 2)

        # 조건 2: CRITICAL 에러 발생
        if recent_critical > 0:
            severity = "🔴 CRITICAL" if recent_critical >= 3 else "🟠 HIGH"
            alerts.append({
                "condition": "CRITICAL 에러 발생",
                "severity": severity,
                "current": f"{recent_critical}건",
                "threshold": "0건",
                "message": f"Database/OutOfMemory/5xx 등 심각한 에러가 {recent_critical}건 발생했습니다."
            })
            severity_scores.append(3 if recent_critical >= 3 else 2)

        # 조건 3: 트래픽 급증/급감
        if abs(traffic_change) > spike_threshold:
            if traffic_change > 0:
                severity = "🟡 MEDIUM"
                direction = "급증"
                severity_scores.append(1)
            else:
                severity = "🟡 MEDIUM"
                direction = "급감"
                severity_scores.append(1)

            alerts.append({
                "condition": f"트래픽 {direction}",
                "severity": severity,
                "current": f"{traffic_change:+.1f}%",
                "threshold": f"±{spike_threshold}%",
                "message": f"트래픽이 기준 대비 {traffic_change:+.1f}% {direction}했습니다."
            })

        # 조건 4: P95 레이턴시 이상 (5초 이상)
        if recent_p95 and recent_p95 > 5000:
            severity = "🟡 MEDIUM" if recent_p95 < 10000 else "🟠 HIGH"
            alerts.append({
                "condition": "응답 시간 지연",
                "severity": severity,
                "current": f"{recent_p95:.0f}ms",
                "threshold": "5000ms",
                "message": f"P95 응답 시간이 {recent_p95:.0f}ms로 매우 느립니다."
            })
            severity_scores.append(2 if recent_p95 >= 10000 else 1)

        # 조건 5: 에러율 급증 (이전 대비)
        if baseline_error_rate > 0:
            error_rate_change = ((recent_error_rate - baseline_error_rate) / baseline_error_rate * 100)
            if error_rate_change > spike_threshold:
                severity = "🟠 HIGH"
                alerts.append({
                    "condition": "에러율 급증",
                    "severity": severity,
                    "current": f"{error_rate_change:+.1f}%",
                    "threshold": f"+{spike_threshold}%",
                    "message": f"에러율이 이전 대비 {error_rate_change:+.1f}% 급증했습니다."
                })
                severity_scores.append(2)

        # 결과 포맷팅
        summary_lines = [
            f"=== 알림 조건 평가 (최근 {time_window_minutes}분) ===",
            ""
        ]

        if service_name:
            summary_lines.append(f"🔧 서비스: {service_name}")
            summary_lines.append("")

        # 종합 판정
        if not alerts:
            summary_lines.append("✅ **정상**: 모든 조건이 정상 범위 내에 있습니다.")
            summary_lines.append("")
            summary_lines.append("📊 현재 상태:")
            summary_lines.append(f"  - 에러율: {recent_error_rate:.2f}% (임계치: {error_rate_threshold}%)")
            summary_lines.append(f"  - 총 로그: {int(recent_total):,}건")
            summary_lines.append(f"  - 에러: {recent_errors}건")
            if recent_p95:
                summary_lines.append(f"  - P95 응답시간: {recent_p95:.0f}ms")
            summary_lines.append("")
            summary_lines.append("🔔 **알림 불필요**: 시스템이 정상 작동 중입니다.")

        else:
            # 최고 심각도 계산
            max_severity = max(severity_scores) if severity_scores else 0
            if max_severity >= 3:
                overall = "🔴 CRITICAL - 즉시 조치 필요"
            elif max_severity >= 2:
                overall = "🟠 HIGH - 긴급 확인 필요"
            else:
                overall = "🟡 MEDIUM - 모니터링 강화"

            summary_lines.append(f"🚨 **알림 필요**: {len(alerts)}개 조건 위반")
            summary_lines.append(f"**종합 심각도**: {overall}")
            summary_lines.append("")

            # 위반 조건 상세
            summary_lines.append("## 🔔 위반된 조건:")
            summary_lines.append("")
            for i, alert in enumerate(alerts, 1):
                summary_lines.append(f"**{i}. {alert['condition']}** | {alert['severity']}")
                summary_lines.append(f"   현재값: {alert['current']} (임계치: {alert['threshold']})")
                summary_lines.append(f"   {alert['message']}")
                summary_lines.append("")

            # 현재 상태
            summary_lines.append("## 📊 현재 상태:")
            summary_lines.append("")
            summary_lines.append(f"| 메트릭 | 현재 ({time_window_minutes}분) | 기준 ({time_window_minutes * 2}분 전) |")
            summary_lines.append("|--------|---------|----------|")
            summary_lines.append(f"| 총 로그 | {int(recent_total):,}건 | {int(baseline_total):,}건 |")
            summary_lines.append(f"| 에러 | {recent_errors}건 | {baseline_errors}건 |")
            summary_lines.append(f"| 에러율 | {recent_error_rate:.2f}% | {baseline_error_rate:.2f}% |")
            summary_lines.append(f"| CRITICAL 에러 | {recent_critical}건 | - |")
            if recent_p95:
                summary_lines.append(f"| P95 응답시간 | {recent_p95:.0f}ms | - |")
            summary_lines.append("")

            # 권장 조치
            summary_lines.append("## 💡 권장 조치:")
            summary_lines.append("")
            if max_severity >= 3:
                summary_lines.append("1. **즉시** 온콜 엔지니어에게 알림")
                summary_lines.append("2. 최근 에러 로그 확인 (get_recent_errors)")
                summary_lines.append("3. 영향받은 사용자 수 확인 (get_affected_users_count)")
                summary_lines.append("4. 서비스 헬스 상태 점검 (get_service_health_status)")
            elif max_severity >= 2:
                summary_lines.append("1. 에러 원인 파악 시작")
                summary_lines.append("2. 에러 빈도 분석 (get_error_frequency_ranking)")
                summary_lines.append("3. 연쇄 장애 가능성 점검 (detect_cascading_failures)")
            else:
                summary_lines.append("1. 모니터링 강화")
                summary_lines.append("2. 추세 관찰 (get_error_rate_trend)")
                summary_lines.append("3. 필요시 용량 점검")

        return "\n".join(summary_lines)

    except Exception as e:
        return f"알림 조건 평가 중 오류 발생: {str(e)}"


@tool
async def detect_resource_issues(
    project_uuid: str,
    time_hours: int = 24,
    service_name: Optional[str] = None
) -> str:
    """
    리소스 관련 이슈를 감지합니다.

    사용 시나리오:
    - "메모리 부족 에러가 있나요?"
    - "데이터베이스 연결 풀 고갈 패턴이 있나요?"
    - "타임아웃 에러가 증가하고 있나요?"
    - "리소스 부족 증상을 보이는 서비스는?"

    입력 파라미터 (JSON 형식):
        time_hours: 분석 시간 범위 (시간 단위, 기본 24시간)
        service_name: 서비스 이름 필터 (선택)

    참고: project_uuid는 자동으로 주입되므로 전달하지 마세요.

    Returns:
        리소스 이슈 타입, 발생 빈도, 영향받은 서비스
    """
    # 인덱스 패턴
    index_pattern = f"{project_uuid.replace('-', '_')}_*"

    # 시간 범위 계산
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)

    # 리소스 이슈 키워드 패턴
    resource_patterns = {
        "memory": {
            "keywords": ["OutOfMemory", "OutOfMemoryError", "memory exhausted", "heap space"],
            "severity": "🔴 CRITICAL",
            "category": "메모리 부족"
        },
        "database": {
            "keywords": ["connection pool", "pool exhausted", "too many connections", "database timeout", "connection refused"],
            "severity": "🔴 CRITICAL",
            "category": "DB 연결 문제"
        },
        "timeout": {
            "keywords": ["timeout", "timed out", "TimeoutException", "execution timeout"],
            "severity": "🟠 HIGH",
            "category": "타임아웃"
        },
        "thread": {
            "keywords": ["thread pool", "ThreadPoolExecutor", "RejectedExecutionException", "too many threads"],
            "severity": "🟠 HIGH",
            "category": "스레드 풀 문제"
        },
        "disk": {
            "keywords": ["disk full", "no space left", "DiskFullException", "storage exhausted"],
            "severity": "🔴 CRITICAL",
            "category": "디스크 공간 부족"
        },
        "cpu": {
            "keywords": ["StackOverflowError", "CPU", "high CPU usage"],
            "severity": "🟡 MEDIUM",
            "category": "CPU 과부하"
        }
    }

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

    if service_name:
        must_clauses.append({"term": {"service_name": service_name}})

    try:
        # 각 리소스 이슈 타입별 검색
        resource_issues = []

        for issue_type, config in resource_patterns.items():
            # 키워드 검색
            should_clauses = [
                {"match": {"message": keyword}} for keyword in config["keywords"]
            ] + [
                {"match": {"log_details.exception_type": keyword}} for keyword in config["keywords"]
            ]

            issue_results = opensearch_client.search(
                index=index_pattern,
                body={
                    "size": 0,
                    "query": {
                        "bool": {
                            "must": must_clauses,
                            "should": should_clauses,
                            "minimum_should_match": 1
                        }
                    },
                    "aggs": {
                        "by_service": {
                            "terms": {
                                "field": "service_name.keyword",
                                "size": 10
                            }
                        },
                        "over_time": {
                            "date_histogram": {
                                "field": "timestamp",
                                "fixed_interval": "1h",
                                "time_zone": "Asia/Seoul"
                            }
                        }
                    }
                }
            )

            total_count = issue_results.get("hits", {}).get("total", {}).get("value", 0)

            if total_count > 0:
                # 서비스별 분포
                service_buckets = issue_results.get("aggregations", {}).get("by_service", {}).get("buckets", [])
                affected_services = [{"name": sb["key"], "count": sb["doc_count"]} for sb in service_buckets]

                # 시간대별 추세
                time_buckets = issue_results.get("aggregations", {}).get("over_time", {}).get("buckets", [])
                non_zero_periods = [b for b in time_buckets if b.get("doc_count", 0) > 0]

                # 추세 판단 (최근 3개 vs 이전 3개)
                if len(non_zero_periods) >= 6:
                    recent_avg = sum([b["doc_count"] for b in non_zero_periods[-3:]]) / 3
                    previous_avg = sum([b["doc_count"] for b in non_zero_periods[-6:-3]]) / 3
                    trend = "증가" if recent_avg > previous_avg * 1.2 else "감소" if recent_avg < previous_avg * 0.8 else "안정"
                else:
                    trend = "판단 불가"

                resource_issues.append({
                    "type": issue_type,
                    "category": config["category"],
                    "severity": config["severity"],
                    "count": total_count,
                    "affected_services": affected_services,
                    "trend": trend,
                    "keywords": config["keywords"]
                })

        if not resource_issues:
            return f"최근 {time_hours}시간 동안 리소스 관련 이슈가 감지되지 않았습니다."

        # 심각도 순으로 정렬
        severity_order = {"🔴 CRITICAL": 1, "🟠 HIGH": 2, "🟡 MEDIUM": 3}
        resource_issues.sort(key=lambda x: (severity_order.get(x["severity"], 99), -x["count"]))

        # 결과 포맷팅
        summary_lines = [
            f"=== 리소스 이슈 감지 (최근 {time_hours}시간) ===",
            f"감지된 이슈: {len(resource_issues)}개 유형",
            ""
        ]

        if service_name:
            summary_lines.append(f"🔧 서비스 필터: {service_name}")
            summary_lines.append("")

        # 이슈별 상세
        total_resource_errors = sum([issue["count"] for issue in resource_issues])
        summary_lines.append(f"**총 리소스 관련 에러**: {total_resource_errors:,}건")
        summary_lines.append("")

        for i, issue in enumerate(resource_issues, 1):
            summary_lines.append(f"## {i}. {issue['severity']} {issue['category']}")
            summary_lines.append(f"   발생 횟수: {issue['count']}건")
            summary_lines.append(f"   추세: {issue['trend']}")
            summary_lines.append("")

            # 영향받은 서비스
            if issue["affected_services"]:
                summary_lines.append("   **영향받은 서비스:**")
                for svc in issue["affected_services"][:5]:
                    summary_lines.append(f"   - {svc['name']}: {svc['count']}건")
                summary_lines.append("")

            # 관련 키워드
            keywords_str = ", ".join(issue["keywords"][:3])
            summary_lines.append(f"   관련 키워드: {keywords_str}")
            summary_lines.append("")

        # 심각도 요약
        critical_issues = [i for i in resource_issues if "CRITICAL" in i["severity"]]
        high_issues = [i for i in resource_issues if "HIGH" in i["severity"]]

        summary_lines.append("## 📊 심각도별 요약:")
        summary_lines.append("")
        if critical_issues:
            summary_lines.append(f"🔴 CRITICAL: {len(critical_issues)}개 이슈")
            for issue in critical_issues:
                summary_lines.append(f"  - {issue['category']}: {issue['count']}건")
        if high_issues:
            summary_lines.append(f"🟠 HIGH: {len(high_issues)}개 이슈")
            for issue in high_issues:
                summary_lines.append(f"  - {issue['category']}: {issue['count']}건")
        summary_lines.append("")

        # 권장 조치
        summary_lines.append("## 💡 권장 조치:")
        summary_lines.append("")

        if critical_issues:
            summary_lines.append("🚨 **긴급 조치 필요**:")
            for issue in critical_issues:
                if issue["type"] == "memory":
                    summary_lines.append("  - 메모리 힙 덤프 분석")
                    summary_lines.append("  - JVM 힙 크기 증설 검토")
                    summary_lines.append("  - 메모리 누수(Memory Leak) 점검")
                elif issue["type"] == "database":
                    summary_lines.append("  - DB 연결 풀 크기 증설")
                    summary_lines.append("  - 장시간 실행 쿼리 점검")
                    summary_lines.append("  - Connection Leak 감지 활성화")
                elif issue["type"] == "disk":
                    summary_lines.append("  - 디스크 공간 확보")
                    summary_lines.append("  - 로그 로테이션 점검")
                    summary_lines.append("  - 임시 파일 정리")
        else:
            summary_lines.append("⚠️ **모니터링 강화**:")
            summary_lines.append("  - 리소스 사용량 추이 관찰")
            summary_lines.append("  - 용량 계획(Capacity Planning) 검토")
            summary_lines.append("  - 알림 임계치 설정")

        return "\n".join(summary_lines)

    except Exception as e:
        return f"리소스 이슈 감지 중 오류 발생: {str(e)}"
