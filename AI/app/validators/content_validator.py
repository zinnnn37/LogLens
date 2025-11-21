"""
Content validator: Korean quality, evidence alignment, action density validation
"""

import re
from typing import List, Dict
from app.validators.base_validator import BaseValidator, ValidationResult
from app.models.analysis import LogAnalysisResult


class ContentValidator(BaseValidator):
    """Content validation: Korean quality, evidence alignment, action density"""

    @property
    def name(self) -> str:
        return "ContentValidator"

    async def validate(
        self, analysis: LogAnalysisResult, log_data: dict
    ) -> ValidationResult:
        """
        Validate content quality

        Args:
            analysis: Log analysis result
            log_data: Original log data

        Returns:
            ValidationResult: Validation result
        """
        # 1. Korean quality
        korean_score = self._calculate_korean_quality(analysis)

        # 2. Evidence alignment (CORE FEATURE!)
        evidence_score = self._calculate_evidence_alignment(analysis, log_data)

        # 3. Action density
        action_metrics = self._calculate_action_density(analysis.solution or "")

        # Overall score (weighted average)
        score = (
            korean_score * 0.3
            + evidence_score * 0.4  # High weight on evidence alignment
            + action_metrics["specificity"] * 0.3
        )
        passed = score >= self.threshold

        details = {
            "korean_quality_score": korean_score,
            "evidence_alignment_score": evidence_score,  # CORE METRIC!
            "action_density": action_metrics["density"],
            "action_specificity": action_metrics["specificity"],
            "action_count": action_metrics["action_count"],
            "code_blocks": action_metrics["code_blocks"],
            "method_references": action_metrics["references"],
        }

        feedback_parts = [
            f"Content quality: {score:.1%}",
            f"(Korean: {korean_score:.1%}",
            f"Evidence: {evidence_score:.1%}",  # CORE!
            f"Action: {action_metrics['specificity']:.1%})",
        ]

        return ValidationResult(
            validator_name=self.name,
            passed=passed,
            score=score,
            details=details,
            feedback=" ".join(feedback_parts),
            suggestions=self._generate_suggestions(details),
        )

    def _calculate_korean_quality(self, analysis: LogAnalysisResult) -> float:
        """Calculate Korean usage quality"""
        texts = [
            analysis.summary,
            analysis.error_cause or "",
            analysis.solution or "",
        ]
        full_text = " ".join(texts)

        if not full_text:
            return 0.0

        # Korean character ratio (excluding markdown symbols)
        korean_chars = len(re.findall(r"[가-힣]", full_text))
        total_chars = len(
            re.sub(r"[\s`*#\[\]()]", "", full_text)
        )  # Exclude markdown symbols
        korean_ratio = korean_chars / max(total_chars, 1)

        # Penalty for long English sentences (50+ consecutive English chars)
        english_sentences = len(re.findall(r"[A-Za-z\s]{50,}", full_text))
        penalty = min(english_sentences * 0.1, 0.3)

        return max(korean_ratio - penalty, 0.0)

    def _calculate_evidence_alignment(
        self, analysis: LogAnalysisResult, log_data: dict
    ) -> float:
        """
        Validate evidence alignment (CORE FEATURE!)

        This function validates "content validity" of the answer.
        It checks if specific information from the original log
        (log_id, service, timestamp, etc.) is accurately included
        in the analysis result.

        Args:
            analysis: Log analysis result
            log_data: Original log data

        Returns:
            float: Evidence alignment score (0.0 ~ 1.0)
        """
        error_cause = analysis.error_cause or ""

        checks = {
            # Check if log_id is included
            "has_log_id": str(log_data.get("log_id", "")) in error_cause,
            # Check if service_name is included
            "has_service": (log_data.get("service_name", "") or "unknown")
            in error_cause,
            # Check if timestamp (date part) is included
            "has_timestamp": (log_data.get("timestamp", "")[:10] or "9999-99-99")
            in error_cause,
            # Check if log_level is included
            "has_level": (
                log_data.get("level", "") or log_data.get("log_level", "")
            )
            in error_cause,
            # Check if part of stack_trace is included (if exists)
            "has_stack_reference": (
                any(
                    line.strip()[:30] in error_cause
                    for line in (log_data.get("stack_trace", "") or "").split("\n")[:3]
                )
                if log_data.get("stack_trace")
                else True  # Pass if no stack_trace
            ),
        }

        # Calculate alignment ratio
        alignment_score = sum(checks.values()) / len(checks)

        return alignment_score

    def _calculate_action_density(self, solution: str) -> Dict[str, float]:
        """Calculate action item density and specificity"""
        if not solution:
            return {
                "density": 0.0,
                "specificity": 0.0,
                "action_count": 0,
                "code_blocks": 0,
                "references": 0,
            }

        # Action item count
        action_items = len(re.findall(r"- \[ \]", solution))

        # Code block count
        code_blocks = len(re.findall(r"```[\s\S]*?```", solution))

        # File/method reference count (e.g., `UserService.java`, `getUser()`)
        references = len(re.findall(r"`[A-Za-z]+\.[A-Za-z]+\(\)`", solution))

        # Density calculation (actions per 100 words)
        word_count = len(solution.split())
        density = action_items / max(word_count / 100, 1)

        # Specificity score (combined: code blocks, references, action items)
        specificity = min(
            (code_blocks * 0.3 + references * 0.2 + action_items * 0.1), 1.0
        )

        return {
            "density": density,
            "specificity": specificity,
            "action_count": action_items,
            "code_blocks": code_blocks,
            "references": references,
        }

    def _generate_suggestions(self, details: Dict) -> List[str]:
        """Generate improvement suggestions"""
        suggestions = []

        if details["korean_quality_score"] < 0.7:
            suggestions.append(
                "Increase Korean usage. Translate English descriptions to Korean."
            )

        if details["evidence_alignment_score"] < 0.6:
            suggestions.append(
                "Include specific log information (log_id, timestamp, service, stack_trace) in error_cause."
            )

        if details["action_count"] < 3:
            suggestions.append("Add at least 3 specific action items to solution.")

        if details["code_blocks"] == 0:
            suggestions.append("Include code examples (```code blocks```) in solution for specificity.")

        if details["method_references"] == 0:
            suggestions.append(
                "Add specific file or method references to solution (e.g., `UserService.getUser()`)."
            )

        return suggestions
