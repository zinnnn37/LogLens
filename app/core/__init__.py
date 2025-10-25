"""
Core modules - configuration and clients
"""

from app.core.config import settings
from app.core.opensearch import opensearch_client

__all__ = ["settings", "opensearch_client"]
