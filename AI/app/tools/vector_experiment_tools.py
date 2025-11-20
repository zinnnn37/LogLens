"""
Vector AI 실험 도구 - ERROR 패턴 분석
- Vector 기반 ERROR 패턴 클러스터링 vs SQL GROUP BY 비교
"""

import json
import asyncio
from typing import Dict, Any, List
from datetime import datetime, timedelta
from langchain_openai import ChatOpenAI
from collections import Counter

from app.core.opensearch import opensearch_client
from app.core.config import settings
from app.services.embedding_service import embedding_service


def _get_db_error_statistics(
    project_uuid: str,
    time_hours: int = 24
) -> Dict[str, Any]:
    """
    OpenSearch 집계로 ERROR 로그 통계 조회 (Ground Truth)

    Returns:
        {
            "total_errors": int,
            "error_types": {
                "NullPointerException": count,
                "TimeoutException": count,
                ...
            },
            "top_errors": [
                {"pattern": str, "count": int, "percentage": float},
                ...
            ]
        }
    """
    index_pattern = f"{project_uuid.replace('-', '_')}_*"
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)

    query_body = {
        "track_total_hits": True,
        "size": 0,
        "query": {
            "bool": {
                "must": [
                    {"term": {"level": "ERROR"}}
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
        "aggs": {
            # 메시지 키워드 추출 (간단한 패턴)
            "error_messages": {
                "terms": {
                    "field": "message.keyword",
                    "size": 20
                }
            }
        }
    }

    results = opensearch_client.search(index=index_pattern, body=query_body)

    # ERROR 로그 총 개수
    total_errors = results.get("hits", {}).get("total", {}).get("value", 0)

    # 에러 메시지별 카운트
    error_buckets = results.get("aggregations", {}).get("error_messages", {}).get("buckets", [])

    # 에러 타입 추출 (간단한 패턴 매칭)
    error_types = {}
    for bucket in error_buckets:
        message = bucket["key"]
        count = bucket["doc_count"]

        # 에러 타입 추출 (Exception 이름 찾기)
        error_type = _extract_error_type(message)
        error_types[error_type] = error_types.get(error_type, 0) + count

    # 상위 에러 리스트
    top_errors = []
    for error_type, count in sorted(error_types.items(), key=lambda x: x[1], reverse=True)[:10]:
        percentage = (count / total_errors * 100) if total_errors > 0 else 0
        top_errors.append({
            "pattern": error_type,
            "count": count,
            "percentage": round(percentage, 2)
        })

    return {
        "total_errors": total_errors,
        "error_types": error_types,
        "top_errors": top_errors,
        "analysis_period_hours": time_hours
    }


def _extract_error_type(message: str) -> str:
    """
    에러 메시지에서 에러 타입 추출 (정규식 기반)

    Examples:
        "NullPointerException: at line 45" -> "NullPointerException"
        "java.lang.TimeoutException" -> "TimeoutException"
        "Database timeout after 30s" -> "TimeoutException"
    """
    import re

    # 1순위: Exception/Error 클래스명 정규식 추출 (Java/Python 패턴)
    # 패턴: 단어로 시작하고 Exception, Error, Failure로 끝나는 것
    exception_pattern = r'\b([A-Z][a-zA-Z]*(?:Exception|Error|Failure))\b'
    matches = re.findall(exception_pattern, message)
    if matches:
        # 가장 먼저 나오는 Exception 반환
        return matches[0]

    # 2순위: 콜론 앞의 단어 (일반적인 에러 형식)
    colon_pattern = r'^([A-Za-z]+(?:Exception|Error)?):.*'
    colon_match = re.match(colon_pattern, message.strip())
    if colon_match:
        error_type = colon_match.group(1)
        # Exception/Error 접미사 추가 (없으면)
        if not error_type.endswith(('Exception', 'Error', 'Failure')):
            return error_type + "Exception"
        return error_type

    # 3순위: 키워드 기반 매칭
    message_lower = message.lower()
    keyword_map = {
        'nullpointer': 'NullPointerException',
        'null pointer': 'NullPointerException',
        'timeout': 'TimeoutException',
        'authentication': 'AuthenticationException',
        'auth': 'AuthenticationException',
        'filenotfound': 'FileNotFoundException',
        'file not found': 'FileNotFoundException',
        'parse': 'ParseException',
        'parsing': 'ParseException',
        'invalid': 'InvalidException',
        'api': 'APIException',
        'service': 'ServiceException',
        'database': 'DatabaseException',
        'connection': 'ConnectionException',
    }

    for keyword, exception_name in keyword_map.items():
        if keyword in message_lower:
            return exception_name

    # 4순위: 첫 단어 사용
    words = message.split()
    first_word = words[0] if words else "Unknown"
    if not first_word.endswith(('Exception', 'Error', 'Failure')):
        return first_word + "Exception"
    return first_word


async def _vector_search_error_patterns(
    project_uuid: str,
    k: int,
    time_hours: int = 24
) -> List[Dict[str, Any]]:
    """
    Vector 검색으로 ERROR 로그 샘플 수집 (패턴 분석용)

    Args:
        project_uuid: 프로젝트 UUID
        k: 수집할 샘플 수
        time_hours: 시간 범위

    Returns:
        ERROR 로그 샘플 리스트
    """
    index_pattern = f"{project_uuid.replace('-', '_')}_*"
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)

    # ERROR 로그 패턴 분석용 쿼리 벡터
    query_vector = await embedding_service.embed_query("error log pattern analysis")

    # KNN 검색 (ERROR만)
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
                    {"term": {"level": "ERROR"}},  # ERROR만 필터링
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
        "_source": ["level", "message", "timestamp", "service_name", "log_id", "class_name", "method_name"]
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
            "level": source.get("level", "ERROR"),
            "message": source.get("message", ""),
            "timestamp": source.get("timestamp", ""),
            "service_name": source.get("service_name", "unknown"),
            "log_id": source.get("log_id"),
            "class_name": source.get("class_name", ""),
            "method_name": source.get("method_name", "")
        })

    return samples


