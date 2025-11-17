"""
모니터링 도구 (Monitoring Tools) - P0 Priority
- 에러율 추세, 서비스 헬스, 에러 빈도, API 에러율, 사용자 영향도 분석
"""

from typing import Optional
from datetime import datetime, timedelta
from langchain_core.tools import tool

from app.core.opensearch import opensearch_client


@tool
async def get_error_rate_trend(
    project_uuid: str,
    interval: str = "1h",  # 1h, 30m, 15m
    time_hours: int = 24,
    service_name: Optional[str] = None
) -> str:
    """시간대별 에러율 추세 분석. "에러율 증가?", "24시간 에러 추세", "에러율 급증" ⚠️ 1회 호출 충분, 자동 추세/경고 판정"""
    # 인덱스 패턴
    index_pattern = f"{project_uuid.replace('-', '_')}_*"

    # 시간 범위 계산
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)

    # Query 구성
    must_clauses = [
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
        # OpenSearch Aggregation Query
        results = opensearch_client.search(
            index=index_pattern,
            body={
                "size": 0,
                "query": {"bool": {"must": must_clauses}},
                "aggs": {
                    "error_rate_over_time": {
                        "date_histogram": {
                            "field": "timestamp",
                            "fixed_interval": interval,
                            "time_zone": "Asia/Seoul",
                            "min_doc_count": 0
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
                            }
                        }
                    }
                }
            }
        )

        # 집계 결과 추출
        buckets = results.get("aggregations", {}).get("error_rate_over_time", {}).get("buckets", [])
        total_hits = results.get("hits", {}).get("total", {}).get("value", 0)

        if not buckets:
            return f"최근 {time_hours}시간 동안 로그 데이터가 없습니다."

        # 에러율 데이터 추출
        error_rates = []
        for bucket in buckets:
            time_str = bucket.get("key_as_string", "")
            total_count = bucket.get("total_logs", {}).get("value", 0)
            error_count = bucket.get("error_logs", {}).get("doc_count", 0)
            error_rate = bucket.get("error_rate", {}).get("value", 0)

            if total_count > 0:  # 데이터가 있는 시간대만
                error_rates.append({
                    "time": time_str,
                    "total": int(total_count),
                    "errors": error_count,
                    "rate": error_rate
                })

        if not error_rates:
            return f"최근 {time_hours}시간 동안 로그가 없습니다."

        # 추세 분석: 최근 3개 vs 이전 3개 비교
        recent_avg = sum([r["rate"] for r in error_rates[-3:]]) / min(3, len(error_rates[-3:]))
        if len(error_rates) >= 6:
            previous_avg = sum([r["rate"] for r in error_rates[-6:-3]]) / 3
            change_rate = ((recent_avg - previous_avg) / previous_avg * 100) if previous_avg > 0 else 0
        else:
            previous_avg = recent_avg
            change_rate = 0

        # 추세 방향 판정
        if abs(change_rate) < 5:
            trend = "안정"
            trend_emoji = "🟢"
        elif change_rate > 0:
            trend = "증가"
            trend_emoji = "🔴" if change_rate > 20 else "🟡"
        else:
            trend = "감소"
            trend_emoji = "🟢"

        # 결과 포맷팅
        summary_lines = [
            f"=== 에러율 추세 분석 (최근 {time_hours}시간, 간격: {interval}) ===",
            ""
        ]

        if service_name:
            summary_lines.append(f"🔧 서비스: {service_name}")
            summary_lines.append("")

        # 현재 상태
        current = error_rates[-1]
        summary_lines.append(f"**현재 에러율**: {current['rate']:.2f}% (최근 {interval})")
        summary_lines.append(f"**추세**: {trend_emoji} {trend} 중 ({change_rate:+.1f}% vs {len(error_rates[-6:-3])}개 이전 구간)")
        summary_lines.append("")

        # 통계
        total_requests = sum([r["total"] for r in error_rates])
        total_errors = sum([r["errors"] for r in error_rates])
        avg_error_rate = (total_errors / total_requests * 100) if total_requests > 0 else 0

        summary_lines.append(f"📊 전체 통계:")
        summary_lines.append(f"  - 총 요청: {total_requests:,}건")
        summary_lines.append(f"  - 총 에러: {total_errors:,}건")
        summary_lines.append(f"  - 평균 에러율: {avg_error_rate:.2f}%")
        summary_lines.append("")

        # 시간대별 상세 (최근 10개만)
        summary_lines.append("⏱️  시간대별 에러율 (최근 10개):")
        for er in error_rates[-10:]:
            time_display = er["time"][:16].replace("T", " ")
            rate_emoji = "❌" if er["rate"] > 5 else "⚠️" if er["rate"] > 2 else "✅"
            summary_lines.append(
                f"  {rate_emoji} {time_display} | {er['rate']:.2f}% ({er['errors']}/{er['total']})"
            )

        # 경고 메시지
        if trend == "증가" and recent_avg > 3:
            summary_lines.append("")
            summary_lines.append(f"⚠️ **경고**: 에러율이 증가 추세이며 현재 {recent_avg:.1f}%로 주의가 필요합니다.")
        elif recent_avg > 5:
            summary_lines.append("")
            summary_lines.append(f"🚨 **위험**: 에러율이 {recent_avg:.1f}%로 매우 높습니다. 즉시 확인이 필요합니다.")

        return "\n".join(summary_lines)

    except Exception as e:
        return f"에러율 추세 분석 중 오류 발생: {str(e)}"


