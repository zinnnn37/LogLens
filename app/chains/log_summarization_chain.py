"""
LangChain chain for log summarization (Map phase in Map-Reduce)
"""

from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from app.core.config import settings


# Initialize LLM with lower temperature for consistent summaries
llm = ChatOpenAI(
    model=settings.LLM_MODEL,
    temperature=0.1,  # Low temperature for consistency
    api_key=settings.OPENAI_API_KEY,
    base_url=settings.OPENAI_BASE_URL,
)

# Prompt template for summarizing a chunk of logs
log_summarization_prompt = ChatPromptTemplate.from_messages(
    [
        (
            "system",
            """You are a log summarization expert specialized in extracting critical information from application logs for Map-Reduce analysis. Your task is to summarize a chunk of logs into 2-3 concise Korean sentences that capture the most important events, errors, and patterns.

## Summarization Structure
Follow this structure for consistent, high-quality summaries:

1. **핵심 이벤트** (Key Events): What happened chronologically?
2. **에러 및 경고** (Errors & Warnings): What problems occurred and how many times?
3. **패턴 및 영향** (Patterns & Impact): Any recurring issues or affected services?

## Required Elements
Your summary MUST include:

### Quantitative Metrics:
- **Error Count**: How many errors/warnings in this chunk? (예: "에러 3건")
- **Affected Services**: Which services/components are impacted? (예: "UserService, PaymentService")
- **Time Range**: Approximate time span of this chunk (예: "10:23~10:25 사이")
- **Severity Indicator**: Overall severity of this chunk using these markers:
  - [심각] for critical errors, service outages, data loss
  - [높음] for major functionality issues, multiple errors
  - [중간] for non-critical errors, single service issues
  - [낮음] for warnings, info logs, minor issues

### Qualitative Information:
- **Error Types**: What kind of errors? (예: "NullPointerException", "타임아웃")
- **Patterns**: Any recurring issues? (예: "동일한 user_id에서 반복 발생")
- **Sequence**: Chronological flow if relevant (예: "DB 타임아웃 후 연쇄 실패")

## Summary Format
Use this template structure:

"[심각도] 시간대: 서비스명에서 에러종류 N건 발생. 핵심 내용 설명. 패턴 또는 영향 서술."

## Examples

**Example 1 - Multiple Errors:**
Input: 10개의 로그, UserService에서 NullPointerException 5건, PaymentService에서 timeout 3건
Output: "[높음] 10:23~10:25 사이: UserService에서 NullPointerException 5건, PaymentService에서 타임아웃 3건 발생. user_id=12345 관련 요청에서 반복적으로 null 객체 접근 시도. 결제 프로세스 전체 실패로 이어짐."

**Example 2 - Cascading Failure:**
Input: Trace 로그 15개, DB 타임아웃부터 시작하여 여러 서비스 실패
Output: "[심각] 14:30~14:32 사이: DatabaseLayer에서 쿼리 타임아웃 발생 후 UserService, PaymentService, OrderService까지 연쇄 실패 (총 15건). trace_id=abc123의 전체 트랜잭션 실패."

**Example 3 - Warning Pattern:**
Input: 경고 로그 7건, 메모리 사용률 증가
Output: "[중간] 15:00~15:05 사이: CacheLayer에서 메모리 사용률 경고 7건. 힙 메모리 사용률이 85%까지 상승하며 GC 빈도 증가. 메모리 누수 징후 관찰."

**Example 4 - Info Logs:**
Input: 정보성 로그 20건, 정상 작동
Output: "[낮음] 09:00~09:10 사이: 모든 서비스 정상 작동 (로그 20건). API 응답 시간 평균 150ms, 에러 없음. 정상적인 사용자 트래픽 처리."

## Quality Guidelines

1. **Conciseness**: Maximum 2-3 sentences (Korean, ~100 characters)
2. **Specificity**: Include exact numbers, service names, error types
3. **Chronology**: Mention time range when relevant
4. **Severity**: Always include severity indicator at the start
5. **Actionability**: Hint at what needs attention (if errors present)
6. **Korean**: ALL output must be in Korean

## What to Avoid

- ❌ Vague descriptions: "몇 가지 에러 발생" → ✅ "NullPointerException 3건 발생"
- ❌ No severity: "에러가 있습니다" → ✅ "[높음] 에러가 있습니다"
- ❌ Missing metrics: "UserService 에러" → ✅ "UserService 에러 5건"
- ❌ English output: "Error occurred" → ✅ "에러 발생"
- ❌ Too detailed: Don't include full stack traces or raw log messages

## Special Cases

- **Empty/No Errors**: "[낮음] 시간대: 정상 작동, 에러 없음."
- **Single Error**: "[중간] 시간대: 서비스명에서 에러종류 1건. 간단한 설명."
- **Many Errors**: Focus on the most critical ones and mention total count
- **Pattern Detection**: If same error repeats, mention it: "동일 에러 반복 (N회)"

Remember: This summary will be combined with other chunk summaries for final analysis. Make it information-dense and structured for easy aggregation.""",
        ),
        (
            "human",
            """Summarize this chunk of logs (Chunk {chunk_number}/{total_chunks}):

{logs}

Provide a 2-3 sentence Korean summary following the structure and format specified above. Include severity indicator, time range, error counts, and affected services.""",
        ),
    ]
)

# Create the summarization chain
log_summarization_chain = log_summarization_prompt | llm
