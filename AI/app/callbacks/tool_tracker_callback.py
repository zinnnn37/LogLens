"""
도구 호출 추적 콜백

Agent가 도구를 호출할 때마다 ToolCallTracker에 기록하고,
무한 루프 감지 시 Agent 실행을 중단합니다.
"""

from typing import Any, Dict, Optional
from langchain.callbacks.base import BaseCallbackHandler
from app.core.tool_tracker import ToolCallTracker


class ToolTrackerCallback(BaseCallbackHandler):
    """
    Agent의 도구 호출을 추적하는 콜백 핸들러

    Features:
    - 모든 도구 호출 기록
    - 무한 루프 감지 및 경고
    - 도구 호출 통계 제공
    """

    def __init__(self):
        """콜백 핸들러 초기화"""
        super().__init__()
        self.tracker = ToolCallTracker()
        self.total_tool_calls = 0

    def on_tool_start(
        self,
        serialized: Dict[str, Any],
        input_str: str,
        **kwargs: Any,
    ) -> None:
        """
        도구 호출 시작 시 호출됨

        Args:
            serialized: 도구 정보
            input_str: 도구 입력 파라미터
        """
        tool_name = serialized.get("name", "unknown")

        # 입력 파라미터 파싱 (JSON 문자열일 수 있음)
        try:
            import json
            tool_input = json.loads(input_str) if input_str else {}
        except (json.JSONDecodeError, TypeError):
            tool_input = {"raw_input": input_str}

        # 도구 호출 기록
        self.tracker.add_call(tool_name, tool_input)
        self.total_tool_calls += 1

        # 무한 루프 감지
        is_loop, warning_msg = self.tracker.is_infinite_loop()
        if is_loop:
            print(f"\n{'='*60}")
            print(warning_msg)
            print(f"총 도구 호출 횟수: {self.total_tool_calls}회")
            print(f"도구 호출 이력:")
            print(self.tracker.get_summary())
            print(f"{'='*60}\n")
            # Note: AgentExecutor의 max_iterations가 루프를 중단시킴

    def on_tool_end(
        self,
        output: str,
        **kwargs: Any,
    ) -> None:
        """
        도구 호출 완료 시 호출됨

        Args:
            output: 도구 실행 결과
        """
        # 현재는 로깅만 수행
        pass

    def on_tool_error(
        self,
        error: Exception,
        **kwargs: Any,
    ) -> None:
        """
        도구 호출 실패 시 호출됨

        Args:
            error: 발생한 예외
        """
        print(f"⚠️ Tool error: {str(error)[:200]}")

    def get_runtime_feedback(self) -> str:
        """
        실시간 피드백 메시지 반환 (Agent 프롬프트에 포함용)

        Returns:
            경고 메시지 또는 정상 메시지
        """
        return self.tracker.get_runtime_feedback()

    def get_summary(self) -> str:
        """
        도구 호출 통계 요약 반환

        Returns:
            도구별 호출 횟수 요약
        """
        return self.tracker.get_summary()

    def clear(self) -> None:
        """이력 초기화"""
        self.tracker.clear()
        self.total_tool_calls = 0
