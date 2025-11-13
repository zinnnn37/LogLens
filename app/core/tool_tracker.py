"""
도구 호출 추적 및 무한 루프 감지

Agent가 동일한 도구를 반복적으로 호출하는 무한 루프를 감지하고 방지합니다.
"""

from collections import deque
from datetime import datetime
from typing import Optional, Dict, Any, List, Tuple


class ToolCallTracker:
    """
    도구 호출 이력 추적 및 무한 루프 감지

    Features:
    - 최근 10개 도구 호출 이력 저장
    - 동일 도구 연속 3회 호출 감지
    - 최근 5회 중 4회 이상 동일 도구 호출 감지
    - 호출 이력 요약 제공
    """

    def __init__(self, max_same_tool_calls: int = 2):
        """
        Args:
            max_same_tool_calls: 동일 도구 연속 호출 허용 횟수 (기본 2회, 3회부터 경고)
        """
        self.history = deque(maxlen=10)  # 최근 10개 호출 기록
        self.max_same_tool_calls = max_same_tool_calls

    def add_call(self, tool_name: str, tool_input: Dict[str, Any]) -> None:
        """
        도구 호출 기록

        Args:
            tool_name: 도구 이름
            tool_input: 도구 입력 파라미터
        """
        self.history.append({
            "tool": tool_name,
            "input": tool_input,
            "timestamp": datetime.utcnow().isoformat()
        })

    def is_infinite_loop(self) -> Tuple[bool, Optional[str]]:
        """
        무한 루프 감지

        Returns:
            (is_loop, message): 무한 루프 여부와 경고 메시지
        """
        if len(self.history) < 2:
            return False, None

        # 패턴 1: 최근 3개 호출이 모두 같은 도구인지 확인
        if len(self.history) >= 3:
            recent_tools = [call["tool"] for call in list(self.history)[-3:]]
            if len(set(recent_tools)) == 1:
                tool_name = recent_tools[0]
                return True, f"⚠️ '{tool_name}' 도구가 연속 {len(recent_tools)}회 호출됨. 무한 루프 가능성 감지."

        # 패턴 2: 최근 5개 호출 중 같은 도구가 4번 이상 나타나는지 확인
        if len(self.history) >= 5:
            recent_5 = [call["tool"] for call in list(self.history)[-5:]]
            for tool in set(recent_5):
                count = recent_5.count(tool)
                if count >= 4:
                    return True, f"⚠️ '{tool}' 도구가 최근 5회 중 {count}회 호출됨. 무한 루프 가능성."

        return False, None

    def get_recent_tool(self) -> Optional[str]:
        """
        가장 최근에 호출된 도구 이름 반환

        Returns:
            도구 이름 또는 None (호출 이력 없음)
        """
        if not self.history:
            return None
        return self.history[-1]["tool"]

    def get_tool_count(self, tool_name: str) -> int:
        """
        특정 도구의 호출 횟수 반환

        Args:
            tool_name: 도구 이름

        Returns:
            호출 횟수
        """
        return sum(1 for call in self.history if call["tool"] == tool_name)

    def get_summary(self) -> str:
        """
        호출 이력 요약 생성

        Returns:
            도구별 호출 횟수 요약 문자열
        """
        if not self.history:
            return "No tool calls yet."

        tool_counts = {}
        for call in self.history:
            tool = call["tool"]
            tool_counts[tool] = tool_counts.get(tool, 0) + 1

        lines = []
        for tool, count in sorted(tool_counts.items(), key=lambda x: x[1], reverse=True):
            lines.append(f"- {tool}: {count}회")

        return "\n".join(lines)

    def get_runtime_feedback(self) -> str:
        """
        실시간 피드백 메시지 생성 (Agent 프롬프트에 포함용)

        Returns:
            경고 메시지 또는 정상 메시지
        """
        if not self.history:
            return "(No tools called yet.)"

        # 최근 3개 호출 확인
        if len(self.history) >= 3:
            recent_calls = list(self.history)[-3:]
            recent_tools = [call["tool"] for call in recent_calls]

            # 모두 같은 도구인 경우
            if len(set(recent_tools)) == 1:
                tool_name = recent_tools[0]
                return f"""⚠️ WARNING: You have called '{tool_name}' {len(recent_tools)} times in a row.
- If you already have enough information, write "Final Answer:" now.
- If you need different data, use a DIFFERENT tool from the Decision Tree.
- DO NOT call '{tool_name}' again unless absolutely necessary."""

        # 최근 도구 사용 통계
        recent_tool = self.get_recent_tool()
        tool_count = self.get_tool_count(recent_tool)

        if tool_count >= 3:
            return f"ℹ️ NOTE: '{recent_tool}' has been called {tool_count} times total. Consider using other tools if needed."

        return "(All good. Continue with your reasoning.)"

    def clear(self) -> None:
        """
        호출 이력 초기화
        """
        self.history.clear()
