"""
Base validator interface for all validators
"""

from abc import ABC, abstractmethod
from typing import Any, Dict, List
from pydantic import BaseModel, Field


class ValidationResult(BaseModel):
    """Validation result model"""

    validator_name: str = Field(..., description="Validator name")
    passed: bool = Field(..., description="Validation passed or not")
    score: float = Field(..., ge=0.0, le=1.0, description="Validation score (0.0 ~ 1.0)")
    details: Dict[str, Any] = Field(default_factory=dict, description="Detailed validation results")
    feedback: str = Field(default="", description="Feedback message")
    suggestions: List[str] = Field(default_factory=list, description="Improvement suggestions")


class BaseValidator(ABC):
    """Base validator interface"""

    def __init__(self, config: Dict[str, Any] = None):
        """
        Args:
            config: Validator configuration (threshold, etc.)
        """
        self.config = config or {}

    @abstractmethod
    async def validate(self, data: Any) -> ValidationResult:
        """
        Perform validation (to be implemented by subclasses)

        Args:
            data: Data to validate

        Returns:
            ValidationResult: Validation result
        """
        pass

    @property
    @abstractmethod
    def name(self) -> str:
        """Validator name"""
        pass

    @property
    def threshold(self) -> float:
        """Pass threshold (default 0.7)"""
        return self.config.get("threshold", 0.7)
