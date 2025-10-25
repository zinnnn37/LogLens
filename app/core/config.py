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

    # OpenAI
    OPENAI_API_KEY: str
    EMBEDDING_MODEL: str = "text-embedding-3-large"
    LLM_MODEL: str = "gpt-4o-mini"

    # OpenSearch
    OPENSEARCH_HOST: str = "localhost"
    OPENSEARCH_PORT: int = 9200
    OPENSEARCH_USER: Optional[str] = None
    OPENSEARCH_PASSWORD: Optional[str] = None

    # Kafka
    KAFKA_BOOTSTRAP_SERVERS: str = "localhost:9092"
    KAFKA_GROUP_ID: str = "log-analysis-consumer"
    KAFKA_TOPIC: str = "application-logs"

    # Analysis Settings
    SIMILARITY_THRESHOLD: float = 0.8
    MAX_CONTEXT_LOGS: int = 5

    class Config:
        env_file = ".env"
        case_sensitive = True


# Global settings instance
settings = Settings()
