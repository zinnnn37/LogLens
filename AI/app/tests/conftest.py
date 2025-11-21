"""
pytest configuration and shared fixtures
"""

import pytest
import sys
import os
from pathlib import Path

# Set dummy environment variables for testing
# These prevent import errors when modules try to initialize API clients
os.environ.setdefault('OPENAI_API_KEY', 'sk-dummy-test-key-for-pytest-only')
os.environ.setdefault('OPENSEARCH_HOST', 'localhost')
os.environ.setdefault('OPENSEARCH_PORT', '9200')
os.environ.setdefault('OPENSEARCH_USER', 'admin')
os.environ.setdefault('OPENSEARCH_PASSWORD', 'admin')

# Add app directory to Python path
app_dir = Path(__file__).parent.parent
sys.path.insert(0, str(app_dir.parent))


@pytest.fixture(scope="session")
def test_data_dir():
    """Test data directory"""
    return Path(__file__).parent / "data"


# Configure pytest-asyncio
pytest_plugins = ["pytest_asyncio"]
