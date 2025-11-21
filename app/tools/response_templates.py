"""
응답 메시지 표준 템플릿
- 무한루프 방지를 위한 명확한 "데이터 없음" 메시지 템플릿
"""


def get_empty_result_message(
    conditions: str,
    time_hours: int,
    suggestions: list = None
) -> str:
    """
    표준 "데이터 없음" 메시지를 생성합니다.

    Args:
        conditions: 검색 조건 설명 (예: "min_execution_time=1500ms, service_name=user-service")
        time_hours: 분석 기간 (시간)
        suggestions: 추가 제안 리스트 (선택)

    Returns:
        표준 형식의 "데이터 없음" 메시지
    """
    base_message = f"""❌ 데이터 없음

검색 조건: {conditions}
분석 기간: 최근 {time_hours}시간
결과: 0건

💡 이것은 정상 결과입니다. 다음을 확인하세요:
1. 시간 범위 확대 (time_hours 증가)
2. 필터 조건 완화
3. 다른 도구 사용"""

    if suggestions:
        base_message += "\n\n추가 제안:\n"
        for i, suggestion in enumerate(suggestions, 1):
            base_message += f"{i}. {suggestion}\n"

    base_message += "\n⚠️ 이 도구를 다시 호출하지 마세요."

    return base_message


def get_conditional_failure_message(
    condition: str,
    current_value: str,
    required_value: str,
    tool_name: str
) -> str:
    """
    조건부 실패 메시지를 생성합니다 (예: 최소 데이터 포인트 미달).

    Args:
        condition: 조건 이름 (예: "최소 데이터 포인트")
        current_value: 현재 값 (예: "5개")
        required_value: 필요한 값 (예: "10개")
        tool_name: 도구 이름

    Returns:
        조건부 실패 메시지
    """
    return f"""❌ 조건 미충족

{condition}: {current_value} (필요: {required_value})

💡 {tool_name}은(는) {required_value} 이상의 데이터가 필요합니다.

해결 방법:
1. 시간 범위를 늘려주세요 (time_hours 증가)
2. 다른 분석 도구를 사용하세요
3. 데이터 수집 설정을 확인하세요

⚠️ 이 도구를 파라미터 변경해서 재호출하지 마세요.
"""


def get_no_problem_message(
    analysis_type: str,
    time_hours: int,
    note: str = None
) -> str:
    """
    "문제 없음" 메시지를 생성합니다 (데이터 없음과 명확히 구분).

    Args:
        analysis_type: 분석 유형 (예: "동시성 문제", "에러 패턴")
        time_hours: 분석 기간
        note: 추가 설명 (선택)

    Returns:
        "문제 없음" 메시지
    """
    message = f"""✅ {analysis_type} 없음

분석 기간: 최근 {time_hours}시간
결과: {analysis_type}이(가) 감지되지 않았습니다.

🟢 이것은 긍정적인 결과입니다."""

    if note:
        message += f"\n\n📝 참고: {note}"

    message += "\n\n⚠️ 이 결과는 최종 결과입니다. 재시도하지 마세요."

    return message