@tool
async def get_service_health_status(
    project_uuid: str,
    time_hours: int = 24,
    service_name: Optional[str] = None
) -> str:
    """서비스별 헬스 상태 분석. "불안정한 서비스?", "서비스 에러율 순위", "헬스 체크" ⚠️ 1회 호출 충분, 자동 헬스 등급 판정"""
    # 인덱스 패턴
    index_pattern = f"{project_uuid.replace('-', '_')}_*"

    # 시간 범위 계산
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)

    # Query 구성
    must_clauses = [
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
        # OpenSearch Aggregation Query
        results = opensearch_client.search(
            index=index_pattern,
            body={
                "size": 0,
                "query": {"bool": {"must": must_clauses}},
                "aggs": {
                    "by_service": {
                        "terms": {
                            "field": "service_name",
                            "size": 50
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
                            "error_rate": {
                                "bucket_script": {
                                    "buckets_path": {
                                        "errors": "error_logs>_count",
                                        "total": "total_logs"
                                    },
                                    "script": "params.total > 0 ? (params.errors / params.total * 100) : 0"
                                }
                            },
                            "success_rate": {
                                "bucket_script": {
                                    "buckets_path": {
                                        "errors": "error_logs>_count",
                                        "total": "total_logs"
                                    },
                                    "script": "params.total > 0 ? ((params.total - params.errors) / params.total * 100) : 100"
                                }
                            },
                            "avg_response_time": {
                                "avg": {"field": "log_details.execution_time"}
                            },
                            "p95_response_time": {
                                "percentiles": {
                                    "field": "log_details.execution_time",
                                    "percents": [95]
                                }
                            }
                        }
                    }
                }
            }
        )

        # 집계 결과 추출
        buckets = results.get("aggregations", {}).get("by_service", {}).get("buckets", [])

        if not buckets:
            return f"최근 {time_hours}시간 동안 서비스 로그가 없습니다."

        # 서비스별 헬스 데이터 처리
        services = []
        for bucket in buckets:
            service = bucket.get("key", "unknown")
            total = bucket.get("total_logs", {}).get("value", 0)
            errors = bucket.get("error_logs", {}).get("doc_count", 0)
            warns = bucket.get("warn_logs", {}).get("doc_count", 0)
            error_rate = bucket.get("error_rate", {}).get("value", 0)
            success_rate = bucket.get("success_rate", {}).get("value", 100)
            avg_time = bucket.get("avg_response_time", {}).get("value")
            p95_time = bucket.get("p95_response_time", {}).get("values", {}).get("95.0")

            # 헬스 점수 계산 (0-100)
            # 성공률 70% + 에러율 페널티 20% + 응답시간 10%
            health_score = success_rate * 0.7
            if error_rate > 10:
                health_score -= 20
            elif error_rate > 5:
                health_score -= 10
            elif error_rate > 2:
                health_score -= 5

            if avg_time and avg_time > 5000:  # 5초 이상
                health_score -= 10
            elif avg_time and avg_time > 2000:  # 2초 이상
                health_score -= 5

            # 헬스 등급 판정
            if health_score >= 90 and error_rate < 1:
                grade = "🟢 Healthy"
            elif health_score >= 75 and error_rate < 3:
                grade = "🟡 Degraded"
            elif health_score >= 60 and error_rate < 10:
                grade = "🟠 Warning"
            else:
                grade = "🔴 Down"

            services.append({
                "name": service,
                "total": int(total),
                "errors": errors,
                "warns": warns,
                "error_rate": error_rate,
                "success_rate": success_rate,
                "health_score": health_score,
                "grade": grade,
                "avg_time": avg_time,
                "p95_time": p95_time
            })

        # 에러율 순으로 정렬
        services.sort(key=lambda s: s["error_rate"], reverse=True)

        # 결과 포맷팅
        summary_lines = [
            f"=== 서비스 헬스 상태 (최근 {time_hours}시간) ===",
            ""
        ]

        if service_name:
            # 특정 서비스 상세 정보
            svc = services[0]
            summary_lines.append(f"## 🏥 {svc['name']} 헬스 상태")
            summary_lines.append("")
            summary_lines.append(f"**종합 등급**: {svc['grade']}")
            summary_lines.append(f"**헬스 점수**: {svc['health_score']:.1f}/100")
            summary_lines.append("")
            summary_lines.append("**메트릭**:")
            summary_lines.append(f"  - 에러율: {svc['error_rate']:.2f}%")
            summary_lines.append(f"  - 성공률: {svc['success_rate']:.2f}%")
            summary_lines.append(f"  - 총 요청: {svc['total']:,}건")
            summary_lines.append(f"  - 에러: {svc['errors']}건")
            summary_lines.append(f"  - 경고: {svc['warns']}건")

            if svc['avg_time']:
                summary_lines.append(f"  - 평균 응답시간: {svc['avg_time']:.0f}ms")
            if svc['p95_time']:
                summary_lines.append(f"  - P95 응답시간: {svc['p95_time']:.0f}ms")

            # 권장 조치
            if "Down" in svc['grade']:
                summary_lines.append("")
                summary_lines.append(f"🚨 **긴급**: {svc['name']}이(가) 심각한 상태입니다. 즉시 확인이 필요합니다.")
            elif "Warning" in svc['grade']:
                summary_lines.append("")
                summary_lines.append(f"⚠️ **주의**: {svc['name']}의 에러율이 높습니다. 모니터링을 강화하세요.")

        else:
            # 전체 서비스 요약
            summary_lines.append(f"총 {len(services)}개 서비스 분석")
            summary_lines.append("")

            # 등급별 분포
            grade_counts = {}
            for svc in services:
                grade_key = svc['grade'].split()[1]  # "🟢 Healthy" -> "Healthy"
                grade_counts[grade_key] = grade_counts.get(grade_key, 0) + 1

            summary_lines.append("등급별 분포:")
            for grade in ["Healthy", "Degraded", "Warning", "Down"]:
                count = grade_counts.get(grade, 0)
                if count > 0:
                    summary_lines.append(f"  - {grade}: {count}개 서비스")
            summary_lines.append("")

            # 서비스 상세 리스트
            summary_lines.append("서비스별 상세:")
            for i, svc in enumerate(services, 1):
                summary_lines.append(f"{i}. **{svc['name']}** | {svc['grade']}")
                summary_lines.append(f"   에러율: {svc['error_rate']:.2f}% | 성공률: {svc['success_rate']:.1f}% | 요청: {svc['total']:,}건")
                if svc['avg_time']:
                    summary_lines.append(f"   평균 응답: {svc['avg_time']:.0f}ms")
                summary_lines.append("")

            # 경고 요약
            critical_services = [s for s in services if "Down" in s['grade'] or "Warning" in s['grade']]
            if critical_services:
                summary_lines.append("⚠️ **주의가 필요한 서비스**:")
                for svc in critical_services:
                    summary_lines.append(f"  - {svc['name']}: {svc['grade']} (에러율 {svc['error_rate']:.1f}%)")

        return "\n".join(summary_lines)

    except Exception as e:
        return f"서비스 헬스 분석 중 오류 발생: {str(e)}"


@tool
async def get_error_frequency_ranking(
    project_uuid: str,
    time_hours: int = 24,  # 기본 24시간 (다른 도구들과 통일)
    limit: int = 10,
    service_name: Optional[str] = None
) -> str:
    """에러 타입별 발생 빈도 순위. "자주 발생하는 에러?", "NullPointerException 몇 번?" ⚠️ 1회 호출 충분"""
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

    if service_name:
        must_clauses.append({"term": {"service_name": service_name}})

    try:
        # OpenSearch Aggregation Query
        results = opensearch_client.search(
            index=index_pattern,
            body={
                "size": 0,
                "query": {"bool": {"must": must_clauses}},
                "aggs": {
                    "by_exception_type": {
                        "terms": {
                            "field": "log_details.exception_type",
                            "size": limit,
                            "order": {"_count": "desc"}
                        },
                        "aggs": {
                            "first_occurrence": {
                                "min": {"field": "timestamp"}
                            },
                            "last_occurrence": {
                                "max": {"field": "timestamp"}
                            },
                            "affected_services": {
                                "terms": {
                                    "field": "service_name",
                                    "size": 10
                                }
                            },
                            "over_time": {
                                "date_histogram": {
                                    "field": "timestamp",
                                    "fixed_interval": "6h",
                                    "min_doc_count": 0
                                }
                            }
                        }
                    }
                }
            }
        )

        # 집계 결과 추출
        buckets = results.get("aggregations", {}).get("by_exception_type", {}).get("buckets", [])
        total_errors = results.get("hits", {}).get("total", {}).get("value", 0)

        if not buckets:
            return f"최근 {time_hours}시간 동안 ERROR 로그가 없습니다."

        # 결과 포맷팅
        summary_lines = [
            f"=== 에러 빈도 순위 (최근 {time_hours}시간) ===",
            f"총 {total_errors}건의 에러 발생, 상위 {len(buckets)}개 표시",
            ""
        ]

        if service_name:
            summary_lines.append(f"🔧 서비스 필터: {service_name}")
            summary_lines.append("")

        # 에러 타입별 상세
        for i, bucket in enumerate(buckets, 1):
            error_type = bucket.get("key", "Unknown")
            count = bucket.get("doc_count", 0)
            first_time = bucket.get("first_occurrence", {}).get("value_as_string", "N/A")
            last_time = bucket.get("last_occurrence", {}).get("value_as_string", "N/A")

            # 시간 계산
            if first_time != "N/A" and last_time != "N/A":
                first_dt = datetime.fromisoformat(first_time.replace("Z", "+00:00"))
                last_dt = datetime.fromisoformat(last_time.replace("Z", "+00:00"))
                duration = last_dt - first_dt
                duration_hours = duration.total_seconds() / 3600

                first_display = first_time[:19].replace("T", " ")
                last_display = last_time[:19].replace("T", " ")
            else:
                duration_hours = 0
                first_display = "N/A"
                last_display = "N/A"

            # 영향받은 서비스
            service_buckets = bucket.get("affected_services", {}).get("buckets", [])
            affected_services = [sb["key"] for sb in service_buckets]

            # 반복 패턴 감지
            time_buckets = bucket.get("over_time", {}).get("buckets", [])
            non_zero_periods = [b for b in time_buckets if b.get("doc_count", 0) > 0]
            is_recurring = len(non_zero_periods) >= 3  # 3개 이상 시간대에 발생하면 반복으로 간주

            # 출력
            summary_lines.append(f"{i}. **{error_type}**")
            summary_lines.append(f"   📊 발생 횟수: {count}건")
            summary_lines.append(f"   ⏰ 최초 발생: {first_display}")
            summary_lines.append(f"   ⏰ 최근 발생: {last_display}")

            if duration_hours > 1:
                summary_lines.append(f"   ⏱️  발생 기간: {duration_hours:.1f}시간")

            if is_recurring:
                summary_lines.append(f"   🔁 패턴: 반복 발생 ({len(non_zero_periods)}개 시간대)")
            else:
                summary_lines.append(f"   📍 패턴: 일시적 발생")

            if affected_services:
                services_str = ", ".join(affected_services[:3])
                if len(affected_services) > 3:
                    services_str += f" 외 {len(affected_services) - 3}개"
                summary_lines.append(f"   🔧 영향 서비스: {services_str}")

            summary_lines.append("")

        # 요약 통계
        total_unique_errors = len(buckets)
        recurring_errors = sum(1 for b in buckets if len([tb for tb in b.get("over_time", {}).get("buckets", []) if tb.get("doc_count", 0) > 0]) >= 3)

        summary_lines.append("📈 요약:")
        summary_lines.append(f"  - 고유 에러 타입: {total_unique_errors}개")
        summary_lines.append(f"  - 반복 발생 에러: {recurring_errors}개")
        summary_lines.append(f"  - 일시적 에러: {total_unique_errors - recurring_errors}개")

        if recurring_errors > 0:
            summary_lines.append("")
            summary_lines.append(f"⚠️ **주의**: {recurring_errors}개의 에러가 반복적으로 발생하고 있습니다. 근본 원인 분석이 필요합니다.")

        return "\n".join(summary_lines)

    except Exception as e:
        return f"에러 빈도 분석 중 오류 발생: {str(e)}"


@tool
async def get_api_error_rates(
    project_uuid: str,
    time_hours: int = 24,
    limit: int = 10,
    min_requests: int = 10  # 최소 요청 수 (너무 적은 API 제외)
) -> str:
    """
    API 엔드포인트별 에러율 및 실패율 비교 분석.
    사용: "에러 많은 API는?", "API별 에러율 비교", "엔드포인트 실패율"
    Args: project_uuid (필수), time_hours (기본 24), limit (기본 10)
    ⚠️ 1회 호출 충분
    """
    # 인덱스 패턴
    index_pattern = f"{project_uuid.replace('-', '_')}_*"

    # 시간 범위 계산
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)

    # Query 구성
    must_clauses = [
        {"exists": {"field": "log_details.request_uri"}},
        {"range": {
            "timestamp": {
                "gte": start_time.isoformat() + "Z",
                "lte": end_time.isoformat() + "Z"
            }
        }}
    ]

    try:
        # OpenSearch Aggregation Query
        results = opensearch_client.search(
            index=index_pattern,
            body={
                "size": 0,
                "query": {"bool": {"must": must_clauses}},
                "aggs": {
                    "by_endpoint": {
                        "terms": {
                            "field": "log_details.request_uri",
                            "size": limit * 2,  # 필터링 후 limit개 남기기 위해 더 가져옴
                            "min_doc_count": min_requests
                        },
                        "aggs": {
                            "total_requests": {
                                "value_count": {"field": "log_id"}
                            },
                            "error_requests": {
                                "filter": {"term": {"level": "ERROR"}}
                            },
                            "error_rate": {
                                "bucket_script": {
                                    "buckets_path": {
                                        "errors": "error_requests>_count",
                                        "total": "total_requests"
                                    },
                                    "script": "params.total > 0 ? (params.errors / params.total * 100) : 0"
                                }
                            },
                            "by_status_code": {
                                "terms": {
                                    "field": "log_details.response_status",
                                    "size": 10
                                }
                            },
                            "by_http_method": {
                                "terms": {
                                    "field": "log_details.http_method",
                                    "size": 5
                                }
                            },
                            "by_service": {
                                "terms": {
                                    "field": "service_name",
                                    "size": 5
                                }
                            }
                        }
                    }
                }
            }
        )

        # 집계 결과 추출
        buckets = results.get("aggregations", {}).get("by_endpoint", {}).get("buckets", [])

        if not buckets:
            return f"최근 {time_hours}시간 동안 API 요청 로그가 없습니다."

        # 에러율 순으로 정렬 후 limit개만
        buckets_sorted = sorted(buckets, key=lambda b: b.get("error_rate", {}).get("value", 0), reverse=True)[:limit]

        # 결과 포맷팅
        summary_lines = [
            f"=== API 엔드포인트 에러율 (최근 {time_hours}시간) ===",
            f"에러율이 높은 상위 {len(buckets_sorted)}개 API",
            ""
        ]

        # API별 상세
        for i, bucket in enumerate(buckets_sorted, 1):
            endpoint = bucket.get("key", "N/A")
            total = bucket.get("total_requests", {}).get("value", 0)
            errors = bucket.get("error_requests", {}).get("doc_count", 0)
            error_rate = bucket.get("error_rate", {}).get("value", 0)
            success_rate = 100 - error_rate

            # HTTP 상태 코드 분포
            status_buckets = bucket.get("by_status_code", {}).get("buckets", [])
            status_5xx = sum([sb["doc_count"] for sb in status_buckets if 500 <= sb.get("key", 0) < 600])
            status_4xx = sum([sb["doc_count"] for sb in status_buckets if 400 <= sb.get("key", 0) < 500])

            # HTTP 메서드
            method_buckets = bucket.get("by_http_method", {}).get("buckets", [])
            methods = [mb["key"] for mb in method_buckets]
            method_str = "/".join(methods) if methods else "N/A"

            # 서비스
            service_buckets = bucket.get("by_service", {}).get("buckets", [])
            services = [sb["key"] for sb in service_buckets]
            service_str = ", ".join(services[:2])

            # 등급 판정
            if error_rate > 10:
                grade_emoji = "🔴"
                grade = "매우 불안정"
            elif error_rate > 5:
                grade_emoji = "🟠"
                grade = "불안정"
            elif error_rate > 2:
                grade_emoji = "🟡"
                grade = "주의"
            else:
                grade_emoji = "🟢"
                grade = "정상"

            # 출력
            summary_lines.append(f"{i}. {grade_emoji} **{endpoint}**")
            summary_lines.append(f"   메서드: {method_str} | 서비스: {service_str}")
            summary_lines.append(f"   📊 에러율: {error_rate:.2f}% | 성공률: {success_rate:.2f}%")
            summary_lines.append(f"   📈 총 요청: {int(total):,}건 | 에러: {errors}건")

            if status_5xx > 0 or status_4xx > 0:
                summary_lines.append(f"   🌐 HTTP: 5xx({status_5xx}건) / 4xx({status_4xx}건)")

            summary_lines.append(f"   등급: {grade}")
            summary_lines.append("")

        # 요약 통계
        critical_apis = sum(1 for b in buckets_sorted if b.get("error_rate", {}).get("value", 0) > 10)
        warning_apis = sum(1 for b in buckets_sorted if 5 < b.get("error_rate", {}).get("value", 0) <= 10)

        summary_lines.append("📊 요약:")
        summary_lines.append(f"  - 매우 불안정 (>10%): {critical_apis}개 API")
        summary_lines.append(f"  - 불안정 (5-10%): {warning_apis}개 API")
        summary_lines.append(f"  - 주의/정상 (<5%): {len(buckets_sorted) - critical_apis - warning_apis}개 API")

        if critical_apis > 0:
            summary_lines.append("")
            summary_lines.append(f"🚨 **긴급**: {critical_apis}개 API의 에러율이 10%를 초과합니다. 즉시 확인이 필요합니다.")

        return "\n".join(summary_lines)

    except Exception as e:
        return f"API 에러율 분석 중 오류 발생: {str(e)}"


