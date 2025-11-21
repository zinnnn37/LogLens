"""
Validation Tools for Log Analysis Agent

2개의 검증 도구:
1. validate_korean: 한국어 출력 검증 (summary, error_cause, solution에 한글 포함 여부)
2. validate_quality: 품질 검증 (ValidationPipeline 사용)
"""

import re
from typing import Dict, Any
from langchain_core.tools import tool

from app.validators.validation_pipeline import ValidationPipeline
from app.models.analysis import LogAnalysisResult
from app.core.config import settings


def create_validate_korean_tool(project_uuid: str):
    """
    한국어 출력 검증 도구 생성 (project_uuid 바인딩)
    """

    @tool
    async def validate_korean(analysis_json: str) -> str:
        """
        분석 결과의 주요 필드에 한국어가 포함되어 있는지 검증합니다.

        Args:
            analysis_json: 분석 결과 JSON 문자열 (summary, error_cause, solution 포함)

        Returns:
            검증 결과 JSON 문자열 (valid: bool, details: dict)
        """
        import json

        try:
            analysis = json.loads(analysis_json)
        except json.JSONDecodeError as e:
            return f"ERROR: Invalid JSON format: {str(e)}"

        # 한글 패턴 (가-힣, ㄱ-ㅎ, ㅏ-ㅣ)
        korean_pattern = re.compile(r'[가-힣ㄱ-ㅎㅏ-ㅣ]')

        def contains_korean(text: str) -> bool:
            """텍스트에 한글이 포함되어 있는지 확인"""
            if not text:
                return False
            return bool(korean_pattern.search(text))

        # 각 필드 검증
        summary = analysis.get("summary", "")
        error_cause = analysis.get("error_cause", "")
        solution = analysis.get("solution", "")

        summary_valid = contains_korean(summary)
        error_cause_valid = contains_korean(error_cause) if error_cause else True  # Optional field
        solution_valid = contains_korean(solution) if solution else True  # Optional field

        # 전체 유효성 (summary는 필수, 나머지는 있으면 검증)
        overall_valid = summary_valid

        if error_cause:  # error_cause가 있으면 한국어 포함 필수
            overall_valid = overall_valid and error_cause_valid

        if solution:  # solution이 있으면 한국어 포함 필수
            overall_valid = overall_valid and solution_valid

        # 상세 결과
        details = {
            "summary_valid": summary_valid,
            "error_cause_valid": error_cause_valid if error_cause else None,
            "solution_valid": solution_valid if solution else None,
        }

        # 실패한 필드 목록
        failed_fields = []
        if not summary_valid:
            failed_fields.append("summary")
        if error_cause and not error_cause_valid:
            failed_fields.append("error_cause")
        if solution and not solution_valid:
            failed_fields.append("solution")

        return json.dumps({
            "valid": overall_valid,
            "details": details,
            "failed_fields": failed_fields,
            "message": "모든 필드에 한국어가 포함되어 있습니다." if overall_valid else f"다음 필드에 한국어가 없습니다: {', '.join(failed_fields)}"
        }, ensure_ascii=False)

    return validate_korean


def create_validate_quality_tool(project_uuid: str):
    """
    품질 검증 도구 생성 (project_uuid 바인딩)
    ValidationPipeline을 사용하여 구조적 검증 + 내용 검증
    """

    # ValidationPipeline 초기화 (설정값 사용)
    validation_pipeline = ValidationPipeline(
        structural_threshold=settings.VALIDATION_STRUCTURAL_THRESHOLD,
        content_threshold=settings.VALIDATION_CONTENT_THRESHOLD,
        overall_threshold=settings.VALIDATION_OVERALL_THRESHOLD,
    )

    @tool
    async def validate_quality(
        analysis_json: str,
        log_data_json: str
    ) -> str:
        """
        분석 결과의 품질을 검증합니다 (구조적 검증 + 내용 검증).

        Args:
            analysis_json: 분석 결과 JSON 문자열
            log_data_json: 원본 로그 데이터 JSON 문자열

        Returns:
            검증 결과 JSON 문자열 (passed, overall_score, suggestions 등)
        """
        import json

        try:
            analysis_dict = json.loads(analysis_json)
            log_data = json.loads(log_data_json)
        except json.JSONDecodeError as e:
            return f"ERROR: Invalid JSON format: {str(e)}"

        try:
            # LogAnalysisResult 객체로 변환
            analysis = LogAnalysisResult(**analysis_dict)

            # ValidationPipeline 실행
            validation_result = await validation_pipeline.validate(
                analysis=analysis,
                log_data=log_data
            )

            # 결과 반환
            return json.dumps({
                "passed": validation_result["passed"],
                "overall_score": validation_result["overall_score"],
                "suggestions": validation_result["suggestions"],
                "summary": validation_result["summary"],
                "individual_results": validation_result["individual_results"]
            }, ensure_ascii=False)

        except Exception as e:
            return f"ERROR: {str(e)}"

    return validate_quality
