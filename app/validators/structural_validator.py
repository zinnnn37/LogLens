"""
Structural validator: Format, field existence, markdown structure validation
"""

from typing import List
from app.validators.base_validator import BaseValidator, ValidationResult
from app.models.analysis import LogAnalysisResult


class StructuralValidator(BaseValidator):
    """Structural validation: format, fields, markdown structure"""

    @property
    def name(self) -> str:
        return "StructuralValidator"

    async def validate(self, analysis: LogAnalysisResult) -> ValidationResult:
        """
        Validate structural requirements

        Args:
            analysis: Log analysis result

        Returns:
            ValidationResult: Validation result
        """
        checks = {
            # Required fields
            "has_summary": bool(analysis.summary),
            "has_error_cause": bool(analysis.error_cause),
            "has_solution": bool(analysis.solution),
            "has_tags": len(analysis.tags) > 0,
            # Summary validation
            "summary_length_ok": 20 <= len(analysis.summary) <= 300,
            "summary_has_markdown": any(
                marker in analysis.summary for marker in ["**", "`"]
            ),
            # Error cause validation
            "has_evidence_section": "### 근거 데이터"
            in (analysis.error_cause or ""),
            "evidence_has_stack_trace": any(
                marker in (analysis.error_cause or "")
                for marker in ["Stack Trace", "스택 트레이스", "호출 트레이스"]
            ),
            "error_cause_length_ok": len(analysis.error_cause or "") >= 100,
            # Solution validation
            "has_priority_headers": all(
                header in (analysis.solution or "")
                for header in ["### 즉시 조치", "### 단기 개선", "### 장기 전략"]
            ),
            "has_checkboxes": "- [ ]" in (analysis.solution or ""),
            "has_code_blocks": "```" in (analysis.solution or ""),
            "solution_length_ok": len(analysis.solution or "") >= 100,
            # Tags validation
            "has_severity_tag": any("SEVERITY_" in tag for tag in analysis.tags),
        }

        score = sum(checks.values()) / len(checks)
        passed = score >= self.threshold

        # Collect failed checks
        failed_checks = [key for key, value in checks.items() if not value]
        suggestions = self._generate_suggestions(failed_checks)

        return ValidationResult(
            validator_name=self.name,
            passed=passed,
            score=score,
            details=checks,
            feedback=f"Structural completeness: {score:.1%} ({sum(checks.values())}/{len(checks)} passed)",
            suggestions=suggestions,
        )

    def _generate_suggestions(self, failed_checks: List[str]) -> List[str]:
        """Generate improvement suggestions based on failed checks"""
        suggestions = []

        if "has_evidence_section" in failed_checks:
            suggestions.append("Add '### 근거 데이터' section to error_cause.")

        if "has_priority_headers" in failed_checks:
            suggestions.append(
                "Add '### 즉시 조치', '### 단기 개선', '### 장기 전략' headers to solution."
            )

        if "has_checkboxes" in failed_checks:
            suggestions.append(
                "Use '- [ ]' checkbox format for each action item in solution."
            )

        if "has_code_blocks" in failed_checks:
            suggestions.append("Add code examples (```code blocks```) to solution.")

        if "has_severity_tag" in failed_checks:
            suggestions.append(
                "Add severity tag (SEVERITY_CRITICAL/HIGH/MEDIUM/LOW) to tags."
            )

        if "summary_has_markdown" in failed_checks:
            suggestions.append("Use **bold** or `code` markdown in summary.")

        if "evidence_has_stack_trace" in failed_checks:
            suggestions.append("Include Stack Trace information in error_cause.")

        if "summary_length_ok" in failed_checks:
            suggestions.append("Write summary between 20-300 characters.")

        if "error_cause_length_ok" in failed_checks:
            suggestions.append("Write error_cause at least 100 characters.")

        if "solution_length_ok" in failed_checks:
            suggestions.append("Write solution at least 100 characters.")

        return suggestions
