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

    class Config:
        env_file = ".env"
        case_sensitive = True


# Global settings instance
settings = Settings()
