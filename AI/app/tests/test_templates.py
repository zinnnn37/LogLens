"""
Tests for Jinja2 Templates
"""

import pytest
from pathlib import Path
from jinja2 import Environment, FileSystemLoader
from app.models.document import StylePreferences


@pytest.fixture
def jinja_env():
    """Create Jinja2 environment"""
    template_dir = Path(__file__).parent.parent / "templates"
    env = Environment(
        loader=FileSystemLoader(template_dir),
        autoescape=True,
        trim_blocks=True,
        lstrip_blocks=True,
    )

    # Register custom filters (same as in HtmlDocumentService)
    def format_number(value):
        """Format number with thousands separator"""
        if isinstance(value, (int, float)):
            return f"{value:,}"
        return str(value)

    def format_percentage(value, total):
        """Format percentage"""
        if total == 0:
            return "0%"
        percentage = (value / total) * 100
        return f"{percentage:.1f}%"

    env.filters["format_number"] = format_number
    env.filters["format_percentage"] = format_percentage

    return env


@pytest.fixture
def project_template_context():
    """Sample context for project analysis template"""
    return {
        "project_info": {
            "name": "Test Project",
            "uuid": "550e8400-e29b-41d4-a716-446655440000",
            "description": "Test project description",
        },
        "time_range": {
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
        "top_errors": [
            {
                "logId": 1,
                "message": "NullPointerException",
                "timestamp": "2024-01-15T10:30:00",
                "componentName": "UserService",
                "logLevel": "ERROR",
                "traceId": "abc123",
            }
        ],
        "options": {"includeCharts": True, "darkMode": False},
        "style": {
            "css_framework": "tailwind",
            "chart_library": "chartjs",
            "color_scheme": "blue",
            "dark_mode": False,
        },
    }


@pytest.fixture
def error_template_context():
    """Sample context for error analysis template"""
    return {
        "log_id": 12345,
        "error_log": {
            "logId": 12345,
            "level": "ERROR",
            "message": "NullPointerException in UserService",
            "stackTrace": "java.lang.NullPointerException\n\tat UserService.getUser",
            "timestamp": "2024-01-15T10:35:00",
            "componentName": "UserService",
            "traceId": "abc123",
        },
        "existing_analysis": {
            "summary": "NPE 발생",
            "errorCause": "null 객체 참조",
            "solution": "[즉시] null 체크 추가\n[단기] Optional 사용",
            "tags": ["SEVERITY_HIGH", "NullPointerException"],
        },
        "related_logs": [
            {
                "logId": 12346,
                "message": "Related log",
                "timestamp": "2024-01-15T10:35:01",
                "componentName": "UserService",
                "logLevel": "WARN",
            }
        ],
        "options": {
            "includeRelatedLogs": True,
            "includeImpactAnalysis": True,
        },
        "style": {
            "css_framework": "tailwind",
            "chart_library": "chartjs",
            "color_scheme": "blue",
            "dark_mode": False,
        },
    }


class TestProjectAnalysisTemplate:
    """Test project analysis template rendering"""

    def test_template_exists(self, jinja_env):
        """Test that project analysis template exists"""
        template = jinja_env.get_template("analysis/project_analysis.html")
        assert template is not None

    def test_template_renders(self, jinja_env, project_template_context):
        """Test that template renders without errors"""
        template = jinja_env.get_template("analysis/project_analysis.html")
        html = template.render(**project_template_context)

        assert html is not None
        assert len(html) > 0

    def test_required_html_tags(self, jinja_env, project_template_context):
        """Test that required HTML tags are present"""
        template = jinja_env.get_template("analysis/project_analysis.html")
        html = template.render(**project_template_context)

        # Required HTML structure
        assert "<!DOCTYPE html>" in html
        assert "<html" in html
        assert "<head" in html
        assert "<body" in html
        assert "</body>" in html
        assert "</html>" in html
        assert "<title>" in html

    def test_project_info_binding(self, jinja_env, project_template_context):
        """Test that project info is correctly bound"""
        template = jinja_env.get_template("analysis/project_analysis.html")
        html = template.render(**project_template_context)

        # Check project info
        assert "Test Project" in html
        assert "Test project description" in html

    def test_metrics_binding(self, jinja_env, project_template_context):
        """Test that metrics are correctly bound"""
        template = jinja_env.get_template("analysis/project_analysis.html")
        html = template.render(**project_template_context)

        # Check metrics (formatted with commas)
        assert "10,000" in html or "10000" in html
        assert "150" in html
        assert "800" in html
        assert "125.5" in html

    def test_tailwind_css_loading(self, jinja_env, project_template_context):
        """Test that Tailwind CSS is loaded"""
        template = jinja_env.get_template("analysis/project_analysis.html")
        html = template.render(**project_template_context)

        assert "cdn.tailwindcss.com" in html

    def test_chartjs_loading(self, jinja_env, project_template_context):
        """Test that Chart.js is loaded when includeCharts is true"""
        project_template_context["options"]["includeCharts"] = True

        template = jinja_env.get_template("analysis/project_analysis.html")
        html = template.render(**project_template_context)

        assert "chart.js" in html.lower()
        assert "<canvas" in html

    def test_no_chartjs_when_disabled(self, jinja_env, project_template_context):
        """Test that Chart.js is not loaded when includeCharts is false"""
        project_template_context["options"]["includeCharts"] = False

        template = jinja_env.get_template("analysis/project_analysis.html")
        html = template.render(**project_template_context)

        # Chart.js script should not be present
        assert "chart.umd.min.js" not in html

    def test_dark_mode_styles(self, jinja_env, project_template_context):
        """Test that dark mode styles are applied"""
        project_template_context["style"]["dark_mode"] = True

        template = jinja_env.get_template("analysis/project_analysis.html")
        html = template.render(**project_template_context)

        # Check for dark mode colors
        assert "#1a1a1a" in html

    def test_light_mode_styles(self, jinja_env, project_template_context):
        """Test that light mode styles are applied"""
        project_template_context["style"]["dark_mode"] = False

        template = jinja_env.get_template("analysis/project_analysis.html")
        html = template.render(**project_template_context)

        # Check for light mode colors
        assert "#f5f5f5" in html

    def test_color_scheme_blue(self, jinja_env, project_template_context):
        """Test blue color scheme"""
        project_template_context["style"]["color_scheme"] = "blue"

        template = jinja_env.get_template("analysis/project_analysis.html")
        html = template.render(**project_template_context)

        assert "#3b82f6" in html

    def test_top_errors_display(self, jinja_env, project_template_context):
        """Test that top errors are displayed"""
        template = jinja_env.get_template("analysis/project_analysis.html")
        html = template.render(**project_template_context)

        # Check error message
        assert "NullPointerException" in html
        assert "UserService" in html

    def test_health_score_display(self, jinja_env, project_template_context):
        """Test that health score is calculated and displayed"""
        template = jinja_env.get_template("analysis/project_analysis.html")
        html = template.render(**project_template_context)

        # Health score should be present
        assert "건강 점수" in html or "health" in html.lower()

    def test_recommendations_section(self, jinja_env, project_template_context):
        """Test that recommendations section is present"""
        template = jinja_env.get_template("analysis/project_analysis.html")
        html = template.render(**project_template_context)

        assert "권장사항" in html or "recommendation" in html.lower()


class TestErrorAnalysisTemplate:
    """Test error analysis template rendering"""

    def test_template_exists(self, jinja_env):
        """Test that error analysis template exists"""
        template = jinja_env.get_template("analysis/error_analysis.html")
        assert template is not None

    def test_template_renders(self, jinja_env, error_template_context):
        """Test that template renders without errors"""
        template = jinja_env.get_template("analysis/error_analysis.html")
        html = template.render(**error_template_context)

        assert html is not None
        assert len(html) > 0

    def test_required_html_tags(self, jinja_env, error_template_context):
        """Test that required HTML tags are present"""
        template = jinja_env.get_template("analysis/error_analysis.html")
        html = template.render(**error_template_context)

        assert "<!DOCTYPE html>" in html
        assert "<html" in html
        assert "<head" in html
        assert "<body" in html
        assert "</body>" in html
        assert "</html>" in html

    def test_error_info_binding(self, jinja_env, error_template_context):
        """Test that error info is correctly bound"""
        template = jinja_env.get_template("analysis/error_analysis.html")
        html = template.render(**error_template_context)

        # Check error info
        assert "12345" in html  # log_id
        assert "NullPointerException" in html
        assert "UserService" in html

    def test_stack_trace_display(self, jinja_env, error_template_context):
        """Test that stack trace is displayed"""
        template = jinja_env.get_template("analysis/error_analysis.html")
        html = template.render(**error_template_context)

        assert "스택 트레이스" in html or "stack" in html.lower()
        assert "java.lang.NullPointerException" in html

    def test_ai_analysis_display(self, jinja_env, error_template_context):
        """Test that AI analysis is displayed"""
        template = jinja_env.get_template("analysis/error_analysis.html")
        html = template.render(**error_template_context)

        # Check AI analysis sections
        assert "요약" in html or "summary" in html.lower()
        assert "근본 원인" in html or "cause" in html.lower()
        assert "해결 방안" in html or "solution" in html.lower()

    def test_related_logs_display(self, jinja_env, error_template_context):
        """Test that related logs are displayed"""
        template = jinja_env.get_template("analysis/error_analysis.html")
        html = template.render(**error_template_context)

        # Check related logs
        assert "관련 로그" in html or "related" in html.lower()
        assert "Related log" in html

    def test_impact_analysis_display(self, jinja_env, error_template_context):
        """Test that impact analysis is displayed when enabled"""
        error_template_context["options"]["includeImpactAnalysis"] = True

        template = jinja_env.get_template("analysis/error_analysis.html")
        html = template.render(**error_template_context)

        assert "영향 분석" in html or "impact" in html.lower()

    def test_severity_badge(self, jinja_env, error_template_context):
        """Test that severity badge is displayed"""
        template = jinja_env.get_template("analysis/error_analysis.html")
        html = template.render(**error_template_context)

        # Check severity badge for ERROR level
        assert "ERROR" in html
        assert "severity" in html.lower()

    def test_recommendations_section(self, jinja_env, error_template_context):
        """Test that recommendations section is present"""
        template = jinja_env.get_template("analysis/error_analysis.html")
        html = template.render(**error_template_context)

        assert "권장사항" in html or "recommendation" in html.lower()

    def test_dark_mode_styles(self, jinja_env, error_template_context):
        """Test that dark mode styles are applied"""
        error_template_context["style"]["dark_mode"] = True

        template = jinja_env.get_template("analysis/error_analysis.html")
        html = template.render(**error_template_context)

        assert "#1a1a1a" in html

    def test_tags_display(self, jinja_env, error_template_context):
        """Test that tags are displayed"""
        template = jinja_env.get_template("analysis/error_analysis.html")
        html = template.render(**error_template_context)

        # Check tags
        assert "SEVERITY_HIGH" in html
        assert "NullPointerException" in html


class TestTemplateFilters:
    """Test custom Jinja2 filters"""

    def test_format_number_filter(self, jinja_env):
        """Test format_number filter"""
        # Note: Filters are defined in html_document_service.py
        # We test them indirectly through template rendering

        template_str = "{{ value | format_number }}"
        template = jinja_env.from_string(template_str)

        # This will fail if filter is not registered
        # The actual filter registration happens in HtmlDocumentService
        pass

    def test_format_percentage_filter(self, jinja_env):
        """Test format_percentage filter"""
        # Note: Filters are defined in html_document_service.py
        # We test them indirectly through template rendering

        template_str = "{{ value | format_percentage(total) }}"
        template = jinja_env.from_string(template_str)

        # This will fail if filter is not registered
        pass


class TestTemplateEdgeCases:
    """Test template edge cases"""

    def test_empty_metrics(self, jinja_env, project_template_context):
        """Test template with empty metrics"""
        project_template_context["metrics"] = {}

        template = jinja_env.get_template("analysis/project_analysis.html")
        html = template.render(**project_template_context)

        # Should still render without errors
        assert html is not None
        assert len(html) > 0

    def test_missing_top_errors(self, jinja_env, project_template_context):
        """Test template without top errors"""
        project_template_context["top_errors"] = []

        template = jinja_env.get_template("analysis/project_analysis.html")
        html = template.render(**project_template_context)

        # Should still render
        assert html is not None

    def test_missing_related_logs(self, jinja_env, error_template_context):
        """Test error template without related logs"""
        error_template_context["related_logs"] = []

        template = jinja_env.get_template("analysis/error_analysis.html")
        html = template.render(**error_template_context)

        # Should still render
        assert html is not None

    def test_none_values(self, jinja_env, error_template_context):
        """Test template with None values"""
        error_template_context["error_log"]["stackTrace"] = None

        template = jinja_env.get_template("analysis/error_analysis.html")
        html = template.render(**error_template_context)

        # Should handle None gracefully
        assert html is not None