@tool
async def get_affected_users_count(
    project_uuid: str,
    time_hours: int = 24,
    error_only: bool = True
) -> str:
    """
    영향받은 사용자 수를 분석합니다 (requester_ip 기반).

    이 도구는 다음을 수행합니다:
    - ✅ 고유 사용자 수 계산 (IP 기반)
    - ✅ 전체 대비 영향 비율 제공
    - ✅ 사용자별 에러 빈도 분석
    - ✅ 반복 에러 사용자 감지
    - ❌ 특정 에러 타입별 분석 안 함 (get_error_frequency_ranking 사용)
    - ❌ 시간대별 추세는 제공 안 함 (get_error_rate_trend 사용)

    사용 시나리오:
    1. "몇 명이 에러를 겪었어?"
    2. "사용자 영향도는?"

    ⚠️ 중요한 제약사항:
    - 1회 호출로 충분합니다
    - requester_ip 필드가 필요합니다

    입력 파라미터 (JSON 형식):
        time_hours: 분석 기간 (기본 24시간)
        error_only: 에러만 분석 (기본 True)

    관련 도구:
    - get_error_frequency_ranking: 에러 타입별 빈도
    - get_service_health_status: 서비스별 헬스

    Returns:
        고유 사용자 수, 영향 비율, 사용자별 빈도
    """
    # 인덱스 패턴
    index_pattern = f"{project_uuid.replace('-', '_')}_*"

    # 시간 범위 계산
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)

    # Query 구성
    must_clauses = [
        {"exists": {"field": "requester_ip"}},
        {"range": {
            "timestamp": {
                "gte": start_time.isoformat() + "Z",
                "lte": end_time.isoformat() + "Z"
            }
        }}
    ]

    try:
        # OpenSearch Aggregation Query
        results = opensearch_client.search(
            index=index_pattern,
            body={
                "size": 0,
                "query": {"bool": {"must": must_clauses}},
                "aggs": {
                    "total_unique_users": {
                        "cardinality": {
                            "script": {
                                "source": "doc['requester_ip'].value.toString()",
                                "lang": "painless"
                            }
                        }
                    },
                    "users_with_errors": {
                        "filter": {"term": {"level": "ERROR"}},
                        "aggs": {
                            "unique_error_users": {
                                "cardinality": {
                                    "script": {
                                        "source": "doc['requester_ip'].value.toString()",
                                        "lang": "painless"
                                    }
                                }
                            },
                            "top_affected_users": {
                                "terms": {
                                    "script": {
                                        "source": "doc['requester_ip'].value.toString()",
                                        "lang": "painless"
                                    },
                                    "size": 10,
                                    "order": {"_count": "desc"}
                                },
                                "aggs": {
                                    "error_types": {
                                        "terms": {
                                            "field": "log_details.exception_type",
                                            "size": 5
                                        }
                                    }
                                }
                            }
                        }
                    },
                    "total_error_count": {
                        "filter": {"term": {"level": "ERROR"}}
                    }
                }
            }
        )

        # 집계 결과 추출
        aggs = results.get("aggregations", {})
        total_unique_users = aggs.get("total_unique_users", {}).get("value", 0)
        users_with_errors_agg = aggs.get("users_with_errors", {})
        unique_error_users = users_with_errors_agg.get("unique_error_users", {}).get("value", 0)
        total_error_count = aggs.get("total_error_count", {}).get("doc_count", 0)

        if total_unique_users == 0:
            return f"최근 {time_hours}시간 동안 사용자 활동이 없습니다 (requester_ip 필드 없음)."

        # 영향 비율 계산
        affected_percentage = (unique_error_users / total_unique_users * 100) if total_unique_users > 0 else 0

        # 결과 포맷팅
        summary_lines = [
            f"=== 영향받은 사용자 분석 (최근 {time_hours}시간) ===",
            ""
        ]

        # 전체 통계
        summary_lines.append("📊 전체 사용자:")
        summary_lines.append(f"  - 총 고유 사용자 (IP): {total_unique_users:,}명")
        summary_lines.append(f"  - 에러를 경험한 사용자: {unique_error_users:,}명")
        summary_lines.append(f"  - 영향 비율: {affected_percentage:.2f}%")
        summary_lines.append("")

        # 에러 통계
        summary_lines.append("❌ 에러 통계:")
        summary_lines.append(f"  - 총 에러 발생: {total_error_count:,}건")
        if unique_error_users > 0:
            avg_errors_per_user = total_error_count / unique_error_users
            summary_lines.append(f"  - 사용자당 평균 에러: {avg_errors_per_user:.1f}건")
        summary_lines.append("")

        # 가장 많이 영향받은 사용자
        top_users_buckets = users_with_errors_agg.get("top_affected_users", {}).get("buckets", [])
        if top_users_buckets:
            summary_lines.append("👥 가장 많이 영향받은 사용자 (IP):")
            for i, bucket in enumerate(top_users_buckets, 1):
                user_ip = bucket.get("key", "N/A")
                error_count = bucket.get("doc_count", 0)

                # 에러 타입
                error_type_buckets = bucket.get("error_types", {}).get("buckets", [])
                error_types = [etb["key"] for etb in error_type_buckets]
                error_types_str = ", ".join(error_types[:3])
                if len(error_types) > 3:
                    error_types_str += f" 외 {len(error_types) - 3}개"

                summary_lines.append(f"  {i}. {user_ip}")
                summary_lines.append(f"     에러 발생: {error_count}건")
                if error_types_str:
                    summary_lines.append(f"     에러 타입: {error_types_str}")

            summary_lines.append("")

        # 심각도 평가
        if affected_percentage > 50:
            severity = "🔴 매우 심각"
            message = f"전체 사용자의 절반 이상({affected_percentage:.1f}%)이 에러를 경험했습니다. 긴급 대응이 필요합니다."
        elif affected_percentage > 20:
            severity = "🟠 심각"
            message = f"전체 사용자의 {affected_percentage:.1f}%가 에러를 경험했습니다. 즉시 확인이 필요합니다."
        elif affected_percentage > 5:
            severity = "🟡 주의"
            message = f"일부 사용자({affected_percentage:.1f}%)가 에러를 경험했습니다. 모니터링을 강화하세요."
        else:
            severity = "🟢 양호"
            message = f"소수 사용자({affected_percentage:.1f}%)만 에러를 경험했습니다."

        summary_lines.append(f"**심각도**: {severity}")
        summary_lines.append(f"**평가**: {message}")

        return "\n".join(summary_lines)

    except Exception as e:
        return f"사용자 영향도 분석 중 오류 발생: {str(e)}"


