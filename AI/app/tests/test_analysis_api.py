"""
Tests for Analysis API Endpoints
"""

import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.models.document import DocumentType, DocumentFormat


@pytest.fixture
def client():
    """Create FastAPI test client"""
    return TestClient(app)


@pytest.fixture
def project_analysis_payload():
    """Sample project analysis request payload"""
    return {
        "project_uuid": "550e8400-e29b-41d4-a716-446655440000",
        "document_type": "PROJECT_ANALYSIS",
        "format": "HTML",
        "data": {
            "projectInfo": {
                "name": "Test Project",
                "uuid": "550e8400-e29b-41d4-a716-446655440000",
                "description": "Test project description",
            },
            "timeRange": {
                "startTime": "2024-01-01T00:00:00",
                "endTime": "2024-01-31T23:59:59",
            },
            "metrics": {
                "totalLogs": 10000,
                "errorCount": 150,
                "warnCount": 800,
                "infoCount": 9050,
                "avgResponseTime": 125.5,
            },
            "topErrors": [
                {
                    "logId": 1,
                    "message": "NullPointerException",
                    "timestamp": "2024-01-15T10:30:00",
                    "componentName": "UserService",
                    "logLevel": "ERROR",
                    "traceId": "abc123",
                }
            ],
        },
        "options": {"includeCharts": True, "darkMode": False},
        "style_preferences": {
            "css_framework": "tailwind",
            "chart_library": "chartjs",
            "color_scheme": "blue",
        },
    }


@pytest.fixture
def error_analysis_payload():
    """Sample error analysis request payload"""
    return {
        "log_id": 12345,
        "project_uuid": "550e8400-e29b-41d4-a716-446655440000",
        "document_type": "ERROR_ANALYSIS",
        "format": "HTML",
        "data": {
            "errorLog": {
                "logId": 12345,
                "level": "ERROR",
                "message": "NullPointerException in UserService",
                "stackTrace": "java.lang.NullPointerException\n\tat UserService.getUser",
                "timestamp": "2024-01-15T10:35:00",
                "componentName": "UserService",
                "traceId": "abc123",
            },
            "existingAnalysis": {
                "summary": "NPE 발생",
                "errorCause": "null 객체 참조",
                "solution": "[즉시] null 체크 추가",
                "tags": ["SEVERITY_HIGH", "NullPointerException"],
            },
            "relatedLogs": [],
        },
        "options": {
            "includeRelatedLogs": True,
            "includeImpactAnalysis": True,
        },
    }


class TestProjectAnalysisEndpoint:
    """Test project analysis HTML endpoint"""

    def test_generate_project_analysis_html_success(
        self, client, project_analysis_payload
    ):
        """Test successful project analysis HTML generation"""
        response = client.post(
            "/api/v2-langgraph/analysis/projects/html-document",
            json=project_analysis_payload,
        )

        assert response.status_code == 200

        data = response.json()

        # Check response structure
        assert "html_content" in data
        assert "metadata" in data
        assert "validation_status" in data

        # Check HTML content
        assert data["html_content"] is not None
        assert len(data["html_content"]) > 0
        assert "<!DOCTYPE html>" in data["html_content"]
        assert "Test Project" in data["html_content"]

        # Check metadata
        metadata = data["metadata"]
        assert metadata["word_count"] is not None
        assert metadata["word_count"] > 0
        assert metadata["estimated_reading_time"] is not None
        assert metadata["health_score"] is not None
        assert metadata["critical_issues"] is not None
        assert metadata["css_libraries_used"] is not None
        assert "tailwindcss" in metadata["css_libraries_used"]

        # Check validation status
        validation = data["validation_status"]
        assert validation["is_valid_html"] is True
        assert validation["has_required_sections"] is True

    def test_generate_with_dark_mode(self, client, project_analysis_payload):
        """Test HTML generation with dark mode"""
        project_analysis_payload["options"]["darkMode"] = True

        response = client.post(
            "/api/v2-langgraph/analysis/projects/html-document",
            json=project_analysis_payload,
        )

        assert response.status_code == 200
        data = response.json()

        # Check dark mode in HTML
        html = data["html_content"]
        assert "#1a1a1a" in html or "background-color: #1a1a1a" in html

    def test_generate_with_charts(self, client, project_analysis_payload):
        """Test HTML generation with charts"""
        project_analysis_payload["options"]["includeCharts"] = True

        response = client.post(
            "/api/v2-langgraph/analysis/projects/html-document",
            json=project_analysis_payload,
        )

        assert response.status_code == 200
        data = response.json()

        # Check chart.js inclusion
        html = data["html_content"].lower()
        assert "chart.js" in html
        assert "canvas" in html

        # Check metadata
        assert "chart.js" in data["metadata"]["js_libraries_used"]

    def test_invalid_document_type(self, client, project_analysis_payload):
        """Test with invalid document type"""
        project_analysis_payload["document_type"] = "ERROR_ANALYSIS"

        response = client.post(
            "/api/v2-langgraph/analysis/projects/html-document",
            json=project_analysis_payload,
        )

        assert response.status_code == 400
        assert "Invalid document type" in response.json()["detail"]

    def test_missing_required_fields(self, client):
        """Test with missing required fields"""
        invalid_payload = {
            "project_uuid": "test-uuid",
            # Missing document_type, format, data
        }

        response = client.post(
            "/api/v2-langgraph/analysis/projects/html-document",
            json=invalid_payload,
        )

        assert response.status_code == 422  # Validation error

    def test_empty_data(self, client):
        """Test with empty data"""
        payload = {
            "project_uuid": "550e8400-e29b-41d4-a716-446655440000",
            "document_type": "PROJECT_ANALYSIS",
            "format": "HTML",
            "data": {},  # Empty data
            "options": {},
        }

        response = client.post(
            "/api/v2-langgraph/analysis/projects/html-document",
            json=payload,
        )

        # Should still generate HTML but with empty content
        assert response.status_code == 200


