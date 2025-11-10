"""
분석 도구 (Analysis Tools)
- 로그 통계, 최근 에러 분석
"""

from typing import Optional
from datetime import datetime, timedelta
from langchain_core.tools import tool

from app.core.opensearch import opensearch_client


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

    Args:
        project_uuid: 프로젝트 UUID (언더스코어 형식)
        time_hours: 통계 기간 (시간 단위, 기본 24시간)
        group_by: 집계 기준 (level, service_name, source_type 중 하나, 기본 level)

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

    # 인덱스 패턴
    index_pattern = f"{project_uuid}_*"

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
        # OpenSearch Aggregation 실행
        results = await opensearch_client.search(
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

    Args:
        project_uuid: 프로젝트 UUID (언더스코어 형식)
        limit: 조회할 개수 (기본 10개)
        service_name: 특정 서비스만 조회 (선택)
        time_hours: 검색할 시간 범위 (시간 단위, 기본 24시간)

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

    # 인덱스 패턴
    index_pattern = f"{project_uuid}_*"

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
        # OpenSearch 검색
        results = await opensearch_client.search(
            index=index_pattern,
            body={
                "query": query,
                "size": limit,
                "sort": [{"timestamp": "desc"}],
                "_source": [
                    "message", "level", "service_name", "timestamp",
                    "log_id", "exception_type", "stack_trace"
                ]
            }
        )

        hits = results.get("hits", {}).get("hits", [])
        total_count = results.get("hits", {}).get("total", {}).get("value", 0)

        if total_count == 0:
            service_filter = f" (서비스: {service_name})" if service_name else ""
            return f"최근 {time_hours}시간 동안 ERROR 레벨 로그가 없습니다{service_filter}."

        # 에러 타입별 카운트
        error_types = {}
        for hit in hits:
            exc_type = hit["_source"].get("exception_type", "Unknown")
            error_types[exc_type] = error_types.get(exc_type, 0) + 1

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

        # 상위 에러 목록
        summary_lines.append("최근 에러 목록:")
        for i, hit in enumerate(hits, 1):
            source = hit["_source"]
            msg = source.get("message", "")[:150]
            timestamp_str = source.get("timestamp", "")[:19]
            service = source.get("service_name", "unknown")
            log_id = source.get("log_id", "")
            exc_type = source.get("exception_type", "Unknown")
            has_stack = bool(source.get("stack_trace"))

            summary_lines.append(f"{i}. [{exc_type}] {timestamp_str}")
            summary_lines.append(f"   서비스: {service}")
            summary_lines.append(f"   메시지: {msg}...")
            if has_stack:
                summary_lines.append(f"   (스택 트레이스 있음)")
            if log_id:
                summary_lines.append(f"   (log_id: {log_id})")
            summary_lines.append("")

        return "\n".join(summary_lines)

    except Exception as e:
        return f"에러 로그 조회 중 오류 발생: {str(e)}"
