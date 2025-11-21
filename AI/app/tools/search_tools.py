"""
검색 도구 (Search Tools)
- 키워드 검색, 의미 유사도 검색
"""

from typing import Optional, List
from datetime import datetime, timedelta
from langchain_core.tools import tool

from app.core.opensearch import opensearch_client
from app.services.similarity_service import similarity_service
from app.services.embedding_service import embedding_service
from app.tools.common_fields import BASE_FIELDS, LOG_DETAILS_FIELDS
from app.utils.sanitizer import sanitize_for_display, log_security_warning


@tool
async def search_logs_by_keyword(
    keyword: str,
    project_uuid: str,
    level: Optional[str] = None,
    service_name: Optional[str] = None,
    time_hours: int = 24
) -> str:
    """
    키워드로 로그 메시지를 검색합니다 (텍스트 매칭).

    이 도구는 다음을 수행합니다:
    - ✅ 특정 키워드가 포함된 로그 검색 (message 필드 대상)
    - ✅ 레벨별, 서비스별 필터링 지원
    - ✅ 최근 20개 로그 반환 (시간 역순)
    - ✅ 레벨별 분포 통계 제공
    - ❌ 의미적 유사도 검색은 하지 않음 (search_logs_by_similarity 사용)
    - ❌ AI 기반 분석은 하지 않음 (analyze_single_log 사용)
    - ❌ 특정 시간대 지정 불가 (search_logs_advanced 사용)

    사용 시나리오:
    1. "NullPointerException이 포함된 로그 찾아줘"
    2. "user-service라는 단어가 들어간 ERROR 로그"
    3. "timeout 문제 검색"

    ⚠️ 중요한 제약사항:
    - "검색 결과가 없습니다" 응답은 **정상 결과**입니다. 다른 도구로 재시도하지 마세요
    - 1회 호출로 충분합니다. 같은 키워드로 반복 검색하지 마세요

    입력 파라미터 (JSON 형식):
        keyword: 검색 키워드 (필수, 예: "NullPointerException")
        level: 로그 레벨 필터 (선택, ERROR/WARN/INFO/DEBUG)
        service_name: 서비스 필터 (선택, 예: "user-service")
        time_hours: 검색 시간 범위 (기본 24시간)

    관련 도구:
    - search_logs_by_similarity: 자연어 질문으로 의미적 유사 로그 검색
    - search_logs_advanced: 커스텀 시간 범위 + 다중 필터
    - get_log_detail: 특정 log_id의 상세 정보 조회

    Returns:
        검색 결과 (건수, 레벨별 분포, 상위 5개 로그)
    """
    # 시간 범위 계산
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)

    time_range = {
        "start": start_time.isoformat() + "Z",
        "end": end_time.isoformat() + "Z"
    }

    # OpenSearch Query 구성
    must_clauses = [
        {"match": {"message": keyword}}
    ]

    if level:
        must_clauses.append({"term": {"level": level.upper()}})

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

    # 인덱스 패턴 (UUID의 하이픈을 언더스코어로 변환)
    index_pattern = f"{project_uuid.replace('-', '_')}_*"

    try:
        # OpenSearch 검색
        results = opensearch_client.search(
            index=index_pattern,
            body={
                "query": query,
                "size": 20,  # 최대 20개
                "sort": [{"timestamp": "desc"}],
                "_source": BASE_FIELDS + LOG_DETAILS_FIELDS + ["ai_analysis.summary"]  # 공통 필드 사용
            }
        )

        hits = results.get("hits", {}).get("hits", [])
        total_count = results.get("hits", {}).get("total", {}).get("value", 0)

        # 보안 경고 로깅
        log_security_warning(keyword, "keyword_search")

        # 응답에 표시할 키워드 위생처리 (XSS/SQL 인젝션 방지)
        safe_keyword = sanitize_for_display(keyword)

        if total_count == 0:
            return f"'{safe_keyword}' 키워드로 검색한 결과 {time_hours}시간 내 로그가 없습니다."

        # 결과 포맷팅
        summary_lines = [
            f"'{safe_keyword}' 검색 결과: 총 {total_count}건 (최근 {time_hours}시간)",
            ""
        ]

        # 레벨별 카운트
        level_counts = {}
        for hit in hits:
            log_level = hit["_source"].get("level", "UNKNOWN")
            level_counts[log_level] = level_counts.get(log_level, 0) + 1

        summary_lines.append(f"레벨별 분포: {', '.join([f'{k}: {v}건' for k, v in level_counts.items()])}")
        summary_lines.append("")

        # 상위 5개 로그
        summary_lines.append("최근 로그 5개:")
        for i, hit in enumerate(hits[:5], 1):
            source = hit["_source"]
            msg = source.get("message", "")[:300]
            level_str = source.get("level", "?")
            timestamp_str = source.get("timestamp", "")[:19]
            service = source.get("service_name", "unknown")
            log_id = source.get("log_id", "")
            layer = source.get("layer", "")
            component = source.get("component_name", "")

            # log_details 접근
            log_details = source.get("log_details", {})
            class_name = log_details.get("class_name", "")
            method_name = log_details.get("method_name", "")
            http_method = log_details.get("http_method", "")
            request_uri = log_details.get("request_uri", "")
            response_status = log_details.get("response_status")

            # AI 분석
            ai_summary = source.get("ai_analysis", {}).get("summary", "")

            # 기본 정보
            summary_lines.append(f"{i}. [{level_str}] {timestamp_str} | {service}")

            # 위치 정보
            if layer:
                summary_lines.append(f"   Layer: {layer}")
            if class_name and method_name:
                summary_lines.append(f"   📍 {class_name}.{method_name}")

            # HTTP 정보
            if http_method and request_uri:
                status_info = f" → {response_status}" if response_status else ""
                summary_lines.append(f"   🌐 {http_method} {request_uri}{status_info}")

            # 메시지
            summary_lines.append(f"   {msg}...")

            # AI 분석 (있는 경우)
            if ai_summary:
                summary_lines.append(f"   🤖 {ai_summary[:150]}")

            # log_id
            if log_id:
                summary_lines.append(f"   (log_id: {log_id})")

        return "\n".join(summary_lines)

    except Exception as e:
        return f"검색 중 오류 발생: {str(e)}"


