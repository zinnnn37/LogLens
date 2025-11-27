"""
Vector 클러스터링 기반 패턴 자동 발견 도구

복잡한 GROUP BY 쿼리를 Vector 클러스터링으로 대체하여 자동으로 패턴 발견
"""

from typing import Dict, Any, List
from datetime import datetime, timedelta
from langchain_core.tools import tool
import numpy as np

from app.core.opensearch import opensearch_client


@tool
async def discover_error_patterns(
    project_uuid: str,
    time_range: str = "24h",
    min_cluster_size: int = 5,
    max_clusters: int = 10
) -> str:
    """
    Vector 클러스터링으로 ERROR 패턴 자동 발견

    전통적 방식:
    SELECT
      SUBSTRING(message, 1, 50) as pattern,
      COUNT(*) as count,
      MIN(timestamp) as first_seen
    FROM logs
    WHERE level = 'ERROR'
    GROUP BY pattern
    HAVING count > 10

    Vector 방식:
    자동으로 유사 에러 클러스터링

    Args:
        project_uuid: 프로젝트 UUID
        time_range: 분석 시간 범위 (기본: 24h)
        min_cluster_size: 최소 클러스터 크기 (기본: 5)
        max_clusters: 최대 클러스터 개수 (기본: 10)

    Returns:
        발견된 에러 패턴 요약
    """
    # ERROR 로그 가져오기
    error_logs = await _get_error_logs_with_vectors(project_uuid, time_range)

    if not error_logs:
        return f"❌ {time_range} 동안 벡터화된 ERROR 로그가 없습니다."

    if len(error_logs) < min_cluster_size:
        return f"⚠️ ERROR 로그가 너무 적습니다 ({len(error_logs)}개). 최소 {min_cluster_size}개 필요."

    # Vector 클러스터링
    clusters = _cluster_error_logs(error_logs, min_cluster_size, max_clusters)

    if not clusters:
        return "❌ 유의미한 패턴을 발견하지 못했습니다."

    # 결과 포맷팅
    return _format_pattern_results(clusters, len(error_logs))


@tool
async def analyze_error_trends(
    project_uuid: str,
    time_range: str = "7d",
    interval: str = "1d"
) -> str:
    """
    시간대별 에러 패턴 트렌드 분석

    전통적 방식:
    SELECT
      DATE_TRUNC('day', timestamp) as date,
      CASE
        WHEN message LIKE '%timeout%' THEN 'timeout'
        WHEN message LIKE '%null%' THEN 'null_error'
        ELSE 'other'
      END as error_type,
      COUNT(*) as count
    FROM logs
    WHERE level = 'ERROR'
    GROUP BY date, error_type

    Vector 방식:
    클러스터링으로 패턴을 자동 식별하고 시간대별 추이 분석

    Args:
        project_uuid: 프로젝트 UUID
        time_range: 분석 기간 (기본: 7d)
        interval: 집계 간격 (기본: 1d)

    Returns:
        시간대별 에러 패턴 트렌드
    """
    # 시간대별 ERROR 로그 가져오기
    time_buckets = _parse_interval(interval)
    all_patterns = {}

    for bucket in time_buckets:
        bucket_logs = await _get_error_logs_with_vectors(
            project_uuid,
            time_range=f"{bucket['hours']}h",
            start_time=bucket['start']
        )

        if bucket_logs:
            patterns = _cluster_error_logs(bucket_logs, min_cluster_size=3)
            all_patterns[bucket['label']] = patterns

    return _format_trend_results(all_patterns)


async def _get_error_logs_with_vectors(
    project_uuid: str,
    time_range: str,
    start_time: datetime = None
) -> List[Dict[str, Any]]:
    """
    log_vector가 있는 ERROR 로그 조회

    Args:
        project_uuid: 프로젝트 UUID
        time_range: 시간 범위 (예: "24h")
        start_time: 시작 시간 (선택적)

    Returns:
        ERROR 로그 리스트 (log_vector 포함)
    """
    index_pattern = f"{project_uuid.replace('-', '_')}_*"

    # 시간 범위 파싱
    if not start_time:
        unit = time_range[-1]
        value = int(time_range[:-1])

        if unit == 'h':
            delta = timedelta(hours=value)
        elif unit == 'd':
            delta = timedelta(days=value)
        else:
            delta = timedelta(hours=24)

        start_time = datetime.utcnow() - delta

    query_body = {
        "size": 1000,  # 최대 1000개
        "query": {
            "bool": {
                "must": [
                    {"term": {"level": "ERROR"}},
                    {"exists": {"field": "log_vector"}},
                    {
                        "range": {
                            "timestamp": {
                                "gte": start_time.isoformat() + "Z"
                            }
                        }
                    }
                ]
            }
        },
        "_source": ["log_id", "message", "timestamp", "service_name", "log_vector"]
    }

    try:
        results = opensearch_client.search(index=index_pattern, body=query_body)
        hits = results.get("hits", {}).get("hits", [])

        logs = []
        for hit in hits:
            source = hit["_source"]
            if "log_vector" in source:
                logs.append(source)

        return logs

    except Exception as e:
        print(f"Error querying ERROR logs: {e}")
        return []


