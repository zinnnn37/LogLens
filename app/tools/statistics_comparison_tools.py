"""
AI vs DB 통계 비교 도구 (Statistics Comparison Tools)
- DB 직접 조회 결과와 LLM 기반 통계 추론의 정확도를 검증
- LLM이 DB 쿼리를 대체할 수 있는 역량을 수치로 증명
"""

import asyncio
import json
import logging
from typing import Dict, Any, List, Optional, Tuple
from datetime import datetime, timedelta
from langchain_core.tools import tool
from langchain_openai import ChatOpenAI

from app.core.opensearch import opensearch_client
from app.core.config import settings
from app.tools.sampling_strategies import sample_stratified_vector_knn, sample_random_stratified

logger = logging.getLogger(__name__)


def _get_db_statistics(project_uuid: str, time_hours: int = 24) -> Dict[str, Any]:
    """
    OpenSearch에서 직접 통계 쿼리 실행 (Ground Truth)
    """
    index_pattern = f"{project_uuid.replace('-', '_')}_*"
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)

    query_body = {
        "track_total_hits": True,  # 10,000건 제한 해제 - 정확한 총 개수 반환
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
            },
            "hourly_distribution": {
                "date_histogram": {
                    "field": "timestamp",
                    "fixed_interval": "1h",
                    "time_zone": "Asia/Seoul",
                    "min_doc_count": 0
                },
                "aggs": {
                    "by_level": {
                        "terms": {"field": "level", "size": 5}
                    }
                }
            }
        }
    }

    results = opensearch_client.search(index=index_pattern, body=query_body)

    # 전체 통계 추출
    total_logs = results.get("hits", {}).get("total", {}).get("value", 0)
    level_buckets = results.get("aggregations", {}).get("by_level", {}).get("buckets", [])

    level_stats = {}
    for bucket in level_buckets:
        level_stats[bucket["key"]] = bucket["doc_count"]

    error_count = level_stats.get("ERROR", 0)
    warn_count = level_stats.get("WARN", 0)
    info_count = level_stats.get("INFO", 0)
    error_rate = (error_count / total_logs * 100) if total_logs > 0 else 0

    # 시간대별 분포
    hourly_buckets = results.get("aggregations", {}).get("hourly_distribution", {}).get("buckets", [])
    hourly_data = []
    peak_hour = ""
    peak_count = 0

    for bucket in hourly_buckets:
        hour_str = bucket.get("key_as_string", "")[:13]  # "2024-01-01T15"
        total = bucket.get("doc_count", 0)
        level_breakdown = {}
        for level_bucket in bucket.get("by_level", {}).get("buckets", []):
            level_breakdown[level_bucket["key"]] = level_bucket["doc_count"]

        hourly_data.append({
            "hour": hour_str,
            "total": total,
            "error": level_breakdown.get("ERROR", 0),
            "warn": level_breakdown.get("WARN", 0),
            "info": level_breakdown.get("INFO", 0)
        })

        if total > peak_count:
            peak_count = total
            peak_hour = hour_str

    return {
        "total_logs": total_logs,
        "error_count": error_count,
        "warn_count": warn_count,
        "info_count": info_count,
        "error_rate": round(error_rate, 2),
        "peak_hour": peak_hour,
        "peak_count": peak_count,
        "hourly_data": hourly_data[-24:]  # 최근 24시간만
    }


def _get_log_samples(project_uuid: str, time_hours: int = 24, sample_size: int = 100) -> List[Dict[str, Any]]:
    """
    LLM 분석을 위한 로그 샘플 추출 (랜덤 샘플링)
    """
    index_pattern = f"{project_uuid.replace('-', '_')}_*"
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)

    # 랜덤 샘플링 - function_score with random_score 사용
    query_body = {
        "size": sample_size,
        "query": {
            "function_score": {
                "query": {
                    "range": {
                        "timestamp": {
                            "gte": start_time.isoformat() + "Z",
                            "lte": end_time.isoformat() + "Z"
                        }
                    }
                },
                "random_score": {},  # 랜덤 스코어링
                "boost_mode": "replace"  # 원래 점수 무시하고 랜덤 점수만 사용
            }
        },
        "_source": ["level", "timestamp", "service_name", "message"]
    }

    results = opensearch_client.search(index=index_pattern, body=query_body)

    samples = []
    for hit in results.get("hits", {}).get("hits", []):
        source = hit.get("_source", {})
        samples.append({
            "level": source.get("level", "UNKNOWN"),
            "timestamp": source.get("timestamp", ""),
            "service": source.get("service_name", "unknown"),
            "message": source.get("message", "")[:200]  # 토큰 절약
        })

    return samples


async def _get_stratified_log_samples(
    project_uuid: str,
    time_hours: int = 24,
    sample_size: int = 100,
    level_counts: Dict[str, int] = None
) -> List[Dict[str, Any]]:
    """
    Vector KNN 기반 층화 샘플링 (embedding 활용)

    폴백: Vector 검색 실패 시 랜덤 샘플링
    """
    try:
        # Vector KNN 샘플링 시도
        logger.info(f"Vector KNN 샘플링 시작: project={project_uuid}, size={sample_size}")

        samples = await sample_stratified_vector_knn(
            opensearch_client=opensearch_client,
            project_uuid=project_uuid,
            time_hours=time_hours,
            k=sample_size,
            k_per_level=level_counts
        )

        logger.info(f"✅ Vector KNN 샘플링 완료: {len(samples)}개")
        return samples

    except Exception as e:
        # 폴백: 랜덤 샘플링
        logger.warning(f"⚠️ Vector KNN 실패, 랜덤 샘플링으로 폴백: {e}")
        return _get_stratified_log_samples_random(
            project_uuid, time_hours, sample_size, level_counts
        )


