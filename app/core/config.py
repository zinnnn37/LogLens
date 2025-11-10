"""
Application configuration using Pydantic Settings
"""

from pydantic_settings import BaseSettings
from typing import Optional


class Settings(BaseSettings):
    """Application settings"""

    # Application
    APP_NAME: str = "log-analysis-api"
    APP_VERSION: str = "1.0.0"
    ENVIRONMENT: str = "development"

    # CORS Settings
    CORS_ORIGINS: str = "http://localhost:3000,http://localhost:5173"  # Comma-separated list

    # OpenAI (GMS)
    OPENAI_API_KEY: str  # GMS_KEY from SSAFY
    OPENAI_BASE_URL: str = "https://gms.ssafy.io/gmsapi/api.openai.com/v1"
    EMBEDDING_MODEL: str = "text-embedding-3-large"
    LLM_MODEL: str = "gpt-4o-mini"

    # OpenSearch
    OPENSEARCH_HOST: str = "opensearch"
    OPENSEARCH_PORT: int = 9200
    OPENSEARCH_USER: str = "admin"  # Default for development
    OPENSEARCH_PASSWORD: str = "Admin123!@#"  # Default for development
    OPENSEARCH_USE_SSL: bool = False  # OpenSearch security plugin disabled

    # Analysis Settings
    SIMILARITY_THRESHOLD: float = 0.8
    MAX_CONTEXT_LOGS: int = 5

    # Caching Settings
    DEFAULT_CACHE_TTL: int = 1800  # 30 minutes (default)
    SHORT_CACHE_TTL: int = 600     # 10 minutes (for relative time queries like "1시간 전")
    LONG_CACHE_TTL: int = 86400    # 1 day (for absolute time queries like "2024-01-15")
    CACHE_CANDIDATE_SIZE: int = 10  # Number of candidates to check for metadata matching

    # Time Range Settings
    DEFAULT_TIME_RANGE_DAYS: int = 7  # Default time range in days when not specified (1 week)

    # Map-Reduce Settings (for token optimization in trace analysis)
    ENABLE_MAP_REDUCE: bool = True  # Enable Map-Reduce pattern for large log sets
    LOG_CHUNK_SIZE: int = 5  # Number of logs per chunk in Map phase
    MAP_REDUCE_THRESHOLD: int = 10  # Apply Map-Reduce only when logs > this threshold

    # Validation Settings (답변 품질 검증)
    VALIDATION_STRUCTURAL_THRESHOLD: float = 0.7  # 구조적 검증 임계값
    VALIDATION_CONTENT_THRESHOLD: float = 0.6     # 내용 검증 임계값 (근거 일치성 포함)
    VALIDATION_OVERALL_THRESHOLD: float = 0.65    # 종합 통과 기준
    VALIDATION_MAX_RETRIES: int = 2               # 검증 실패 시 최대 재시도 횟수
    VALIDATION_ENABLE_RETRY: bool = True          # 검증 실패 시 재시도 활성화

    @property
    def cors_origins_list(self) -> list[str]:
        """Parse CORS_ORIGINS string into list"""
        if self.CORS_ORIGINS == "*":
            return ["*"]
        return [origin.strip() for origin in self.CORS_ORIGINS.split(",") if origin.strip()]

    class Config:
        env_file = ".env"
        case_sensitive = True
        extra = "ignore"  # Allow extra fields from .env that aren't used by AI service


# Global settings instance
settings = Settings()
