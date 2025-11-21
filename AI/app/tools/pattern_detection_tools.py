"""
패턴 탐지 도구 (Pattern Detection Tools)
- 스택 트레이스 클러스터링, 동시성 이슈, 재발 주기, 에러 생존 시간
"""

from typing import Optional, List, Dict, Any
from datetime import datetime, timedelta
from collections import Counter, defaultdict
from langchain_core.tools import tool
import re

from app.core.opensearch import opensearch_client
from app.tools.common_fields import BASE_FIELDS, LOG_DETAILS_FIELDS


def calculate_text_similarity(text1: str, text2: str) -> float:
    """
    두 텍스트의 단순 유사도 계산 (Jaccard similarity)

    Returns:
        0.0 ~ 1.0 사이의 유사도 점수
    """
    if not text1 or not text2:
        return 0.0

    # 단어 단위로 분리 (개행, 공백, 특수문자)
    words1 = set(re.findall(r'\w+', text1.lower()))
    words2 = set(re.findall(r'\w+', text2.lower()))

    if not words1 or not words2:
        return 0.0

    # Jaccard similarity
    intersection = len(words1 & words2)
    union = len(words1 | words2)

    return intersection / union if union > 0 else 0.0


@tool
async def cluster_stack_traces(
    project_uuid: str,
    exception_type: Optional[str] = None,
    time_hours: int = 168,
    similarity_threshold: float = 0.8
) -> str:
    """
    유사한 스택 트레이스를 클러스터링하여 근본 원인 그룹을 찾습니다.

    이 도구는 다음을 수행합니다:
    - ✅ stack_trace 텍스트 유사도 계산 (Jaccard similarity)
    - ✅ 같은 exception_type + 유사 stack_trace 그룹핑
    - ✅ 각 클러스터의 대표 에러 선정
    - ✅ 발생 빈도 및 서비스 분포 분석
    - ❌ 복잡한 ML 클러스터링은 미사용 (단순 유사도 기반)

    사용 시나리오:
    1. "비슷한 에러끼리 묶어줘"
    2. "NullPointerException의 패턴 분석"
    3. "같은 원인의 에러들"
    4. "중복 에러 제거"

    ⚠️ 중요한 제약사항:
    - 1회 호출로 충분합니다
    - stack_trace가 없는 로그는 제외됩니다
    - 기본 분석 기간은 7일입니다

    입력 파라미터 (JSON 형식):
        exception_type: 예외 타입 필터 (선택, 예: "NullPointerException")
        time_hours: 분석 기간 (기본 168시간=7일)
        similarity_threshold: 유사도 임계값 (기본 0.8)

    관련 도구:
    - get_error_frequency_ranking: 에러 타입별 빈도
    - analyze_errors_unified: 에러 통합 분석
    - get_recent_errors: 최근 에러 목록

    Returns:
        스택 트레이스 클러스터 (그룹별 대표 에러, 빈도)
    """
    # 인덱스 패턴
    index_pattern = f"{project_uuid.replace('-', '_')}_*"

    # 시간 범위 계산
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)

    # Query 구성
    must_clauses = [
        {"term": {"level": "ERROR"}},
        {"exists": {"field": "stack_trace"}},
        {"range": {
            "timestamp": {
                "gte": start_time.isoformat() + "Z",
                "lte": end_time.isoformat() + "Z"
            }
        }}
    ]

    if exception_type:
        must_clauses.append({
            "bool": {
                "should": [
                    {"term": {"log_details.exception_type": exception_type}},
                    {"match": {"message": exception_type}}
                ],
                "minimum_should_match": 1
            }
        })

    try:
        # OpenSearch 검색
        results = opensearch_client.search(
            index=index_pattern,
            body={
                "size": 100,  # 클러스터링용 샘플
                "query": {"bool": {"must": must_clauses}},
                "sort": [{"timestamp": "desc"}],
                "_source": BASE_FIELDS + LOG_DETAILS_FIELDS
            }
        )

        hits = results.get("hits", {}).get("hits", [])
        total_count = results.get("hits", {}).get("total", {}).get("value", 0)

        if total_count == 0:
            from app.tools.response_templates import get_empty_result_message

            conditions = ["stack_trace 필드 존재"]
            if exception_type:
                conditions.append(f"예외 타입 = {exception_type}")

            suggestions = [
                "stack_trace 필드가 로그에 포함되어 있는지 확인하세요",
                "exception_type 필터를 제거해보세요" if exception_type else "필터 없이 전체 조회",
                "시간 범위를 늘려보세요 (time_hours=336 for 14일)"
            ]

            return get_empty_result_message(
                conditions=", ".join(conditions),
                time_hours=time_hours,
                suggestions=suggestions
            ) + "\n\n⚠️ similarity_threshold를 조정해서 재호출하지 마세요."

        # 클러스터링
        clusters = []  # List[Dict]
        processed_indices = set()

        for i, hit in enumerate(hits):
            if i in processed_indices:
                continue

            source = hit["_source"]
            stack_trace = source.get("stack_trace", "")
            log_details = source.get("log_details", {})
            exc_type = log_details.get("exception_type", "Unknown")

            # 새 클러스터 생성
            cluster = {
                "exception_type": exc_type,
                "representative": source,
                "members": [source],
                "log_ids": [source.get("log_id")]
            }

            processed_indices.add(i)

            # 나머지 로그와 유사도 비교
            for j, other_hit in enumerate(hits[i+1:], start=i+1):
                if j in processed_indices:
                    continue

                other_source = other_hit["_source"]
                other_stack = other_source.get("stack_trace", "")
                other_exc = other_source.get("log_details", {}).get("exception_type", "Unknown")

                # 같은 예외 타입만 비교
                if exc_type != other_exc:
                    continue

                # 유사도 계산
                similarity = calculate_text_similarity(stack_trace, other_stack)

                if similarity >= similarity_threshold:
                    cluster["members"].append(other_source)
                    cluster["log_ids"].append(other_source.get("log_id"))
                    processed_indices.add(j)

            clusters.append(cluster)

        # 빈도 순으로 정렬
        clusters.sort(key=lambda x: len(x["members"]), reverse=True)

        # 결과 포맷팅
        filter_suffix = f" ({exception_type})" if exception_type else ""
        lines = [
            f"## 🔍 스택 트레이스 클러스터 분석{filter_suffix}",
            f"**기간:** 최근 {time_hours}시간",
            f"**분석 샘플:** {len(hits)}건",
            f"**발견 클러스터:** {len(clusters)}개",
            f"**유사도 임계값:** {similarity_threshold}",
            ""
        ]

        # 클러스터별 상세 정보
        for i, cluster in enumerate(clusters[:10], 1):  # 상위 10개만
            rep = cluster["representative"]
            member_count = len(cluster["members"])
            exc_type = cluster["exception_type"]

            lines.append(f"### 그룹 {i}: {exc_type} ({member_count}건)")
            lines.append("")

            # 대표 에러 정보
            class_name = rep.get("class_name", "")
            method_name = rep.get("method_name", "")
            service = rep.get("service_name", "unknown")
            message = rep.get("message", "")[:200]

            lines.append(f"**발생 위치:** {service}")
            if class_name and method_name:
                lines.append(f"**메서드:** {class_name}.{method_name}")

            lines.append(f"**메시지:** {message}")

            # 서비스 분포
            services = [m.get("service_name", "unknown") for m in cluster["members"]]
            service_counts = Counter(services)
            if len(service_counts) > 1:
                lines.append(f"**서비스 분포:** {', '.join([f'{s}({c})' for s, c in service_counts.most_common(3)])}")

            # 스택 트레이스 샘플 (간략)
            stack_trace = rep.get("stack_trace", "")
            stack_lines = stack_trace.split("\n")

            if stack_lines:
                lines.append("")
                lines.append("**스택 트레이스 샘플 (처음 5줄):**")
                lines.append("```")
                for line in stack_lines[:5]:
                    if line.strip():
                        lines.append(line.strip())
                lines.append("...")
                lines.append("```")

            # log_id 목록 (처음 5개)
            lines.append("")
            lines.append(f"**포함 log_id:** {', '.join([str(lid) for lid in cluster['log_ids'][:5]])}")
            if member_count > 5:
                lines.append(f"(외 {member_count - 5}건)")

            lines.append("")

        # 요약
        lines.append("### 💡 클러스터링 요약")
        lines.append("")

        total_errors = sum([len(c["members"]) for c in clusters])
        lines.append(f"- **총 에러:** {total_errors}건")
        lines.append(f"- **클러스터 수:** {len(clusters)}개")

        if clusters:
            largest_cluster = clusters[0]
            largest_percentage = (len(largest_cluster["members"]) / total_errors * 100)
            lines.append(f"- **최대 클러스터:** {largest_cluster['exception_type']} ({len(largest_cluster['members'])}건, {largest_percentage:.1f}%)")

        # 중복 에러 통계
        single_occurrence = len([c for c in clusters if len(c["members"]) == 1])
        multiple_occurrence = len(clusters) - single_occurrence

        lines.append(f"- **반복 에러 패턴:** {multiple_occurrence}개 (클러스터 크기 2+)")
        lines.append(f"- **단독 에러:** {single_occurrence}개")

        # 권장 조치
        lines.append("")
        lines.append("### ✅ 권장 조치")
        lines.append("")

        if clusters:
            top_cluster = clusters[0]
            lines.append(f"1. **{top_cluster['exception_type']} 우선 수정** ({len(top_cluster['members'])}건으로 가장 많음)")

            if len(top_cluster["members"]) > 5:
                lines.append(f"   → 같은 원인으로 반복 발생 중, 근본 원인 파악 필요")

        if multiple_occurrence > 0:
            lines.append(f"2. **반복 패턴 {multiple_occurrence}개 집중 분석** (효율적 수정 가능)")

        if single_occurrence > 10:
            lines.append(f"3. **단독 에러 {single_occurrence}개 모니터링** (재발 시 패턴 확인)")

        return "\n".join(lines)

    except Exception as e:
        return f"스택 트레이스 클러스터링 중 오류 발생: {str(e)}"


