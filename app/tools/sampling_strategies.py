"""
다양한 샘플링 전략 구현
- 단순 Vector KNN
- 층화 Vector KNN (레벨별 필터)
- 비례 층화 샘플링
- 랜덤 층화 샘플링
"""

import asyncio
from typing import Dict, Any, List
from datetime import datetime, timedelta

from app.core.opensearch import opensearch_client
from app.services.embedding_service import embedding_service


async def _get_level_distribution(
    project_uuid: str,
    time_hours: int = 24
) -> Dict[str, int]:
    """
    레벨별 개수만 빠르게 조회 (aggregation)

    Returns:
        {"ERROR": 50, "WARN": 150, "INFO": 800}
    """
    index_pattern = f"{project_uuid.replace('-', '_')}_*"
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)

    query_body = {
        "size": 0,  # 문서는 안 가져옴
        "query": {
            "range": {
                "timestamp": {
                    "gte": start_time.isoformat() + "Z",
                    "lte": end_time.isoformat() + "Z"
                }
            }
        },
        "aggs": {
            "by_level": {
                "terms": {"field": "level", "size": 10}
            }
        }
    }

    results = await asyncio.to_thread(
        opensearch_client.search,
        index=index_pattern,
        body=query_body
    )

    level_counts = {}
    for bucket in results.get("aggregations", {}).get("by_level", {}).get("buckets", []):
        level_counts[bucket["key"]] = bucket["doc_count"]

    return level_counts


# ============================================================================
# 전략 1: 단순 Vector KNN (기존 방식)
# ============================================================================

async def sample_simple_vector_knn(
    project_uuid: str,
    k: int,
    time_hours: int = 24
) -> List[Dict[str, Any]]:
    """
    단순 Vector KNN 검색 (기존 방식)

    - 의미 없는 쿼리로 벡터 생성
    - 유사도만으로 k개 선택
    - 레벨 분포 보장 없음
    """
    index_pattern = f"{project_uuid.replace('-', '_')}_*"
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)

    # 일반적인 쿼리로 벡터 생성
    query_vector = await embedding_service.embed_query("전체 로그 패턴 분석")

    query_body = {
        "size": min(k, 10000),
        "query": {
            "bool": {
                "must": [
                    {
                        "knn": {
                            "log_vector": {
                                "vector": query_vector,
                                "k": min(k, 10000)
                            }
                        }
                    }
                ],
                "filter": [
                    {
                        "range": {
                            "timestamp": {
                                "gte": start_time.isoformat() + "Z",
                                "lte": end_time.isoformat() + "Z"
                            }
                        }
                    }
                ]
            }
        },
        "_source": ["level", "message", "timestamp", "service_name", "log_id"]
    }

    results = await asyncio.to_thread(
        opensearch_client.search,
        index=index_pattern,
        body=query_body
    )

    samples = []
    for hit in results.get("hits", {}).get("hits", []):
        source = hit.get("_source", {})
        samples.append({
            "level": source.get("level", "UNKNOWN"),
            "message": source.get("message", ""),
            "timestamp": source.get("timestamp", ""),
            "service_name": source.get("service_name", "unknown"),
            "log_id": source.get("log_id")
        })

    return samples


# ============================================================================
# 전략 2: 층화 Vector KNN (레벨별 필터 + Vector)
# ============================================================================

async def sample_stratified_vector_knn(
    project_uuid: str,
    k_per_level: Dict[str, int],
    time_hours: int = 24
) -> List[Dict[str, Any]]:
    """
    레벨별 층화 Vector KNN 검색

    - 각 레벨에 대해 별도로 Vector 검색
    - level 필터로 정확한 분포 보장
    - 레벨별로 의미 있는 쿼리 벡터 사용

    Args:
        k_per_level: {"ERROR": 10, "WARN": 30, "INFO": 160}
    """
    index_pattern = f"{project_uuid.replace('-', '_')}_*"
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)

    all_samples = []

    # 레벨별로 별도 검색
    for level, k in k_per_level.items():
        if k == 0:
            continue

        # 각 레벨에 맞는 쿼리 벡터 생성
        query_text = f"{level} level log analysis"
        query_vector = await embedding_service.embed_query(query_text)

        # Vector KNN + level 필터
        query_body = {
            "size": k,
            "query": {
                "bool": {
                    "must": [
                        {
                            "knn": {
                                "log_vector": {
                                    "vector": query_vector,
                                    "k": k
                                }
                            }
                        }
                    ],
                    "filter": [
                        {"term": {"level": level}},  # 레벨 필터!
                        {
                            "range": {
                                "timestamp": {
                                    "gte": start_time.isoformat() + "Z",
                                    "lte": end_time.isoformat() + "Z"
                                }
                            }
                        }
                    ]
                }
            },
            "_source": ["level", "message", "timestamp", "service_name", "log_id"]
        }

        results = await asyncio.to_thread(
            opensearch_client.search,
            index=index_pattern,
            body=query_body
        )

        for hit in results.get("hits", {}).get("hits", []):
            source = hit.get("_source", {})
            all_samples.append({
                "level": source.get("level", "UNKNOWN"),
                "message": source.get("message", ""),
                "timestamp": source.get("timestamp", ""),
                "service_name": source.get("service_name", "unknown"),
                "log_id": source.get("log_id")
            })

    return all_samples