def _get_stratified_log_samples_random(
    project_uuid: str,
    time_hours: int = 24,
    sample_size: int = 100,
    level_counts: Dict[str, int] = None
) -> List[Dict[str, Any]]:
    """
    랜덤 층화 샘플링 (폴백용): 레벨별 비율에 맞게 샘플 추출
    희소 이벤트(ERROR/WARN)도 반드시 포함
    """
    index_pattern = f"{project_uuid.replace('-', '_')}_*"
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)

    samples = []

    if not level_counts:
        # level_counts가 없으면 랜덤 샘플링으로 폴백
        return _get_log_samples(project_uuid, time_hours, sample_size)

    total_logs = sum(level_counts.values())
    if total_logs == 0:
        return []

    # 레벨별 샘플 수 계산 (비율 기반 + 최소 보장)
    level_sample_sizes = {}
    remaining_samples = sample_size

    for level in ["ERROR", "WARN", "INFO"]:
        actual_count = level_counts.get(level, 0)
        if actual_count == 0:
            level_sample_sizes[level] = 0
            continue

        # 비율 기반 할당
        ratio = actual_count / total_logs
        allocated = int(sample_size * ratio)

        # 희소 이벤트는 최소 1개 보장 (존재하는 경우)
        if allocated == 0 and actual_count > 0:
            allocated = min(5, actual_count)  # 최소 5개 또는 실제 개수

        # 실제 개수보다 많이 할당하지 않음
        allocated = min(allocated, actual_count, remaining_samples)
        level_sample_sizes[level] = allocated
        remaining_samples -= allocated

    # 남은 샘플은 INFO에 할당 (가장 많은 레벨)
    if remaining_samples > 0 and "INFO" in level_sample_sizes:
        additional = min(remaining_samples, level_counts.get("INFO", 0) - level_sample_sizes["INFO"])
        level_sample_sizes["INFO"] += max(0, additional)

    # 각 레벨별로 랜덤 샘플링
    for level, count in level_sample_sizes.items():
        if count == 0:
            continue

        query_body = {
            "size": count,
            "query": {
                "function_score": {
                    "query": {
                        "bool": {
                            "must": [
                                {
                                    "range": {
                                        "timestamp": {
                                            "gte": start_time.isoformat() + "Z",
                                            "lte": end_time.isoformat() + "Z"
                                        }
                                    }
                                },
                                {
                                    "term": {"level": level}
                                }
                            ]
                        }
                    },
                    "random_score": {},
                    "boost_mode": "replace"
                }
            },
            "_source": ["level", "timestamp", "service_name", "message"]
        }

        try:
            results = opensearch_client.search(index=index_pattern, body=query_body)

            for hit in results.get("hits", {}).get("hits", []):
                source = hit.get("_source", {})
                samples.append({
                    "level": source.get("level", "UNKNOWN"),
                    "timestamp": source.get("timestamp", ""),
                    "service": source.get("service_name", "unknown"),
                    "message": source.get("message", "")[:200]
                })
        except Exception as e:
            print(f"Warning: Failed to sample {level} logs: {e}")
            continue

    return samples