class TestErrorAnalysisEndpoint:
    """Test error analysis HTML endpoint"""

    def test_generate_error_analysis_html_success(self, client, error_analysis_payload):
        """Test successful error analysis HTML generation"""
        response = client.post(
            "/api/v2-langgraph/analysis/errors/html-document",
            json=error_analysis_payload,
        )

        assert response.status_code == 200

        data = response.json()

        # Check response structure
        assert "html_content" in data
        assert "metadata" in data
        assert "validation_status" in data

        # Check HTML content
        assert "<!DOCTYPE html>" in data["html_content"]
        assert "에러 분석 리포트" in data["html_content"]
        assert "NullPointerException" in data["html_content"]

        # Check error-specific metadata
        metadata = data["metadata"]
        assert metadata["severity"] is not None
        assert metadata["severity"] in ["LOW", "MEDIUM", "HIGH"]
        assert metadata["root_cause"] is not None

        # Check validation
        assert data["validation_status"]["is_valid_html"] is True

    def test_generate_with_related_logs(self, client, error_analysis_payload):
        """Test error analysis with related logs"""
        error_analysis_payload["data"]["relatedLogs"] = [
            {
                "logId": 12346,
                "message": "Related log",
                "timestamp": "2024-01-15T10:35:01",
                "componentName": "UserService",
                "logLevel": "WARN",
            }
        ]

        response = client.post(
            "/api/v2-langgraph/analysis/errors/html-document",
            json=error_analysis_payload,
        )

        assert response.status_code == 200
        data = response.json()

        # Check related logs in HTML
        html = data["html_content"]
        assert "관련 로그" in html or "Related log" in html

    def test_generate_with_impact_analysis(self, client, error_analysis_payload):
        """Test error analysis with impact analysis"""
        error_analysis_payload["options"]["includeImpactAnalysis"] = True

        response = client.post(
            "/api/v2-langgraph/analysis/errors/html-document",
            json=error_analysis_payload,
        )

        assert response.status_code == 200
        data = response.json()

        # Check impact analysis in HTML
        html = data["html_content"]
        assert "영향 분석" in html or "Impact" in html

    def test_invalid_document_type(self, client, error_analysis_payload):
        """Test with invalid document type"""
        error_analysis_payload["document_type"] = "PROJECT_ANALYSIS"

        response = client.post(
            "/api/v2-langgraph/analysis/errors/html-document",
            json=error_analysis_payload,
        )

        assert response.status_code == 400
        assert "Invalid document type" in response.json()["detail"]

    def test_missing_log_id(self, client, error_analysis_payload):
        """Test with missing log_id"""
        del error_analysis_payload["log_id"]

        response = client.post(
            "/api/v2-langgraph/analysis/errors/html-document",
            json=error_analysis_payload,
        )

        # Should still work as log_id is optional in the model
        # but will be None
        assert response.status_code == 200

    def test_regeneration_feedback(self, client, error_analysis_payload):
        """Test with regeneration feedback"""
        error_analysis_payload["regeneration_feedback"] = [
            "Missing <html> tag",
            "Invalid structure",
        ]

        response = client.post(
            "/api/v2-langgraph/analysis/errors/html-document",
            json=error_analysis_payload,
        )

        assert response.status_code == 200
        data = response.json()

        # Check warnings include feedback
        warnings = data["validation_status"]["warnings"]
        assert any("Regenerated due to" in w for w in warnings)


class TestResponseSchema:
    """Test response schema validation"""

    def test_response_schema_project(self, client, project_analysis_payload):
        """Test project analysis response schema"""
        response = client.post(
            "/api/v2-langgraph/analysis/projects/html-document",
            json=project_analysis_payload,
        )

        assert response.status_code == 200
        data = response.json()

        # Required fields
        assert "html_content" in data
        assert "metadata" in data
        assert "validation_status" in data

        # Metadata fields
        metadata = data["metadata"]
        required_metadata = [
            "word_count",
            "estimated_reading_time",
            "sections_generated",
            "css_libraries_used",
            "js_libraries_used",
            "generation_time",
        ]
        for field in required_metadata:
            assert field in metadata

        # Project-specific metadata
        project_metadata = ["health_score", "critical_issues", "total_issues"]
        for field in project_metadata:
            assert field in metadata

        # Validation status fields
        validation = data["validation_status"]
        assert "is_valid_html" in validation
        assert "has_required_sections" in validation
        assert "warnings" in validation
        assert isinstance(validation["warnings"], list)

    def test_response_schema_error(self, client, error_analysis_payload):
        """Test error analysis response schema"""
        response = client.post(
            "/api/v2-langgraph/analysis/errors/html-document",
            json=error_analysis_payload,
        )

        assert response.status_code == 200
        data = response.json()

        # Error-specific metadata
        metadata = data["metadata"]
        error_metadata = ["severity", "root_cause", "affected_users"]
        for field in error_metadata:
            assert field in metadata