@tool
async def detect_concurrency_issues(
    project_uuid: str,
    time_hours: int = 24,
    min_thread_errors: int = 3
) -> str:
    """
    스레드 이름 패턴으로 동시성 이슈를 탐지합니다.

    이 도구는 다음을 수행합니다:
    - ✅ thread_name으로 그룹핑
    - ✅ 같은 스레드에서 짧은 시간 내 다수 에러 탐지
    - ✅ exception_type에 "Deadlock", "Timeout", "Lock" 포함 시 경고
    - ✅ 스레드 풀 고갈 패턴 식별
    - ❌ 실제 데드락 분석은 JVM 프로파일링 필요

    사용 시나리오:
    1. "동시성 문제 있어?"
    2. "같은 스레드에서 반복 에러"
    3. "데드락 의심 패턴"
    4. "스레드 풀 이슈 탐지"

    ⚠️ 중요한 제약사항:
    - 1회 호출로 충분합니다
    - thread_name이 없는 로그는 제외됩니다
    - 기본 분석 기간은 24시간입니다

    입력 파라미터 (JSON 형식):
        time_hours: 분석 기간 (기본 24시간)
        min_thread_errors: 의심 스레드 최소 에러 수 (기본 3건)

    관련 도구:
    - detect_resource_issues: 리소스 이슈 (메모리, CPU)
    - analyze_error_by_layer: 레이어별 에러 분석

    Returns:
        동시성 이슈 패턴, 의심 스레드 목록
    """
    # 인덱스 패턴
    index_pattern = f"{project_uuid.replace('-', '_')}_*"

    # 시간 범위 계산
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)

    try:
        # OpenSearch 검색
        results = opensearch_client.search(
            index=index_pattern,
            body={
                "size": 200,  # 동시성 이슈 분석용
                "query": {
                    "bool": {
                        "must": [
                            {"term": {"level": "ERROR"}},
                            {"exists": {"field": "thread_name"}},
                            {"range": {
                                "timestamp": {
                                    "gte": start_time.isoformat() + "Z",
                                    "lte": end_time.isoformat() + "Z"
                                }
                            }}
                        ]
                    }
                },
                "sort": [{"timestamp": "asc"}],
                "_source": BASE_FIELDS + LOG_DETAILS_FIELDS
            }
        )

        hits = results.get("hits", {}).get("hits", [])

        if not hits:
            from app.tools.response_templates import get_no_problem_message
            return get_no_problem_message(
                analysis_type="동시성 문제",
                time_hours=time_hours,
                note="로그에 thread_name 필드가 없으면 이 분석을 수행할 수 없습니다"
            )

        # 스레드별 에러 그룹핑
        thread_errors = defaultdict(list)  # {thread_name: [error_logs]}

        for hit in hits:
            source = hit["_source"]
            thread_name = source.get("thread_name", "unknown")
            thread_errors[thread_name].append(source)

        # 의심 스레드 필터링
        suspicious_threads = {}

        for thread, errors in thread_errors.items():
            if len(errors) < min_thread_errors:
                continue

            # 시간 간격 분석 (짧은 시간 내 다수 에러 = 의심)
            timestamps = [e.get("timestamp", "") for e in errors]
            timestamps_dt = []

            for ts in timestamps:
                try:
                    timestamps_dt.append(datetime.fromisoformat(ts.replace("Z", "")))
                except:
                    pass

            if len(timestamps_dt) < 2:
                continue

            # 평균 간격 계산
            intervals = []
            for i in range(len(timestamps_dt) - 1):
                interval = (timestamps_dt[i+1] - timestamps_dt[i]).total_seconds()
                intervals.append(interval)

            avg_interval = sum(intervals) / len(intervals) if intervals else 0

            # 동시성 키워드 탐지
            concurrency_keywords = ["deadlock", "timeout", "lock", "concurrent", "pool"]
            keyword_matches = []

            for error in errors:
                message = error.get("message", "").lower()
                exc_type = error.get("log_details", {}).get("exception_type", "").lower()

                for keyword in concurrency_keywords:
                    if keyword in message or keyword in exc_type:
                        keyword_matches.append(keyword)

            suspicious_threads[thread] = {
                "error_count": len(errors),
                "avg_interval": avg_interval,
                "keywords": list(set(keyword_matches)),
                "errors": errors
            }

        # 결과 포맷팅
        lines = [
            f"## 🧵 동시성 이슈 탐지",
            f"**기간:** 최근 {time_hours}시간",
            f"**분석 스레드:** {len(thread_errors)}개",
            f"**의심 스레드:** {len(suspicious_threads)}개",
            ""
        ]

        if not suspicious_threads:
            lines.append("### 🟢 발견된 동시성 이슈 없음")
            lines.append("")
            lines.append("최근 기간 동안 동시성 관련 문제 패턴이 탐지되지 않았습니다.")
            return "\n".join(lines)

        # 의심 스레드 상세 정보
        lines.append("### 🚨 의심 스레드 목록")
        lines.append("")

        # 에러 수 기준 정렬
        sorted_threads = sorted(suspicious_threads.items(), key=lambda x: x[1]["error_count"], reverse=True)

        for i, (thread_name, info) in enumerate(sorted_threads[:10], 1):
            error_count = info["error_count"]
            avg_interval = info["avg_interval"]
            keywords = info["keywords"]

            lines.append(f"#### {i}. {thread_name}")
            lines.append(f"- **에러 수:** {error_count}건")
            lines.append(f"- **평균 간격:** {avg_interval:.1f}초")

            if keywords:
                lines.append(f"- **🔴 동시성 키워드 탐지:** {', '.join(keywords)}")

            # 에러 패턴 판정
            if avg_interval < 5 and error_count >= 3:
                lines.append(f"- **패턴:** 🔴 짧은 시간 내 반복 에러 (동시성 이슈 의심)")
            elif "deadlock" in keywords or "lock" in keywords:
                lines.append(f"- **패턴:** 🔴 데드락 또는 락 경합 의심")
            elif "timeout" in keywords or "pool" in keywords:
                lines.append(f"- **패턴:** 🟡 리소스 고갈 또는 타임아웃")

            # 대표 에러 메시지
            first_error = info["errors"][0]
            message = first_error.get("message", "")[:150]
            lines.append(f"- **에러 예시:** {message}")

            lines.append("")

        # 전체 통계
        lines.append("### 📊 통계 요약")
        lines.append("")

        total_suspicious_errors = sum([t["error_count"] for t in suspicious_threads.values()])
        lines.append(f"- **의심 스레드 총 에러:** {total_suspicious_errors}건")

        # 키워드 통계
        all_keywords = []
        for info in suspicious_threads.values():
            all_keywords.extend(info["keywords"])

        if all_keywords:
            keyword_counts = Counter(all_keywords)
            lines.append(f"- **가장 많은 키워드:** {', '.join([f'{k}({c})' for k, c in keyword_counts.most_common(3)])}")

        # 권장 조치
        lines.append("")
        lines.append("### ✅ 권장 조치")
        lines.append("")

        if "deadlock" in all_keywords or "lock" in all_keywords:
            lines.append("1. **데드락 분석:** JVM Thread Dump 수집 및 분석")
            lines.append("2. **락 경합 해소:** synchronized 블록 최소화, Lock 타임아웃 설정")

        if "timeout" in all_keywords or "pool" in all_keywords:
            lines.append("3. **커넥션 풀 증설:** DB 커넥션 풀, 스레드 풀 크기 증가")
            lines.append("4. **타임아웃 조정:** 적절한 타임아웃 설정")

        if sorted_threads:
            worst_thread = sorted_threads[0][0]
            lines.append(f"5. **{worst_thread} 스레드 집중 분석:** 코드 리뷰 및 프로파일링")

        lines.append("")
        lines.append("💡 **추가 분석:** detect_resource_issues 도구로 메모리/CPU 이슈 확인")

        return "\n".join(lines)

    except Exception as e:
        return f"동시성 이슈 탐지 중 오류 발생: {str(e)}"


