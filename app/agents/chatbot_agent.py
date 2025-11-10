"""
ReAct Agent for Log Analysis

LLM이 자율적으로 도구를 선택하여 로그 분석 수행
"""

from typing import List

from langchain.agents import create_react_agent, AgentExecutor
from langchain_openai import ChatOpenAI
from langchain_core.prompts import PromptTemplate
from langchain_core.tools import Tool

from app.tools.search_tools import search_logs_by_keyword, search_logs_by_similarity
from app.tools.analysis_tools import get_log_statistics, get_recent_errors
from app.tools.detail_tools import get_log_detail, get_logs_by_trace_id
from app.core.config import settings


# ReAct Agent System Prompt (한국어) - LangChain 표준 형식
AGENT_PROMPT_TEMPLATE = """Answer the following questions as best you can. You have access to the following tools:

{tools}

CRITICAL RULES FOR "NO DATA FOUND" RESPONSES:
- If a tool returns "로그가 없습니다" or "ERROR 레벨 로그가 없습니다" or "검색 결과가 없습니다", this is a VALID FINAL RESULT
- DO NOT retry with different parameters
- DO NOT try other tools
- IMMEDIATELY write: "Thought: I now know the final answer" followed by "Final Answer: [explain no logs found]"
- Example response format when no data found:
  Thought: I now know the final answer
  Final Answer: 최근 24시간 동안 ERROR 로그가 발생하지 않았습니다. 시스템이 정상 작동 중입니다.

SEVERITY ASSESSMENT GUIDELINES (for "가장 심각한", "most serious" questions):
- Database/Connection errors (DatabaseTimeout, ConnectionPoolExhausted, ConnectionRefused) = MOST SERIOUS (affects all users)
- Authentication/Security errors (InvalidToken, AuthFailure, UnauthorizedAccess) = HIGH severity (security risk)
- NullPointerException, IllegalStateException, RuntimeException = MEDIUM severity (specific feature broken)
- Slow queries, cache misses, warnings = LOW severity (performance degradation only)
- When asked "가장 심각한 에러", "most critical error": Call get_recent_errors ONCE, analyze error types using above criteria, provide Final Answer
- DO NOT call get_recent_errors multiple times with different service_name filters unless specifically requested

EFFICIENCY RULES TO PREVENT ITERATION LOOPS:
- For "most X" questions (가장 심각한, 가장 많은, most frequent), use ONE broad query first without filters
- Analyze the results and make a decision immediately - DO NOT iterate through all possible combinations
- AVOID calling the same tool multiple times with slightly different parameters
- Example workflow: "가장 심각한 에러가 뭐야?" → get_recent_errors(limit=10) → analyze types based on severity → Final Answer (total: 1-2 tool calls)

FORMATTING GUIDELINES FOR FINAL ANSWER:
- For ANALYSIS questions (통계, 분석, 요약): Use structured markdown with ## headers, **bold** numbers, tables
- For SIMPLE questions (인사, 단순 조회): Keep it concise and natural
- Always include specific numbers, timestamps, service names when available
- Example structured: "## 📊 요약\n최근 7일간 **4건**의 에러 발생\n\n| 서비스 | 건수 |\n|------|------|\n| user-service | 2건 |"
- Example simple: "안녕하세요! 로그 분석이 필요하시면 질문해주세요."

DETAIL REQUIREMENTS FOR ANALYSIS RESPONSES:
- Include FULL error messages (do not truncate or summarize)
- Show complete stack traces when analyzing errors (minimum 5-10 lines of context)
- Always state the time range analyzed (e.g., "최근 24시간", "2025-11-01 ~ 2025-11-07")
- Include HTTP details when relevant (method, path, status code)
- Cite specific log_ids so users can reference them (e.g., "log_id: 101")
- Use code blocks (```) for stack traces and technical details
- Target 1000-3000 characters for comprehensive analysis responses

EXAMPLE SCENARIO - "가장 심각한 에러가 뭐야?":
Question: 가장 심각한 에러가 뭐야?
Thought: I need to get recent errors first and determine which is most serious based on severity guidelines
Action: get_recent_errors
Action Input: {{"limit": 10, "time_hours": 24}}
Observation: === 최근 에러 로그 (최근 24시간) ===
총 3건의 에러 발생, 상위 3건 표시

에러 타입별 분포:
  - Unknown: 3건

최근 에러 목록:
1. [Unknown] 2025-11-10T09:19
   서비스: user-service
   메시지: NullPointerException: User object is null at line 45...
   (log_id: 101)

2. [Unknown] 2025-11-10T06:19
   서비스: payment-service
   메시지: DatabaseTimeout: Connection pool exhausted after 30s...
   (log_id: 102)

3. [Unknown] 2025-11-09T21:19
   서비스: auth-service
   메시지: InvalidTokenException: JWT signature verification failed...
   (log_id: 103)

Thought: According to severity guidelines, DatabaseTimeout (affects all users) is MOST SERIOUS, more critical than NullPointerException or InvalidToken
Final Answer: ## 🚨 가장 심각한 에러

**DatabaseTimeout** (log_id: 102)

**발생 시간:** 3시간 전 (2025-11-10 06:19)
**서비스:** payment-service
**심각도:** 최고 (CRITICAL)

**에러 내용:**
```
DatabaseTimeout: Connection pool exhausted after 30s
```

**영향 범위:**
결제 서비스의 데이터베이스 연결 풀이 고갈되어 **모든 사용자의 결제 요청이 실패**했습니다.

**권장 조치:**
1. 즉시 DB 연결 풀 크기 확인 및 증설
2. 장시간 실행 중인 쿼리 확인
3. Connection leak 여부 점검

Use the following format:

Question: the input question you must answer
Thought: you should always think about what to do
Action: the action to take, should be one of [{tool_names}]
Action Input: the input to the action (JSON format)
Observation: the result of the action
... (this Thought/Action/Action Input/Observation can repeat N times)
Thought: I now know the final answer
Final Answer: the final answer to the original input question (in Korean)

Begin!

Question: {input}
Thought:{agent_scratchpad}"""

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
        api_key=settings.OPENAI_API_KEY,
        stop=["\nObservation"]  # Observation 환각 방지
    )

    # project_uuid가 바인딩된 wrapper 함수들 생성
    # Note: LangChain Tool requires sync 'func' and async 'coroutine'
    # We provide dummy sync func and actual async coroutine

    def _dummy_func(*args, **kwargs):
        raise NotImplementedError("Use async version")

    async def _search_logs_by_keyword_wrapper(tool_input: str = "", **kwargs):
        # tool_input is passed by agent, parse it if it's a JSON string
        import json
        if isinstance(tool_input, str) and tool_input:
            try:
                kwargs.update(json.loads(tool_input))
            except json.JSONDecodeError as e:
                print(f"⚠️ JSON parsing error in search_logs_by_keyword: {e}, input: {tool_input}")
                # Continue with default parameters
        # Inject project_uuid and call the tool function directly
        params = {**kwargs, "project_uuid": project_uuid}
        return await search_logs_by_keyword.ainvoke(params)

    async def _search_logs_by_similarity_wrapper(tool_input: str = "", **kwargs):
        import json
        if isinstance(tool_input, str) and tool_input:
            try:
                kwargs.update(json.loads(tool_input))
            except json.JSONDecodeError as e:
                print(f"⚠️ JSON parsing error in search_logs_by_similarity: {e}, input: {tool_input}")
                # Continue with default parameters
        params = {**kwargs, "project_uuid": project_uuid}
        return await search_logs_by_similarity.ainvoke(params)

    async def _get_log_statistics_wrapper(tool_input: str = "", **kwargs):
        import json
        if isinstance(tool_input, str) and tool_input:
            try:
                kwargs.update(json.loads(tool_input))
            except json.JSONDecodeError as e:
                print(f"⚠️ JSON parsing error in get_log_statistics: {e}, input: {tool_input}")
                # Continue with default parameters
        params = {**kwargs, "project_uuid": project_uuid}
        return await get_log_statistics.ainvoke(params)

    async def _get_recent_errors_wrapper(tool_input: str = "", **kwargs):
        import json
        if isinstance(tool_input, str) and tool_input:
            try:
                kwargs.update(json.loads(tool_input))
            except json.JSONDecodeError as e:
                print(f"⚠️ JSON parsing error in get_recent_errors: {e}, input: {tool_input}")
                # Continue with default parameters
        params = {**kwargs, "project_uuid": project_uuid}
        return await get_recent_errors.ainvoke(params)

    async def _get_log_detail_wrapper(tool_input: str = "", **kwargs):
        import json
        if isinstance(tool_input, str) and tool_input:
            try:
                kwargs.update(json.loads(tool_input))
            except json.JSONDecodeError as e:
                print(f"⚠️ JSON parsing error in get_log_detail: {e}, input: {tool_input}")
                # Continue with default parameters
        params = {**kwargs, "project_uuid": project_uuid}
        return await get_log_detail.ainvoke(params)

    async def _get_logs_by_trace_id_wrapper(tool_input: str = "", **kwargs):
        import json
        if isinstance(tool_input, str) and tool_input:
            try:
                kwargs.update(json.loads(tool_input))
            except json.JSONDecodeError as e:
                print(f"⚠️ JSON parsing error in get_logs_by_trace_id: {e}, input: {tool_input}")
                # Continue with default parameters
        params = {**kwargs, "project_uuid": project_uuid}
        return await get_logs_by_trace_id.ainvoke(params)

    # Tool 목록 (wrapper 함수 사용)
    tools: List[Tool] = [
        Tool(
            name="search_logs_by_keyword",
            description=search_logs_by_keyword.description,
            func=_dummy_func,
            coroutine=_search_logs_by_keyword_wrapper
        ),
        Tool(
            name="search_logs_by_similarity",
            description=search_logs_by_similarity.description,
            func=_dummy_func,
            coroutine=_search_logs_by_similarity_wrapper
        ),
        Tool(
            name="get_log_statistics",
            description=get_log_statistics.description,
            func=_dummy_func,
            coroutine=_get_log_statistics_wrapper
        ),
        Tool(
            name="get_recent_errors",
            description=get_recent_errors.description,
            func=_dummy_func,
            coroutine=_get_recent_errors_wrapper
        ),
        Tool(
            name="get_log_detail",
            description=get_log_detail.description,
            func=_dummy_func,
            coroutine=_get_log_detail_wrapper
        ),
        Tool(
            name="get_logs_by_trace_id",
            description=get_logs_by_trace_id.description,
            func=_dummy_func,
            coroutine=_get_logs_by_trace_id_wrapper
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
        max_iterations=settings.AGENT_MAX_ITERATIONS,  # 최대 10회 도구 호출
        early_stopping_method="force",  # "generate"는 langchain 0.2.x에서 broken (known bug)
        handle_parsing_errors=True,  # 파싱 에러 자동 처리
        return_intermediate_steps=False,  # 중간 단계 반환 (선택)
    )

    return agent_executor
