"""
Core modules - configuration and clients
"""

from app.core.config import settings

# OpenSearch client is imported lazily to avoid requiring opensearchpy for testing
# Import directly when needed: from app.core.opensearch import opensearch_client

__all__ = ["settings"]
