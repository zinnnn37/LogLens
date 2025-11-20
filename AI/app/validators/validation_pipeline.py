"""
Validation pipeline orchestrator
"""

from typing import List, Dict, Any
from app.validators.base_validator import BaseValidator, ValidationResult
from app.validators.error_type_validator import ErrorTypeValidator
from app.validators.structural_validator import StructuralValidator
from app.validators.content_validator import ContentValidator
from app.models.analysis import LogAnalysisResult


class ValidationPipeline:
    """Validation pipeline orchestrator"""

    def __init__(
        self,
        structural_threshold: float = 0.7,
        content_threshold: float = 0.6,
        overall_threshold: float = 0.65,
    ):
        """
        Args:
            structural_threshold: Structural validation threshold
            content_threshold: Content validation threshold
            overall_threshold: Overall passing threshold
        """
        self.validators: List[BaseValidator] = []
        self.overall_threshold = overall_threshold

        # Setup validators (order matters - ErrorTypeValidator runs first)
        self.validators.append(
            ErrorTypeValidator()  # 🆕 NEW - 1순위: 에러 타입 분류 검증
        )
        self.validators.append(
            StructuralValidator(config={"threshold": structural_threshold})
        )
        self.validators.append(
            ContentValidator(config={"threshold": content_threshold})
        )

    async def validate(
        self, analysis: LogAnalysisResult, log_data: dict
    ) -> Dict[str, Any]:
        """
        Execute entire validation pipeline

        Args:
            analysis: Log analysis result
            log_data: Original log data

        Returns:
            Dict: Comprehensive validation result
                - passed (bool): Overall pass/fail
                - overall_score (float): Overall score
                - individual_results (List): Results from each validator
                - suggestions (List[str]): All improvement suggestions
                - summary (str): Validation result summary
        """
        results = []

        # Execute each validator
        for validator in self.validators:
            if validator.name == "StructuralValidator":
                result = await validator.validate(analysis)
            else:
                # ErrorTypeValidator and ContentValidator require log_data
                result = await validator.validate(analysis, log_data)

            results.append(result)

        # Overall evaluation
        overall_passed = all(r.passed for r in results)
        overall_score = sum(r.score for r in results) / len(results)

        # Check if overall score meets threshold (separate from individual pass/fail)
        overall_passed = overall_passed and (overall_score >= self.overall_threshold)

        # Collect all suggestions
        all_suggestions = []
        for r in results:
            all_suggestions.extend(r.suggestions)

        return {
            "passed": overall_passed,
            "overall_score": overall_score,
            "individual_results": [r.dict() for r in results],
            "suggestions": all_suggestions,
            "summary": self._generate_summary(results, overall_score),
        }

    def _generate_summary(
        self, results: List[ValidationResult], overall_score: float
    ) -> str:
        """Generate validation result summary"""
        passed_count = sum(1 for r in results if r.passed)
        total_count = len(results)

        if passed_count == total_count:
            return f"✅ All validations passed ({passed_count}/{total_count}) - Overall score: {overall_score:.1%}"
        else:
            failed = [r.validator_name for r in results if not r.passed]
            return f"⚠️ Some validations failed ({passed_count}/{total_count}) - Overall score: {overall_score:.1%} - Failed: {', '.join(failed)}"
