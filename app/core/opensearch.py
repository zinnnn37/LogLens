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
        "use_ssl": settings.OPENSEARCH_USE_SSL,
    }

    # Add SSL settings only if SSL is enabled
    if settings.OPENSEARCH_USE_SSL:
        import ssl
        ssl_context = ssl.create_default_context()
        ssl_context.check_hostname = False
        ssl_context.verify_mode = ssl.CERT_NONE

        client_params.update({
            "verify_certs": False,
            "ssl_assert_hostname": False,
            "ssl_show_warn": False,
            "ssl_context": ssl_context,
        })

    # Add authentication (required when security plugin is enabled)
    if settings.OPENSEARCH_USER and settings.OPENSEARCH_PASSWORD:
        client_params["http_auth"] = (
            settings.OPENSEARCH_USER,
            settings.OPENSEARCH_PASSWORD,
        )

    return OpenSearch(**client_params)


# Global OpenSearch client instance
opensearch_client = create_opensearch_client()
