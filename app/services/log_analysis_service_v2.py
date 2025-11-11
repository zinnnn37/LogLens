"""
Log Analysis Service V2 - LangGraph Edition

LangGraph를 사용한 로그 분석 서비스
V1의 기능을 유지하면서 더 유연하고 확장 가능한 구조 제공
"""

from typing import Optional
from app.graphs.log_analysis_graph import create_log_analysis_graph
from app.models.analysis import LogAnalysisResponse, LogAnalysisResult


class LogAnalysisServiceV2:
    """
    Log Analysis Service V2 using LangGraph

    Features:
    - 3-tier 캐시 전략 (Direct → Trace → Similarity)
    - 동적 분석 전략 선택 (Single / Direct / Map-Reduce)
    - 한국어 출력 검증
    - 품질 검증 (ValidationPipeline)
    - Trace 전파
    """

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

        return LogAnalysisResponse(
            log_id=log_id,
            analysis=LogAnalysisResult(**analysis_dict),
            from_cache=result.get("from_cache", False),
            similar_log_id=result.get("similar_log_id"),
            similarity_score=result.get("similarity_score"),
        )


# Global instance
log_analysis_service_v2 = LogAnalysisServiceV2()