async def _llm_analyze_error_patterns(
    samples: List[Dict[str, Any]],
    total_errors: int,
    time_hours: int
) -> Dict[str, Any]:
    """
    LLM이 ERROR 로그 샘플을 보고 패턴 분석 및 클러스터링

    Args:
        samples: Vector 검색으로 수집한 ERROR 샘플
        total_errors: 실제 총 ERROR 로그 수
        time_hours: 분석 기간

    Returns:
        {
            "error_clusters": [
                {"type": str, "estimated_count": int, "severity": str, "description": str},
                ...
            ],
            "anomalies": [str],
            "confidence": int,
            "reasoning": str
        }
    """
    sample_size = len(samples)

    # 샘플 메시지 준비
    sample_messages = [s["message"] for s in samples[:50]]  # 최대 50개만 LLM에 전달

    # LLM 프롬프트
    llm = ChatOpenAI(
        model=settings.LLM_MODEL,
        temperature=0.1,
        openai_api_key=settings.OPENAI_API_KEY,
        base_url=settings.OPENAI_BASE_URL
    )

    prompt = f"""당신은 로그 분석 전문가입니다. ERROR 로그 샘플을 분석하여 **정확한 Exception 클래스명**으로 패턴을 파악하고 클러스터링하세요.

## ⚠️ 중요: Exception 클래스명 규칙
반드시 **정확한 Java/Python Exception 클래스명**을 사용하세요:
- ✅ 올바른 예시: "NullPointerException", "FileNotFoundException", "TimeoutException", "AuthenticationException"
- ❌ 잘못된 예시: "Null Pointer Error", "File Not Found", "Timeout", "Auth Error"
- 규칙: 단어 첫글자 대문자 + Exception/Error 접미사 (예: ParseException, APIException)

## 분석 정보
- 총 ERROR 로그 수: {total_errors:,}개
- 샘플 크기: {sample_size}개
- 분석 기간: 최근 {time_hours}시간

## ERROR 로그 샘플 (최대 50개)
{chr(10).join(f"{i+1}. {msg}" for i, msg in enumerate(sample_messages[:50]))}

## 작업
1. 각 로그에서 **정확한 Exception 클래스명** 식별
2. 동일한 Exception은 하나의 클러스터로 그룹화
3. 각 클러스터가 전체 ERROR의 몇 %인지 추정
4. 심각도 평가 (high/medium/low)
5. 이상 패턴 감지 (평소와 다른 에러)

반드시 아래 JSON 형식으로만 응답하세요:
{{
    "error_clusters": [
        {{
            "type": "정확한Exception클래스명",
            "estimated_count": 추정 개수,
            "estimated_percentage": 추정 비율(%),
            "severity": "high|medium|low",
            "description": "패턴 설명 1-2문장"
        }}
    ],
    "anomalies": ["이상 패턴 설명"],
    "confidence": 0-100 사이 정수,
    "reasoning": "분석 근거 1-2문장"
}}

중요:
- type 필드는 반드시 정확한 Exception 클래스명 (예: NullPointerException, FileNotFoundException)
- 일반화된 설명 금지 (예: "Null Pointer Error" → "NullPointerException")
- error_clusters의 estimated_count 합이 {total_errors}에 근접해야 함
- 최소 3개 이상의 주요 클러스터 식별
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
        return result

    except Exception as e:
        # 폴백: 간단한 패턴 카운팅
        error_types = Counter(_extract_error_type(s["message"]) for s in samples)

        clusters = []
        for error_type, count in error_types.most_common(5):
            estimated_total = int(count / sample_size * total_errors) if sample_size > 0 else 0
            percentage = (count / sample_size * 100) if sample_size > 0 else 0

            clusters.append({
                "type": error_type,
                "estimated_count": estimated_total,
                "estimated_percentage": round(percentage, 2),
                "severity": "high" if "null" in error_type.lower() or "memory" in error_type.lower() else "medium",
                "description": f"샘플에서 {count}회 발견됨"
            })

        return {
            "error_clusters": clusters,
            "anomalies": [],
            "confidence": 65,
            "reasoning": f"LLM 분석 실패, 간단한 패턴 매칭 사용 ({str(e)})"
        }


def _calculate_cluster_accuracy(
    db_stats: Dict[str, Any],
    ai_result: Dict[str, Any]
) -> Dict[str, Any]:
    """
    DB 통계와 AI 클러스터링 결과의 정확도 계산

    비교 기준:
    - ERROR 타입별 개수 매칭 정확도
    - 전체 ERROR 수 추정 정확도

    Returns:
        {
            "total_error_accuracy": float,
            "cluster_type_match_score": float,
            "cluster_count_accuracy": float,
            "overall_accuracy": float
        }
    """
    # 1. 전체 ERROR 수 정확도
    db_total = db_stats["total_errors"]
    ai_clusters = ai_result.get("error_clusters", [])
    ai_total = sum(c.get("estimated_count", 0) for c in ai_clusters)

    total_accuracy = max(0, round((1 - abs(db_total - ai_total) / db_total) * 100, 2)) if db_total > 0 else 100

    # 2. 에러 타입 매칭 점수
    db_types = set(db_stats["error_types"].keys())
    ai_types = set(c["type"] for c in ai_clusters)

    type_intersection = len(db_types & ai_types)
    type_union = len(db_types | ai_types)
    type_match_score = round((type_intersection / type_union * 100), 2) if type_union > 0 else 0

    # 3. 각 클러스터 개수 정확도 (매칭된 타입에 한해)
    count_accuracies = []
    for ai_cluster in ai_clusters:
        ai_type = ai_cluster["type"]
        ai_count = ai_cluster.get("estimated_count", 0)

        if ai_type in db_stats["error_types"]:
            db_count = db_stats["error_types"][ai_type]
            if db_count > 0:
                accuracy = max(0, (1 - abs(db_count - ai_count) / db_count) * 100)
                count_accuracies.append(accuracy)

    cluster_count_accuracy = round(sum(count_accuracies) / len(count_accuracies), 2) if count_accuracies else 0

    # 4. 종합 정확도
    overall_accuracy = round(
        total_accuracy * 0.4 +
        type_match_score * 0.3 +
        cluster_count_accuracy * 0.3,
        2
    )

    return {
        "total_error_accuracy": total_accuracy,
        "cluster_type_match_score": type_match_score,
        "cluster_count_accuracy": cluster_count_accuracy,
        "overall_accuracy": overall_accuracy,
        "matched_types": list(db_types & ai_types),
        "missed_types": list(db_types - ai_types),
        "false_positive_types": list(ai_types - db_types)
    }