def _cluster_error_logs(
    logs: List[Dict[str, Any]],
    min_cluster_size: int = 5,
    max_clusters: int = 10
) -> List[Dict[str, Any]]:
    """
    DBSCAN을 사용하여 ERROR 로그 클러스터링

    Args:
        logs: ERROR 로그 리스트 (log_vector 포함)
        min_cluster_size: 최소 클러스터 크기
        max_clusters: 최대 클러스터 개수

    Returns:
        클러스터 정보 리스트
    """
    try:
        from sklearn.cluster import DBSCAN
        from sklearn.metrics.pairwise import cosine_distances
    except ImportError:
        return [{"error": "sklearn이 설치되지 않았습니다. pip install scikit-learn"}]

    # Vector 추출
    vectors = np.array([log["log_vector"] for log in logs])

    # DBSCAN 클러스터링 (코사인 거리 사용)
    # eps=0.15: 15% 이내의 거리를 같은 클러스터로 간주
    clustering = DBSCAN(
        eps=0.15,
        min_samples=min_cluster_size,
        metric='cosine'
    )
    labels = clustering.fit_predict(vectors)

    # 클러스터별 로그 그룹화
    clusters = {}
    for i, label in enumerate(labels):
        if label == -1:  # 노이즈 제외
            continue

        if label not in clusters:
            clusters[label] = []

        clusters[label].append(logs[i])

    # 클러스터 정보 생성
    cluster_info = []
    for label, cluster_logs in sorted(clusters.items(), key=lambda x: len(x[1]), reverse=True)[:max_clusters]:
        # 대표 로그 선택 (첫 번째 로그)
        representative = cluster_logs[0]

        # 첫 발생 시간
        timestamps = [log.get("timestamp", "") for log in cluster_logs]
        first_seen = min(timestamps) if timestamps else "Unknown"

        # 영향받은 서비스
        services = list(set(log.get("service_name", "unknown") for log in cluster_logs))

        cluster_info.append({
            "pattern_id": f"pattern_{label}",
            "count": len(cluster_logs),
            "representative_message": representative.get("message", "")[:150],
            "first_seen": first_seen,
            "affected_services": services[:5],  # 최대 5개
            "example_log_ids": [log.get("log_id") for log in cluster_logs[:3]]
        })

    return cluster_info


def _format_pattern_results(clusters: List[Dict[str, Any]], total_errors: int) -> str:
    """
    패턴 발견 결과를 마크다운으로 포맷팅

    Args:
        clusters: 클러스터 정보 리스트
        total_errors: 총 ERROR 로그 수

    Returns:
        마크다운 형식 문자열
    """
    result = f"""🔍 ERROR 패턴 자동 발견 결과

📊 총 ERROR 로그: {total_errors}개
🎯 발견된 주요 패턴: {len(clusters)}개
📈 클러스터링 커버리지: {sum(c['count'] for c in clusters) / total_errors * 100:.1f}%

---

"""

    for i, cluster in enumerate(clusters, 1):
        result += f"""### 패턴 #{i}: {cluster['pattern_id']}

- **발생 횟수**: {cluster['count']}회
- **첫 발생**: {cluster['first_seen'][:19] if cluster['first_seen'] != 'Unknown' else 'Unknown'}
- **영향 서비스**: {', '.join(cluster['affected_services'])}
- **대표 메시지**: `{cluster['representative_message']}`
- **예시 로그 ID**: {', '.join(cluster['example_log_ids'])}

"""

    result += """---

✅ Vector 클러스터링으로 복잡한 GROUP BY 없이 자동으로 패턴을 발견했습니다.
"""

    return result


def _format_trend_results(all_patterns: Dict[str, List[Dict[str, Any]]]) -> str:
    """
    트렌드 분석 결과 포맷팅

    Args:
        all_patterns: 시간대별 패턴 딕셔너리

    Returns:
        마크다운 형식 문자열
    """
    result = "📈 시간대별 ERROR 패턴 트렌드\n\n"

    for time_label, patterns in all_patterns.items():
        result += f"**{time_label}**\n"
        if patterns:
            for pattern in patterns[:3]:  # 상위 3개만
                result += f"  - {pattern['pattern_id']}: {pattern['count']}회\n"
        else:
            result += "  - 패턴 없음\n"
        result += "\n"

    return result


def _parse_interval(interval: str) -> List[Dict[str, Any]]:
    """
    간격 문자열을 시간 버킷으로 변환

    Args:
        interval: 간격 (예: "1d", "6h")

    Returns:
        시간 버킷 리스트
    """
    # 간단한 구현: 1일 간격 기본
    return [
        {"label": "오늘", "hours": 24, "start": datetime.utcnow() - timedelta(days=1)},
        {"label": "어제", "hours": 24, "start": datetime.utcnow() - timedelta(days=2)},
        {"label": "그저께", "hours": 24, "start": datetime.utcnow() - timedelta(days=3)}
    ]
