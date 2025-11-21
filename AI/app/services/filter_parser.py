"""
자연어 질문에서 로그 필터 조건을 자동으로 추출하는 서비스

사용자가 입력한 question에서 다음 항목들을 자동으로 파싱합니다:
- 로그 레벨 (ERROR, WARN, INFO)
- 서비스 이름 (user-service, payment-api 등)
- 시간 표현 (최근 1시간, 오늘, 어제 등)
- 소스 타입 (FE, BE)
- IP 주소
- 기타 OpenSearch 필드

추출되지 않은 필드는 자동으로 제외됩니다 (null 처리).
"""

import re
from datetime import datetime, timedelta
from typing import Dict, Any, Optional
from langchain_openai import ChatOpenAI
from langchain_core.output_parsers import JsonOutputParser
from langchain_core.prompts import ChatPromptTemplate
from pydantic import BaseModel, Field

from app.core.config import settings


class ExtractedFilters(BaseModel):
    """추출된 필터 정보"""

    level: Optional[str] = Field(
        None,
        description="로그 레벨: ERROR, WARN, INFO 중 하나. 언급되지 않으면 null"
    )
    service_name: Optional[str] = Field(
        None,
        description="서비스 이름 (예: user-service, payment-api). 언급되지 않으면 null"
    )
    source_type: Optional[str] = Field(
        None,
        description="소스 타입: FE 또는 BE. 언급되지 않으면 null"
    )
    class_name: Optional[str] = Field(
        None,
        description="Java 클래스 이름. 언급되지 않으면 null"
    )
    method_name: Optional[str] = Field(
        None,
        description="Java 메서드 이름. 언급되지 않으면 null"
    )
    layer: Optional[str] = Field(
        None,
        description="레이어: Controller, Service, Repository 중 하나. 언급되지 않으면 null"
    )
    ip: Optional[str] = Field(
        None,
        description="IP 주소 (IPv4 형식). 언급되지 않으면 null"
    )


class ExtractedTimeRange(BaseModel):
    """추출된 시간 범위 정보"""

    relative_time: Optional[str] = Field(
        None,
        description="상대적 시간 표현 (예: '1시간', '오늘', '어제', '1주일'). 언급되지 않으면 null"
    )
    absolute_start: Optional[str] = Field(
        None,
        description="절대적 시작 날짜 (YYYY-MM-DD 형식). 언급되지 않으면 null"
    )
    absolute_end: Optional[str] = Field(
        None,
        description="절대적 종료 날짜 (YYYY-MM-DD 형식). 언급되지 않으면 null"
    )


# LLM 설정
llm = ChatOpenAI(
    model=settings.LLM_MODEL,
    base_url=settings.OPENAI_BASE_URL,
    temperature=0,  # 일관된 추출을 위해 temperature=0
)

# 필터 추출 프롬프트
filter_extraction_prompt = ChatPromptTemplate.from_messages([
    ("system", """당신은 로그 검색 쿼리를 분석하는 전문가입니다.
사용자의 자연어 질문에서 로그 필터 조건을 추출해주세요.

추출 규칙:
1. 로그 레벨:
   - "에러", "error" → ERROR
   - "경고", "warn", "warning" → WARN
   - "정보", "info" → INFO

2. 서비스 이름:
   - "user-service", "payment-api" 등 하이픈으로 구분된 서비스명 추출
   - "유저 서비스" → user-service (변환)

3. 소스 타입:
   - "프론트엔드", "프론트", "FE", "frontend" → FE
   - "백엔드", "백", "BE", "backend", "서버" → BE

4. 레이어:
   - "컨트롤러", "controller" → Controller
   - "서비스", "service" (문맥상 레이어를 의미하면) → Service
   - "리포지토리", "repository" → Repository

5. IP 주소:
   - IPv4 형식 (xxx.xxx.xxx.xxx) 추출

6. 클래스/메서드:
   - Java 클래스명이나 메서드명이 명시적으로 언급된 경우만 추출

중요: 언급되지 않은 필드는 반드시 null로 설정하세요.
"""),
    ("human", "{question}")
])