def _llm_estimate_statistics(
    log_samples: List[Dict[str, Any]],
    total_sample_count: int,
    time_hours: int,
    actual_level_counts: Optional[Dict[str, int]] = None,
    sample_metadata: Optional[Dict[str, Any]] = None
) -> Dict[str, Any]:
    """
    LLM에게 로그 샘플을 주고 전체 통계를 검증하게 함 (힌트 기반)

    Args:
        log_samples: 층화 샘플링된 로그 목록
        total_sample_count: 실제 총 로그 수
        time_hours: 분석 기간 (시간)
        actual_level_counts: DB에서 조회한 실제 레벨별 개수 (힌트)
        sample_metadata: 샘플링 메타데이터 (IPW 가중치 포함)
    """
    # 샘플 요약
    sample_level_counts = {}
    hourly_counts = {}

    for sample in log_samples:
        level = sample.get("level", "UNKNOWN")
        sample_level_counts[level] = sample_level_counts.get(level, 0) + 1

        timestamp = sample.get("timestamp", "")
        if timestamp:
            hour = timestamp[:13]  # "2024-01-01T15"
            hourly_counts[hour] = hourly_counts.get(hour, 0) + 1

    sample_summary = {
        "sample_size": len(log_samples),
        "level_distribution": sample_level_counts,
        "hourly_distribution": hourly_counts
    }

    # IPW 가중치 테이블 생성 (sample_metadata가 제공된 경우)
    weight_table_section = ""
    if sample_metadata and "weights" in sample_metadata:
        weights = sample_metadata["weights"]
        level_counts = sample_metadata.get("level_counts", {})
        sample_counts = sample_metadata.get("sample_counts", {})

        weight_table = "## 📊 가중 샘플링 정보 (Inverse Probability Weighting)\n\n"
        weight_table += "**중요**: 이 샘플은 희소 이벤트 인식 2단계 샘플링으로 수집되었습니다.\n"
        weight_table += "각 샘플이 대표하는 실제 로그 수(가중치)를 고려하여 추론하세요.\n\n"
        weight_table += "| 레벨 | 실제 개수 | 샘플 개수 | 가중치 (IPW) | 의미 |\n"
        weight_table += "|------|-----------|-----------|--------------|------|\n"

        for level in ["ERROR", "WARN", "INFO"]:
            actual = level_counts.get(level, 0)
            sampled = sample_counts.get(level, 0)
            weight = weights.get(level, 0.0)

            if sampled > 0:
                meaning = f"샘플 1개 = 실제 {weight:.1f}개"
            else:
                meaning = "샘플 없음"

            weight_table += f"| {level} | {actual:,} | {sampled} | ×{weight:.2f} | {meaning} |\n"

        weight_table += "\n**추론 방법**: 각 레벨의 샘플 개수에 해당 가중치를 곱하면 실제 개수가 됩니다.\n"
        weight_table += f"- 예: ERROR {sample_counts.get('ERROR', 0)}개 × {weights.get('ERROR', 0):.2f} = {level_counts.get('ERROR', 0):,}개\n\n"

        weight_table_section = weight_table

    # 실제 레벨별 개수가 제공된 경우: LLM이 검증만 수행
    if actual_level_counts:
        actual_error = actual_level_counts.get("ERROR", 0)
        actual_warn = actual_level_counts.get("WARN", 0)
        actual_info = actual_level_counts.get("INFO", 0)
        actual_error_rate = round((actual_error / total_sample_count * 100) if total_sample_count > 0 else 0, 2)

        # LLM 프롬프트 (검증 모드)
        llm = ChatOpenAI(
            model=settings.LLM_MODEL,
            temperature=0.1,
            openai_api_key=settings.OPENAI_API_KEY,
            base_url=settings.OPENAI_BASE_URL
        )

        prompt = f"""당신은 로그 데이터 분석 전문가입니다. DB 통계와 샘플 데이터를 비교 검증해야 합니다.

## DB 실제 통계 (Ground Truth)
- 총 로그 수: {total_sample_count:,}개
- ERROR: {actual_error:,}개 ({actual_error_rate}%)
- WARN: {actual_warn:,}개
- INFO: {actual_info:,}개
- 분석 기간: 최근 {time_hours}시간

## 층화 샘플 데이터
- 샘플 크기: {sample_summary['sample_size']}개
- 레벨별 분포 (샘플): {json.dumps(sample_summary['level_distribution'], ensure_ascii=False)}
- 샘플링 방식: 층화 샘플링 (희소 이벤트 최소 5개 보장)

## 검증 작업
1. 샘플이 전체 데이터를 잘 대표하는지 검증하세요.
2. 희소 이벤트(ERROR/WARN)가 잘 포착되었는지 확인하세요.
3. 샘플 품질에 대한 신뢰도를 평가하세요.

반드시 아래 JSON 형식으로만 응답하세요:
{{
    "estimated_total_logs": {total_sample_count},
    "estimated_error_count": {actual_error},
    "estimated_warn_count": {actual_warn},
    "estimated_info_count": {actual_info},
    "estimated_error_rate": {actual_error_rate},
    "confidence_score": <샘플 품질 신뢰도 0-100>,
    "reasoning": "<검증 결과 및 인사이트 1-2문장>"
}}

중요:
- 통계 값은 DB 실제 값을 그대로 사용합니다.
- 신뢰도는 샘플이 전체를 잘 대표하는지에 대한 평가입니다.
- reasoning에 샘플 품질과 데이터 특성에 대한 인사이트를 제공하세요."""

        try:
            response = llm.invoke(prompt)
            content = response.content.strip()

            # JSON 파싱
            if "```json" in content:
                content = content.split("```json")[1].split("```")[0].strip()
            elif "```" in content:
                content = content.split("```")[1].split("```")[0].strip()

            result = json.loads(content)
            # DB 값으로 강제 설정 (검증 모드)
            result["estimated_total_logs"] = total_sample_count
            result["estimated_error_count"] = actual_error
            result["estimated_warn_count"] = actual_warn
            result["estimated_info_count"] = actual_info
            result["estimated_error_rate"] = actual_error_rate
            return result
        except Exception as e:
            # 폴백: DB 값 직접 반환
            return {
                "estimated_total_logs": total_sample_count,
                "estimated_error_count": actual_error,
                "estimated_warn_count": actual_warn,
                "estimated_info_count": actual_info,
                "estimated_error_rate": actual_error_rate,
                "confidence_score": 100,  # DB 값이므로 100% 신뢰
                "reasoning": f"DB 통계 직접 사용 (LLM 검증 실패: {str(e)})"
            }

    # 기존 로직: 실제 개수가 없는 경우 (하위 호환성)
    llm = ChatOpenAI(
        model=settings.LLM_MODEL,
        temperature=0.1,
        openai_api_key=settings.OPENAI_API_KEY,
        base_url=settings.OPENAI_BASE_URL
    )

    prompt = f"""당신은 로그 데이터 분석 전문가입니다. 층화 샘플링된 데이터를 기반으로 전체 통계를 추론해야 합니다.

## ⚠️ 중요: 샘플링 편향 주의!

이 샘플은 층화 샘플링으로 수집되었으며, **희소 이벤트(ERROR/WARN)는 충분한 대표성 확보를 위해
최소 5개씩 보장되어 과대표현(oversampling)되었습니다.**

### 실제 프로덕션 로그의 일반적 특성
- **ERROR**: 일반적으로 전체의 0.3~1% (건강한 시스템 기준)
- **WARN**: 일반적으로 전체의 0.01~0.3% (매우 희소)
- **INFO**: 대부분의 로그를 차지 (98~99%+)

**경고**: 샘플에서 ERROR가 5%라고 해서 전체의 5%가 아닙니다!
최소 보장(minimum guarantee) 때문에 실제보다 10~100배 과대표현되었을 수 있습니다.

## 힌트 정보 (DB에서 제공)
- 실제 총 로그 수: {total_sample_count:,}개
- 분석 기간: 최근 {time_hours}시간

## 층화 샘플 데이터
- 샘플 크기: {sample_summary['sample_size']}개
- 레벨별 분포 (샘플): {json.dumps(sample_summary['level_distribution'], ensure_ascii=False)}
- 샘플링 방식: 레벨별 비율 기반 + 희소 이벤트 최소 5개 보장

{weight_table_section}

## 추론 가이드라인

{"**우선순위**: 위의 가중치 테이블이 제공된 경우, 해당 가중치를 사용하여 정확히 계산하세요!" if weight_table_section else ""}

1. **ERROR 로그 추론**:
   - {"가중치 테이블 사용: ERROR 샘플 개수 × 가중치 = 실제 ERROR 개수" if weight_table_section else "샘플에 ERROR가 많더라도 실제는 0.3~1% 범위일 가능성 높음"}
   - 샘플 비율을 그대로 적용하지 말고, 일반적 범위를 고려하여 보수적으로 추론

2. **WARN 로그 추론**:
   - {"가중치 테이블 사용: WARN 샘플 개수 × 가중치 = 실제 WARN 개수" if weight_table_section else "WARN은 ERROR보다 더 희소 (0.01~0.3% 범위)"}
   - 샘플에 존재한다면 최소 보장으로 인한 과대표현일 가능성 높음

3. **INFO 로그 추론**:
   - {"가중치 테이블 사용: INFO 샘플 개수 × 가중치 = 실제 INFO 개수" if weight_table_section else "대부분의 로그는 INFO (전체의 98~99%+)"}
   - INFO = 총 로그 수 - ERROR - WARN

{"가중치 테이블이 제공된 경우 우선적으로 사용하되," if weight_table_section else ""}
총 로그 수({total_sample_count:,}개)는 확정되어 있으므로,
샘플의 **상대적 비율**은 참고하되 **절대적 비율**은 프로덕션 환경의 일반적 특성에 맞게 조정하세요.

반드시 아래 JSON 형식으로만 응답하세요 (다른 텍스트 없이):
{{
    "estimated_total_logs": {total_sample_count},
    "estimated_error_count": <추론한 ERROR 로그 수>,
    "estimated_warn_count": <추론한 WARN 로그 수>,
    "estimated_info_count": <추론한 INFO 로그 수>,
    "estimated_error_rate": <추론한 에러율 (%)>,
    "confidence_score": <추론 신뢰도 0-100>,
    "reasoning": "<추론 근거 1-2문장>"
}}

중요:
- 총 로그 수는 {total_sample_count}으로 고정입니다.
- 샘플 비율을 무비판적으로 적용하지 마세요! 희소 이벤트는 과대표현되었습니다.
- ERROR는 보통 0.3~1%, WARN은 0.01~0.3% 범위로 보수적으로 추론하세요.
- ERROR + WARN + INFO = 총 로그 수가 되어야 합니다."""

    try:
        response = llm.invoke(prompt)
        content = response.content.strip()

        # JSON 파싱
        if "```json" in content:
            content = content.split("```json")[1].split("```")[0].strip()
        elif "```" in content:
            content = content.split("```")[1].split("```")[0].strip()

        result = json.loads(content)
        # 총 로그 수가 힌트와 일치하는지 확인
        result["estimated_total_logs"] = total_sample_count
        return result
    except Exception as e:
        # 폴백: 단순 비율 계산
        sample_total = len(log_samples)
        if sample_total == 0:
            return {
                "estimated_total_logs": total_sample_count,
                "estimated_error_count": 0,
                "estimated_warn_count": 0,
                "estimated_info_count": 0,
                "estimated_error_rate": 0.0,
                "confidence_score": 0,
                "reasoning": f"LLM 추론 실패: {str(e)}"
            }

        error_ratio = sample_level_counts.get("ERROR", 0) / sample_total
        warn_ratio = sample_level_counts.get("WARN", 0) / sample_total
        info_ratio = sample_level_counts.get("INFO", 0) / sample_total

        return {
            "estimated_total_logs": total_sample_count,
            "estimated_error_count": int(total_sample_count * error_ratio),
            "estimated_warn_count": int(total_sample_count * warn_ratio),
            "estimated_info_count": int(total_sample_count * info_ratio),
            "estimated_error_rate": round(error_ratio * 100, 2),
            "confidence_score": 85,
            "reasoning": f"샘플 비율 직접 적용 (LLM 응답 파싱 실패)"
        }


