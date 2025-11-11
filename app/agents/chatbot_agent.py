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
from app.tools.performance_tools import get_slowest_apis, get_traffic_by_time
from app.tools.monitoring_tools import (
    get_error_rate_trend,
    get_service_health_status,
    get_error_frequency_ranking,
    get_api_error_rates,
    get_affected_users_count
)
from app.tools.comparison_tools import compare_time_periods, detect_cascading_failures
from app.tools.alert_tools import evaluate_alert_conditions, detect_resource_issues
from app.tools.deployment_tools import analyze_deployment_impact
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
- CRITICAL (즉시 조치): Database/Connection errors, OutOfMemory, StackOverflow, Deadlock, 5xx errors (affects all users)
- HIGH (긴급 조치): Authentication/Security errors, InvalidToken, AuthFailure, UnauthorizedAccess (security risk)
- MEDIUM (우선 조치): NullPointerException, IllegalStateException, RuntimeException (specific feature broken)
- LOW (모니터링): 4xx errors, validation errors, slow queries (client-side or performance issues only)
- When asked "가장 심각한 에러", "most critical error": Call get_recent_errors ONCE, analyze results, provide Final Answer
- DO NOT call get_recent_errors multiple times with different service_name filters unless specifically requested
- The tool returns errors sorted by severity automatically - trust the order

EFFICIENCY RULES TO PREVENT ITERATION LOOPS:
- For "most X" questions (가장 심각한, 가장 많은, most frequent), use ONE broad query first without filters
- Analyze the results and make a decision immediately - DO NOT iterate through all possible combinations
- AVOID calling the same tool multiple times with slightly different parameters
- Example workflow: "가장 심각한 에러가 뭐야?" → get_recent_errors(limit=10) → analyze types based on severity → Final Answer (total: 1-2 tool calls)

