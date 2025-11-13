"""
ReAct Agent for Log Analysis

LLM이 자율적으로 도구를 선택하여 로그 분석 수행
"""

from typing import List

from langchain.agents import create_react_agent, AgentExecutor
from langchain_openai import ChatOpenAI
from langchain_core.prompts import PromptTemplate
from langchain_core.tools import Tool

from app.tools.search_tools import search_logs_by_keyword, search_logs_by_similarity, search_logs_advanced
from app.tools.analysis_tools import get_log_statistics, get_recent_errors, correlate_logs, analyze_errors_unified
from app.tools.detail_tools import get_log_detail, get_logs_by_trace_id
from app.tools.performance_tools import get_slowest_apis, get_traffic_by_time
from app.tools.monitoring_tools import (
    get_error_rate_trend,
    get_service_health_status,
    get_error_frequency_ranking,
    get_api_error_rates,
    get_affected_users_count,
    detect_anomalies
)
from app.tools.comparison_tools import compare_time_periods, detect_cascading_failures
from app.tools.alert_tools import evaluate_alert_conditions, detect_resource_issues
from app.tools.deployment_tools import analyze_deployment_impact
from app.core.config import settings


# ReAct Agent System Prompt (한국어) - LangChain 표준 형식
AGENT_PROMPT_TEMPLATE = """Answer the following questions as best you can. You have access to the following tools:

{tools}

⚠️ YOUR ROLE & SCOPE:
You are a LOG ANALYSIS assistant. ONLY answer questions about: 에러/로그/성능/API/서비스/통계/분석/트래픽/모니터링.
If question has NONE of these keywords → Off-topic → Immediately write "Final Answer:" explaining your scope (NO tools).

**Off-Topic Format:**
Thought: This is off-topic (no log keywords). I will explain my scope.
Final Answer: [Polite Korean explanation: 로그 분석 전문 AI, can help with 에러/성능/로그 검색]

📋 KEY RULES:

**No Data Found:** If tool returns "로그 없음" → Try once without filters → Accept result → "Final Answer: [explain checks]"

**Severity Levels:** CRITICAL (DB/OOM/5xx) > HIGH (Auth/Security) > MEDIUM (NPE/Runtime) > LOW (4xx/slow)

**Time Parsing:** "지금"/"방금" = 5분 | "아까" = 30분 | "최근" = 24h | "N일" = N×24h | "이번 주" = 168h

**AI Analysis:** If tool returns 🤖 AI 분석/error_cause/solution → Use it prominently in your answer

**Tool Selection Decision Tree:**

1️⃣ **Group by what?**
   - By SERVICE → get_service_health_status
   - By ERROR TYPE → get_error_frequency_ranking
   - By TIME → get_error_rate_trend
   - By API → get_api_error_rates

2️⃣ **Question Type?**
   - "trace_id", "traceId", "추적ID", "추적" → get_recent_errors (includes trace_id in output)
   - "request_id", "requestId" → get_recent_errors (includes request_id in output)
   - "가장 심각한 에러" → get_recent_errors (sorted by severity)
   - "서비스별 에러", "에러 많은 서비스", "서비스들" → get_service_health_status (groups BY service)
   - "가장 자주 발생" → get_error_frequency_ranking (sorted by frequency)
   - "느린 API", "slowest" → get_slowest_apis
   - "트래픽 많은 시간" → get_traffic_by_time
   - "에러율 증가" → get_error_rate_trend
   - "영향받은 사용자" → get_affected_users_count
   - "오늘 vs 어제" → compare_time_periods
   - "연쇄 장애" → detect_cascading_failures
   - "리소스 이슈" → detect_resource_issues
   - "배포 영향" → analyze_deployment_impact

3️⃣ **Efficiency:** Use ONE broad query first → Analyze → If needed, 1-2 more refined calls (max 3-4 total)

✅ FORMATTING TEMPLATE (MUST FOLLOW):

**Length:** Analysis = 800+ chars | Simple = 300+ chars

**Structure (Analysis Questions):**
## 📊 [Title with emoji]
**기간/범위:** [time]
**발견:** [key metric in **bold**]

### 🔴 주요 발견사항
[Details with table/code block/bullets]

### ✅ 권장 조치
1. [Specific action]
2. [Next step]

**Formatting:**
- **Bold** ALL numbers, services, error types
- Tables for 3+ items | Code blocks for traces/errors (7+ lines)
- Emojis: 📊요약 🔴에러 🟢정상 ✅해결 💡권장 🤖AI분석 ⏱️시간 🌐API
- Cite log_ids: "(log_id: 12345)"
- Stack traces: Exception → Root cause → 3-5 calls → Entry point

📚 EXAMPLES:

Example 1: "가장 심각한 에러"
Thought: Need errors from 240h, find most critical
Action: get_recent_errors
Action Input: {{"limit": 10, "time_hours": 240}}
Observation: [Tool returns DatabaseTimeout CRITICAL with AI analysis...]
Thought: DatabaseTimeout is CRITICAL with AI analysis. I now know the final answer
Final Answer: ## 🚨 가장 심각한 에러
**DatabaseTimeout** (log_id: 12345)
**발생:** 2025-11-03 14:32 | **서비스:** payment-service | **심각도:** CRITICAL
```
DatabaseTimeout: Connection pool exhausted after 30s
Active: 20, Idle: 0, Max: 20
```
**🤖 AI 분석:** DB 연결 풀 고갈로 결제 기능 마비
**📌 원인:** 배치 작업이 연결 미반환
**✅ 권장 조치:**
1. 연결 풀 크기 증설 (20 → 50)
2. 배치 작업 타임아웃 설정
3. Connection leak 모니터링 추가

Example 2: "느린 API"
Thought: Need slowest APIs by response time
Action: get_slowest_apis
Action Input: {{"limit": 5}}
Observation: [Returns GET /api/reports/export avg 3421ms...]
Thought: I now know the final answer
Final Answer: ## ⚡ 가장 느린 API
**GET /api/reports/export** (평균 **3.4초**)
| 지표 | 값 | 평가 |
|------|-----|------|
| P95 | **7.2초** | 🔴 문제 |
| P99 | **8.5초** | 🚨 심각 |
**✅ 권장 조치:**
1. 비동기 처리 도입 (백그라운드 작업)
2. 캐싱 전략 (Redis)
3. 페이지네이션 구현

🔄 REACT FORMAT (STRICT - MUST FOLLOW):

Question: {input}
Thought: [What to do]
Action: [{tool_names}]
Action Input: {{"param": "value"}}
Observation: [Tool result]
... (repeat if needed, max 3-4 iterations)
Thought: I now know the final answer
Final Answer: [Comprehensive Korean answer, 800+ chars for analysis]

⚠️ CRITICAL RULES - PARSING WILL FAIL IF NOT FOLLOWED:
1. After EVERY "Thought:", write EXACTLY ONE of:
   - "Action: tool_name" (if you need more data)
   - "Final Answer: " (if you have enough information)
2. NEVER write markdown (##, **) or text immediately after "Thought:"
3. NEVER skip the "Final Answer:" label before your answer
4. The "Final Answer:" line MUST be on its own line, followed by your answer

❌ WRONG FORMAT (causes parsing error):
Thought: I have the data
## 🚨 최근 에러...  ← Missing "Final Answer:" label!

✅ CORRECT FORMAT:
Thought: I now know the final answer
Final Answer: ## 🚨 최근 에러...  ← Label present!

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
        temperature=0,  # 결정적 답변, 형식 일관성 최우선 (파싱 에러 최소화)
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

    # New tools wrappers
    async def _search_logs_advanced_wrapper(tool_input: str = "", **kwargs):
        import json
        if isinstance(tool_input, str) and tool_input:
            try:
                kwargs.update(json.loads(tool_input))
            except json.JSONDecodeError as e:
                print(f"⚠️ JSON parsing error in search_logs_advanced: {e}")
        params = {**kwargs, "project_uuid": project_uuid}
        return await search_logs_advanced.ainvoke(params)

    async def _correlate_logs_wrapper(tool_input: str = "", **kwargs):
        import json
        if isinstance(tool_input, str) and tool_input:
            try:
                kwargs.update(json.loads(tool_input))
            except json.JSONDecodeError as e:
                print(f"⚠️ JSON parsing error in correlate_logs: {e}")
        params = {**kwargs, "project_uuid": project_uuid}
        return await correlate_logs.ainvoke(params)

    async def _detect_anomalies_wrapper(tool_input: str = "", **kwargs):
        import json
        if isinstance(tool_input, str) and tool_input:
            try:
                kwargs.update(json.loads(tool_input))
            except json.JSONDecodeError as e:
                print(f"⚠️ JSON parsing error in detect_anomalies: {e}")
        params = {**kwargs, "project_uuid": project_uuid}
        return await detect_anomalies.ainvoke(params)

    async def _analyze_errors_unified_wrapper(tool_input: str = "", **kwargs):
        import json
        if isinstance(tool_input, str) and tool_input:
            try:
                kwargs.update(json.loads(tool_input))
            except json.JSONDecodeError as e:
                print(f"⚠️ JSON parsing error in analyze_errors_unified: {e}")
        params = {**kwargs, "project_uuid": project_uuid}
        return await analyze_errors_unified.ainvoke(params)

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
        # New tools (V2 enhancements)
        Tool(
            name="search_logs_advanced",
            description=search_logs_advanced.description,
            func=_dummy_func,
            coroutine=_search_logs_advanced_wrapper
        ),
        Tool(
            name="correlate_logs",
            description=correlate_logs.description,
            func=_dummy_func,
            coroutine=_correlate_logs_wrapper
        ),
        Tool(
            name="detect_anomalies",
            description=detect_anomalies.description,
            func=_dummy_func,
            coroutine=_detect_anomalies_wrapper
        ),
        Tool(
            name="analyze_errors_unified",
            description=analyze_errors_unified.description,
            func=_dummy_func,
            coroutine=_analyze_errors_unified_wrapper
        ),
    ]

    # 커스텀 파싱 에러 핸들러 (무한 루프 방지)
    def _parsing_error_handler(error) -> str:
        """
        파싱 에러 처리: 1회만 재시도 허용

        LangChain 0.2.16의 ReAct 파서는 "Final Answer:" 라벨이 없으면 실패
        """
        error_msg = str(error)
        print(f"⚠️ Parsing error detected: {error_msg[:200]}...")

        # 에러 원인 분석
        if "Could not parse LLM output" in error_msg:
            # LLM이 "Final Answer:" 없이 바로 마크다운 출력한 경우
            return """❌ PARSING ERROR: You forgot "Final Answer:" label!

You MUST write this EXACT format:

Thought: I now know the final answer
Final Answer: [Your answer here]

Do NOT write:
Thought: [something]
## 🚨 [answer without "Final Answer:" label]  ← This causes error!

The "Final Answer:" label is MANDATORY. Write it on its own line, then your answer.
Try again with the EXACT format above."""

        # 기타 파싱 에러
        return """Parsing error detected. Follow this format:

After "Thought:", write EITHER:
1. "Action: tool_name" followed by "Action Input: {json}" (if you need more data)
2. "Final Answer: your response in Korean" (if you have enough information)

CRITICAL: "Final Answer:" must be present before your answer.
Try once more with correct format."""

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
        max_iterations=settings.AGENT_MAX_ITERATIONS,  # 최대 반복 횟수
        max_execution_time=120,  # 2분 타임아웃 (무한 루프 방지)
        early_stopping_method="force",  # "generate"는 langchain 0.2.x에서 broken (known bug)
        handle_parsing_errors=_parsing_error_handler,  # 커스텀 핸들러 (1회 재시도만 허용)
        return_intermediate_steps=False,  # 중간 단계 반환 (선택)
    )

    return agent_executor