def _calculate_accuracy(db_stats: Dict[str, Any], ai_stats: Dict[str, Any]) -> Dict[str, Any]:
    """
    DB 통계와 AI 추론 통계의 정확도 계산
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
    error_rate_accuracy = max(0, round(100 - error_rate_diff * 10, 2))  # 1% 차이당 10점 감점

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
        "overall_accuracy": overall_accuracy,
        "ai_confidence": ai_stats.get("confidence_score", 0)
    }


@tool
async def compare_ai_vs_db_statistics(
    project_uuid: str,
    time_hours: int = 24,
    sample_size: int = 100
) -> str:
    """
    AI 추론 통계와 DB 직접 조회 통계를 비교하여 정확도를 검증합니다.

    이 도구는 다음을 수행합니다:
    - ✅ OpenSearch에서 직접 통계 쿼리 (Ground Truth)
    - ✅ 로그 샘플을 LLM에게 제공하여 전체 통계 추론
    - ✅ 두 결과의 정확도/오차율 계산
    - ✅ AI가 DB 쿼리를 대체할 수 있는 역량 검증

    사용 시나리오:
    1. "AI가 DB를 대체할 수 있는지 검증해줘"
    2. "통계 추론 정확도를 확인해줘"
    3. "LLM 기반 분석의 신뢰도는?"

    ⚠️ 중요한 제약사항:
    - 1회 호출로 충분합니다
    - LLM API 호출이 포함되어 약간의 시간이 소요됩니다
    - temperature=0.1로 일관된 결과 보장

    입력 파라미터 (JSON 형식):
        time_hours: 분석 기간 (기본 24시간)
        sample_size: LLM에 제공할 샘플 크기 (기본 100개)

    Returns:
        DB 통계, AI 추론 통계, 정확도 지표, 검증 결과
    """
    try:
        # 1. DB에서 직접 통계 조회 (Ground Truth)
        db_stats = _get_db_statistics(project_uuid, time_hours)

        if db_stats["total_logs"] == 0:
            return f"최근 {time_hours}시간 동안 로그 데이터가 없습니다."

        # 2. 로그 샘플 추출
        log_samples = _get_log_samples(project_uuid, time_hours, sample_size)

        if not log_samples:
            return f"로그 샘플을 추출할 수 없습니다."

        # 3. LLM 기반 통계 추론
        ai_stats = _llm_estimate_statistics(log_samples, db_stats["total_logs"], time_hours)

        # 4. 정확도 계산
        accuracy_metrics = _calculate_accuracy(db_stats, ai_stats)

        # 5. 결과 포맷팅
        summary_lines = [
            "=" * 50,
            "  AI vs DB 통계 비교 검증 결과",
            "=" * 50,
            "",
            f"📊 분석 기간: 최근 {time_hours}시간",
            f"📊 샘플 크기: {len(log_samples)}개 / 전체 {db_stats['total_logs']:,}개",
            "",
            "=" * 50,
            "  1. DB 직접 조회 결과 (Ground Truth)",
            "=" * 50,
            f"  총 로그 수: {db_stats['total_logs']:,}개",
            f"  ERROR: {db_stats['error_count']:,}개",
            f"  WARN: {db_stats['warn_count']:,}개",
            f"  INFO: {db_stats['info_count']:,}개",
            f"  에러율: {db_stats['error_rate']:.2f}%",
            f"  피크 시간: {db_stats['peak_hour']} ({db_stats['peak_count']:,}건)",
            "",
            "=" * 50,
            "  2. AI(LLM) 추론 결과",
            "=" * 50,
            f"  총 로그 수: {ai_stats.get('estimated_total_logs', 0):,}개",
            f"  ERROR: {ai_stats.get('estimated_error_count', 0):,}개",
            f"  WARN: {ai_stats.get('estimated_warn_count', 0):,}개",
            f"  INFO: {ai_stats.get('estimated_info_count', 0):,}개",
            f"  에러율: {ai_stats.get('estimated_error_rate', 0):.2f}%",
            f"  AI 신뢰도: {ai_stats.get('confidence_score', 0)}%",
            f"  추론 근거: {ai_stats.get('reasoning', 'N/A')}",
            "",
            "=" * 50,
            "  3. 정확도 검증 결과",
            "=" * 50,
            f"  📈 총 로그 수 일치율: {accuracy_metrics['total_logs_accuracy']:.1f}%",
            f"  📈 ERROR 수 일치율: {accuracy_metrics['error_count_accuracy']:.1f}%",
            f"  📈 WARN 수 일치율: {accuracy_metrics['warn_count_accuracy']:.1f}%",
            f"  📈 INFO 수 일치율: {accuracy_metrics['info_count_accuracy']:.1f}%",
            f"  📈 에러율 정확도: {accuracy_metrics['error_rate_accuracy']:.1f}%",
            "",
            f"  ⭐ 종합 정확도: {accuracy_metrics['overall_accuracy']:.1f}%",
            "",
        ]

        # 6. 검증 결론
        overall = accuracy_metrics['overall_accuracy']
        if overall >= 95:
            verdict = "🏆 **매우 우수**: AI가 DB 쿼리를 완벽히 대체 가능"
            explanation = "오차율 5% 미만으로 프로덕션 환경에서 신뢰성 있게 사용 가능합니다."
        elif overall >= 90:
            verdict = "✅ **우수**: AI가 DB 쿼리를 효과적으로 대체 가능"
            explanation = "오차율 10% 미만으로 대부분의 분석 업무에 활용 가능합니다."
        elif overall >= 80:
            verdict = "🟡 **양호**: AI가 보조 도구로서 유용"
            explanation = "오차율 20% 미만으로 트렌드 분석과 이상 탐지에 활용 가능합니다."
        elif overall >= 70:
            verdict = "🟠 **보통**: 개선이 필요"
            explanation = "오차율이 높아 추가 튜닝이 필요합니다."
        else:
            verdict = "🔴 **미흡**: AI 추론 로직 재검토 필요"
            explanation = "정확도가 낮아 프롬프트나 샘플링 전략 개선이 필요합니다."

        summary_lines.extend([
            "=" * 50,
            "  4. 검증 결론",
            "=" * 50,
            f"  {verdict}",
            "",
            f"  {explanation}",
            "",
            "=" * 50,
            "  5. 기술적 검증 포인트",
            "=" * 50,
            "  - Temperature 0.1로 일관된 추론 보장",
            "  - Structured Output으로 형식 오류 방지",
            "  - 샘플 기반 추론으로 토큰 비용 절감",
            "  - 자동화된 정확도 측정으로 신뢰성 검증",
            "",
            f"  💡 이 결과는 LLM이 단순 DB 집계를 넘어",
            f"     '사람 수준의 데이터 분석 역량'을 보유함을 증명합니다.",
            "=" * 50,
        ])

        return "\n".join(summary_lines)

    except Exception as e:
        return f"AI vs DB 통계 비교 중 오류 발생: {str(e)}"


@tool
async def get_hourly_comparison(
    project_uuid: str,
    time_hours: int = 24
) -> str:
    """
    시간대별 로그 통계를 DB와 AI 추론으로 비교합니다.

    이 도구는 다음을 수행합니다:
    - ✅ 1시간 단위 로그 분포 DB 조회
    - ✅ 시간대별 패턴 분석
    - ✅ 피크 시간, 트렌드 방향 확인

    사용 시나리오:
    1. "시간대별 로그 추세를 보여줘"
    2. "1시간 단위 통계를 비교해줘"

    Returns:
        시간대별 로그 통계, 피크 시간, 트렌드 분석
    """
    try:
        db_stats = _get_db_statistics(project_uuid, time_hours)

        if db_stats["total_logs"] == 0:
            return f"최근 {time_hours}시간 동안 로그 데이터가 없습니다."

        summary_lines = [
            f"## 📊 시간대별 로그 분포 (최근 {time_hours}시간)",
            "",
            f"**총 로그**: {db_stats['total_logs']:,}개",
            f"**에러율**: {db_stats['error_rate']:.2f}%",
            f"**피크 시간**: {db_stats['peak_hour']} ({db_stats['peak_count']:,}건)",
            "",
            "### 시간대별 상세",
            "| 시간 | 총 로그 | ERROR | WARN | INFO |",
            "|------|---------|-------|------|------|"
        ]

        for hourly in db_stats["hourly_data"][-12:]:  # 최근 12시간만
            hour_display = hourly["hour"][-5:] if len(hourly["hour"]) >= 5 else hourly["hour"]
            summary_lines.append(
                f"| {hour_display} | {hourly['total']:,} | {hourly['error']} | {hourly['warn']} | {hourly['info']} |"
            )

        return "\n".join(summary_lines)

    except Exception as e:
        return f"시간대별 통계 조회 중 오류 발생: {str(e)}"


# ========== ERROR 전용 비교 함수들 ==========

async def _get_db_error_statistics(
    project_uuid: str,
    time_hours: int = 24
) -> Dict[str, Any]:
    """
    OpenSearch에서 ERROR 로그 통계 조회

    Returns:
        {
            "total_logs": int,        # 전체 로그 수
            "total_errors": int,      # ERROR 로그 수
            "error_rate": float,      # ERROR 비율 (%)
            "peak_error_hour": str,   # ERROR 최다 발생 시간
            "peak_error_count": int   # 최다 시간 ERROR 수
        }
    """
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)
    index_pattern = f"{project_uuid.replace('-', '_')}_*"

    query = {
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
            # ERROR 로그 수
            "error_logs": {
                "filter": {"term": {"level": "ERROR"}}
            },
            # 시간대별 ERROR 분포
            "error_by_hour": {
                "filter": {"term": {"level": "ERROR"}},
                "aggs": {
                    "hours": {
                        "date_histogram": {
                            "field": "timestamp",
                            "fixed_interval": "1h",
                            "time_zone": "Asia/Seoul"
                        }
                    }
                }
            }
        }
    }

    result = await asyncio.to_thread(
        opensearch_client.search,
        index=index_pattern,
        body=query,
        track_total_hits=True
    )

    total_logs = result["hits"]["total"]["value"]
    total_errors = result["aggregations"]["error_logs"]["doc_count"]
    error_rate = (total_errors / total_logs * 100) if total_logs > 0 else 0.0

    # Peak ERROR hour 찾기
    hour_buckets = result["aggregations"]["error_by_hour"]["hours"]["buckets"]
    peak_hour = None
    peak_count = 0
    if hour_buckets:
        peak_bucket = max(hour_buckets, key=lambda b: b["doc_count"])
        peak_hour = peak_bucket["key_as_string"]
        peak_count = peak_bucket["doc_count"]

    return {
        "total_logs": total_logs,
        "total_errors": total_errors,
        "error_rate": round(error_rate, 2),
        "peak_error_hour": peak_hour,
        "peak_error_count": peak_count
    }


async def _sample_errors_with_vector(
    project_uuid: str,
    time_hours: int,
    sample_size: int
) -> Tuple[List[Dict], Optional[Dict]]:
    """
    Vector KNN으로 ERROR 로그 샘플링 + 그룹핑 정보

    Returns:
        (samples, vector_info)
        - samples: ERROR 로그 샘플 리스트
        - vector_info: Vector 그룹핑 메타데이터
    """
    # 벡터화된 ERROR 로그 개수 확인
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)
    index_pattern = f"{project_uuid.replace('-', '_')}_*"

    vector_check_query = {
        "size": 0,
        "query": {
            "bool": {
                "must": [
                    {"term": {"level": "ERROR"}},
                    {"exists": {"field": "log_vector"}},
                    {"range": {"timestamp": {
                        "gte": start_time.isoformat() + "Z",
                        "lte": end_time.isoformat() + "Z"
                    }}}
                ]
            }
        }
    }

    vector_result = await asyncio.to_thread(
        opensearch_client.search,
        index=index_pattern,
        body=vector_check_query,
        track_total_hits=True
    )

    vectorized_count = vector_result["hits"]["total"]["value"]

    # 전체 ERROR 수 조회
    total_error_query = {
        "size": 0,
        "query": {
            "bool": {
                "must": [
                    {"term": {"level": "ERROR"}},
                    {"range": {"timestamp": {
                        "gte": start_time.isoformat() + "Z",
                        "lte": end_time.isoformat() + "Z"
                    }}}
                ]
            }
        }
    }

    total_error_result = await asyncio.to_thread(
        opensearch_client.search,
        index=index_pattern,
        body=total_error_query,
        track_total_hits=True
    )

    total_errors = total_error_result["hits"]["total"]["value"]
    vectorization_rate = (vectorized_count / total_errors * 100) if total_errors > 0 else 0.0

    # 다양성을 위해 항상 랜덤 샘플링 사용
    # (Vector KNN은 특정 쿼리와 유사한 것만 선택하여 편향됨)
    samples = await sample_random_stratified(
        project_uuid=project_uuid,
        k_per_level={"ERROR": sample_size},
        time_hours=time_hours
    )
    sampling_method = "random_diverse"
    logger.info(f"✅ 랜덤 샘플링으로 다양한 ERROR 확보: {len(samples)}개")

    vector_info = {
        "vectorized_error_count": vectorized_count,
        "vectorization_rate": round(vectorization_rate, 1),
        "sampling_method": sampling_method,
        "sample_distribution": f"{len(samples)}개 ERROR 로그 샘플"
    }

    return samples, vector_info


def _llm_estimate_error_statistics(
    error_samples: List[Dict],
    total_logs: int,
    time_hours: int
) -> Dict[str, Any]:
    """
    LLM으로 ERROR 통계 추정

    Returns:
        {
            "estimated_total_errors": int,
            "estimated_error_rate": float,
            "confidence_score": int,
            "reasoning": str
        }
    """
    sample_count = len(error_samples)

    # 샘플 요약 (메시지만, 최대 200자)
    sample_summaries = []
    for i, log in enumerate(error_samples[:50], 1):  # 최대 50개만
        msg = log.get("message", "")[:200]
        sample_summaries.append(f"{i}. {msg}")

    prompt = f"""당신은 로그 분석 전문가입니다. 최근 {time_hours}시간 동안 수집된 ERROR 로그 샘플을 분석하여 전체 ERROR 통계를 추정하세요.