# ============================================================================
# 전략 3: 비례 층화 Vector KNN (자동 분배)
# ============================================================================

async def sample_proportional_vector_knn(
    project_uuid: str,
    total_k: int,
    time_hours: int = 24,
    min_per_level: Dict[str, int] = None
) -> List[Dict[str, Any]]:
    """
    비례 층화 Vector KNN 검색

    1. 레벨 분포 조회 (aggregation)
    2. 비율대로 k 분배
    3. 레벨별 Vector 검색

    Args:
        total_k: 총 샘플 수 (예: 200)
        min_per_level: 레벨별 최소 보장 (예: {"ERROR": 5, "WARN": 10})
    """
    if min_per_level is None:
        min_per_level = {"ERROR": 5, "WARN": 10, "INFO": 0}

    # 1. 레벨 분포 조회
    level_counts = await _get_level_distribution(project_uuid, time_hours)

    if not level_counts:
        return []

    # 2. 비율 계산
    total = sum(level_counts.values())
    k_per_level = {}

    for level in ["ERROR", "WARN", "INFO"]:
        count = level_counts.get(level, 0)
        if count == 0:
            k_per_level[level] = 0
        else:
            ratio = count / total
            proportional_k = int(total_k * ratio)
            min_k = min_per_level.get(level, 0)
            k_per_level[level] = max(min_k, proportional_k)

    # 재조정 (total_k 초과 시)
    total_allocated = sum(k_per_level.values())
    if total_allocated > total_k:
        # INFO에서 줄임
        excess = total_allocated - total_k
        k_per_level["INFO"] = max(0, k_per_level["INFO"] - excess)

    # 3. 레벨별 Vector 검색
    return await sample_stratified_vector_knn(project_uuid, k_per_level, time_hours)


# ============================================================================
# 전략 4: 랜덤 층화 샘플링 (Vector 없음)
# ============================================================================

async def sample_random_stratified(
    project_uuid: str,
    k_per_level: Dict[str, int],
    time_hours: int = 24
) -> List[Dict[str, Any]]:
    """
    레벨별 랜덤 샘플링 (Vector 불필요)

    - Vector 검색 없이 random_score 사용
    - 공정한 랜덤 샘플링
    - 레벨 분포 보장
    """
    index_pattern = f"{project_uuid.replace('-', '_')}_*"
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)

    all_samples = []

    for level, k in k_per_level.items():
        if k == 0:
            continue

        query_body = {
            "size": k,
            "query": {
                "function_score": {
                    "query": {
                        "bool": {
                            "filter": [
                                {"term": {"level": level}},
                                {
                                    "range": {
                                        "timestamp": {
                                            "gte": start_time.isoformat() + "Z",
                                            "lte": end_time.isoformat() + "Z"
                                        }
                                    }
                                }
                            ]
                        }
                    },
                    "random_score": {"seed": 42, "field": "_seq_no"}
                }
            },
            "_source": ["level", "message", "timestamp", "service_name", "log_id"]
        }

        results = await asyncio.to_thread(
            opensearch_client.search,
            index=index_pattern,
            body=query_body
        )

        for hit in results.get("hits", {}).get("hits", []):
            source = hit.get("_source", {})
            all_samples.append({
                "level": source.get("level", "UNKNOWN"),
                "message": source.get("message", ""),
                "timestamp": source.get("timestamp", ""),
                "service_name": source.get("service_name", "unknown"),
                "log_id": source.get("log_id")
            })

    return all_samples


# ============================================================================
# 전략 5: 비례 랜덤 층화 샘플링 (자동 분배)
# ============================================================================

async def sample_proportional_random(
    project_uuid: str,
    total_k: int,
    time_hours: int = 24,
    min_per_level: Dict[str, int] = None
) -> List[Dict[str, Any]]:
    """
    비례 랜덤 층화 샘플링

    1. 레벨 분포 조회
    2. 비율대로 k 분배
    3. 레벨별 랜덤 샘플링
    """
    if min_per_level is None:
        min_per_level = {"ERROR": 5, "WARN": 10, "INFO": 0}

    # 1. 레벨 분포 조회
    level_counts = await _get_level_distribution(project_uuid, time_hours)

    if not level_counts:
        return []

    # 2. 비율 계산
    total = sum(level_counts.values())
    k_per_level = {}

    for level in ["ERROR", "WARN", "INFO"]:
        count = level_counts.get(level, 0)
        if count == 0:
            k_per_level[level] = 0
        else:
            ratio = count / total
            proportional_k = int(total_k * ratio)
            min_k = min_per_level.get(level, 0)
            k_per_level[level] = max(min_k, proportional_k)

    # 재조정
    total_allocated = sum(k_per_level.values())
    if total_allocated > total_k:
        excess = total_allocated - total_k
        k_per_level["INFO"] = max(0, k_per_level["INFO"] - excess)

    # 3. 레벨별 랜덤 샘플링
    return await sample_random_stratified(project_uuid, k_per_level, time_hours)


