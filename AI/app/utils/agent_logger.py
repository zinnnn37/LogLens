"""
Agent 실행 과정 구조화된 로깅

Agent의 Tool 선택, 실행, 파싱 에러 등을 구조화된 형식으로 로깅하여
디버깅과 모니터링을 용이하게 함
"""
import logging
import json
from datetime import datetime
from typing import Dict, Any, Optional, List

# 로거 설정
logger = logging.getLogger("agent_execution")


class AgentLogger:
    """Agent 실행 과정을 추적하는 로거"""

    def __init__(self, project_uuid: str, question: str):
        """
        Args:
            project_uuid: 프로젝트 UUID
            question: 사용자 질문
        """
        self.project_uuid = project_uuid
        self.question = question
        self.start_time = datetime.utcnow()
        self.events: List[Dict[str, Any]] = []

    def log_tool_selection(self, tool_name: str, tool_input: Dict[str, Any]):
        """
        도구 선택 로깅

        Args:
            tool_name: 선택된 도구 이름
            tool_input: 도구에 전달된 입력 파라미터
        """
        self.events.append({
            "timestamp": datetime.utcnow().isoformat(),
            "event": "tool_selected",
            "tool_name": tool_name,
            "tool_input": tool_input
        })
        logger.info(f"[{self.project_uuid}] Tool selected: {tool_name}")

    def log_tool_result(self, tool_name: str, success: bool, result_length: int, error: Optional[str] = None):
        """
        도구 실행 결과 로깅

        Args:
            tool_name: 실행된 도구 이름
            success: 성공 여부
            result_length: 결과 길이 (문자 수)
            error: 에러 메시지 (실패 시)
        """
        self.events.append({
            "timestamp": datetime.utcnow().isoformat(),
            "event": "tool_executed",
            "tool_name": tool_name,
            "success": success,
            "result_length": result_length,
            "error": error
        })

        if success:
            logger.info(f"[{self.project_uuid}] Tool executed successfully: {tool_name} ({result_length} chars)")
        else:
            logger.error(f"[{self.project_uuid}] Tool execution failed: {tool_name} - {error}")

    def log_parsing_error(self, error_msg: str, retry_count: int):
        """
        파싱 에러 로깅

        Args:
            error_msg: 에러 메시지
            retry_count: 재시도 횟수
        """
        self.events.append({
            "timestamp": datetime.utcnow().isoformat(),
            "event": "parsing_error",
            "error": error_msg[:200],
            "retry": retry_count
        })
        logger.warning(f"[{self.project_uuid}] Parsing error (retry {retry_count}): {error_msg[:100]}")

    def log_timeout(self, duration_seconds: float):
        """
        타임아웃 로깅

        Args:
            duration_seconds: 경과 시간 (초)
        """
        self.events.append({
            "timestamp": datetime.utcnow().isoformat(),
            "event": "timeout",
            "duration_seconds": duration_seconds
        })
        logger.error(f"[{self.project_uuid}] Agent timeout after {duration_seconds}s")

    def log_completion(self, success: bool, answer_length: int, error: Optional[str] = None):
        """
        Agent 실행 완료 로깅

        Args:
            success: 성공 여부
            answer_length: 생성된 답변 길이
            error: 에러 메시지 (실패 시)
        """
        duration = (datetime.utcnow() - self.start_time).total_seconds()
        self.events.append({
            "timestamp": datetime.utcnow().isoformat(),
            "event": "completion",
            "success": success,
            "answer_length": answer_length,
            "duration_seconds": duration,
            "error": error
        })

        # 전체 실행 로그를 JSON으로 저장
        log_entry = {
            "project_uuid": self.project_uuid,
            "question": self.question[:100],  # 질문은 100자까지만
            "start_time": self.start_time.isoformat(),
            "duration_seconds": duration,
            "success": success,
            "events": self.events
        }

        if success:
            logger.info(f"[{self.project_uuid}] Agent completed successfully in {duration:.2f}s")
            logger.debug(json.dumps(log_entry, ensure_ascii=False))
        else:
            logger.error(f"[{self.project_uuid}] Agent failed after {duration:.2f}s: {error}")
            logger.error(json.dumps(log_entry, ensure_ascii=False))

    def get_summary(self) -> Dict[str, Any]:
        """
        실행 요약 반환 (디버깅용)

        Returns:
            실행 요약 딕셔너리
        """
        duration = (datetime.utcnow() - self.start_time).total_seconds()
        tool_executions = [e for e in self.events if e["event"] == "tool_executed"]

        return {
            "project_uuid": self.project_uuid,
            "question": self.question[:100],
            "duration_seconds": duration,
            "total_events": len(self.events),
            "tools_used": [e["tool_name"] for e in tool_executions],
            "parsing_errors": len([e for e in self.events if e["event"] == "parsing_error"])
        }
