"""
OpenSearch client configuration
"""

from opensearchpy import OpenSearch
from app.core.config import settings


def create_opensearch_client() -> OpenSearch:
    """Create and return OpenSearch client"""

    # Connection parameters
    client_params = {
        "hosts": [{"host": settings.OPENSEARCH_HOST, "port": settings.OPENSEARCH_PORT}],
        "http_compress": True,
        "use_ssl": False,
        "verify_certs": False,
        "ssl_assert_hostname": False,
        "ssl_show_warn": False,
    }

    # Add authentication if provided
    if settings.OPENSEARCH_USER and settings.OPENSEARCH_PASSWORD:
        client_params["http_auth"] = (
            settings.OPENSEARCH_USER,
            settings.OPENSEARCH_PASSWORD,
        )

    return OpenSearch(**client_params)


# Global OpenSearch client instance
opensearch_client = create_opensearch_client()
