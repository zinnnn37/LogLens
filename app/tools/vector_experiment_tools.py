"""
Vector AI 실험 도구
- Vector 검색으로 통계 추론 실험
"""

import json
import asyncio
from typing import Dict, Any, List
from datetime import datetime, timedelta
from langchain_openai import ChatOpenAI

from app.core.opensearch import opensearch_client
from app.core.config import settings
from app.services.embedding_service import embedding_service


def _get_db_statistics_for_experiment(
    project_uuid: str,
    time_hours: int = 24
) -> Dict[str, Any]:
    """
    OpenSearch 집계로 정확한 통계 조회 (Ground Truth)

    Returns:
        {
            "total_logs": int,
            "error_count": int,
            "warn_count": int,
            "info_count": int,
            "error_rate": float
        }
    """
    index_pattern = f"{project_uuid.replace('-', '_')}_*"
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)

    query_body = {
        "track_total_hits": True,
        "size": 0,
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

    results = opensearch_client.search(index=index_pattern, body=query_body)

    # 통계 추출
    total_logs = results.get("hits", {}).get("total", {}).get("value", 0)
    level_buckets = results.get("aggregations", {}).get("by_level", {}).get("buckets", [])

    level_stats = {}
    for bucket in level_buckets:
        level_stats[bucket["key"]] = bucket["doc_count"]

    error_count = level_stats.get("ERROR", 0)
    warn_count = level_stats.get("WARN", 0)
    info_count = level_stats.get("INFO", 0)
    error_rate = (error_count / total_logs * 100) if total_logs > 0 else 0

    return {
        "total_logs": total_logs,
        "error_count": error_count,
        "warn_count": warn_count,
        "info_count": info_count,
        "error_rate": round(error_rate, 2)
    }


async def _vector_search_all(
    project_uuid: str,
    k: int,
    time_hours: int = 24
) -> List[Dict[str, Any]]:
    """
    Vector 검색으로 최대한 많은 로그 수집

    Args:
        project_uuid: 프로젝트 UUID
        k: 수집할 샘플 수
        time_hours: 시간 범위

    Returns:
        샘플 로그 리스트
    """
    index_pattern = f"{project_uuid.replace('-', '_')}_*"
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)

    # 일반적인 로그 패턴으로 벡터 생성
    query_vector = await embedding_service.embed_query("전체 로그 패턴 분석")

    # KNN 검색
    query_body = {
        "size": min(k, 10000),  # OpenSearch 최대 제한
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


async def _llm_estimate_from_vectors(
    samples: List[Dict[str, Any]],
    total_logs_hint: int,
    time_hours: int
) -> Dict[str, Any]:
    """
    LLM이 Vector 샘플을 보고 전체 통계 추론

    Args:
        samples: Vector 검색으로 수집한 샘플
        total_logs_hint: 실제 총 로그 수 (힌트로 제공)
        time_hours: 분석 기간

    Returns:
        {
            "estimated_total_logs": int,
            "estimated_error_count": int,
            "estimated_warn_count": int,
            "estimated_info_count": int,
            "estimated_error_rate": float,
            "confidence_score": int,
            "reasoning": str
        }
    """
    # 샘플 분석
    sample_level_counts = {}
    for sample in samples:
        level = sample.get("level", "UNKNOWN")
        sample_level_counts[level] = sample_level_counts.get(level, 0) + 1

    sample_size = len(samples)
    sample_summary = {
        "size": sample_size,
        "ERROR": sample_level_counts.get("ERROR", 0),
        "WARN": sample_level_counts.get("WARN", 0),
        "INFO": sample_level_counts.get("INFO", 0)
    }

    # LLM 프롬프트
    llm = ChatOpenAI(
        model=settings.LLM_MODEL,
        temperature=0.1,
        openai_api_key=settings.OPENAI_API_KEY,
        base_url=settings.OPENAI_BASE_URL
    )

    prompt = f"""당신은 데이터 분석 전문가입니다. Vector 검색으로 수집한 로그 샘플을 보고 전체 통계를 추론해야 합니다.

## 힌트 정보
- 실제 총 로그 수: {total_logs_hint:,}개
- 분석 기간: 최근 {time_hours}시간

## Vector 검색 샘플
- 샘플 크기: {sample_size}개
- ERROR: {sample_summary['ERROR']}개
- WARN: {sample_summary['WARN']}개
- INFO: {sample_summary['INFO']}개

## 작업
총 로그 수({total_logs_hint:,}개)는 이미 알려져 있습니다.
샘플의 레벨별 비율을 전체에 적용하여 각 레벨별 개수를 추론하세요.

비율 계산:
- ERROR 비율: {sample_summary['ERROR']}/{sample_size} = {sample_summary['ERROR']/sample_size if sample_size > 0 else 0:.3f}
- WARN 비율: {sample_summary['WARN']}/{sample_size} = {sample_summary['WARN']/sample_size if sample_size > 0 else 0:.3f}
- INFO 비율: {sample_summary['INFO']}/{sample_size} = {sample_summary['INFO']/sample_size if sample_size > 0 else 0:.3f}

반드시 아래 JSON 형식으로만 응답하세요:
{{
    "estimated_total_logs": {total_logs_hint},
    "estimated_error_count": <추론한 ERROR 로그 수>,
    "estimated_warn_count": <추론한 WARN 로그 수>,
    "estimated_info_count": <추론한 INFO 로그 수>,
    "estimated_error_rate": <추론한 에러율 (%)>,
    "confidence_score": <추론 신뢰도 0-100>,
    "reasoning": "<추론 근거 1-2문장>"
}}

중요:
- 총 로그 수는 {total_logs_hint}으로 고정
- ERROR + WARN + INFO = {total_logs_hint}이 되어야 함
- 샘플 비율을 전체에 적용하여 계산
"""

    try:
        response = await llm.ainvoke(prompt)
        content = response.content.strip()

        # JSON 파싱
        if "```json" in content:
            content = content.split("```json")[1].split("```")[0].strip()
        elif "```" in content:
            content = content.split("```")[1].split("```")[0].strip()

        result = json.loads(content)
        result["estimated_total_logs"] = total_logs_hint  # 강제 설정
        return result

    except Exception as e:
        # 폴백: 단순 비율 계산
        error_ratio = sample_level_counts.get("ERROR", 0) / sample_size if sample_size > 0 else 0
        warn_ratio = sample_level_counts.get("WARN", 0) / sample_size if sample_size > 0 else 0
        info_ratio = sample_level_counts.get("INFO", 0) / sample_size if sample_size > 0 else 0

        return {
            "estimated_total_logs": total_logs_hint,
            "estimated_error_count": int(total_logs_hint * error_ratio),
            "estimated_warn_count": int(total_logs_hint * warn_ratio),
            "estimated_info_count": int(total_logs_hint * info_ratio),
            "estimated_error_rate": round(error_ratio * 100, 2),
            "confidence_score": 70,
            "reasoning": f"샘플 비율 직접 적용 (LLM 응답 파싱 실패: {str(e)})"
        }


def _calculate_accuracy_for_experiment(
    db_stats: Dict[str, Any],
    ai_stats: Dict[str, Any]
) -> Dict[str, Any]:
    """
    DB 통계와 AI 추론 통계의 정확도 계산

    Returns:
        {
            "total_logs_accuracy": float,
            "error_count_accuracy": float,
            "warn_count_accuracy": float,
            "info_count_accuracy": float,
            "error_rate_accuracy": float,
            "overall_accuracy": float
        }
    """
    def accuracy_percentage(actual, predicted):
        if actual == 0:
            return 100.0 if predicted == 0 else 0.0
        error = abs(actual - predicted)
        return max(0, round((1 - error / actual) * 100, 2))

    total_accuracy = accuracy_percentage(
        db_stats["total_logs"],
        ai_stats.get("estimated_total_logs", 0)
    )
    error_count_accuracy = accuracy_percentage(
        db_stats["error_count"],
        ai_stats.get("estimated_error_count", 0)
    )
    warn_count_accuracy = accuracy_percentage(
        db_stats["warn_count"],
        ai_stats.get("estimated_warn_count", 0)
    )
    info_count_accuracy = accuracy_percentage(
        db_stats["info_count"],
        ai_stats.get("estimated_info_count", 0)
    )

    # 에러율 정확도 (절대 오차 기반)
    error_rate_diff = abs(db_stats["error_rate"] - ai_stats.get("estimated_error_rate", 0))
    error_rate_accuracy = max(0, round(100 - error_rate_diff * 10, 2))

    # 종합 정확도 (가중 평균)
    overall_accuracy = round(
        total_accuracy * 0.3 +
        error_count_accuracy * 0.3 +
        error_rate_accuracy * 0.2 +
        warn_count_accuracy * 0.1 +
        info_count_accuracy * 0.1,
        2
    )

    return {
        "total_logs_accuracy": total_accuracy,
        "error_count_accuracy": error_count_accuracy,
        "warn_count_accuracy": warn_count_accuracy,
        "info_count_accuracy": info_count_accuracy,
        "error_rate_accuracy": error_rate_accuracy,
        "overall_accuracy": overall_accuracy
    }