# 시간 범위 추출 프롬프트
time_extraction_prompt = ChatPromptTemplate.from_messages([
    ("system", """당신은 시간 표현을 분석하는 전문가입니다.
사용자의 자연어 질문에서 시간 범위를 추출해주세요.

추출 규칙:
1. 상대적 시간:
   - "최근 N시간", "N시간 전" → "N시간"
   - "오늘", "금일" → "오늘"
   - "어제" → "어제"
   - "이번주" → "이번주"
   - "최근 N일", "N일 전" → "N일"

2. 절대적 시간:
   - "YYYY-MM-DD" 형식의 날짜가 있으면 추출
   - 범위가 명시되면 시작일과 종료일 모두 추출
   - 하나만 명시되면 해당 날짜를 시작일로 설정

중요: 시간 표현이 없으면 모든 필드를 null로 설정하세요.
"""),
    ("human", "{question}")
])

# 파서 설정
filter_parser = JsonOutputParser(pydantic_object=ExtractedFilters)
time_parser = JsonOutputParser(pydantic_object=ExtractedTimeRange)

# 체인 구성
filter_chain = filter_extraction_prompt | llm | filter_parser
time_chain = time_extraction_prompt | llm | time_parser


async def parse_filters_from_question(question: str) -> Optional[Dict[str, Any]]:
    """
    자연어 질문에서 로그 필터 조건을 추출합니다.

    Args:
        question: 사용자의 자연어 질문

    Returns:
        추출된 필터 딕셔너리 (null 값 제외). 필터가 없으면 None 반환.

    Examples:
        >>> await parse_filters_from_question("에러 로그 보여줘")
        {"level": "ERROR"}

        >>> await parse_filters_from_question("user-service의 WARNING")
        {"level": "WARN", "service_name": "user-service"}

        >>> await parse_filters_from_question("로그 분석해줘")
        None
    """
    try:
        # LLM으로 필터 추출
        result = await filter_chain.ainvoke({"question": question})

        # null 값 제거
        filters = {k: v for k, v in result.items() if v is not None}

        # 빈 딕셔너리면 None 반환
        return filters if filters else None

    except Exception as e:
        # 파싱 실패 시 None 반환 (질문에 필터가 없는 것으로 간주)
        print(f"Filter parsing error: {e}")
        return None


def _parse_relative_time(relative_time: str) -> Dict[str, str]:
    """
    상대적 시간 표현을 ISO 8601 형식의 시간 범위로 변환합니다.

    Args:
        relative_time: 상대적 시간 표현 (예: "1시간", "오늘", "어제")

    Returns:
        {"start": ISO_8601_timestamp, "end": ISO_8601_timestamp}
    """
    now = datetime.utcnow()

    # 시간 단위 파싱
    if "시간" in relative_time or "hour" in relative_time.lower():
        # "1시간", "2시간 전", "1 hour" 등
        hours = int(re.search(r'\d+', relative_time).group())
        start = now - timedelta(hours=hours)
        end = now

    elif relative_time in ["오늘", "금일", "today"]:
        # 오늘 00:00 ~ 현재
        start = now.replace(hour=0, minute=0, second=0, microsecond=0)
        end = now

    elif relative_time in ["어제", "yesterday"]:
        # 어제 00:00 ~ 어제 23:59:59
        yesterday = now - timedelta(days=1)
        start = yesterday.replace(hour=0, minute=0, second=0, microsecond=0)
        end = yesterday.replace(hour=23, minute=59, second=59, microsecond=999999)

    elif "주" in relative_time or "week" in relative_time.lower():
        # "이번주", "1주일", "1 week" 등
        if "이번" in relative_time or "this" in relative_time.lower():
            # 이번 주 월요일 00:00 ~ 현재
            days_since_monday = now.weekday()
            start = now - timedelta(days=days_since_monday)
            start = start.replace(hour=0, minute=0, second=0, microsecond=0)
        else:
            weeks = int(re.search(r'\d+', relative_time).group()) if re.search(r'\d+', relative_time) else 1
            start = now - timedelta(weeks=weeks)
        end = now

    elif "일" in relative_time or "day" in relative_time.lower():
        # "1일", "3일 전", "1 day" 등
        days = int(re.search(r'\d+', relative_time).group())
        start = now - timedelta(days=days)
        end = now

    elif "달" in relative_time or "month" in relative_time.lower():
        # "1달", "1개월", "1 month" 등
        months = int(re.search(r'\d+', relative_time).group()) if re.search(r'\d+', relative_time) else 1
        start = now - timedelta(days=months * 30)  # 근사치
        end = now

    else:
        # 파싱 실패 시 기본값: 7일
        start = now - timedelta(days=settings.DEFAULT_TIME_RANGE_DAYS)
        end = now

    return {
        "start": start.strftime("%Y-%m-%dT%H:%M:%SZ"),
        "end": end.strftime("%Y-%m-%dT%H:%M:%SZ")
    }