@tool
async def detect_anomalies(
    project_uuid: str,
    metric: str = "error_rate",
    time_hours: int = 168,
    sensitivity: float = 2.0
) -> str:
    """
    통계 기반 이상치 탐지 (평소보다 비정상적인 패턴 감지)

    과거 데이터의 평균과 표준편차를 기준으로 현재 값이 이상한지 판단

    Args:
        project_uuid: 프로젝트 UUID
        metric: 측정 지표 (error_rate|traffic|latency)
        time_hours: 분석 기간 (기본 7일)
        sensitivity: 민감도 (표준편차 배수, 기본 2.0)
            - 2.0 = ±2σ 벗어나면 이상 (95%)
            - 3.0 = ±3σ 벗어나면 이상 (99.7%, 더 엄격)

    Returns:
        이상치 탐지 결과 + 정상 범위 + 현재 값

    Examples:
        - "평소보다 에러가 많나요?"
        - "트래픽이 비정상적으로 급증했나요?"
    """
    try:
        from app.core.opensearch import opensearch_client
        from datetime import datetime, timedelta
        import statistics

        client = opensearch_client
        index_name = f"logs_{project_uuid}"

        now = datetime.now()
        start_time = now - timedelta(hours=time_hours)

        summary_lines = [f"=== 이상 탐지 분석 ({metric}) ===", ""]

        # 시간대별 데이터 수집 (1시간 단위)
        interval = "1h"
        response = client.search(
            index=index_name,
            body={
                "size": 0,
                "query": {
                    "range": {
                        "timestamp": {
                            "gte": start_time.isoformat(),
                            "lte": now.isoformat()
                        }
                    }
                },
                "aggs": {
                    "time_buckets": {
                        "date_histogram": {
                            "field": "timestamp",
                            "fixed_interval": interval
                        },
                        "aggs": {
                            "error_count": {
                                "filter": {"term": {"level.keyword": "ERROR"}}
                            }
                        }
                    }
                }
            }
        )

        buckets = response['aggregations']['time_buckets']['buckets']

        if len(buckets) < 10:
            from app.tools.response_templates import get_conditional_failure_message
            return get_conditional_failure_message(
                condition="데이터 포인트",
                current_value=f"{len(buckets)}개",
                required_value="10개 이상",
                tool_name="이상 탐지 분석"
            )

        # 메트릭별 값 계산
        values = []
        timestamps = []

        for bucket in buckets:
            timestamp = bucket['key_as_string']
            total_count = bucket['doc_count']
            error_count = bucket['error_count']['doc_count']

            if metric == "error_rate":
                value = (error_count / total_count * 100) if total_count > 0 else 0.0
            elif metric == "traffic":
                value = total_count
            elif metric == "latency":
                # latency는 별도 aggregation 필요 (여기서는 단순화)
                value = 0.0  # 실제 구현 시 avg 사용
            else:
                return f"지원하지 않는 metric: {metric}"

            values.append(value)
            timestamps.append(timestamp)

        if len(values) < 10:
            from app.tools.response_templates import get_conditional_failure_message
            return get_conditional_failure_message(
                condition="유효한 데이터 포인트",
                current_value=f"{len(values)}개",
                required_value="10개 이상",
                tool_name="이상 탐지 분석"
            )

        # 통계 계산
        mean_value = statistics.mean(values)
        stdev_value = statistics.stdev(values) if len(values) > 1 else 0.0

        # 정상 범위 계산 (평균 ± sensitivity×표준편차)
        upper_threshold = mean_value + (sensitivity * stdev_value)
        lower_threshold = max(0, mean_value - (sensitivity * stdev_value))

        # 현재 값 (최근 1시간)
        current_value = values[-1] if values else 0.0
        current_time = timestamps[-1] if timestamps else "N/A"

        # 이상 여부 판단
        is_anomaly = current_value > upper_threshold or current_value < lower_threshold
        anomaly_type = ""
        if current_value > upper_threshold:
            anomaly_type = "급증 (상한 초과)"
        elif current_value < lower_threshold:
            anomaly_type = "급감 (하한 미달)"

        # 결과 요약
        summary_lines.append(f"**분석 기간:** {time_hours}시간 ({len(buckets)}개 데이터 포인트)")
        summary_lines.append(f"**측정 지표:** {metric}")
        summary_lines.append(f"**민감도:** ±{sensitivity}σ (표준편차)")
        summary_lines.append("")

        summary_lines.append("**📊 통계 정보:**")
        unit = "%" if metric == "error_rate" else "건"
        summary_lines.append(f"- 평균값: **{mean_value:.2f}{unit}**")
        summary_lines.append(f"- 표준편차: {stdev_value:.2f}{unit}")
        summary_lines.append(f"- 정상 범위: **{lower_threshold:.2f} ~ {upper_threshold:.2f}{unit}**")
        summary_lines.append("")

        summary_lines.append("**🔍 현재 상태:**")
        summary_lines.append(f"- 시간: {current_time}")
        summary_lines.append(f"- 현재 값: **{current_value:.2f}{unit}**")
        summary_lines.append("")

        if is_anomaly:
            summary_lines.append(f"**🚨 이상 탐지: {anomaly_type}**")
            deviation = abs(current_value - mean_value) / stdev_value if stdev_value > 0 else 0
            summary_lines.append(f"- 평균 대비 {deviation:.1f}σ 벗어남")
            summary_lines.append(f"- 평균 대비 {abs(current_value - mean_value):.2f}{unit} 차이")
            summary_lines.append("")
            summary_lines.append("**💡 권장 조치:**")
            if current_value > upper_threshold:
                summary_lines.append("1. 최근 배포나 설정 변경 확인")
                summary_lines.append("2. 서비스 로그 상세 분석")
                summary_lines.append("3. 알림 설정 및 모니터링 강화")
            else:
                summary_lines.append("1. 데이터 수집 정상 작동 확인")
                summary_lines.append("2. 트래픽 유입 경로 점검")
        else:
            summary_lines.append("**✅ 정상 범위 내**")
            summary_lines.append(f"- 평균 대비 정상적인 수준입니다")
            summary_lines.append(f"- 현재 값은 정상 범위 ({lower_threshold:.2f} ~ {upper_threshold:.2f}{unit}) 안에 있습니다")

        return "\n".join(summary_lines)

    except Exception as e:
        return f"이상 탐지 분석 중 오류 발생: {str(e)}"


