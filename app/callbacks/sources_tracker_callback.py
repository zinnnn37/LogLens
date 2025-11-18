"""
출처 추적 콜백 - Agent 도구 호출 시 로그 출처 자동 수집

LangChain Callback을 이용하여 Agent가 호출하는 도구의 출력을 모니터링하고
LogSource를 자동으로 수집
"""

from typing import Any, Dict, List, Optional
from langchain.callbacks.base import BaseCallbackHandler
from app.utils.sources_tracker import SourcesTracker
import json
import re


class SourcesTrackerCallback(BaseCallbackHandler):
    """
    Agent 도구 호출을 추적하여 로그 출처 자동 수집

    사용 방법:
    ```python
    tracker = SourcesTracker()
    callback = SourcesTrackerCallback(tracker)
    agent_executor.invoke(input, config={"callbacks": [callback]})
    sources = tracker.get_top_sources()
    validation = tracker.get_validation_info()
    ```
    """

    def __init__(self, sources_tracker: SourcesTracker):
        """
        Args:
            sources_tracker: SourcesTracker 인스턴스 (공유)
        """
        self.sources_tracker = sources_tracker

    def on_tool_start(
        self,
        serialized: Dict[str, Any],
        input_str: str,
        **kwargs: Any
    ) -> None:
        """
        도구 시작 시 호출

        Args:
            serialized: 도구 정보
            input_str: 도구 입력 (JSON 문자열)
        """
        tool_name = serialized.get("name", "unknown")

        # 입력 파싱 (JSON 형식)
        try:
            params = json.loads(input_str) if input_str.startswith("{") else {"raw": input_str}
        except json.JSONDecodeError:
            params = {"raw": input_str}

        # 도구 호출 기록
        self.sources_tracker.add_tool_call(tool_name, params)

    def on_tool_end(
        self,
        output: str,
        **kwargs: Any
    ) -> None:
        """
        도구 종료 시 호출 - 출력에서 로그 추출

        Args:
            output: 도구 출력 (문자열)
        """
        # 출력이 JSON인지 확인
        logs = self._extract_logs_from_output(output)

        if logs:
            # 로그 출처 추가
            # 검색 타입 추론 (출력 분석)
            search_type = self._infer_search_type(output)
            self.sources_tracker.add_sources(logs, search_type)

    def _extract_logs_from_output(self, output: str) -> List[Dict[str, Any]]:
        """
        도구 출력에서 로그 정보 추출

        도구 출력 형식 예:
        - "📋 검색 결과: 5건\n\n1. [ERROR] ..."
        - JSON 형식: {"logs": [...], "count": 5}
        - 마크다운 표 형식

        Returns:
            List[Dict]: 추출된 로그 리스트
        """
        logs = []

        # 1. JSON 형식 시도
        try:
            data = json.loads(output)
            if isinstance(data, dict) and "logs" in data:
                return data["logs"]
        except json.JSONDecodeError:
            pass

        # 2. 마크다운 로그 엔트리 패턴 매칭
        # 패턴: "1. [ERROR] NullPointerException ... (log_id: 12345, 2024-01-15 10:30:00)"
        log_pattern = re.compile(
            r'\d+\.\s*\[([A-Z]+)\]\s*(.+?)\s*\(log_id:\s*(\d+),\s*(.+?)\)',
            re.MULTILINE | re.DOTALL
        )

        matches = log_pattern.findall(output)
        for match in matches:
            level, message, log_id, timestamp = match
            log = {
                "log_id": log_id.strip(),
                "level": level.strip(),
                "message": message.strip()[:500],  # 최대 500자
                "timestamp": timestamp.strip(),
                "service_name": self._extract_service_name(message)
            }
            logs.append(log)

        # 3. 간단한 ERROR/WARN/INFO 패턴 매칭 (fallback)
        if not logs:
            simple_pattern = re.compile(r'\[?(ERROR|WARN|INFO)\]?\s*(.+)', re.MULTILINE)
            simple_matches = simple_pattern.findall(output)

            for i, (level, message) in enumerate(simple_matches[:10], 1):  # 최대 10개
                log = {
                    "log_id": f"extracted_{i}",
                    "level": level.strip(),
                    "message": message.strip()[:500],
                    "timestamp": "",
                    "service_name": self._extract_service_name(message)
                }
                logs.append(log)

        return logs

    def _extract_service_name(self, message: str) -> str:
        """
        메시지에서 서비스 이름 추출 (간단한 패턴 매칭)

        Args:
            message: 로그 메시지

        Returns:
            str: 서비스 이름 (찾지 못하면 "unknown")
        """
        # "user-service", "payment-api" 같은 패턴
        service_pattern = re.compile(r'([\w-]+)-(service|api|gateway|worker)')
        match = service_pattern.search(message)
        if match:
            return match.group(0)

        # "UserService", "PaymentAPI" 같은 패턴
        class_pattern = re.compile(r'([A-Z][a-z]+)+(?:Service|API|Controller|Manager)')
        match = class_pattern.search(message)
        if match:
            return match.group(0)

        return "unknown"

    def _infer_search_type(self, output: str) -> str:
        """
        도구 출력에서 검색 타입 추론

        Args:
            output: 도구 출력 문자열

        Returns:
            "vector_knn" | "keyword" | "filter" | "aggregation" | "unknown"
        """
        output_lower = output.lower()

        if "vector" in output_lower or "유사도" in output_lower or "similarity" in output_lower:
            return "vector_knn"
        elif "keyword" in output_lower or "키워드" in output_lower:
            return "keyword"
        elif "filter" in output_lower or "필터" in output_lower:
            return "filter"
        elif "통계" in output_lower or "집계" in output_lower or "statistics" in output_lower:
            return "aggregation"
        else:
            return "unknown"