async def parse_time_range_from_question(question: str) -> Dict[str, str]:
    """
    자연어 질문에서 시간 범위를 추출합니다.
    시간 표현이 없으면 기본값(최근 7일)을 반환합니다.

    Args:
        question: 사용자의 자연어 질문

    Returns:
        시간 범위 딕셔너리 {"start": ISO_8601, "end": ISO_8601}

    Examples:
        >>> await parse_time_range_from_question("최근 1시간 에러")
        {"start": "2024-01-15T13:00:00Z", "end": "2024-01-15T14:00:00Z"}

        >>> await parse_time_range_from_question("오늘 로그")
        {"start": "2024-01-15T00:00:00Z", "end": "2024-01-15T14:30:00Z"}

        >>> await parse_time_range_from_question("로그 보여줘")
        {"start": "2024-01-08T14:30:00Z", "end": "2024-01-15T14:30:00Z"}  # 7일 전
    """
    try:
        # LLM으로 시간 표현 추출
        result = await time_chain.ainvoke({"question": question})

        # 상대적 시간 표현이 있으면 변환
        if result.get("relative_time"):
            return _parse_relative_time(result["relative_time"])

        # 절대적 날짜가 있으면 사용
        if result.get("absolute_start"):
            start_date = datetime.strptime(result["absolute_start"], "%Y-%m-%d")

            if result.get("absolute_end"):
                end_date = datetime.strptime(result["absolute_end"], "%Y-%m-%d")
                end_date = end_date.replace(hour=23, minute=59, second=59)
            else:
                # 종료일이 없으면 시작일 하루 전체
                end_date = start_date.replace(hour=23, minute=59, second=59)

            return {
                "start": start_date.strftime("%Y-%m-%dT%H:%M:%SZ"),
                "end": end_date.strftime("%Y-%m-%dT%H:%M:%SZ")
            }

    except Exception as e:
        print(f"Time range parsing error: {e}")

    # 기본값: 최근 7일
    now = datetime.utcnow()
    start = now - timedelta(days=settings.DEFAULT_TIME_RANGE_DAYS)

    return {
        "start": start.strftime("%Y-%m-%dT%H:%M:%SZ"),
        "end": now.strftime("%Y-%m-%dT%H:%M:%SZ")
    }


async def parse_question(question: str) -> tuple[Optional[Dict[str, Any]], Dict[str, str]]:
    """
    질문에서 필터와 시간 범위를 동시에 추출합니다.

    Args:
        question: 사용자의 자연어 질문

    Returns:
        (filters, time_range) 튜플
        - filters: 추출된 필터 (없으면 None)
        - time_range: 시간 범위 (항상 반환, 기본값 7일)

    Examples:
        >>> await parse_question("user-service의 최근 1시간 에러")
        (
            {"level": "ERROR", "service_name": "user-service"},
            {"start": "2024-01-15T13:00:00Z", "end": "2024-01-15T14:00:00Z"}
        )
    """
    # 동시에 추출 (비동기)
    import asyncio
    filters, time_range = await asyncio.gather(
        parse_filters_from_question(question),
        parse_time_range_from_question(question)
    )

    return filters, time_range
