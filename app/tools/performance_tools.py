"""
성능 분석 도구 (Performance Analysis Tools)
- API 응답 시간 분석, 트래픽 분포 분석
"""

from typing import Optional
from datetime import datetime, timedelta
from langchain_core.tools import tool

from app.core.opensearch import opensearch_client


@tool
async def get_slowest_apis(
    project_uuid: str,
    limit: int = 10,
    time_hours: int = 168,  # 기본 7일
    service_name: Optional[str] = None,
    min_execution_time: Optional[int] = None
) -> str:
    """
    응답 시간이 가장 느린 API 엔드포인트를 조회합니다.

    사용 시나리오:
    - "응답 시간이 가장 느린 API는?"
    - "평균 응답 시간이 긴 엔드포인트 분석"
    - "성능 병목 지점 파악"

    입력 파라미터 (JSON 형식):
        limit: 조회할 API 개수 (기본 10개)
        time_hours: 분석 시간 범위 (시간 단위, 기본 168시간=7일)
        service_name: 서비스 이름 필터 (선택)
        min_execution_time: 최소 실행 시간 필터 (밀리초, 선택)

    참고: project_uuid는 자동으로 주입되므로 전달하지 마세요.

    Returns:
        응답 시간이 느린 API 목록 (평균/최대/P95 포함)
    """
    # 인덱스 패턴 (UUID의 하이픈을 언더스코어로 변환)
    index_pattern = f"{project_uuid.replace('-', '_')}_*"

    # 시간 범위 계산
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)

    # Query 구성
    # 평면 구조와 nested 구조 모두 지원 (execution_time, duration, log_details.execution_time)
    must_clauses = [
        {
            "bool": {
                "should": [
                    {"exists": {"field": "log_details.execution_time"}},
                    {"exists": {"field": "execution_time"}},
                    {"exists": {"field": "duration"}}
                ],
                "minimum_should_match": 1
            }
        },
        {"range": {
            "timestamp": {
                "gte": start_time.isoformat() + "Z",
                "lte": end_time.isoformat() + "Z"
            }
        }},
        # 실제 HTTP API만 필터링 (Controller 레이어)
        {
            "bool": {
                "should": [
                    {"exists": {"field": "log_details.request_uri"}},
                    {"exists": {"field": "log_details.request_uri.keyword"}}
                ],
                "minimum_should_match": 1
            }
        }
    ]

    if service_name:
        must_clauses.append({"term": {"service_name": service_name}})

    if min_execution_time:
        # 모든 execution_time 필드에 대해 필터 적용
        must_clauses.append({
            "bool": {
                "should": [
                    {"range": {"log_details.execution_time": {"gte": min_execution_time}}},
                    {"range": {"execution_time": {"gte": min_execution_time}}},
                    {"range": {"duration": {"gte": min_execution_time}}}
                ],
                "minimum_should_match": 1
            }
        })

    try:
        # OpenSearch Aggregation Query
        results = opensearch_client.search(
            index=index_pattern,
            body={
                "size": 0,  # 집계만 수행
                "query": {
                    "bool": {"must": must_clauses}
                },
                "aggs": {
                    "by_api": {
                        "terms": {
                            # HTTP 메서드 + request_uri 조합으로 API 식별
                            "script": {
                                "source": """
                                    String httpMethod = 'N/A';
                                    String requestUri = 'unknown';

                                    if (doc.containsKey('log_details.http_method.keyword') &&
                                        doc['log_details.http_method.keyword'].size() > 0) {
                                        httpMethod = doc['log_details.http_method.keyword'].value;
                                    }

                                    if (doc.containsKey('log_details.request_uri.keyword') &&
                                        doc['log_details.request_uri.keyword'].size() > 0) {
                                        requestUri = doc['log_details.request_uri.keyword'].value;
                                    }

                                    if (requestUri != 'unknown') {
                                        return httpMethod + ' ' + requestUri;
                                    } else {
                                        return 'unknown';
                                    }
                                """
                            },
                            "size": limit,
                            "order": {"avg_time": "desc"}  # 평균 시간 내림차순
                        },
                        "aggs": {
                            "avg_time": {
                                "avg": {
                                    # 평면/nested 구조 모두 지원
                                    "script": {
                                        "source": """
                                            if (doc.containsKey('log_details.execution_time') &&
                                                doc['log_details.execution_time'].size() > 0) {
                                                return doc['log_details.execution_time'].value;
                                            } else if (doc.containsKey('execution_time') &&
                                                       doc['execution_time'].size() > 0) {
                                                return doc['execution_time'].value;
                                            } else if (doc.containsKey('duration') &&
                                                       doc['duration'].size() > 0) {
                                                return doc['duration'].value;
                                            } else {
                                                return null;
                                            }
                                        """
                                    }
                                }
                            },
                            "max_time": {
                                "max": {
                                    "script": {
                                        "source": """
                                            if (doc.containsKey('log_details.execution_time') &&
                                                doc['log_details.execution_time'].size() > 0) {
                                                return doc['log_details.execution_time'].value;
                                            } else if (doc.containsKey('execution_time') &&
                                                       doc['execution_time'].size() > 0) {
                                                return doc['execution_time'].value;
                                            } else if (doc.containsKey('duration') &&
                                                       doc['duration'].size() > 0) {
                                                return doc['duration'].value;
                                            } else {
                                                return null;
                                            }
                                        """
                                    }
                                }
                            },
                            "min_time": {
                                "min": {
                                    "script": {
                                        "source": """
                                            if (doc.containsKey('log_details.execution_time') &&
                                                doc['log_details.execution_time'].size() > 0) {
                                                return doc['log_details.execution_time'].value;
                                            } else if (doc.containsKey('execution_time') &&
                                                       doc['execution_time'].size() > 0) {
                                                return doc['execution_time'].value;
                                            } else if (doc.containsKey('duration') &&
                                                       doc['duration'].size() > 0) {
                                                return doc['duration'].value;
                                            } else {
                                                return null;
                                            }
                                        """
                                    }
                                }
                            },
                            "percentiles": {
                                "percentiles": {
                                    "script": {
                                        "source": """
                                            if (doc.containsKey('log_details.execution_time') &&
                                                doc['log_details.execution_time'].size() > 0) {
                                                return doc['log_details.execution_time'].value;
                                            } else if (doc.containsKey('execution_time') &&
                                                       doc['execution_time'].size() > 0) {
                                                return doc['execution_time'].value;
                                            } else if (doc.containsKey('duration') &&
                                                       doc['duration'].size() > 0) {
                                                return doc['duration'].value;
                                            } else {
                                                return null;
                                            }
                                        """
                                    },
                                    "percents": [50, 95, 99]
                                }
                            },
                            "by_http_method": {
                                "terms": {
                                    "field": "log_details.http_method.keyword",
                                    "size": 5,
                                    "missing": "N/A"  # 필드가 없으면 N/A로 표시
                                }
                            }
                        }
                    }
                }
            }
        )

        # 집계 결과 추출
        buckets = results.get("aggregations", {}).get("by_api", {}).get("buckets", [])
        total_hits = results.get("hits", {}).get("total", {}).get("value", 0)

        # 'unknown' API 제외 (request_uri가 없는 내부 메서드)
        buckets = [b for b in buckets if b.get("key", "unknown") != "unknown"]

        if not buckets:
            return f"최근 {time_hours}시간 동안 응답 시간 데이터가 있는 API가 없습니다."

        # 결과 포맷팅
        summary_lines = [
            f"=== 응답 시간이 느린 API 분석 (최근 {time_hours}시간) ===",
            f"총 {total_hits}건의 요청 분석, 상위 {len(buckets)}개 API 표시",
            ""
        ]

        # API 목록
        for i, bucket in enumerate(buckets, 1):
            api_path = bucket.get("key", "N/A")
            doc_count = bucket.get("doc_count", 0)
            avg_time = bucket.get("avg_time", {}).get("value", 0)
            max_time = bucket.get("max_time", {}).get("value", 0)
            min_time = bucket.get("min_time", {}).get("value", 0)
            percentiles = bucket.get("percentiles", {}).get("values", {})
            p50 = percentiles.get("50.0", 0)
            p95 = percentiles.get("95.0", 0)
            p99 = percentiles.get("99.0", 0)

            summary_lines.append(f"{i}. {api_path}")
            summary_lines.append(f"   📊 요청 수: {doc_count}건")
            summary_lines.append(f"   ⏱️  평균 응답 시간: {avg_time:.0f}ms")
            summary_lines.append(f"   ⏱️  최대 응답 시간: {max_time:.0f}ms")
            summary_lines.append(f"   ⏱️  최소 응답 시간: {min_time:.0f}ms")
            summary_lines.append(f"   📈 P50 (중앙값): {p50:.0f}ms")
            summary_lines.append(f"   📈 P95: {p95:.0f}ms")
            summary_lines.append(f"   📈 P99: {p99:.0f}ms")

            # 성능 등급 판정
            if avg_time > 5000:
                grade = "🔴 매우 느림 (5초 이상)"
            elif avg_time > 2000:
                grade = "🟠 느림 (2-5초)"
            elif avg_time > 1000:
                grade = "🟡 보통 (1-2초)"
            else:
                grade = "🟢 빠름 (1초 이하)"
            summary_lines.append(f"   등급: {grade}")
            summary_lines.append("")

        return "\n".join(summary_lines)

    except Exception as e:
        return f"API 성능 분석 중 오류 발생: {str(e)}"


