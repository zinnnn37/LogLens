"""
배포 분석 도구 (Deployment Tools) - P2 Priority
- 배포 영향 분석
"""

from typing import Optional
from datetime import datetime, timedelta
from langchain_core.tools import tool

from app.core.opensearch import opensearch_client


@tool
async def analyze_deployment_impact(
    project_uuid: str,
    deployment_time: str,  # ISO format: "2025-11-11T14:30:00"
    before_hours: int = 1,
    after_hours: int = 1,
    service_name: Optional[str] = None
) -> str:
    """
    배포 전후 영향을 비교 분석합니다.

    사용 시나리오:
    - "새 배포 이후 에러가 증가했나요?"
    - "배포 시점 전후 비교해줘"
    - "롤백이 필요한가요?"
    - "배포가 성공적이었나요?"

    입력 파라미터 (JSON 형식):
        deployment_time: 배포 시각 (ISO 형식, 예: "2025-11-11T14:30:00")
        before_hours: 배포 전 분석 시간 (시간 단위, 기본 1시간)
        after_hours: 배포 후 분석 시간 (시간 단위, 기본 1시간)
        service_name: 서비스 이름 필터 (선택)

    참고: project_uuid는 자동으로 주입되므로 전달하지 마세요.

    Returns:
        배포 전후 에러/성능 비교, 롤백 권장 여부
    """
    # 인덱스 패턴
    index_pattern = f"{project_uuid.replace('-', '_')}_*"

    # 배포 시간 파싱
    try:
        deploy_dt = datetime.fromisoformat(deployment_time.replace("Z", ""))
    except ValueError:
        return f"잘못된 배포 시간 형식입니다. ISO 형식을 사용하세요 (예: 2025-11-11T14:30:00)"

    # 시간 범위 계산
    before_start = deploy_dt - timedelta(hours=before_hours)
    before_end = deploy_dt
    after_start = deploy_dt
    after_end = deploy_dt + timedelta(hours=after_hours)

    # Query 구성
    must_clauses = []
    if service_name:
        must_clauses.append({"term": {"service_name": service_name}})

    try:
        # OpenSearch Aggregation Query (배포 전/후 비교)
        results = opensearch_client.search(
            index=index_pattern,
            body={
                "size": 0,
                "query": {"bool": {"must": must_clauses}} if must_clauses else {"match_all": {}},
                "aggs": {
                    "before_deployment": {
                        "filter": {
                            "range": {
                                "timestamp": {
                                    "gte": before_start.isoformat() + "Z",
                                    "lt": before_end.isoformat() + "Z"
                                }
                            }
                        },
                        "aggs": {
                            "total_logs": {"value_count": {"field": "log_id"}},
                            "error_logs": {"filter": {"term": {"level": "ERROR"}}},
                            "warn_logs": {"filter": {"term": {"level": "WARN"}}},
                            "avg_response_time": {
                                "avg": {"field": "log_details.execution_time"}
                            },
                            "p95_response_time": {
                                "percentiles": {
                                    "field": "log_details.execution_time",
                                    "percents": [95]
                                }
                            },
                            "error_types": {
                                "terms": {
                                    "field": "log_details.exception_type.keyword",
                                    "size": 5
                                }
                            }
                        }
                    },
                    "after_deployment": {
                        "filter": {
                            "range": {
                                "timestamp": {
                                    "gte": after_start.isoformat() + "Z",
                                    "lte": after_end.isoformat() + "Z"
                                }
                            }
                        },
                        "aggs": {
                            "total_logs": {"value_count": {"field": "log_id"}},
                            "error_logs": {"filter": {"term": {"level": "ERROR"}}},
                            "warn_logs": {"filter": {"term": {"level": "WARN"}}},
                            "avg_response_time": {
                                "avg": {"field": "log_details.execution_time"}
                            },
                            "p95_response_time": {
                                "percentiles": {
                                    "field": "log_details.execution_time",
                                    "percents": [95]
                                }
                            },
                            "error_types": {
                                "terms": {
                                    "field": "log_details.exception_type.keyword",
                                    "size": 5
                                }
                            }
                        }
                    }
                }
            }
        )

        # 집계 결과 추출
        aggs = results.get("aggregations", {})
        before = aggs.get("before_deployment", {})
        after = aggs.get("after_deployment", {})

        # 배포 전 데이터
        before_total = before.get("total_logs", {}).get("value", 0)
        before_errors = before.get("error_logs", {}).get("doc_count", 0)
        before_warns = before.get("warn_logs", {}).get("doc_count", 0)
        before_error_rate = (before_errors / before_total * 100) if before_total > 0 else 0
        before_avg_time = before.get("avg_response_time", {}).get("value")
        before_p95_time = before.get("p95_response_time", {}).get("values", {}).get("95.0")
        before_error_types = {b["key"]: b["doc_count"] for b in before.get("error_types", {}).get("buckets", [])}

        # 배포 후 데이터
        after_total = after.get("total_logs", {}).get("value", 0)
        after_errors = after.get("error_logs", {}).get("doc_count", 0)
        after_warns = after.get("warn_logs", {}).get("doc_count", 0)
        after_error_rate = (after_errors / after_total * 100) if after_total > 0 else 0
        after_avg_time = after.get("avg_response_time", {}).get("value")
        after_p95_time = after.get("p95_response_time", {}).get("values", {}).get("95.0")
        after_error_types = {b["key"]: b["doc_count"] for b in after.get("error_types", {}).get("buckets", [])}

        if before_total == 0 and after_total == 0:
            return f"배포 전후 기간에 로그 데이터가 없습니다."

        # 변화율 계산
        if before_error_rate > 0:
            error_rate_change = ((after_error_rate - before_error_rate) / before_error_rate * 100)
        else:
            error_rate_change = 100 if after_error_rate > 0 else 0

        error_count_change = after_errors - before_errors

        if before_avg_time and after_avg_time:
            avg_time_change_pct = ((after_avg_time - before_avg_time) / before_avg_time * 100)
        else:
            avg_time_change_pct = 0

        # 롤백 권장 판정
        rollback_reasons = []
        rollback_score = 0

        # 조건 1: 에러율 급증 (50% 이상)
        if error_rate_change > 50:
            rollback_reasons.append(f"에러율 급증 ({error_rate_change:+.1f}%)")
            rollback_score += 3

        # 조건 2: 새로운 에러 타입 등장
        new_errors = set(after_error_types.keys()) - set(before_error_types.keys())
        if new_errors:
            rollback_reasons.append(f"새로운 에러 타입 등장 ({len(new_errors)}개)")
            rollback_score += 2

        # 조건 3: 응답 시간 크게 증가 (30% 이상)
        if avg_time_change_pct > 30:
            rollback_reasons.append(f"응답 시간 급증 ({avg_time_change_pct:+.1f}%)")
            rollback_score += 2

        # 조건 4: 에러 건수 급증 (20건 이상 증가)
        if error_count_change > 20:
            rollback_reasons.append(f"에러 건수 급증 ({error_count_change:+}건)")
            rollback_score += 1

        # 롤백 권장 여부
        if rollback_score >= 4:
            rollback_decision = "🔴 **즉시 롤백 권장**"
        elif rollback_score >= 2:
            rollback_decision = "🟡 **롤백 검토 필요**"
        else:
            rollback_decision = "🟢 **롤백 불필요 (정상 배포)**"

        # 결과 포맷팅
        summary_lines = [
            f"=== 배포 영향 분석 ===",
            ""
        ]

        if service_name:
            summary_lines.append(f"🔧 서비스: {service_name}")
            summary_lines.append("")

        # 배포 정보
        summary_lines.append("📅 배포 정보:")
        summary_lines.append(f"  - 배포 시각: {deploy_dt.strftime('%Y-%m-%d %H:%M:%S')}")
        summary_lines.append(f"  - 분석 기간: 배포 전 {before_hours}시간 vs 배포 후 {after_hours}시간")
        summary_lines.append("")

        # 종합 판정
        summary_lines.append(f"## {rollback_decision}")
        if rollback_reasons:
            summary_lines.append("")
            summary_lines.append("**롤백 고려 사유:**")
            for reason in rollback_reasons:
                summary_lines.append(f"  - {reason}")
        summary_lines.append("")

        # 상세 비교표
        summary_lines.append("## 📊 배포 전/후 비교")
        summary_lines.append("")
        summary_lines.append(f"| 메트릭 | 배포 전 ({before_hours}h) | 배포 후 ({after_hours}h) | 변화 |")
        summary_lines.append("|--------|---------|---------|------|")
        summary_lines.append(f"| 총 로그 | {int(before_total):,}건 | {int(after_total):,}건 | {int(after_total - before_total):+,}건 |")
        summary_lines.append(f"| 에러 | {before_errors}건 | {after_errors}건 | {error_count_change:+}건 |")
        summary_lines.append(f"| 경고 | {before_warns}건 | {after_warns}건 | {after_warns - before_warns:+}건 |")
        summary_lines.append(f"| 에러율 | {before_error_rate:.2f}% | {after_error_rate:.2f}% | {after_error_rate - before_error_rate:+.2f}%p |")

        if before_avg_time and after_avg_time:
            summary_lines.append(f"| 평균 응답시간 | {before_avg_time:.0f}ms | {after_avg_time:.0f}ms | {after_avg_time - before_avg_time:+.0f}ms |")
        if before_p95_time and after_p95_time:
            summary_lines.append(f"| P95 응답시간 | {before_p95_time:.0f}ms | {after_p95_time:.0f}ms | {after_p95_time - before_p95_time:+.0f}ms |")

        summary_lines.append("")

        # 에러 타입 비교
        if before_error_types or after_error_types:
            summary_lines.append("## ❌ 에러 타입 비교")
            summary_lines.append("")

            all_error_types = set(before_error_types.keys()) | set(after_error_types.keys())

            if new_errors:
                summary_lines.append(f"🆕 **새로 등장한 에러** ({len(new_errors)}개):")
                for error_type in new_errors:
                    count = after_error_types.get(error_type, 0)
                    summary_lines.append(f"  - {error_type}: {count}건")
                summary_lines.append("")

            disappeared_errors = set(before_error_types.keys()) - set(after_error_types.keys())
            if disappeared_errors:
                summary_lines.append(f"✅ **사라진 에러** ({len(disappeared_errors)}개):")
                for error_type in disappeared_errors:
                    summary_lines.append(f"  - {error_type}")
                summary_lines.append("")

            common_errors = set(before_error_types.keys()) & set(after_error_types.keys())
            if common_errors:
                summary_lines.append("📊 **기존 에러 변화:**")
                for error_type in common_errors:
                    before_count = before_error_types.get(error_type, 0)
                    after_count = after_error_types.get(error_type, 0)
                    change = after_count - before_count
                    change_emoji = "🔴" if change > 0 else "🟢" if change < 0 else "⚪"
                    summary_lines.append(f"  {change_emoji} {error_type}: {before_count}건 → {after_count}건 ({change:+})")
                summary_lines.append("")

        # 권장 조치
        summary_lines.append("## 💡 권장 조치")
        summary_lines.append("")

        if rollback_score >= 4:
            summary_lines.append("🚨 **즉시 롤백**:")
            summary_lines.append("  1. 이전 버전으로 즉시 롤백")
            summary_lines.append("  2. 영향받은 사용자 수 확인 (get_affected_users_count)")
            summary_lines.append("  3. 롤백 후 에러 감소 확인")
            summary_lines.append("  4. 배포 전 테스트 프로세스 개선")
        elif rollback_score >= 2:
            summary_lines.append("⚠️ **롤백 검토**:")
            summary_lines.append("  1. 에러 로그 상세 분석 (get_recent_errors)")
            summary_lines.append("  2. 사용자 영향도 평가 (get_affected_users_count)")
            summary_lines.append("  3. 핫픽스 가능 여부 판단")
            summary_lines.append("  4. 필요시 롤백 결정")
        else:
            summary_lines.append("✅ **배포 성공**:")
            summary_lines.append("  1. 지속적인 모니터링 유지")
            summary_lines.append("  2. 추가 {after_hours}시간 관찰 권장")
            if disappeared_errors:
                summary_lines.append(f"  3. 해결된 에러 {len(disappeared_errors)}개 확인됨 - 배포 효과 있음")

        return "\n".join(summary_lines)

    except Exception as e:
        return f"배포 영향 분석 중 오류 발생: {str(e)}"
