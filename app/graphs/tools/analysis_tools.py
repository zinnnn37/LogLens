"""
Analysis Tools for Log Analysis Agent

4개의 분석 도구:
1. collect_trace_logs: Trace ID로 관련 로그 수집
2. analyze_single_log: 단일 로그 분석 (LLM 호출)
3. analyze_logs_direct: 직접 분석 (2-30개 로그, Map-Reduce 없이)
4. analyze_with_map_reduce: Map-Reduce 패턴 분석 (31-100개 로그)
"""

from typing import Dict, Any, Optional, List
from langchain_core.tools import tool

from app.core.opensearch import opensearch_client
from app.services.similarity_service import similarity_service
from app.chains.log_analysis_chain import log_analysis_chain
from app.chains.log_summarization_chain import log_summarization_chain
from app.core.config import settings


def create_collect_trace_logs_tool(project_uuid: str):
    """
    Trace Logs 수집 도구 생성 (project_uuid 바인딩)
    """

    @tool
    async def collect_trace_logs(
        trace_id: str,
        center_timestamp: str,
        time_window_seconds: int = 3,
        max_logs: int = 100
    ) -> str:
        """
        동일한 trace_id를 가진 관련 로그들을 수집합니다.

        Args:
            trace_id: Trace ID (문자열)
            center_timestamp: 중심 타임스탬프 (ISO 형식 문자열)
            time_window_seconds: 시간 윈도우 (±N초, 기본 3)
            max_logs: 최대 로그 수 (기본 100)

        Returns:
            수집된 로그 목록 JSON 문자열 또는 에러 메시지
        """
        import json

        if not trace_id:
            return "ERROR: trace_id가 없습니다."

        if not center_timestamp:
            return "ERROR: center_timestamp가 없습니다."

        try:
            # similarity_service를 사용하여 trace 로그 수집
            related_logs = await similarity_service.find_logs_by_trace_id(
                trace_id=trace_id,
                center_timestamp=center_timestamp,
                project_uuid=project_uuid,
                max_logs=max_logs,
                time_window_seconds=time_window_seconds,
            )

            if not related_logs:
                return json.dumps({
                    "success": True,
                    "log_count": 0,
                    "message": "trace에 속한 로그를 찾을 수 없습니다."
                }, ensure_ascii=False)

            # 로그 데이터 반환
            return json.dumps({
                "success": True,
                "log_count": len(related_logs),
                "logs": related_logs
            }, ensure_ascii=False)

        except Exception as e:
            return f"ERROR: {str(e)}"

    return collect_trace_logs


def create_analyze_single_log_tool(project_uuid: str):
    """
    단일 로그 분석 도구 생성 (project_uuid 바인딩)
    """

    @tool
    async def analyze_single_log(log_data_json: str) -> str:
        """
        단일 로그를 LLM을 사용하여 분석합니다.

        Args:
            log_data_json: 로그 데이터 JSON 문자열 (필수 필드: message, log_level 등)

        Returns:
            분석 결과 JSON 문자열 또는 에러 메시지
        """
        import json

        try:
            # JSON 파싱
            log_data = json.loads(log_data_json)
        except json.JSONDecodeError as e:
            return f"ERROR: Invalid JSON format: {str(e)}"

        try:
            # log_analysis_chain 호출
            result = await log_analysis_chain.ainvoke(
                {
                    "service_name": log_data.get("service_name", "Unknown"),
                    "level": log_data.get("log_level") or log_data.get("level", "Unknown"),
                    "message": log_data.get("message", ""),
                    "method_name": log_data.get("method_name", "N/A"),
                    "class_name": log_data.get("class_name", "N/A"),
                    "timestamp": log_data.get("timestamp", ""),
                    "duration": log_data.get("duration", "N/A"),
                    "stack_trace": log_data.get("stack_trace", "N/A"),
                    "additional_context": log_data.get("additional_context", {}),
                }
            )

            # LogAnalysisResult를 dict로 변환
            return json.dumps({
                "success": True,
                "analysis": result.dict(),
                "analysis_method": "single"
            }, ensure_ascii=False)

        except Exception as e:
            return f"ERROR: {str(e)}"

    return analyze_single_log


