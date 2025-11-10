"""
ReAct Agent for Log Analysis

LLM이 자율적으로 도구를 선택하여 로그 분석 수행
"""

from typing import List
from functools import partial

from langchain.agents import create_react_agent, AgentExecutor
from langchain_openai import ChatOpenAI
from langchain_core.prompts import PromptTemplate
from langchain_core.tools import Tool

from app.tools.search_tools import search_logs_by_keyword, search_logs_by_similarity
from app.tools.analysis_tools import get_log_statistics, get_recent_errors
from app.tools.detail_tools import get_log_detail, get_logs_by_trace_id
from app.core.config import settings


# ReAct Agent System Prompt (한국어)
AGENT_PROMPT_TEMPLATE = """당신은 로그 분석 전문가입니다. 사용자의 질문에 답하기 위해 적절한 도구를 선택하세요.

## 사용 가능한 도구
{tools}

## 도구 선택 가이드

1. **시스템 상태/통계 질문** (예: "시스템 상태는?", "오늘 에러가 몇 개야?")
   → get_log_statistics 사용

2. **최근 에러 확인** (예: "최근 에러 보여줘", "에러 목록")
   → get_recent_errors 사용

3. **키워드 검색** (예: "NullPointerException 찾아줘", "user-service 로그")
   → search_logs_by_keyword 사용

4. **의미적 검색** (예: "인증 실패 관련 로그", "데이터베이스 문제")
   → search_logs_by_similarity 사용

5. **특정 로그 상세** (log_id가 주어진 경우)
   → get_log_detail 사용

6. **트레이스 추적** (trace_id가 주어진 경우)
   → get_logs_by_trace_id 사용

7. **인사말/간단한 질문** (예: "안녕", "고마워")
   → 도구 사용 없이 바로 답변

## 중요 규칙
- project_uuid는 항상 제공되므로 모든 도구 호출 시 전달하세요
- 도구 실행 결과를 받으면 사용자가 이해하기 쉽게 요약하세요
- 필요하면 여러 도구를 연속으로 사용하세요 (최대 5회)
- 정보가 부족하면 추가 도구를 사용하거나 사용자에게 명확히 안내하세요
- 한국어로 답변하세요

## 답변 형식

Thought: (무엇을 해야 할지 생각)
Action: (사용할 도구 이름)
Action Input: (도구에 전달할 입력, JSON 형식)
Observation: (도구 실행 결과)
... (필요시 Thought/Action/Observation 반복)
Thought: I now know the final answer
Final Answer: (최종 답변, 한국어)

---

## Tools
{tool_names}

## Question
{input}

## Agent Scratchpad
{agent_scratchpad}
"""

AGENT_PROMPT = PromptTemplate.from_template(AGENT_PROMPT_TEMPLATE)


def create_log_analysis_agent(project_uuid: str) -> AgentExecutor:
    """
    로그 분석용 ReAct Agent 생성

    Args:
        project_uuid: 프로젝트 UUID (언더스코어 형식)

    Returns:
        AgentExecutor (Agent 실행 엔진)
    """
    # LLM 설정
    llm = ChatOpenAI(
        model=settings.AGENT_MODEL,
        temperature=0,  # 일관된 답변
        api_key=settings.OPENAI_API_KEY
    )

    # project_uuid를 바인딩한 Tool 목록
    # partial을 사용하여 project_uuid를 미리 전달
    tools: List[Tool] = [
        Tool(
            name="search_logs_by_keyword",
            description=search_logs_by_keyword.description,
            func=partial(search_logs_by_keyword.invoke, project_uuid=project_uuid),
            coroutine=partial(search_logs_by_keyword.ainvoke, project_uuid=project_uuid)
        ),
        Tool(
            name="search_logs_by_similarity",
            description=search_logs_by_similarity.description,
            func=partial(search_logs_by_similarity.invoke, project_uuid=project_uuid),
            coroutine=partial(search_logs_by_similarity.ainvoke, project_uuid=project_uuid)
        ),
        Tool(
            name="get_log_statistics",
            description=get_log_statistics.description,
            func=partial(get_log_statistics.invoke, project_uuid=project_uuid),
            coroutine=partial(get_log_statistics.ainvoke, project_uuid=project_uuid)
        ),
        Tool(
            name="get_recent_errors",
            description=get_recent_errors.description,
            func=partial(get_recent_errors.invoke, project_uuid=project_uuid),
            coroutine=partial(get_recent_errors.ainvoke, project_uuid=project_uuid)
        ),
        Tool(
            name="get_log_detail",
            description=get_log_detail.description,
            func=partial(get_log_detail.invoke, project_uuid=project_uuid),
            coroutine=partial(get_log_detail.ainvoke, project_uuid=project_uuid)
        ),
        Tool(
            name="get_logs_by_trace_id",
            description=get_logs_by_trace_id.description,
            func=partial(get_logs_by_trace_id.invoke, project_uuid=project_uuid),
            coroutine=partial(get_logs_by_trace_id.ainvoke, project_uuid=project_uuid)
        ),
    ]

    # ReAct Agent 생성
    agent = create_react_agent(
        llm=llm,
        tools=tools,
        prompt=AGENT_PROMPT
    )

    # AgentExecutor로 래핑
    agent_executor = AgentExecutor(
        agent=agent,
        tools=tools,
        verbose=settings.AGENT_VERBOSE,  # 디버깅 로그
        max_iterations=settings.AGENT_MAX_ITERATIONS,  # 최대 5회 도구 호출
        handle_parsing_errors=True,  # 파싱 에러 자동 처리
        return_intermediate_steps=False,  # 중간 단계 반환 (선택)
    )

    return agent_executor