@tool
async def detect_recurring_errors(
    project_uuid: str,
    time_hours: int = 720,  # 30일
    min_occurrences: int = 3
) -> str:
    """
    주기적으로 반복되는 에러를 탐지합니다 (매일 같은 시간, 매주 특정 요일).

    이 도구는 다음을 수행합니다:
    - ✅ exception_type + class_name으로 에러 그룹핑
    - ✅ 발생 시간 패턴 분석 (시간대, 요일)
    - ✅ 주기성 탐지 (매일, 매주, 특정 시간대)
    - ✅ 배치 작업 또는 스케줄러 이슈 식별
    - ❌ 복잡한 통계 분석은 미지원

    사용 시나리오:
    1. "주기적으로 발생하는 에러 있어?"
    2. "매일 같은 시간에 발생하는 에러"
    3. "배치 작업 에러 패턴"
    4. "스케줄러 이슈 탐지"

    ⚠️ 중요한 제약사항:
    - 1회 호출로 충분합니다
    - 기본 분석 기간은 30일입니다 (패턴 발견에 필요)
    - exception_type이 없으면 정확도 낮음

    입력 파라미터 (JSON 형식):
        time_hours: 분석 기간 (기본 720시간=30일)
        min_occurrences: 패턴 인식 최소 발생 횟수 (기본 3회)

    관련 도구:
    - analyze_error_lifetime: 에러 생존 시간
    - cluster_stack_traces: 스택 트레이스 클러스터링

    Returns:
        재발 주기 패턴, 스케줄 기반 에러 목록
    """
    # 인덱스 패턴
    index_pattern = f"{project_uuid.replace('-', '_')}_*"

    # 시간 범위 계산
    end_time = datetime.utcnow()
    start_time = end_time - timedelta(hours=time_hours)

    try:
        # OpenSearch 검색
        results = opensearch_client.search(
            index=index_pattern,
            body={
                "size": 500,  # 주기 분석용 충분한 샘플
                "query": {
                    "bool": {
                        "must": [
                            {"term": {"level": "ERROR"}},
                            {"range": {
                                "timestamp": {
                                    "gte": start_time.isoformat() + "Z",
                                    "lte": end_time.isoformat() + "Z"
                                }
                            }}
                        ]
                    }
                },
                "sort": [{"timestamp": "asc"}],
                "_source": BASE_FIELDS + LOG_DETAILS_FIELDS
            }
        )

        hits = results.get("hits", {}).get("hits", [])

        if not hits:
            from app.tools.response_templates import get_no_problem_message
            return get_no_problem_message(
                analysis_type="에러",
                time_hours=time_hours,
                note="시스템이 안정적으로 운영되고 있습니다"
            ) + "\n\n⚠️ min_occurrences를 조정해서 재호출하지 마세요."

        # 에러 그룹핑 (exception_type + class_name)
        error_groups = defaultdict(list)  # {error_key: [timestamps]}

        for hit in hits:
            source = hit["_source"]
            log_details = source.get("log_details", {})
            exc_type = log_details.get("exception_type", "Unknown")
            class_name = source.get("class_name", "Unknown")
            timestamp = source.get("timestamp", "")

            error_key = f"{exc_type} @ {class_name}"

            try:
                dt = datetime.fromisoformat(timestamp.replace("Z", ""))
                error_groups[error_key].append(dt)
            except:
                pass

        # 패턴 분석
        recurring_patterns = []

        for error_key, timestamps in error_groups.items():
            if len(timestamps) < min_occurrences:
                continue

            # 시간대 분석 (hour of day)
            hours = [dt.hour for dt in timestamps]
            hour_counts = Counter(hours)

            # 요일 분석 (0=Monday, 6=Sunday)
            weekdays = [dt.weekday() for dt in timestamps]
            weekday_counts = Counter(weekdays)

            # 주기성 판정
            patterns_found = []

            # 1. 매일 같은 시간 (특정 시간대에 80% 이상 집중)
            most_common_hour = hour_counts.most_common(1)[0]
            if most_common_hour[1] / len(hours) >= 0.8:
                patterns_found.append(f"매일 {most_common_hour[0]:02d}:00 경")

            # 2. 특정 요일 (특정 요일에 80% 이상 집중)
            most_common_weekday = weekday_counts.most_common(1)[0]
            weekday_names = ["월", "화", "수", "목", "금", "토", "일"]
            if most_common_weekday[1] / len(weekdays) >= 0.8:
                patterns_found.append(f"매주 {weekday_names[most_common_weekday[0]]}요일")

            # 3. 시간 간격 분석 (동일 간격 반복)
            if len(timestamps) >= 3:
                intervals = []
                for i in range(len(timestamps) - 1):
                    interval_hours = (timestamps[i+1] - timestamps[i]).total_seconds() / 3600
                    intervals.append(interval_hours)

                # 평균 간격
                avg_interval = sum(intervals) / len(intervals) if intervals else 0

                # 간격이 일정한지 (표준편차 확인)
                if intervals:
                    variance = sum([(x - avg_interval) ** 2 for x in intervals]) / len(intervals)
                    std_dev = variance ** 0.5

                    # 간격이 비교적 일정 (표준편차가 평균의 20% 이하)
                    if avg_interval > 0 and std_dev / avg_interval < 0.2:
                        if 23 <= avg_interval <= 25:  # 하루
                            patterns_found.append(f"매일 (24시간 간격)")
                        elif 167 <= avg_interval <= 169:  # 일주일
                            patterns_found.append(f"매주 (7일 간격)")
                        else:
                            patterns_found.append(f"일정 간격 ({avg_interval:.1f}시간)")

            if patterns_found:
                recurring_patterns.append({
                    "error_key": error_key,
                    "count": len(timestamps),
                    "patterns": patterns_found,
                    "first_occurrence": min(timestamps),
                    "last_occurrence": max(timestamps),
                    "hour_distribution": hour_counts.most_common(3),
                    "weekday_distribution": weekday_counts.most_common(3)
                })

        # 결과 포맷팅
        lines = [
            f"## 🔁 재발 주기 에러 분석",
            f"**기간:** 최근 {time_hours}시간 ({time_hours//24}일)",
            f"**분석 에러:** {len(error_groups)}개 타입",
            f"**주기 패턴 발견:** {len(recurring_patterns)}개",
            ""
        ]

        if not recurring_patterns:
            lines.append("### 🟢 주기적 에러 패턴 없음")
            lines.append("")
            lines.append("분석 기간 동안 특정 주기로 반복되는 에러가 발견되지 않았습니다.")
            lines.append("")
            lines.append("💡 **참고:** 더 긴 기간 분석을 위해 time_hours를 증가시켜 보세요 (예: 1440=60일)")
            return "\n".join(lines)

        # 패턴별 상세 정보
        lines.append("### 🚨 주기적 에러 목록")
        lines.append("")

        # 발생 횟수 기준 정렬
        sorted_patterns = sorted(recurring_patterns, key=lambda x: x["count"], reverse=True)

        for i, pattern in enumerate(sorted_patterns[:10], 1):
            error_key = pattern["error_key"]
            count = pattern["count"]
            patterns = pattern["patterns"]
            first = pattern["first_occurrence"]
            last = pattern["last_occurrence"]

            lines.append(f"#### {i}. {error_key}")
            lines.append(f"- **발생 횟수:** {count}회")
            lines.append(f"- **기간:** {first.strftime('%Y-%m-%d')} ~ {last.strftime('%Y-%m-%d')}")
            lines.append(f"- **🔁 주기 패턴:** {', '.join(patterns)}")

            # 시간대 분포
            hour_dist = pattern["hour_distribution"]
            hour_str = ", ".join([f"{h:02d}시({c}회)" for h, c in hour_dist])
            lines.append(f"- **주요 시간대:** {hour_str}")

            # 요일 분포
            weekday_dist = pattern["weekday_distribution"]
            weekday_names = ["월", "화", "수", "목", "금", "토", "일"]
            weekday_str = ", ".join([f"{weekday_names[w]}요일({c}회)" for w, c in weekday_dist])
            lines.append(f"- **주요 요일:** {weekday_str}")

            lines.append("")

        # 통계 요약
        lines.append("### 📊 주기 패턴 통계")
        lines.append("")

        # 패턴 타입별 집계
        all_pattern_types = []
        for p in recurring_patterns:
            all_pattern_types.extend(p["patterns"])

        pattern_type_counts = Counter(all_pattern_types)
        lines.append("**주기 타입 분포:**")
        for pattern_type, count in pattern_type_counts.most_common():
            lines.append(f"- {pattern_type}: {count}개 에러")

        lines.append("")

        # 권장 조치
        lines.append("### ✅ 권장 조치")
        lines.append("")

        # 배치 작업 의심
        batch_patterns = [p for p in recurring_patterns if any("매일" in pat or "매주" in pat for pat in p["patterns"])]
        if batch_patterns:
            lines.append("1. **배치 작업 점검:**")
            for p in batch_patterns[:3]:
                lines.append(f"   - {p['error_key']}: {', '.join(p['patterns'])}")

        # 스케줄러 설정 확인
        lines.append("2. **스케줄러 설정 확인:** Cron 표현식 및 실행 조건 검토")
        lines.append("3. **리소스 가용성:** 해당 시간대의 시스템 리소스 확인")

        if sorted_patterns:
            worst_pattern = sorted_patterns[0]
            lines.append(f"4. **우선 수정 대상:** {worst_pattern['error_key']} ({worst_pattern['count']}회 발생)")

        lines.append("")
        lines.append("💡 **추가 분석:** get_traffic_by_time 도구로 해당 시간대 트래픽 확인")

        return "\n".join(lines)

    except Exception as e:
        return f"재발 주기 에러 탐지 중 오류 발생: {str(e)}"


