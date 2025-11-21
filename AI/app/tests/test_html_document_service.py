"""
Tests for HTML Document Service
"""

import pytest
from pydantic_core import ValidationError
from app.services.html_document_service import HtmlDocumentService
from app.models.document import (
    AiHtmlDocumentRequest,
    DocumentType,
    DocumentFormat,
    StylePreferences,
)


@pytest.fixture
def html_service():
    """Create HtmlDocumentService instance"""
    return HtmlDocumentService()


@pytest.fixture
def project_analysis_request():
    """Sample project analysis request"""
    return AiHtmlDocumentRequest(
        project_uuid="550e8400-e29b-41d4-a716-446655440000",
        document_type=DocumentType.PROJECT_ANALYSIS,
        format=DocumentFormat.HTML,
        data={
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
                    "message": "NullPointerException in UserService",
                    "timestamp": "2024-01-15T10:30:00",
                    "componentName": "UserService",
                    "logLevel": "ERROR",
                    "traceId": "abc123",
                }
            ],
        },
        options={"includeCharts": True, "darkMode": False},
        style_preferences=StylePreferences(
            css_framework="tailwind", chart_library="chartjs", color_scheme="blue"
        ),
    )


@pytest.fixture
def error_analysis_request():
    """Sample error analysis request"""
    return AiHtmlDocumentRequest(
        log_id=12345,
        project_uuid="550e8400-e29b-41d4-a716-446655440000",
        document_type=DocumentType.ERROR_ANALYSIS,
        format=DocumentFormat.HTML,
        data={
            "errorLog": {
                "logId": 12345,
                "level": "ERROR",
                "message": "NullPointerException in UserService",
                "stackTrace": "java.lang.NullPointerException\n\tat com.example.UserService.getUser(UserService.java:45)",
                "timestamp": "2024-01-15T10:35:00",
                "componentName": "UserService",
                "traceId": "abc123",
            },
            "existingAnalysis": {
                "summary": "UserService의 getUser() 메서드에서 NPE 발생",
                "errorCause": "user_id에 해당하는 User 객체가 null",
                "solution": "[즉시] null 체크 추가\n[단기] Optional 사용\n[장기] 검증 로직 추가",
                "tags": ["SEVERITY_HIGH", "NullPointerException", "UserService"],
            },
            "relatedLogs": [
                {
                    "logId": 12346,
                    "message": "Related log 1",
                    "timestamp": "2024-01-15T10:35:01",
                    "componentName": "UserService",
                    "logLevel": "WARN",
                }
            ],
        },
        options={
            "includeRelatedLogs": True,
            "includeImpactAnalysis": True,
            "includeSimilarErrors": False,
            "includeCodeExamples": False,
            "maxRelatedLogs": 10,
        },
        style_preferences=StylePreferences(),
    )