def create_analyze_logs_direct_tool(project_uuid: str):
    """
    직접 분석 도구 생성 - Map-Reduce 없이 (project_uuid 바인딩)
    2-30개 로그에 적합
    """

    @tool
    async def analyze_logs_direct(
        related_logs_json: str,
        center_log_json: str
    ) -> str:
        """
        여러 로그를 Map-Reduce 없이 직접 분석합니다 (2-30개 로그 권장).

        Args:
            related_logs_json: 관련 로그 목록 JSON 문자열
            center_log_json: 중심 로그 데이터 JSON 문자열

        Returns:
            분석 결과 JSON 문자열 또는 에러 메시지
        """
        import json

        try:
            related_logs = json.loads(related_logs_json)
            center_log = json.loads(center_log_json)
        except json.JSONDecodeError as e:
            return f"ERROR: Invalid JSON format: {str(e)}"

        if not isinstance(related_logs, list):
            return "ERROR: related_logs는 리스트여야 합니다."

        try:
            # 로그 포맷팅
            formatted_logs = _format_logs_for_analysis(related_logs)

            # LLM 호출 (trace context)
            result = await log_analysis_chain.ainvoke(
                {
                    "total_logs": len(related_logs),
                    "trace_id": center_log.get("trace_id", "Unknown"),
                    "center_log_id": center_log.get("log_id", ""),
                    "center_timestamp": center_log.get("timestamp", ""),
                    "logs_context": formatted_logs,
                    "service_name": center_log.get("service_name", "Unknown"),
                }
            )

            # LogAnalysisResult를 dict로 변환
            return json.dumps({
                "success": True,
                "analysis": result.dict(),
                "analysis_method": "direct",
                "log_count": len(related_logs)
            }, ensure_ascii=False)

        except Exception as e:
            return f"ERROR: {str(e)}"

    return analyze_logs_direct


def create_analyze_with_map_reduce_tool(project_uuid: str):
    """
    Map-Reduce 분석 도구 생성 (project_uuid 바인딩)
    31-100개 로그에 적합
    """

    @tool
    async def analyze_with_map_reduce(
        related_logs_json: str,
        center_log_json: str,
        chunk_size: int = 5
    ) -> str:
        """
        많은 로그를 Map-Reduce 패턴으로 분석합니다 (31-100개 로그 권장).

        Args:
            related_logs_json: 관련 로그 목록 JSON 문자열
            center_log_json: 중심 로그 데이터 JSON 문자열
            chunk_size: 청크 크기 (기본 5)

        Returns:
            분석 결과 JSON 문자열 또는 에러 메시지
        """
        import json

        try:
            related_logs = json.loads(related_logs_json)
            center_log = json.loads(center_log_json)
        except json.JSONDecodeError as e:
            return f"ERROR: Invalid JSON format: {str(e)}"

        if not isinstance(related_logs, list):
            return "ERROR: related_logs는 리스트여야 합니다."

        try:
            # Map Phase: 로그를 청크로 분할하고 각 청크 요약
            summaries = await _map_summarize_chunks(related_logs, chunk_size)

            # Reduce Phase: 요약본들을 종합하여 최종 분석
            result = await _reduce_analyze(summaries, center_log)

            # LogAnalysisResult를 dict로 변환
            return json.dumps({
                "success": True,
                "analysis": result.dict(),
                "analysis_method": "map_reduce",
                "log_count": len(related_logs),
                "chunk_size": chunk_size,
                "chunk_count": len(summaries)
            }, ensure_ascii=False)

        except Exception as e:
            return f"ERROR: {str(e)}"

    return analyze_with_map_reduce


# ===== Helper Functions =====