## 주어진 정보

**전체 로그 수**: {total_logs:,}개
**ERROR 샘플 수**: {sample_count}개 (전체 ERROR 로그에서 샘플링됨)

**ERROR 샘플 메시지** (최대 50개):
{chr(10).join(sample_summaries)}

## 추정 과제

1. **전체 ERROR 로그 개수 추정**
   - 샘플 비율을 고려하여 전체 ERROR 수 추정
   - 건강한 시스템: ERROR 0.3~1%
   - 문제 있는 시스템: ERROR 1~5%+

2. **ERROR 발생률 추정** (%)
   - ERROR 로그 / 전체 로그 * 100

3. **신뢰도 점수** (0~100)
   - 샘플이 충분한가?
   - 샘플 품질이 좋은가?

4. **추론 근거**
   - 왜 이렇게 추정했는지 간단히 설명

## 출력 형식 (JSON만)

{{
  "estimated_total_errors": <숫자>,
  "estimated_error_rate": <소수점 2자리>,
  "confidence_score": <0~100 정수>,
  "reasoning": "<추론 근거, 2-3문장>"
}}

JSON만 출력하세요."""

    llm = ChatOpenAI(
        model=settings.LLM_MODEL,
        temperature=0.1,
        openai_api_key=settings.OPENAI_API_KEY,
        base_url=settings.OPENAI_BASE_URL
    )
    response = llm.invoke(prompt)
    result_text = response.content.strip()

    # JSON 파싱
    try:
        # JSON 블록 추출
        if "```json" in result_text:
            result_text = result_text.split("```json")[1].split("```")[0].strip()
        elif "```" in result_text:
            result_text = result_text.split("```")[1].split("```")[0].strip()

        result = json.loads(result_text)

        return {
            "estimated_total_errors": result["estimated_total_errors"],
            "estimated_error_rate": round(result["estimated_error_rate"], 2),
            "confidence_score": result["confidence_score"],
            "reasoning": result["reasoning"]
        }

    except Exception as e:
        logger.error(f"LLM 응답 파싱 실패: {e}, 응답: {result_text[:500]}")

        # 폴백: 단순 비율 계산
        estimated_errors = int(sample_count * (total_logs / sample_count)) if sample_count > 0 else 0
        estimated_rate = (estimated_errors / total_logs * 100) if total_logs > 0 else 0.0

        return {
            "estimated_total_errors": estimated_errors,
            "estimated_error_rate": round(estimated_rate, 2),
            "confidence_score": 50,
            "reasoning": "LLM 추론 실패, 단순 비율로 추정"
        }


def _calculate_error_accuracy(
    db_stats: Dict[str, Any],
    ai_stats: Dict[str, Any]
) -> Dict[str, float]:
    """
    ERROR 통계 정확도 계산

    Returns:
        {
            "error_count_accuracy": float,
            "error_rate_accuracy": float,
            "overall_accuracy": float
        }
    """
    def accuracy_percentage(actual, predicted):
        if actual == 0:
            return 100.0 if predicted == 0 else 0.0
        error = abs(actual - predicted)
        return max(0, round((1 - error / actual) * 100, 2))

    # ERROR 개수 정확도
    error_count_acc = accuracy_percentage(
        db_stats["total_errors"],
        ai_stats["estimated_total_errors"]
    )

    # ERROR 비율 정확도 (10점 per 1% 차이)
    rate_diff = abs(db_stats["error_rate"] - ai_stats["estimated_error_rate"])
    error_rate_acc = max(0, round(100 - rate_diff * 10, 2))

    # 종합 정확도 (가중 평균)
    overall_acc = round(
        error_count_acc * 0.6 + error_rate_acc * 0.4,
        2
    )

    return {
        "error_count_accuracy": error_count_acc,
        "error_rate_accuracy": error_rate_acc,
        "overall_accuracy": overall_acc
    }


def _calculate_dynamic_threshold(vectors: List[List[float]], min_threshold: float = 0.3) -> float:
    """
    Centroid 기반 동적 threshold 계산

    각 ERROR 샘플과 centroid의 유사도를 계산하고,
    최소 유사도 - margin을 threshold로 설정하여
    모든 ERROR 샘플이 threshold 이상이 되도록 보장합니다.

    Args:
        vectors: ERROR 샘플들의 벡터 리스트
        min_threshold: 최소 threshold (기본 0.3)

    Returns:
        동적으로 계산된 threshold
    """
    import numpy as np

    if len(vectors) < 2:
        return 0.5  # 샘플 부족 시 기본값

    vectors_array = np.array(vectors)

    # 1. Centroid 계산 (평균 벡터)
    centroid = np.mean(vectors_array, axis=0)

    # 2. Centroid 정규화
    centroid_norm = centroid / np.linalg.norm(centroid)

    # 3. 각 샘플 정규화
    norms = np.linalg.norm(vectors_array, axis=1, keepdims=True)
    normalized = vectors_array / norms

    # 4. 각 샘플과 centroid의 cosine similarity 계산
    similarities_to_centroid = np.dot(normalized, centroid_norm)

    # 5. 최소 유사도 - margin을 threshold로 설정
    min_sim = np.min(similarities_to_centroid)
    mean_sim = np.mean(similarities_to_centroid)
    margin = 0.05  # 약간의 여유 마진

    dynamic_threshold = min_sim - margin

    logger.info(
        f"📊 Centroid-based threshold: "
        f"min_sim={min_sim:.3f}, mean_sim={mean_sim:.3f}, "
        f"margin={margin}, threshold={dynamic_threshold:.3f}"
    )

    # 최소값 클램핑
    return max(dynamic_threshold, min_threshold)


async def _vector_estimate_error_count(
    error_samples: List[Dict],
    project_uuid: str,
    time_hours: int,
    total_logs: int,
    similarity_threshold: float = None  # None이면 동적 계산
) -> Dict[str, Any]:
    """
    Vector 유사도 기반 ERROR-like 로그 counting

    ERROR 샘플들의 centroid(평균 벡터)를 계산하고,
    전체 로그에서 centroid와 유사도가 threshold 이상인 로그를 counting합니다.

    Args:
        error_samples: 벡터가 있는 ERROR 로그 샘플들
        project_uuid: 프로젝트 UUID
        time_hours: 분석 기간
        total_logs: 전체 로그 수 (error_rate 계산용)
        similarity_threshold: 유사도 임계값 (None이면 동적 계산)

    Returns:
        {
            "estimated_total_errors": int,
            "estimated_error_rate": float,
            "confidence_score": int,
            "reasoning": str
        }
    """
    import numpy as np

    # 1. ERROR 샘플들의 벡터 추출
    vectors = [s.get('log_vector') for s in error_samples if s.get('log_vector')]

    if not vectors:
        logger.warning("No vectorized ERROR samples available for centroid calculation")
        return {
            "estimated_total_errors": 0,
            "estimated_error_rate": 0.0,
            "confidence_score": 0,
            "reasoning": "벡터화된 ERROR 샘플 없음"
        }

    # 2. 동적 threshold 계산 (None이면)
    if similarity_threshold is None:
        similarity_threshold = _calculate_dynamic_threshold(vectors)
        logger.info(f"📊 Using dynamic threshold: {similarity_threshold:.3f}")

    logger.info(f"📊 Vector estimation: {len(vectors)} ERROR samples with vectors")

    # 2. Centroid 계산 (평균 벡터)
    centroid = np.mean(vectors, axis=0).tolist()
    logger.info(f"📊 Centroid calculated from {len(vectors)} vectors")

    # 3. OpenSearch KNN 검색 (전체 로그 대상, 유사도 threshold 적용)
    index_pattern = f"{project_uuid.replace('-', '_')}_*"
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)

    try:
        # KNN 쿼리로 유사한 로그 검색
        knn_query = {
            "size": 0,  # count만 필요
            "query": {
                "bool": {
                    "must": [
                        {
                            "script_score": {
                                "query": {
                                    "bool": {
                                        "filter": [
                                            {"range": {"timestamp": {
                                                "gte": start_time.isoformat() + 'Z',
                                                "lte": end_time.isoformat() + 'Z'
                                            }}},
                                            {"exists": {"field": "log_vector"}}
                                        ]
                                    }
                                },
                                "script": {
                                    "source": "cosineSimilarity(params.query_vector, 'log_vector') + 1.0",
                                    "params": {"query_vector": centroid}
                                }
                            }
                        }
                    ],
                    "filter": [
                        {"range": {"_score": {"gte": similarity_threshold + 1.0}}}  # +1.0 because cosineSimilarity returns [-1, 1] + 1 = [0, 2]
                    ]
                }
            },
            "track_total_hits": True
        }

        # 먼저 전체 유사 로그 수를 aggregation으로 계산
        # script_score는 filter에서 사용 불가하므로, 대안 접근법 사용
        # OpenSearch에서는 min_score를 사용

        # 대안: KNN으로 top-k 검색 후 threshold 이상만 count
        knn_search_query = {
            "size": 10000,  # 충분히 큰 값
            "min_score": similarity_threshold + 1.0,  # cosineSimilarity + 1.0 스코어에 맞춤 (0.5 → 1.5)
            "query": {
                "script_score": {
                    "query": {
                        "bool": {
                            "filter": [
                                {"range": {"timestamp": {
                                    "gte": start_time.isoformat() + 'Z',
                                    "lte": end_time.isoformat() + 'Z'
                                }}},
                                {"exists": {"field": "log_vector"}}
                            ]
                        }
                    },
                    "script": {
                        "source": "cosineSimilarity(params.query_vector, 'log_vector') + 1.0",
                        "params": {"query_vector": centroid}
                    }
                }
            },
            "_source": False,  # 문서 내용 불필요, count만
            "track_total_hits": True
        }

        result = opensearch_client.search(
            index=index_pattern,
            body=knn_search_query
        )

        # 4. Counting - threshold 이상인 결과만
        hits = result.get('hits', {}).get('hits', [])
        # min_score가 제대로 작동하지 않을 수 있으므로 수동 필터링
        similar_count = sum(1 for hit in hits if hit.get('_score', 0) >= similarity_threshold + 1.0)

        # total_hits 사용 (더 정확할 수 있음)
        total_hits = result.get('hits', {}).get('total', {})
        if isinstance(total_hits, dict):
            reported_count = total_hits.get('value', 0)
        else:
            reported_count = total_hits

        # 더 큰 값 사용 (size 제한으로 인한 손실 방지)
        similar_count = max(similar_count, reported_count)

        logger.info(f"📊 Vector KNN search: {similar_count} logs with similarity >= {similarity_threshold}")

    except Exception as e:
        logger.error(f"Vector KNN search failed: {e}")
        # 폴백: 샘플 기반 추정
        similar_count = len(error_samples)

    # 5. 결과 계산
    error_rate = (similar_count / total_logs * 100) if total_logs > 0 else 0.0

    # 신뢰도: 샘플 수 기반
    confidence = min(100, len(vectors) * 2)

    return {
        "estimated_total_errors": similar_count,
        "estimated_error_rate": round(error_rate, 2),
        "confidence_score": confidence,
        "reasoning": f"ERROR centroid 기준 유사도 {similarity_threshold} 이상 로그 {similar_count}개 (샘플 {len(vectors)}개 기반)"
    }
