"""
Log Analysis Service V2 - LangGraph Edition

LangGraph를 사용한 로그 분석 서비스
V1의 기능을 유지하면서 더 유연하고 확장 가능한 구조 제공
"""

from typing import Optional, List
from app.graphs.log_analysis_graph import create_log_analysis_graph
from app.models.analysis import LogAnalysisResponse, LogAnalysisResult
from app.models.experiment import LogSource, ValidationInfo
from app.core.opensearch import opensearch_client


class LogAnalysisServiceV2:
    """
    Log Analysis Service V2 using LangGraph

    Features:
    - 3-tier 캐시 전략 (Direct → Trace → Similarity)
    - 동적 분석 전략 선택 (Single / Direct / Map-Reduce)
    - 한국어 출력 검증
    - 품질 검증 (ValidationPipeline)
    - Trace 전파
    - V2 RAG 검증 (sources + validation)
    """

    def _create_log_sources(
        self,
        logs: List[dict],
        similarity_scores: Optional[List[float]] = None
    ) -> List[LogSource]:
        """
        로그 리스트를 LogSource 리스트로 변환 (V2 RAG 검증용)

        Args:
            logs: 로그 딕셔너리 리스트
            similarity_scores: 각 로그의 유사도 점수 (optional)

        Returns:
            LogSource 리스트 (최대 10개)
        """
        sources = []
        for i, log in enumerate(logs[:10]):  # 최대 10개만
            source = LogSource(
                log_id=str(log.get("log_id", "")),
                timestamp=log.get("timestamp", ""),
                level=log.get("level", "UNKNOWN"),
                message=log.get("message", "")[:500],  # 메시지 500자 제한
                service_name=log.get("service_name", "unknown"),
                relevance_score=similarity_scores[i] if similarity_scores and i < len(similarity_scores) else None,
                class_name=log.get("class_name"),
                method_name=log.get("method_name"),
            )
            sources.append(source)
        return sources

    def _create_validation_info(
        self,
        sample_count: int,
        analysis_type: str,
        log_level: str,
        from_cache: bool = False
    ) -> ValidationInfo:
        """
        ValidationInfo 객체 생성 (V2 RAG 검증용)

        Args:
            sample_count: 분석에 사용된 로그 수
            analysis_type: 분석 타입 ("langgraph_agent", "trace", "similarity", "direct")
            log_level: 로그 레벨
            from_cache: 캐시 사용 여부

        Returns:
            ValidationInfo 객체
        """
        # 신뢰도 계산
        if from_cache:
            confidence = 95  # 캐시된 분석은 높은 신뢰도
        else:
            # 샘플 수에 따라 신뢰도 조정
            if sample_count >= 10:
                confidence = 90
            elif sample_count >= 5:
                confidence = 80
            elif sample_count > 1:
                confidence = 70
            else:
                confidence = 60  # 단일 로그

        # 샘플링 전략 결정
        if analysis_type == "trace":
            sampling_strategy = "trace_id_filter"
            coverage = f"Trace ID 기반 관련 로그 {sample_count}개 분석"
        elif analysis_type == "similarity":
            sampling_strategy = "proportional_vector_knn"
            coverage = f"Vector 검색으로 유사 로그 {sample_count}개 분석"
        elif analysis_type == "direct":
            sampling_strategy = "direct_cache_hit"
            coverage = f"캐시된 분석 결과 사용 (원본 로그 ID 기반)"
        elif analysis_type == "langgraph_agent":
            sampling_strategy = "langgraph_agent_analysis"
            coverage = f"LangGraph 에이전트 기반 {sample_count}개 로그 분석"
        else:
            sampling_strategy = "single_log"
            coverage = "단일 로그 분석"

        # 데이터 품질 평가
        if log_level == "ERROR" and sample_count >= 5:
            data_quality = "high"
        elif sample_count >= 3:
            data_quality = "medium"
        else:
            data_quality = "low"

        # 제한사항
        limitation = ""
        if sample_count == 1:
            limitation = "단일 로그 기반 분석으로 컨텍스트가 제한적일 수 있습니다"
        elif from_cache:
            limitation = "캐시된 결과로 최신 로그 변화가 반영되지 않을 수 있습니다"
        elif sample_count < 5:
            limitation = f"제한된 샘플 수({sample_count}개)로 패턴 파악이 불완전할 수 있습니다"

        return ValidationInfo(
            confidence=confidence,
            sample_count=sample_count,
            sampling_strategy=sampling_strategy,
            coverage=coverage,
            data_quality=data_quality,
            limitation=limitation,
            note="LangGraph V2 에이전트 기반 분석" if not from_cache else None
        )

    async def analyze_log(self, log_id: int, project_uuid: str) -> LogAnalysisResponse:
        """
        로그 분석 실행 (LangGraph 사용)

        Args:
            log_id: 로그 ID (정수)
            project_uuid: 프로젝트 UUID

        Returns:
            LogAnalysisResponse: 분석 결과

        Raises:
            ValueError: 로그를 찾을 수 없거나 분석 실패 시
        """
        # LangGraph 생성
        graph = create_log_analysis_graph(project_uuid)

        # 그래프 실행
        result = await graph.analyze(log_id)

        # 에러 체크
        if result.get("error"):
            raise ValueError(result["error"])

        # LogAnalysisResponse 변환
        analysis_dict = result.get("analysis")

        if not analysis_dict:
            raise ValueError(f"No analysis result for log_id={log_id}")

        # V2 RAG 검증: sources와 validation 수집
        sources = None
        validation = None

        try:
            # 분석에 사용된 로그 수집
            target_log = result.get("target_log", {})
            related_logs = result.get("related_logs", [])

            # 로그 리스트 생성
            all_logs = [target_log] if target_log else []
            if related_logs:
                all_logs.extend(related_logs)

            # sources 생성
            if all_logs:
                similarity_scores = result.get("similarity_scores")
                sources = self._create_log_sources(all_logs, similarity_scores)

            # validation 생성
            sample_count = len(all_logs) if all_logs else 1
            log_level = target_log.get("level", "UNKNOWN") if target_log else "UNKNOWN"
            from_cache = result.get("from_cache", False)

            # 분석 타입 결정
            if result.get("similar_log_id"):
                analysis_type = "similarity"
            elif result.get("trace_id"):
                analysis_type = "trace"
            elif from_cache:
                analysis_type = "direct"
            else:
                analysis_type = "langgraph_agent"

            validation = self._create_validation_info(
                sample_count=sample_count,
                analysis_type=analysis_type,
                log_level=log_level,
                from_cache=from_cache
            )

        except Exception as e:
            print(f"⚠️ Failed to create V2 fields (sources/validation): {e}")
            # Graceful fallback - continue without V2 fields

        return LogAnalysisResponse(
            log_id=log_id,
            analysis=LogAnalysisResult(**analysis_dict),
            from_cache=result.get("from_cache", False),
            similar_log_id=result.get("similar_log_id"),
            similarity_score=result.get("similarity_score"),
            sources=sources,  # V2 추가
            validation=validation,  # V2 추가
        )


# Global instance
log_analysis_service_v2 = LogAnalysisServiceV2()