@tool
async def get_traffic_by_time(
    project_uuid: str,
    interval: str = "1h",  # 1h, 30m, 1d 등
    time_hours: int = 168,  # 기본 7일
    level: Optional[str] = None,
    service_name: Optional[str] = None
) -> str:
    """
    시간대별 트래픽 분포를 조회합니다.

    사용 시나리오:
    - "트래픽이 가장 많은 시간대는?"
    - "시간대별 에러 발생 패턴 분석"
    - "피크 타임 파악"

    입력 파라미터 (JSON 형식):
        interval: 집계 간격 (1h=1시간, 30m=30분, 1d=1일, 기본 1h)
        time_hours: 분석 시간 범위 (시간 단위, 기본 168시간=7일)
        level: 로그 레벨 필터 (ERROR, WARN, INFO, DEBUG 중 하나, 선택)
        service_name: 서비스 이름 필터 (선택)

    참고: project_uuid는 자동으로 주입되므로 전달하지 마세요.

    Returns:
        시간대별 트래픽 분포 (요청 수, 레벨별 분포 포함)
    """
    # 인덱스 패턴 (UUID의 하이픈을 언더스코어로 변환)
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

    if level:
        must_clauses.append({"term": {"level": level.upper()}})

    if service_name:
        must_clauses.append({"term": {"service_name": service_name}})

    try:
        # OpenSearch Date Histogram Aggregation
        results = opensearch_client.search(
            index=index_pattern,
            body={
                "size": 0,  # 집계만 수행
                "query": {
                    "bool": {"must": must_clauses}
                },
                "aggs": {
                    "traffic_over_time": {
                        "date_histogram": {
                            "field": "timestamp",
                            "fixed_interval": interval,
                            "time_zone": "Asia/Seoul",  # KST 시간대
                            "min_doc_count": 0  # 데이터 없는 시간대도 표시
                        },
                        "aggs": {
                            "by_level": {
                                "terms": {
                                    "field": "level.keyword",
                                    "size": 10
                                }
                            },
                            "by_service": {
                                "terms": {
                                    "field": "service_name.keyword",
                                    "size": 5
                                }
                            }
                        }
                    }
                }
            }
        )

        # 집계 결과 추출
        buckets = results.get("aggregations", {}).get("traffic_over_time", {}).get("buckets", [])
        total_hits = results.get("hits", {}).get("total", {}).get("value", 0)

        if not buckets:
            return f"최근 {time_hours}시간 동안 트래픽 데이터가 없습니다."

        # 결과 포맷팅
        summary_lines = [
            f"=== 시간대별 트래픽 분석 (최근 {time_hours}시간, 간격: {interval}) ===",
            f"총 {total_hits}건의 로그 분석, {len(buckets)}개 시간대",
            ""
        ]

        # 피크 타임 찾기
        peak_bucket = max(buckets, key=lambda b: b.get("doc_count", 0))
        peak_time = peak_bucket.get("key_as_string", "N/A")
        peak_count = peak_bucket.get("doc_count", 0)

        summary_lines.append(f"🔥 피크 타임: {peak_time[:16].replace('T', ' ')} ({peak_count}건)")
        summary_lines.append("")

        # 시간대별 상세 정보 (상위 10개 또는 전체)
        summary_lines.append("시간대별 트래픽:")
        display_buckets = sorted(buckets, key=lambda b: b.get("doc_count", 0), reverse=True)[:15]

        for bucket in display_buckets:
            time_str = bucket.get("key_as_string", "")[:16].replace("T", " ")  # YYYY-MM-DD HH:MM
            doc_count = bucket.get("doc_count", 0)

            # 레벨별 분포
            level_buckets = bucket.get("by_level", {}).get("buckets", [])
            level_dist = {lb["key"]: lb["doc_count"] for lb in level_buckets}

            # 서비스별 분포
            service_buckets = bucket.get("by_service", {}).get("buckets", [])
            top_service = service_buckets[0]["key"] if service_buckets else "N/A"

            # 기본 정보
            summary_lines.append(f"  📅 {time_str} | 총 {doc_count}건")

            # 레벨별 분포 (ERROR가 있으면 강조)
            if level_dist:
                level_str_parts = []
                for lvl in ["ERROR", "WARN", "INFO", "DEBUG"]:
                    if lvl in level_dist:
                        count = level_dist[lvl]
                        emoji = "❌" if lvl == "ERROR" else "⚠️" if lvl == "WARN" else "ℹ️" if lvl == "INFO" else "🐛"
                        level_str_parts.append(f"{emoji}{lvl}({count})")
                if level_str_parts:
                    summary_lines.append(f"     레벨: {', '.join(level_str_parts)}")

            # 주요 서비스
            if top_service != "N/A":
                summary_lines.append(f"     주요 서비스: {top_service}")

            summary_lines.append("")

        # 전체 레벨 분포 요약
        summary_lines.append("전체 레벨 분포:")
        all_level_counts = {}
        for bucket in buckets:
            for lb in bucket.get("by_level", {}).get("buckets", []):
                level_key = lb["key"]
                all_level_counts[level_key] = all_level_counts.get(level_key, 0) + lb["doc_count"]

        for lvl in ["ERROR", "WARN", "INFO", "DEBUG"]:
            if lvl in all_level_counts:
                count = all_level_counts[lvl]
                percentage = (count / total_hits * 100) if total_hits > 0 else 0
                summary_lines.append(f"  - {lvl}: {count}건 ({percentage:.1f}%)")

        return "\n".join(summary_lines)

    except Exception as e:
        return f"트래픽 분석 중 오류 발생: {str(e)}"