@tool
async def analyze_error_lifetime(
    project_uuid: str,
    exception_type: Optional[str] = None,
    time_hours: int = 720  # 30일
) -> str:
    """
    동일 에러의 최초 발생 ~ 해결 시점까지 시간을 분석합니다.

    이 도구는 다음을 수행합니다:
    - ✅ exception_type + class_name + method_name으로 에러 그룹핑
    - ✅ 각 에러의 최초 발생 시간 ~ 마지막 발생 시간 계산
    - ✅ 7일 이상 지속 → "장기 미해결"
    - ✅ 최근 24시간 미발생 → "해결됨"
    - ❌ 실제 코드 수정 여부는 알 수 없음 (로그 기반 추정)

    사용 시나리오:
    1. "이 에러가 언제부터 발생했어?"
    2. "얼마나 오래 지속된 문제야?"
    3. "해결되지 않은 오래된 에러"
    4. "에러 해결 속도 평가"

    ⚠️ 중요한 제약사항:
    - 1회 호출로 충분합니다
    - 기본 분석 기간은 30일입니다
    - "해결됨" 판정은 로그 부재 기준 (실제 수정과 다를 수 있음)

    입력 파라미터 (JSON 형식):
        exception_type: 예외 타입 필터 (선택)
        time_hours: 분석 기간 (기본 720시간=30일)

    관련 도구:
    - detect_recurring_errors: 재발 주기 분석
    - cluster_stack_traces: 스택 트레이스 클러스터링

    Returns:
        에러 생존 시간, 장기 미해결 에러, 해결된 에러
    """
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

    if exception_type:
        must_clauses.append({
            "bool": {
                "should": [
                    {"term": {"log_details.exception_type": exception_type}},
                    {"match": {"message": exception_type}}
                ],
                "minimum_should_match": 1
            }
        })

    try:
        # OpenSearch 검색
        results = opensearch_client.search(
            index=index_pattern,
            body={
                "size": 500,
                "query": {"bool": {"must": must_clauses}},
                "sort": [{"timestamp": "asc"}],
                "_source": BASE_FIELDS + LOG_DETAILS_FIELDS
            }
        )

        hits = results.get("hits", {}).get("hits", [])

        if not hits:
            filter_msg = f" ({exception_type})" if exception_type else ""
            return (
                f"최근 {time_hours}시간 동안 에러{filter_msg}가 없습니다.\n\n"
                f"🟢 시스템이 안정적으로 운영되고 있습니다."
            )

        # 에러 그룹핑 (exception_type + class_name + method_name)
        error_lifetimes = defaultdict(lambda: {"first": None, "last": None, "count": 0, "sample": None})

        for hit in hits:
            source = hit["_source"]
            log_details = source.get("log_details", {})
            exc_type = log_details.get("exception_type", "Unknown")
            class_name = source.get("class_name", "Unknown")
            method_name = source.get("method_name", "Unknown")
            timestamp = source.get("timestamp", "")

            error_key = f"{exc_type} @ {class_name}.{method_name}"

            try:
                dt = datetime.fromisoformat(timestamp.replace("Z", ""))

                if error_lifetimes[error_key]["first"] is None or dt < error_lifetimes[error_key]["first"]:
                    error_lifetimes[error_key]["first"] = dt

                if error_lifetimes[error_key]["last"] is None or dt > error_lifetimes[error_key]["last"]:
                    error_lifetimes[error_key]["last"] = dt

                error_lifetimes[error_key]["count"] += 1

                if error_lifetimes[error_key]["sample"] is None:
                    error_lifetimes[error_key]["sample"] = source
            except:
                pass

        # 생존 시간 계산
        error_analysis = []
        now = datetime.utcnow()

        for error_key, data in error_lifetimes.items():
            first = data["first"]
            last = data["last"]

            if not first or not last:
                continue

            # 생존 기간 (일)
            lifetime_hours = (last - first).total_seconds() / 3600
            lifetime_days = lifetime_hours / 24

            # 마지막 발생 이후 경과 시간
            time_since_last = (now - last).total_seconds() / 3600

            # 상태 판정
            if time_since_last <= 24:
                status = "🔴 진행 중"
            elif time_since_last <= 72:
                status = "🟡 최근 해결"
            else:
                status = "🟢 해결됨"

            # 장기 미해결
            if lifetime_days >= 7 and time_since_last <= 24:
                status = "🚨 장기 미해결"

            error_analysis.append({
                "error_key": error_key,
                "first": first,
                "last": last,
                "lifetime_days": lifetime_days,
                "time_since_last": time_since_last,
                "count": data["count"],
                "status": status,
                "sample": data["sample"]
            })

        # 결과 포맷팅
        filter_suffix = f" ({exception_type})" if exception_type else ""
        lines = [
            f"## ⏳ 에러 생존 시간 분석{filter_suffix}",
            f"**기간:** 최근 {time_hours}시간 ({time_hours//24}일)",
            f"**분석 에러:** {len(error_analysis)}개 타입",
            ""
        ]

        # 에러별 생존 시간 테이블
        lines.append("### 📋 에러별 생존 기간")
        lines.append("")
        lines.append("| 에러 | 최초 발생 | 마지막 발생 | 지속 기간 | 상태 | 발생 횟수 |")
        lines.append("|------|-----------|-------------|-----------|------|-----------|")

        # 지속 기간 기준 정렬 (긴 것부터)
        sorted_errors = sorted(error_analysis, key=lambda x: x["lifetime_days"], reverse=True)

        for err in sorted_errors[:20]:  # 상위 20개
            first_str = err["first"].strftime("%m-%d %H:%M")
            last_str = err["last"].strftime("%m-%d %H:%M")
            lifetime_str = f"{err['lifetime_days']:.1f}일" if err['lifetime_days'] >= 1 else f"{err['lifetime_days']*24:.1f}시간"

            lines.append(
                f"| {err['error_key'][:50]} | {first_str} | {last_str} | **{lifetime_str}** | {err['status']} | {err['count']} |"
            )

        lines.append("")

        # 상태별 분류
        status_groups = defaultdict(list)
        for err in error_analysis:
            status_groups[err["status"]].append(err)

        lines.append("### 📊 상태별 통계")
        lines.append("")

        for status in ["🚨 장기 미해결", "🔴 진행 중", "🟡 최근 해결", "🟢 해결됨"]:
            count = len(status_groups[status])
            if count > 0:
                lines.append(f"- **{status}**: {count}개 ({count / len(error_analysis) * 100:.1f}%)")

        # 장기 미해결 에러 강조
        long_term_errors = [e for e in error_analysis if "장기 미해결" in e["status"]]

        if long_term_errors:
            lines.append("")
            lines.append("### 🚨 장기 미해결 에러 (7일 이상)")
            lines.append("")

            for i, err in enumerate(sorted(long_term_errors, key=lambda x: x["lifetime_days"], reverse=True)[:5], 1):
                lines.append(f"{i}. **{err['error_key']}**")
                lines.append(f"   - 지속 기간: {err['lifetime_days']:.0f}일")
                lines.append(f"   - 최초 발생: {err['first'].strftime('%Y-%m-%d %H:%M')}")
                lines.append(f"   - 발생 횟수: {err['count']}회")

                # 샘플 메시지
                sample = err['sample']
                if sample:
                    message = sample.get("message", "")[:150]
                    lines.append(f"   - 메시지: {message}")

                lines.append("")

        # 평균 해결 시간
        resolved_errors = [e for e in error_analysis if "해결됨" in e["status"] or "최근 해결" in e["status"]]
        if resolved_errors:
            avg_resolution_days = sum([e["lifetime_days"] for e in resolved_errors]) / len(resolved_errors)
            lines.append("")
            lines.append(f"### ⚡ 평균 해결 시간: {avg_resolution_days:.1f}일")
            lines.append(f"(해결된 에러 {len(resolved_errors)}개 기준)")

        # 권장 조치
        lines.append("")
        lines.append("### ✅ 권장 조치")
        lines.append("")

        if long_term_errors:
            worst_error = max(long_term_errors, key=lambda x: x["lifetime_days"])
            lines.append(f"1. **즉시 수정 필요:** {worst_error['error_key']} ({worst_error['lifetime_days']:.0f}일 지속)")

        ongoing_errors = status_groups.get("🔴 진행 중", [])
        if len(ongoing_errors) > 5:
            lines.append(f"2. **진행 중 에러 {len(ongoing_errors)}개 모니터링:** 재발 방지 대책 수립")

        if resolved_errors and avg_resolution_days > 7:
            lines.append(f"3. **해결 속도 개선:** 평균 {avg_resolution_days:.1f}일 → 목표 3일 이내")

        lines.append("")
        lines.append("💡 **참고:** '해결됨'은 최근 3일간 미발생 기준이며, 실제 수정과 다를 수 있습니다.")

        return "\n".join(lines)

    except Exception as e:
        return f"에러 생존 시간 분석 중 오류 발생: {str(e)}"