@tool
async def search_logs_by_similarity(
    query: str,
    project_uuid: str,
    k: int = 5,
    level: Optional[str] = None,
    time_hours: int = 168  # 기본 7일
) -> str:
    """
    의미적 유사도로 로그를 검색합니다 (Vector 임베딩 기반).

    이 도구는 다음을 수행합니다:
    - ✅ 자연어 질문을 임베딩으로 변환하여 의미적 유사 로그 검색
    - ✅ 코사인 유사도 기반 상위 k개 반환
    - ✅ 유사도 점수 제공 (0.0~1.0)
    - ✅ 레벨별 필터링 지원
    - ❌ 정확한 키워드 매칭은 하지 않음 (search_logs_by_keyword 사용)
    - ❌ AI 분석은 하지 않음 (analyze_single_log 사용)
    - ❌ 특정 시간대 지정 불가 (search_logs_advanced 사용)

    사용 시나리오:
    1. "사용자 인증 실패와 유사한 로그 찾아줘"
    2. "데이터베이스 연결 문제 비슷한 상황"
    3. "결제 실패 관련 로그"

    ⚠️ 중요한 제약사항:
    - "유사한 로그를 찾을 수 없습니다" 응답은 **정상 결과**입니다. 다른 도구로 재시도하지 마세요
    - 1회 호출로 충분합니다. 같은 쿼리로 반복 검색하지 마세요
    - 기본 검색 범위는 7일입니다 (키워드 검색보다 넓음)

    입력 파라미터 (JSON 형식):
        query: 자연어 검색 쿼리 (필수, 예: "데이터베이스 연결 문제")
        k: 반환 개수 (기본 5개, 최대 20개)
        level: 로그 레벨 필터 (선택, ERROR/WARN/INFO/DEBUG)
        time_hours: 검색 시간 범위 (기본 168시간=7일)

    관련 도구:
    - search_logs_by_keyword: 정확한 키워드 매칭 검색
    - search_logs_advanced: 커스텀 시간 범위 + 다중 필터
    - analyze_single_log: 특정 log_id AI 분석

    Returns:
        유사도순 로그 목록 (유사도 점수, 상세 정보 포함)
    """
    try:
        # 쿼리 임베딩
        query_vector = await embedding_service.embed_query(query)

        # 시간 범위 계산
        end_time = datetime.utcnow()
        start_time = end_time - timedelta(hours=time_hours)

        time_range = {
            "start": start_time.isoformat() + "Z",
            "end": end_time.isoformat() + "Z"
        }

        # 필터 구성
        filters = {}
        if level:
            filters["level"] = level.upper()

        # 유사도 검색 (기존 similarity_service 활용)
        results = await similarity_service.find_similar_logs(
            log_vector=query_vector,
            k=k,
            filters=filters,
            project_uuid=project_uuid,
            time_range=time_range
        )

        # 보안 경고 로깅 및 위생처리
        log_security_warning(query, "similarity_search")
        safe_query = sanitize_for_display(query)

        if not results:
            return f"'{safe_query}' 쿼리로 검색한 결과 {time_hours}시간 내 유사한 로그가 없습니다."

        # 결과 포맷팅
        summary_lines = [
            f"'{safe_query}' 유사도 검색 결과: {len(results)}건 (최근 {time_hours}시간)",
            ""
        ]

        for i, result in enumerate(results, 1):
            log_data = result.get("data", {})
            score = result.get("score", 0.0)

            msg = log_data.get("message", "")[:200]
            level_str = log_data.get("level", "?")
            timestamp_str = log_data.get("timestamp", "")[:19]
            service = log_data.get("service_name", "unknown")
            log_id = log_data.get("log_id", "")
            layer = log_data.get("layer", "")

            # log_details 접근
            log_details = log_data.get("log_details", {})
            class_name = log_details.get("class_name", "")
            method_name = log_details.get("method_name", "")
            http_method = log_details.get("http_method", "")
            request_uri = log_details.get("request_uri", "")
            response_status = log_details.get("response_status")

            # AI 분석
            ai_summary = log_data.get("ai_analysis", {}).get("summary", "")

            # 기본 정보
            summary_lines.append(f"{i}. [{level_str}] {timestamp_str} | 유사도: {score:.3f}")
            summary_lines.append(f"   서비스: {service}")

            # 위치 정보
            if layer:
                summary_lines.append(f"   Layer: {layer}")
            if class_name and method_name:
                summary_lines.append(f"   📍 {class_name}.{method_name}")

            # HTTP 정보
            if http_method and request_uri:
                status_info = f" → {response_status}" if response_status else ""
                summary_lines.append(f"   🌐 {http_method} {request_uri}{status_info}")

            # 메시지
            summary_lines.append(f"   메시지: {msg}...")

            # AI 분석 (있는 경우)
            if ai_summary:
                summary_lines.append(f"   🤖 {ai_summary[:150]}")

            # log_id
            if log_id:
                summary_lines.append(f"   (log_id: {log_id})")
            summary_lines.append("")

        return "\n".join(summary_lines)

    except Exception as e:
        return f"유사도 검색 중 오류 발생: {str(e)}"