# ============================================================================
# 전략 6: 2단계 희소 이벤트 인식 샘플링 (IPW 가중치 포함)
# ============================================================================

async def sample_two_stage_rare_aware(
    project_uuid: str,
    total_k: int,
    time_hours: int = 24,
    rare_threshold: int = 100
) -> tuple[List[Dict[str, Any]], Dict[str, Any]]:
    """
    희소 이벤트 인식 2단계 샘플링 + IPW 가중치 메타데이터

    목적:
    - 희소 이벤트(ERROR/WARN)의 과대표현 문제 해결
    - Inverse Probability Weighting으로 정확한 추론 지원

    알고리즘:
    1. 실제 레벨 분포 조회 (aggregation)
    2. 희소/정상 이벤트 분류 (rare_threshold 기준)
    3. 희소 이벤트: 50% 샘플링 또는 전수조사 (census)
    4. 정상 이벤트: 비례 샘플링
    5. IPW 가중치 계산: weight = actual_count / sample_count

    Args:
        project_uuid: 프로젝트 UUID
        total_k: 목표 샘플 크기
        time_hours: 분석 기간 (시간)
        rare_threshold: 희소 이벤트 임계값 (기본: 100)

    Returns:
        (samples, metadata)
        samples: 샘플 로그 리스트
        metadata: {
            "weights": {"ERROR": 2.5, "WARN": 1.8, "INFO": 1.2},
            "level_counts": {"ERROR": 50, "WARN": 90, "INFO": 10000},
            "sample_counts": {"ERROR": 20, "WARN": 50, "INFO": 8333},
            "sampling_strategy": "two_stage_rare_aware",
            "rare_threshold": 100,
            "rare_levels": ["ERROR"]
        }

    Example:
        # 실제: ERROR 50개, WARN 200개, INFO 100,000개
        # 샘플 100개 요청 시:
        # - ERROR: 50개 (전수) → weight = 50/50 = 1.0
        # - WARN: 100개 (50% 샘플링) → weight = 200/100 = 2.0
        # - INFO: 비례 (0.1%) → weight = 100,000/100 = 1000.0
    """
    # 1. 실제 레벨 분포 조회
    level_counts = await _get_level_distribution(project_uuid, time_hours)

    if not level_counts:
        return [], {
            "weights": {},
            "level_counts": {},
            "sample_counts": {},
            "sampling_strategy": "two_stage_rare_aware",
            "rare_threshold": rare_threshold,
            "rare_levels": [],
            "error": "No logs found"
        }

    total_logs = sum(level_counts.values())

    # 2. 희소/정상 이벤트 분류
    rare_levels = []
    k_per_level = {}

    for level in ["ERROR", "WARN", "INFO"]:
        actual_count = level_counts.get(level, 0)

        if actual_count == 0:
            k_per_level[level] = 0
            continue

        # 희소 이벤트 판정 (count < rare_threshold)
        if actual_count < rare_threshold:
            rare_levels.append(level)
            # 희소: 50% 샘플링 또는 전수 (census)
            # 최소 10개 보장, 최대 actual_count
            k_level = min(actual_count, max(10, int(actual_count * 0.5)))
        else:
            # 정상: 비례 샘플링
            ratio = actual_count / total_logs
            k_level = int(total_k * ratio)
            # 최소 5개 보장 (있다면)
            k_level = max(5, k_level)

        # 실제 개수를 초과할 수 없음
        k_per_level[level] = min(k_level, actual_count)

    # 3. 총 샘플 수 조정 (total_k 초과 시)
    total_allocated = sum(k_per_level.values())
    if total_allocated > total_k:
        # INFO에서 우선 조정
        excess = total_allocated - total_k
        info_k = k_per_level.get("INFO", 0)
        k_per_level["INFO"] = max(0, info_k - excess)

    # 4. Vector KNN 샘플링 실행
    samples = await sample_stratified_vector_knn(project_uuid, k_per_level, time_hours)

    # 5. 실제 샘플 개수 확인 (Vector KNN이 부족할 수 있음)
    actual_sample_counts = {"ERROR": 0, "WARN": 0, "INFO": 0}
    for sample in samples:
        level = sample.get("level", "INFO")
        actual_sample_counts[level] = actual_sample_counts.get(level, 0) + 1

    # 6. IPW 가중치 계산
    weights = {}
    for level in ["ERROR", "WARN", "INFO"]:
        actual = level_counts.get(level, 0)
        sampled = actual_sample_counts.get(level, 0)

        if sampled > 0:
            weights[level] = round(actual / sampled, 2)
        else:
            weights[level] = 0.0

    # 7. 메타데이터 구성
    metadata = {
        "weights": weights,
        "level_counts": level_counts,
        "sample_counts": actual_sample_counts,
        "sampling_strategy": "two_stage_rare_aware",
        "rare_threshold": rare_threshold,
        "rare_levels": rare_levels,
        "total_logs": total_logs,
        "total_samples": len(samples)
    }

    return samples, metadata