@tool
async def compare_source_types(
    project_uuid: str,
    time_hours: int = 24
) -> str:
    """FE/BE/INFRA 소스별 에러율 및 로그 분포 비교. 사용: "프론트엔드 vs 백엔드 에러 비교" ⚠️ 1회 호출 충분"""
    from datetime import datetime, timedelta
    from app.core.opensearch import opensearch_client
    
    index_pattern = f"{project_uuid.replace('-', '_')}_*"
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)
    
    try:
        results = opensearch_client.search(index=index_pattern, body={
            "size": 0, "query": {"range": {"timestamp": {"gte": start_time.isoformat()+"Z", "lte": end_time.isoformat()+"Z"}}},
            "aggs": {"by_source": {"terms": {"field": "source_type", "size": 5}, 
                "aggs": {"error_count": {"filter": {"term": {"level": "ERROR"}}}}}}})
        
        buckets = results.get("aggregations", {}).get("by_source", {}).get("buckets", [])
        if not buckets: return f"최근 {time_hours}시간 로그 없음"
        
        lines = [f"## 📊 Source Type별 비교 (최근 {time_hours}시간)", ""]
        lines.extend(["| Source | 총 로그 | 에러 | 에러율 |", "|--------|---------|------|--------|"])
        for b in buckets:
            src = b.get("key", "Unknown")
            total = b.get("doc_count", 0)
            errors = b.get("error_count", {}).get("doc_count", 0)
            rate = (errors/total*100) if total > 0 else 0
            lines.append(f"| {src} | {total} | {errors} | {rate:.1f}% |")
        return "\n".join(lines)
    except Exception as e:
        return f"Source Type 비교 중 오류: {str(e)}"