class TestHtmlDocumentService:
    """Test HtmlDocumentService"""

    @pytest.mark.asyncio
    async def test_generate_project_analysis_html(
        self, html_service, project_analysis_request
    ):
        """Test project analysis HTML generation"""
        response = await html_service.generate_html_document(project_analysis_request)

        # Check response structure
        assert response.html_content is not None
        assert len(response.html_content) > 0
        assert response.metadata is not None
        assert response.validation_status is not None

        # Check HTML content
        assert "<!DOCTYPE html>" in response.html_content
        assert "<html" in response.html_content
        assert "<body" in response.html_content
        assert "Test Project" in response.html_content

        # Check validation
        assert response.validation_status.is_valid_html is True
        assert response.validation_status.has_required_sections is True

    @pytest.mark.asyncio
    async def test_generate_error_analysis_html(
        self, html_service, error_analysis_request
    ):
        """Test error analysis HTML generation"""
        response = await html_service.generate_html_document(error_analysis_request)

        # Check response structure
        assert response.html_content is not None
        assert len(response.html_content) > 0
        assert response.metadata is not None
        assert response.validation_status is not None

        # Check HTML content
        assert "<!DOCTYPE html>" in response.html_content
        assert "에러 분석 리포트" in response.html_content
        assert "NullPointerException" in response.html_content

        # Check validation
        assert response.validation_status.is_valid_html is True

    @pytest.mark.asyncio
    async def test_metadata_generation_project(
        self, html_service, project_analysis_request
    ):
        """Test metadata generation for project analysis"""
        response = await html_service.generate_html_document(project_analysis_request)

        metadata = response.metadata

        # Check basic metadata
        assert metadata.word_count is not None
        assert metadata.word_count > 0
        assert metadata.estimated_reading_time is not None
        assert "minutes" in metadata.estimated_reading_time

        # Check project-specific metadata
        assert metadata.health_score is not None
        assert 0 <= metadata.health_score <= 100
        assert metadata.critical_issues is not None
        assert metadata.total_issues is not None
        assert metadata.recommendations is not None

        # Check sections and libraries
        assert metadata.sections_generated is not None
        assert len(metadata.sections_generated) > 0
        assert metadata.css_libraries_used is not None
        assert "tailwindcss" in metadata.css_libraries_used

    @pytest.mark.asyncio
    async def test_metadata_generation_error(
        self, html_service, error_analysis_request
    ):
        """Test metadata generation for error analysis"""
        response = await html_service.generate_html_document(error_analysis_request)

        metadata = response.metadata

        # Check error-specific metadata
        assert metadata.severity is not None
        assert metadata.severity in ["LOW", "MEDIUM", "HIGH"]
        assert metadata.root_cause is not None
        assert metadata.affected_users is not None

    @pytest.mark.asyncio
    async def test_health_score_calculation(self, html_service):
        """Test health score calculation"""
        # Good health: low error rate
        assert html_service._calculate_health_score(10000, 10, 100) > 80

        # Medium health: moderate error rate
        assert 40 < html_service._calculate_health_score(10000, 200, 1000) < 80

        # Poor health: high error rate
        assert html_service._calculate_health_score(1000, 500, 300) < 40

        # Edge case: no logs
        assert html_service._calculate_health_score(0, 0, 0) == 100

    @pytest.mark.asyncio
    async def test_dark_mode_option(self, html_service, project_analysis_request):
        """Test dark mode option"""
        # Enable dark mode
        project_analysis_request.options["darkMode"] = True

        response = await html_service.generate_html_document(project_analysis_request)

        # Check dark mode styles in HTML
        assert "#1a1a1a" in response.html_content or "background-color: #1a1a1a" in response.html_content

    @pytest.mark.asyncio
    async def test_charts_option(self, html_service, project_analysis_request):
        """Test charts option"""
        # Enable charts
        project_analysis_request.options["includeCharts"] = True

        response = await html_service.generate_html_document(project_analysis_request)

        # Check for chart.js script
        assert "chart.js" in response.html_content.lower()
        assert "canvas" in response.html_content.lower()

        # Check metadata
        assert len(response.metadata.charts_included) > 0
        assert "chart.js" in response.metadata.js_libraries_used

    @pytest.mark.asyncio
    async def test_regeneration_feedback(self, html_service, project_analysis_request):
        """Test regeneration feedback"""
        # Add regeneration feedback
        project_analysis_request.regeneration_feedback = [
            "Missing <html> tag",
            "Missing <title> tag",
        ]

        response = await html_service.generate_html_document(project_analysis_request)

        # Check warnings include feedback
        assert len(response.validation_status.warnings) > 0
        assert any(
            "Regenerated due to" in w for w in response.validation_status.warnings
        )

    @pytest.mark.asyncio
    async def test_html_validation(self, html_service):
        """Test HTML validation"""
        # Valid HTML with required sections for project analysis
        valid_html = "<!DOCTYPE html><html><head><title>Test</title></head><body><h1>Project Overview</h1><div class='metrics'>Total Logs: 1000</div></body></html>"
        validation = html_service._validate_html(valid_html, DocumentType.PROJECT_ANALYSIS)
        assert validation.is_valid_html is True
        assert len(validation.warnings) == 0

        # Invalid HTML (missing tags)
        invalid_html = "<div>Test</div>"
        validation = html_service._validate_html(invalid_html, DocumentType.PROJECT_ANALYSIS)
        assert validation.is_valid_html is False
        assert len(validation.warnings) > 0

    def test_severity_determination(self, html_service):
        """Test severity determination"""
        assert html_service._determine_severity("ERROR") == "HIGH"
        assert html_service._determine_severity("WARN") == "MEDIUM"
        assert html_service._determine_severity("INFO") == "LOW"
        assert html_service._determine_severity("UNKNOWN") == "MEDIUM"

    def test_reading_time_estimation(self, html_service):
        """Test reading time estimation"""
        # Short text (~200 words)
        short_text = " ".join(["word"] * 200)
        assert "1 minutes" in html_service._estimate_reading_time(short_text)

        # Long text (~1000 words)
        long_text = " ".join(["word"] * 1000)
        assert "5 minutes" in html_service._estimate_reading_time(long_text)

    def test_format_number(self, html_service):
        """Test number formatting"""
        assert html_service._format_number(1000) == "1,000"
        assert html_service._format_number(1000000) == "1,000,000"
        assert html_service._format_number(123.45) == "123.45"

    def test_format_percentage(self, html_service):
        """Test percentage formatting"""
        assert html_service._format_percentage(50, 100) == "50.0%"
        assert html_service._format_percentage(1, 3) == "33.3%"
        assert html_service._format_percentage(0, 100) == "0.0%"
        assert html_service._format_percentage(10, 0) == "0%"

    @pytest.mark.asyncio
    async def test_invalid_document_type(self, html_service):
        """Test invalid document type"""
        # Pydantic validates enum at object creation time, so we catch it here
        with pytest.raises(ValidationError):
            invalid_request = AiHtmlDocumentRequest(
                project_uuid="test-uuid",
                document_type="INVALID_TYPE",  # This will fail at Pydantic validation level
                format=DocumentFormat.HTML,
                data={},
            )
