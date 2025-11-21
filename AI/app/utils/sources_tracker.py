"""
출처 및 검증 정보 추적 유틸리티

Chatbot V2 Agent가 사용한 로그 출처와 샘플링 전략을 추적하여
응답에 ValidationInfo를 추가
"""

from typing import List, Dict, Any, Optional
from app.models.experiment import LogSource, ValidationInfo


class SourcesTracker:
    """
    Agent 실행 중 수집된 로그 출처 추적

    사용 방법:
    1. Agent 실행 전에 tracker = SourcesTracker() 생성
    2. 각 도구가 로그를 검색할 때 tracker.add_sources() 호출
    3. Agent 실행 후 tracker.get_validation_info() 호출
    """

    def __init__(self):
        self.sources: List[LogSource] = []
        self.tool_calls: List[Dict[str, Any]] = []
        self.error_logs_count = 0
        self.warn_logs_count = 0
        self.info_logs_count = 0
        self.used_vector_search = False
        self.used_keyword_search = False
        self.sampling_strategies: List[str] = []

    def add_sources(
        self,
        logs: List[Dict[str, Any]],
        search_type: str = "unknown",
        relevance_scores: Optional[List[float]] = None
    ):
        """
        검색된 로그를 출처로 추가

        Args:
            logs: OpenSearch 검색 결과 (list of dicts)
            search_type: "vector_knn" | "keyword" | "filter" | "aggregation"
            relevance_scores: 각 로그의 관련성 점수 (0-1)
        """
        for i, log in enumerate(logs):
            level = log.get("level", "INFO")

            # 레벨별 카운트
            if level == "ERROR":
                self.error_logs_count += 1
            elif level == "WARN":
                self.warn_logs_count += 1
            elif level == "INFO":
                self.info_logs_count += 1

            # 검색 타입 추적
            if search_type == "vector_knn":
                self.used_vector_search = True
            elif search_type == "keyword":
                self.used_keyword_search = True

            # LogSource 생성
            relevance_score = None
            if relevance_scores and i < len(relevance_scores):
                relevance_score = relevance_scores[i]

            source = LogSource(
                log_id=str(log.get("log_id", log.get("_id", "unknown"))),
                timestamp=log.get("timestamp", ""),
                level=level,
                message=log.get("message", "")[:500],  # 메시지는 500자까지만
                service_name=log.get("service_name", "unknown"),
                relevance_score=relevance_score,
                class_name=log.get("class_name"),
                method_name=log.get("method_name")
            )

            self.sources.append(source)

    def add_tool_call(self, tool_name: str, params: Dict[str, Any]):
        """도구 호출 기록"""
        self.tool_calls.append({
            "tool": tool_name,
            "params": params
        })

        # 샘플링 전략 추론
        if tool_name == "search_logs_by_similarity":
            self.sampling_strategies.append("proportional_vector_knn")
        elif tool_name == "search_logs_by_keyword":
            self.sampling_strategies.append("keyword_filter")
        elif tool_name in ["get_log_statistics", "get_recent_errors"]:
            self.sampling_strategies.append("aggregation")
        elif tool_name == "get_logs_by_trace_id":
            self.sampling_strategies.append("trace_id_filter")

    def get_top_sources(self, limit: int = 10) -> List[LogSource]:
        """
        상위 N개 출처 반환 (관련성 점수 기준)

        Args:
            limit: 반환할 최대 개수

        Returns:
            List[LogSource]: 관련성 점수 높은 순
        """
        # 관련성 점수가 있는 것 우선 정렬
        sorted_sources = sorted(
            self.sources,
            key=lambda s: (s.relevance_score is not None, s.relevance_score or 0),
            reverse=True
        )
        return sorted_sources[:limit]

    def get_validation_info(self) -> ValidationInfo:
        """
        수집된 정보를 기반으로 ValidationInfo 생성

        Returns:
            ValidationInfo: 답변 유효성 검증 정보
        """
        total_samples = len(self.sources)

        # 신뢰도 계산
        confidence = self._calculate_confidence()

        # 샘플링 전략 결정
        if not self.sampling_strategies:
            sampling_strategy = "no_data_retrieved"
        elif "proportional_vector_knn" in self.sampling_strategies:
            sampling_strategy = "proportional_vector_knn"
        elif "keyword_filter" in self.sampling_strategies:
            sampling_strategy = "keyword_filter"
        elif "aggregation" in self.sampling_strategies:
            sampling_strategy = "aggregation"
        else:
            sampling_strategy = "mixed"

        # 커버리지 설명
        coverage_parts = []
        if self.error_logs_count > 0:
            coverage_parts.append(f"ERROR {self.error_logs_count}건")
        if self.warn_logs_count > 0:
            coverage_parts.append(f"WARN {self.warn_logs_count}건")
        if self.info_logs_count > 0:
            coverage_parts.append(f"INFO {self.info_logs_count}건")

        coverage = ", ".join(coverage_parts) if coverage_parts else "데이터 없음"

        # 데이터 품질 평가
        data_quality = self._assess_data_quality()

        # 제약사항
        limitation = "Vector 검색은 ERROR 로그만 지원. WARN/INFO는 기본 필터 사용"

        # 추가 메모
        note = None
        if total_samples == 0:
            note = "조건을 만족하는 로그가 없습니다"
        elif total_samples < 5:
            note = f"샘플 수가 적습니다 ({total_samples}건). 결과가 부정확할 수 있습니다"
        elif self.used_vector_search and self.used_keyword_search:
            note = "Vector 검색(ERROR)과 키워드 검색(WARN/INFO)을 혼합 사용"

        return ValidationInfo(
            confidence=confidence,
            sample_count=total_samples,
            sampling_strategy=sampling_strategy,
            coverage=coverage,
            data_quality=data_quality,
            limitation=limitation,
            note=note
        )

    def _calculate_confidence(self) -> int:
        """
        신뢰도 계산 (0-100)

        기준:
        - 샘플 수: 많을수록 좋음 (최대 50점)
        - Vector 검색 사용: +20점
        - 다양한 레벨: +20점
        - 관련성 점수: 높을수록 좋음 (최대 10점)
        """
        confidence = 0
        total_samples = len(self.sources)

        # 1. 샘플 수 (최대 50점)
        if total_samples == 0:
            confidence += 0
        elif total_samples < 5:
            confidence += 20
        elif total_samples < 10:
            confidence += 35
        else:
            confidence += 50

        # 2. Vector 검색 사용 (ERROR 패턴 파악) (+20점)
        if self.used_vector_search:
            confidence += 20

        # 3. 레벨 다양성 (+20점)
        levels_count = (
            (1 if self.error_logs_count > 0 else 0) +
            (1 if self.warn_logs_count > 0 else 0) +
            (1 if self.info_logs_count > 0 else 0)
        )
        if levels_count == 3:
            confidence += 20
        elif levels_count == 2:
            confidence += 10

        # 4. 관련성 점수 평균 (최대 10점)
        scores_with_value = [s.relevance_score for s in self.sources if s.relevance_score is not None]
        if scores_with_value:
            avg_score = sum(scores_with_value) / len(scores_with_value)
            confidence += int(avg_score * 10)

        return min(confidence, 100)

    def _assess_data_quality(self) -> str:
        """
        데이터 품질 평가

        Returns:
            "high" | "medium" | "low"
        """
        total_samples = len(self.sources)

        if total_samples == 0:
            return "low"

        # 관련성 점수가 있는 샘플 비율
        scored_samples = [s for s in self.sources if s.relevance_score is not None]
        scored_ratio = len(scored_samples) / total_samples if total_samples > 0 else 0

        # 평균 관련성 점수
        avg_relevance = 0
        if scored_samples:
            avg_relevance = sum(s.relevance_score for s in scored_samples) / len(scored_samples)

        # 품질 판단
        if total_samples >= 10 and avg_relevance >= 0.7:
            return "high"
        elif total_samples >= 5 and avg_relevance >= 0.5:
            return "medium"
        else:
            return "low"


# Global instance (thread-local 아님 - FastAPI는 요청마다 새로 생성해야 함)
# Service에서 각 요청마다 새로 생성할 것