@tool
async def analyze_logger_activity(
    project_uuid: str,
    time_hours: int = 24,
    top_n: int = 10
) -> str:
    """로거별 활동량과 에러율을 분석. 사용: "어떤 클래스가 로그를 많이 남겨?", "로그 노이즈 많은 클래스" ⚠️ 1회 호출 충분"""
    from datetime import datetime, timedelta
    from app.core.opensearch import opensearch_client
    
    index_pattern = f"{project_uuid.replace('-', '_')}_*"
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)
    
    try:
        results = opensearch_client.search(index=index_pattern, body={
            "size": 0, "query": {"range": {"timestamp": {"gte": start_time.isoformat()+"Z", "lte": end_time.isoformat()+"Z"}}},
            "aggs": {"by_logger": {"terms": {"field": "logger", "size": top_n}, 
                "aggs": {"errors": {"filter": {"term": {"level": "ERROR"}}}}}}})
        
        buckets = results.get("aggregations", {}).get("by_logger", {}).get("buckets", [])
        if not buckets: return f"최근 {time_hours}시간 로거 데이터 없음"
        
        lines = [f"## 📝 로거 활동 TOP {top_n}", ""]
        lines.extend(["| 로거 | 로그 수 | 에러 | 평가 |", "|------|---------|------|------|"])
        for b in buckets:
            logger = b.get("key", "")[:50]
            count = b.get("doc_count", 0)
            errors = b.get("errors", {}).get("doc_count", 0)
            noise = "🔊 높음" if count > 1000 else "낮음"
            lines.append(f"| {logger} | {count} | {errors} | {noise} |")
        return "\n".join(lines)
    except Exception as e:
        return f"로거 활동 분석 중 오류: {str(e)}"