AI ANALYSIS FIELD USAGE (IMPORTANT):
- Tools now return ai_analysis fields: summary, error_cause, solution, tags, analysis_type
- **IF ai_analysis.summary EXISTS**: Include it prominently in your Final Answer (it's already analyzed by AI)
- **IF ai_analysis.error_cause EXISTS**: Use it to explain the root cause
- **IF ai_analysis.solution EXISTS**: Include it as recommended action
- **IF ai_analysis.tags EXIST**: Use them to categorize or identify error types
- **IMPORTANT**: ai_analysis fields may be empty for some logs - handle gracefully
- Prioritize AI analysis results over manual analysis when available
- Example: If tool returns "🤖 AI 분석: ...", integrate it into your answer

TIME PARSING GUIDELINES:
- "최근 N일" or "N일 동안" → time_hours = N * 24
- "어제" → time_hours = 24
- "이번 주" → time_hours = 168 (7 days)
- "최근 1시간" or "1시간 동안" → time_hours = 1
- "오늘" → time_hours = 24
- Always extract time values accurately from user questions

PERFORMANCE ANALYSIS GUIDELINES (IMPORTANT):
- For "응답 시간이 가장 느린 API", "slowest API" questions: Use get_slowest_apis tool
- For "트래픽이 가장 많은 시간대", "peak traffic time" questions: Use get_traffic_by_time tool
- For "평균 응답 시간", "average response time" questions: Use get_slowest_apis with appropriate limit
- get_slowest_apis returns: avg/max/min response times, P50/P95/P99 percentiles, request counts
- get_traffic_by_time returns: hourly/interval-based traffic distribution, peak times, level distribution
- Default time range for performance analysis: 168 hours (7 days) unless specified otherwise
- Interval options for get_traffic_by_time: "1h" (hourly), "30m" (30 minutes), "1d" (daily)
- When analyzing performance, always mention:
  1. Time range analyzed
  2. Total request count
  3. Specific metrics (avg/max/P95)
  4. Performance grade (빠름/보통/느림/매우 느림)
- Example workflow: "응답 시간이 가장 느린 API는?" → get_slowest_apis(limit=5) → analyze results → Final Answer

MONITORING & ALERTING GUIDELINES (NEW TOOLS - IMPORTANT):
- For "에러율이 증가", "error rate trend" questions: Use get_error_rate_trend tool
- For "서비스가 정상", "service health" questions: Use get_service_health_status tool
- For "가장 자주 발생하는 에러", "most frequent error" questions: Use get_error_frequency_ranking tool
- For "가장 에러가 많은 API", "API error rate" questions: Use get_api_error_rates tool
- For "몇 명의 사용자가 영향", "affected users" questions: Use get_affected_users_count tool
- For "오늘 vs 어제", "time period comparison" questions: Use compare_time_periods tool
- For "연쇄 장애", "cascading failure" questions: Use detect_cascading_failures tool
- For "알림이 필요한", "alert conditions" questions: Use evaluate_alert_conditions tool
- For "메모리 부족", "리소스 이슈", "resource issues" questions: Use detect_resource_issues tool
- For "배포 이후", "deployment impact" questions: Use analyze_deployment_impact tool
- These tools provide comprehensive monitoring/alerting insights - prioritize them over generic tools for DevOps/SRE questions

FORMATTING GUIDELINES FOR FINAL ANSWER:
- For ANALYSIS questions (통계, 분석, 요약): Use structured markdown with ## headers, **bold** numbers, tables
- For SIMPLE questions (인사, 단순 조회): Keep it concise and natural
- Always include specific numbers, timestamps, service names when available
- **Include AI analysis results when provided by tools**
- Example structured: "## 📊 요약\n최근 7일간 **4건**의 에러 발생\n\n**🤖 AI 분석:**\n결제 서비스 DB 연결 문제\n\n| 서비스 | 건수 |\n|------|------|\n| user-service | 2건 |"
- Example simple: "안녕하세요! 로그 분석이 필요하시면 질문해주세요."

DETAIL REQUIREMENTS FOR ANALYSIS RESPONSES:
- Include FULL error messages (do not truncate or summarize)
- Show complete stack traces when analyzing errors (minimum 5-10 lines of context)
- Always state the time range analyzed (e.g., "최근 24시간", "2025-11-01 ~ 2025-11-07")
- Include HTTP details when relevant (method, path, status code)
- Cite specific log_ids so users can reference them (e.g., "log_id: 101")
- Use code blocks (```) for stack traces and technical details
- Target 1000-3000 characters for comprehensive analysis responses

EXAMPLE SCENARIO - "최근 10일 동안 가장 심각한 에러가 뭐야?":
Question: 최근 10일 동안 가장 심각한 에러가 뭐야?
Thought: I need to get recent errors from the last 10 days (240 hours) and identify the most serious one
Action: get_recent_errors
Action Input: {{"limit": 10, "time_hours": 240}}
Observation: === 최근 에러 로그 (최근 240시간) ===
총 5건의 에러 발생, 상위 5건 표시

에러 타입별 분포:
  - DatabaseTimeout: 2건
  - NullPointerException: 2건
  - InvalidTokenException: 1건

최근 에러 목록 (심각도순):
1. [DatabaseTimeout] 2025-11-03T14:32:10 | 심각도: CRITICAL
   서비스: payment-service
   📍 PaymentService.processPayment
   🌐 POST /api/v1/payments → 500
   ⏱️  15234ms
   💬 DatabaseTimeout: Connection pool exhausted after 30s. Active: 20, Idle: 0, Max: 20
   🤖 AI 분석: 결제 서비스의 DB 연결 풀이 고갈되어 전체 결제 기능이 마비됨
   📌 원인: 장시간 실행되는 배치 작업이 연결을 반환하지 않음
   💡 해결책: 1. DB 연결 풀 크기 증설 (20 → 50), 2. 배치 작업에 타임아웃 설정, 3. Connection leak 감지 모니터링 추가
   📚 (스택 트레이스 있음)
   (log_id: 12345)

2. [NullPointerException] 2025-11-04T09:15:22 | 심각도: MEDIUM
   서비스: user-service
   📍 UserController.getUser
   🌐 GET /api/users/123 → 500
   💬 NullPointerException: User object is null at line 45...
   (log_id: 12346)

...

Thought: The tool has already sorted errors by severity. The first error (DatabaseTimeout) is marked as CRITICAL and has AI analysis with root cause and solution. This is the most serious error.
Final Answer: ## 🚨 가장 심각한 에러

**DatabaseTimeout** (log_id: 12345)

**발생 시간:** 2025-11-03 14:32:10 (7일 전)
**서비스:** payment-service
**위치:** PaymentService.processPayment
**심각도:** CRITICAL (최고)

**에러 내용:**
```
DatabaseTimeout: Connection pool exhausted after 30s
Active: 20, Idle: 0, Max: 20
```

**🤖 AI 분석:**
결제 서비스의 DB 연결 풀이 고갈되어 전체 결제 기능이 마비됨

**📌 원인:**
장시간 실행되는 배치 작업이 연결을 반환하지 않음

**💡 권장 해결책:**
1. DB 연결 풀 크기 증설 (20 → 50)
2. 배치 작업에 타임아웃 설정
3. Connection leak 감지 모니터링 추가

**영향 범위:**
- HTTP 500 에러 발생
- 요청 처리 시간: 15.2초 (정상의 약 30배)
- **모든 사용자의 결제 요청 실패**

**추가 정보:**
- 요청: POST /api/v1/payments
- 최근 10일간 동일 에러 2건 더 발생

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

    async def _get_slowest_apis_wrapper(tool_input: str = "", **kwargs):
        import json
        if isinstance(tool_input, str) and tool_input:
            try:
                kwargs.update(json.loads(tool_input))
            except json.JSONDecodeError as e:
                print(f"⚠️ JSON parsing error in get_slowest_apis: {e}, input: {tool_input}")
                # Continue with default parameters
        params = {**kwargs, "project_uuid": project_uuid}
        return await get_slowest_apis.ainvoke(params)

    async def _get_traffic_by_time_wrapper(tool_input: str = "", **kwargs):
        import json
        if isinstance(tool_input, str) and tool_input:
            try:
                kwargs.update(json.loads(tool_input))
            except json.JSONDecodeError as e:
                print(f"⚠️ JSON parsing error in get_traffic_by_time: {e}, input: {tool_input}")
                # Continue with default parameters
        params = {**kwargs, "project_uuid": project_uuid}
        return await get_traffic_by_time.ainvoke(params)

    # New monitoring tools wrappers
    async def _get_error_rate_trend_wrapper(tool_input: str = "", **kwargs):
        import json
        if isinstance(tool_input, str) and tool_input:
            try:
                kwargs.update(json.loads(tool_input))
            except json.JSONDecodeError as e:
                print(f"⚠️ JSON parsing error in get_error_rate_trend: {e}, input: {tool_input}")
        params = {**kwargs, "project_uuid": project_uuid}
        return await get_error_rate_trend.ainvoke(params)

    async def _get_service_health_status_wrapper(tool_input: str = "", **kwargs):
        import json
        if isinstance(tool_input, str) and tool_input:
            try:
                kwargs.update(json.loads(tool_input))
            except json.JSONDecodeError as e:
                print(f"⚠️ JSON parsing error in get_service_health_status: {e}, input: {tool_input}")
        params = {**kwargs, "project_uuid": project_uuid}
        return await get_service_health_status.ainvoke(params)

    async def _get_error_frequency_ranking_wrapper(tool_input: str = "", **kwargs):
        import json
        if isinstance(tool_input, str) and tool_input:
            try:
                kwargs.update(json.loads(tool_input))
            except json.JSONDecodeError as e:
                print(f"⚠️ JSON parsing error in get_error_frequency_ranking: {e}, input: {tool_input}")
        params = {**kwargs, "project_uuid": project_uuid}
        return await get_error_frequency_ranking.ainvoke(params)

    async def _get_api_error_rates_wrapper(tool_input: str = "", **kwargs):
        import json
        if isinstance(tool_input, str) and tool_input:
            try:
                kwargs.update(json.loads(tool_input))
            except json.JSONDecodeError as e:
                print(f"⚠️ JSON parsing error in get_api_error_rates: {e}, input: {tool_input}")
        params = {**kwargs, "project_uuid": project_uuid}
        return await get_api_error_rates.ainvoke(params)

    async def _get_affected_users_count_wrapper(tool_input: str = "", **kwargs):
        import json
        if isinstance(tool_input, str) and tool_input:
            try:
                kwargs.update(json.loads(tool_input))
            except json.JSONDecodeError as e:
                print(f"⚠️ JSON parsing error in get_affected_users_count: {e}, input: {tool_input}")
        params = {**kwargs, "project_uuid": project_uuid}
        return await get_affected_users_count.ainvoke(params)

    # Comparison tools wrappers
    async def _compare_time_periods_wrapper(tool_input: str = "", **kwargs):
        import json
        if isinstance(tool_input, str) and tool_input:
            try:
                kwargs.update(json.loads(tool_input))
            except json.JSONDecodeError as e:
                print(f"⚠️ JSON parsing error in compare_time_periods: {e}, input: {tool_input}")
        params = {**kwargs, "project_uuid": project_uuid}
        return await compare_time_periods.ainvoke(params)

    async def _detect_cascading_failures_wrapper(tool_input: str = "", **kwargs):
        import json
        if isinstance(tool_input, str) and tool_input:
            try:
                kwargs.update(json.loads(tool_input))
            except json.JSONDecodeError as e:
                print(f"⚠️ JSON parsing error in detect_cascading_failures: {e}, input: {tool_input}")
        params = {**kwargs, "project_uuid": project_uuid}
        return await detect_cascading_failures.ainvoke(params)

    # Alert tools wrappers
    async def _evaluate_alert_conditions_wrapper(tool_input: str = "", **kwargs):
        import json
        if isinstance(tool_input, str) and tool_input:
            try:
                kwargs.update(json.loads(tool_input))
            except json.JSONDecodeError as e:
                print(f"⚠️ JSON parsing error in evaluate_alert_conditions: {e}, input: {tool_input}")
        params = {**kwargs, "project_uuid": project_uuid}
        return await evaluate_alert_conditions.ainvoke(params)

    async def _detect_resource_issues_wrapper(tool_input: str = "", **kwargs):
        import json
        if isinstance(tool_input, str) and tool_input:
            try:
                kwargs.update(json.loads(tool_input))
            except json.JSONDecodeError as e:
                print(f"⚠️ JSON parsing error in detect_resource_issues: {e}, input: {tool_input}")
        params = {**kwargs, "project_uuid": project_uuid}
        return await detect_resource_issues.ainvoke(params)

    # Deployment tools wrapper
    async def _analyze_deployment_impact_wrapper(tool_input: str = "", **kwargs):
        import json
        if isinstance(tool_input, str) and tool_input:
            try:
                kwargs.update(json.loads(tool_input))
            except json.JSONDecodeError as e:
                print(f"⚠️ JSON parsing error in analyze_deployment_impact: {e}, input: {tool_input}")
        params = {**kwargs, "project_uuid": project_uuid}
        return await analyze_deployment_impact.ainvoke(params)

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
        Tool(
            name="get_slowest_apis",
            description=get_slowest_apis.description,
            func=_dummy_func,
            coroutine=_get_slowest_apis_wrapper
        ),
        Tool(
            name="get_traffic_by_time",
            description=get_traffic_by_time.description,
            func=_dummy_func,
            coroutine=_get_traffic_by_time_wrapper
        ),
        # New monitoring tools
        Tool(
            name="get_error_rate_trend",
            description=get_error_rate_trend.description,
            func=_dummy_func,
            coroutine=_get_error_rate_trend_wrapper
        ),
        Tool(
            name="get_service_health_status",
            description=get_service_health_status.description,
            func=_dummy_func,
            coroutine=_get_service_health_status_wrapper
        ),
        Tool(
            name="get_error_frequency_ranking",
            description=get_error_frequency_ranking.description,
            func=_dummy_func,
            coroutine=_get_error_frequency_ranking_wrapper
        ),
        Tool(
            name="get_api_error_rates",
            description=get_api_error_rates.description,
            func=_dummy_func,
            coroutine=_get_api_error_rates_wrapper
        ),
        Tool(
            name="get_affected_users_count",
            description=get_affected_users_count.description,
            func=_dummy_func,
            coroutine=_get_affected_users_count_wrapper
        ),
        # Comparison tools
        Tool(
            name="compare_time_periods",
            description=compare_time_periods.description,
            func=_dummy_func,
            coroutine=_compare_time_periods_wrapper
        ),
        Tool(
            name="detect_cascading_failures",
            description=detect_cascading_failures.description,
            func=_dummy_func,
            coroutine=_detect_cascading_failures_wrapper
        ),
        # Alert tools
        Tool(
            name="evaluate_alert_conditions",
            description=evaluate_alert_conditions.description,
            func=_dummy_func,
            coroutine=_evaluate_alert_conditions_wrapper
        ),
        Tool(
            name="detect_resource_issues",
            description=detect_resource_issues.description,
            func=_dummy_func,
            coroutine=_detect_resource_issues_wrapper
        ),
        # Deployment tools
        Tool(
            name="analyze_deployment_impact",
            description=analyze_deployment_impact.description,
            func=_dummy_func,
            coroutine=_analyze_deployment_impact_wrapper
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