def _format_logs_for_analysis(logs: List[dict]) -> str:
    """
    여러 로그를 LLM이 읽을 수 있는 문자열로 포맷팅
    """
    formatted = []
    for idx, log in enumerate(logs, 1):
        log_str = f"\n--- Log {idx}/{len(logs)} ---\n"
        log_str += f"Timestamp: {log.get('timestamp', 'N/A')}\n"
        log_str += f"Level: {log.get('log_level') or log.get('level', 'N/A')}\n"
        log_str += f"Service: {log.get('service_name', 'N/A')}\n"
        log_str += f"Class: {log.get('class_name', 'N/A')}\n"
        log_str += f"Method: {log.get('method_name', 'N/A')}\n"
        log_str += f"Message: {log.get('message', 'N/A')}\n"

        if log.get("stack_trace"):
            log_str += f"Stack Trace:\n{log.get('stack_trace')}\n"

        if log.get("additional_context"):
            log_str += f"Context: {log.get('additional_context')}\n"

        formatted.append(log_str)

    return "\n".join(formatted)


def _format_logs_for_summary(logs: List[dict]) -> str:
    """
    Map phase 요약을 위한 로그 포맷팅 (경량화)
    필수 필드만 포함하여 토큰 사용량 최소화
    """
    formatted = []
    for idx, log in enumerate(logs, 1):
        level = log.get("log_level") or log.get("level", "INFO")
        message = log.get("message", "N/A")
        stack_trace = log.get("stack_trace", "")

        log_str = f"{idx}. [{level}] {message}"

        # ERROR/WARN인 경우에만 스택 트레이스 포함 (처음 3줄만)
        if stack_trace and level in ["ERROR", "FATAL", "WARN"]:
            lines = stack_trace.split("\n")[:3]
            log_str += f"\n   Stack: {' | '.join(lines)}"

        formatted.append(log_str)

    return "\n".join(formatted)


async def _map_summarize_chunks(logs: List[dict], chunk_size: int) -> List[str]:
    """
    Map phase: 로그를 청크로 분할하고 각 청크를 요약

    Args:
        logs: 로그 목록
        chunk_size: 청크 크기

    Returns:
        요약 문자열 목록
    """
    # 청크로 분할
    chunks = [logs[i : i + chunk_size] for i in range(0, len(logs), chunk_size)]
    summaries = []

    for idx, chunk in enumerate(chunks):
        # 청크 포맷팅 (경량화)
        formatted_chunk = _format_logs_for_summary(chunk)

        # 요약 생성
        try:
            response = await log_summarization_chain.ainvoke(
                {
                    "chunk_number": idx + 1,
                    "total_chunks": len(chunks),
                    "logs": formatted_chunk,
                }
            )
            summary = response.content
            summaries.append(summary)
        except Exception as e:
            print(f"  ❌ Error summarizing chunk {idx + 1}: {e}")
            # Fallback: 포맷된 청크를 그대로 사용 (단축)
            summaries.append(formatted_chunk[:500] + "...")

    return summaries


async def _reduce_analyze(summaries: List[str], center_log: dict):
    """
    Reduce phase: 청크 요약본들을 종합하여 최종 분석

    Args:
        summaries: 청크 요약 목록
        center_log: 중심 로그

    Returns:
        LogAnalysisResult
    """
    # 요약본 결합
    combined_summary = "\n\n".join(
        [f"[Chunk {i + 1}] {summary}" for i, summary in enumerate(summaries)]
    )

    # 최종 분석
    result = await log_analysis_chain.ainvoke(
        {
            "total_logs": len(summaries),  # 청크 수
            "trace_id": center_log.get("trace_id", "Unknown"),
            "center_log_id": center_log.get("log_id", ""),
            "center_timestamp": center_log.get("timestamp", ""),
            "logs_context": combined_summary,
            "service_name": center_log.get("service_name", "Unknown"),
        }
    )

    return result