@tool
async def search_logs_advanced(
    project_uuid: str,
    start_time: Optional[str] = None,
    end_time: Optional[str] = None,
    service_name: Optional[str] = None,
    level: Optional[str] = None,
    keyword: Optional[str] = None,
    limit: int = 50
) -> str:
    """
    고급 로그 검색 (커스텀 시간 범위 + 다중 필터 조합).

    이 도구는 다음을 수행합니다:
    - ✅ 정확한 날짜/시간 범위 지정 가능 (ISO 8601 형식)
    - ✅ 여러 조건 동시 필터링 (서비스, 레벨, 키워드)
    - ✅ 최대 50개 로그 반환
    - ✅ 조건별 카운트 통계 제공
    - ❌ 의미적 유사도 검색은 하지 않음 (search_logs_by_similarity 사용)
    - ❌ AI 분석은 하지 않음 (analyze_single_log 사용)
    - ❌ 상대적 시간 표현 불가 ("최근 24시간"은 time_hours 파라미터를 쓰는 다른 도구 사용)

    사용 시나리오:
    1. "2025-11-05 14:00부터 16:00까지 payment-service ERROR 로그"
    2. "어제 오후 2시~4시 사이 DatabaseTimeout 검색"
    3. "특정 배포 시점 전후 로그 비교"

    ⚠️ 중요한 제약사항:
    - 시간 형식은 ISO 8601 준수 ("2025-11-05T14:00:00" 또는 "2025-11-05")
    - "검색 결과가 없습니다" 응답은 **정상 결과**입니다
    - 1회 호출로 충분합니다

    입력 파라미터 (JSON 형식):
        start_time: 시작 시간 (ISO 형식, 예: "2025-11-05T14:00:00")
        end_time: 종료 시간 (ISO 형식, 예: "2025-11-05T16:00:00")
        service_name: 서비스 필터 (선택)
        level: 로그 레벨 (선택, ERROR/WARN/INFO/DEBUG)
        keyword: 키워드 필터 (선택)
        limit: 최대 결과 수 (기본 50)

    관련 도구:
    - search_logs_by_keyword: 상대적 시간 (time_hours) 사용 검색
    - compare_time_periods: 두 시간대 비교 분석
    - analyze_deployment_impact: 배포 전후 영향 분석

    Returns:
        검색 조건 요약, 결과 목록 (최대 50개)
    """
    try:
        from datetime import datetime, timezone
        from app.core.opensearch import opensearch_client

        client = opensearch_client
        index_name = f"logs_{project_uuid}"

        # 쿼리 구성
        must_conditions = []

        # 시간 범위
        if start_time or end_time:
            time_range = {}
            if start_time:
                # ISO 8601 파싱
                if 'T' not in start_time:
                    start_time += 'T00:00:00'
                time_range['gte'] = start_time
            if end_time:
                if 'T' not in end_time:
                    end_time += 'T23:59:59'
                time_range['lte'] = end_time

            must_conditions.append({
                "range": {
                    "timestamp": time_range
                }
            })

        # 서비스 필터
        if service_name:
            must_conditions.append({
                "term": {"service_name.keyword": service_name}
            })

        # 레벨 필터
        if level:
            must_conditions.append({
                "term": {"level.keyword": level.upper()}
            })

        # 키워드 검색
        if keyword:
            must_conditions.append({
                "match": {"message": keyword}
            })

        # 쿼리 실행
        query = {
            "bool": {
                "must": must_conditions if must_conditions else [{"match_all": {}}]
            }
        }

        response = client.search(
            index=index_name,
            body={
                "query": query,
                "sort": [{"timestamp": "desc"}],
                "size": limit
            }
        )

        hits = response['hits']['hits']
        total_count = response['hits']['total']['value']

        # 키워드 위생처리 (보안)
        if keyword:
            log_security_warning(keyword, "advanced_search")
        safe_keyword = sanitize_for_display(keyword) if keyword else None

        if total_count == 0:
            conditions_desc = []
            if start_time: conditions_desc.append(f"시작: {start_time}")
            if end_time: conditions_desc.append(f"종료: {end_time}")
            if service_name: conditions_desc.append(f"서비스: {service_name}")
            if level: conditions_desc.append(f"레벨: {level}")
            if safe_keyword: conditions_desc.append(f"키워드: {safe_keyword}")

            return f"=== 고급 검색 결과 ===\n\n조건: {', '.join(conditions_desc) if conditions_desc else '전체'}\n\n검색 결과가 없습니다."

        # 결과 포맷팅
        summary_lines = ["=== 고급 로그 검색 결과 ===", ""]

        # 검색 조건 요약
        conditions = []
        if start_time: conditions.append(f"📅 시작: {start_time}")
        if end_time: conditions.append(f"📅 종료: {end_time}")
        if service_name: conditions.append(f"🔧 서비스: {service_name}")
        if level: conditions.append(f"📊 레벨: {level}")
        if safe_keyword: conditions.append(f"🔍 키워드: {safe_keyword}")

        summary_lines.append("**검색 조건:**")
        summary_lines.extend([f"- {cond}" for cond in conditions])
        summary_lines.append("")
        summary_lines.append(f"**결과:** 총 {total_count}건 중 상위 {len(hits)}건 표시")
        summary_lines.append("")

        # 로그 목록
        for i, hit in enumerate(hits, 1):
            source = hit['_source']
            log_id = hit.get('_id')
            timestamp = source.get('timestamp', 'N/A')
            level_val = source.get('level', 'INFO')
            service = source.get('service_name', 'unknown')
            message = source.get('message', '')[:200]

            summary_lines.append(f"{i}. [{level_val}] {timestamp}")
            summary_lines.append(f"   🔧 {service}")
            summary_lines.append(f"   💬 {message}")
            if log_id:
                summary_lines.append(f"   (log_id: {log_id})")
            summary_lines.append("")

        return "\n".join(summary_lines)

    except Exception as e:
        return f"고급 검색 중 오류 발생: {str(e)}"
