"""
HTML Document Generation Service

Generates HTML documents for project and error analysis using Jinja2 templates.
Reuses existing log analysis logic and adds HTML formatting.
"""

import time
from pathlib import Path
from typing import Dict, Any, List, Optional
from jinja2 import Environment, FileSystemLoader, Template
import markdown
from markupsafe import Markup

from app.models.document import (
    AiHtmlDocumentRequest,
    AiHtmlDocumentResponse,
    AiDocumentMetadata,
    AiValidationStatus,
    DocumentType,
)
from app.core.opensearch import opensearch_client
from langchain_openai import ChatOpenAI
from langchain.prompts import ChatPromptTemplate
from app.core.config import settings


class HtmlDocumentService:
    """
    HTML Document Generation Service

    Features:
    - Jinja2 templating for clean HTML generation
    - Reuses existing analysis data from BE
    - Supports project and error analysis documents
    - Validates generated HTML structure
    """

    def __init__(self):
        """Initialize Jinja2 environment"""
        template_dir = Path(__file__).parent.parent / "templates"
        self.env = Environment(
            loader=FileSystemLoader(template_dir),
            autoescape=True,  # Prevent XSS
            trim_blocks=True,
            lstrip_blocks=True,
        )

        # Add custom filters
        self.env.filters["format_number"] = self._format_number
        self.env.filters["format_percentage"] = self._format_percentage
        self.env.filters["markdown"] = self._markdown_to_html

    async def generate_html_document(
        self, request: AiHtmlDocumentRequest
    ) -> AiHtmlDocumentResponse:
        """
        Generate HTML document based on request

        Args:
            request: HTML document generation request

        Returns:
            AiHtmlDocumentResponse: Generated HTML document with metadata

        Raises:
            ValueError: If document generation fails
        """
        start_time = time.time()

        try:
            # Generate HTML based on document type
            if request.document_type == DocumentType.PROJECT_ANALYSIS:
                html_content, metadata = await self._generate_project_analysis(request)
            elif request.document_type == DocumentType.ERROR_ANALYSIS:
                html_content, metadata = await self._generate_error_analysis(request)
            else:
                raise ValueError(f"Unsupported document type: {request.document_type}")

            # Calculate generation time
            generation_time = time.time() - start_time
            metadata.generation_time = round(generation_time, 2)

            # Validate HTML
            validation_status = self._validate_html(html_content, request.document_type)

            # Add regeneration feedback if provided
            if request.regeneration_feedback:
                validation_status.warnings.extend(
                    [f"Regenerated due to: {fb}" for fb in request.regeneration_feedback]
                )

            return AiHtmlDocumentResponse(
                html_content=html_content,
                metadata=metadata,
                validation_status=validation_status,
            )

        except Exception as e:
            raise ValueError(f"HTML document generation failed: {str(e)}")

    async def _generate_project_analysis(
        self, request: AiHtmlDocumentRequest
    ) -> tuple[str, AiDocumentMetadata]:
        """
        Generate project analysis HTML document

        Args:
            request: Document generation request

        Returns:
            tuple: (html_content, metadata)
        """
        # Load template
        template = self.env.get_template("analysis/project_analysis.html")

        # Extract data from request
        data = request.data
        options = request.options or {}
        style = request.style_preferences

        # Generate AI health analysis
        ai_health_summary = None
        if request.project_uuid and data.get("timeRange"):
            ai_health_summary = await self._analyze_project_health(
                project_uuid=request.project_uuid,
                time_range=data.get("timeRange", {}),
                metrics=data.get("metrics", {})
            )

        # Prepare template context
        context = {
            "project_info": data.get("projectInfo", {}),
            "time_range": data.get("timeRange", {}),
            "metrics": data.get("metrics", {}),
            "top_errors": data.get("topErrors", []),
            "options": options,
            "style": {
                "css_framework": style.css_framework if style else "tailwind",
                "chart_library": style.chart_library if style else "chartjs",
                "color_scheme": style.color_scheme if style else "blue",
                "dark_mode": options.get("darkMode", False),
            },
            "ai_insights": {
                "system_health_summary": ai_health_summary
            } if ai_health_summary else None,
        }

        # Render template
        html_content = template.render(**context)

        # Calculate metadata
        metrics = data.get("metrics", {})
        total_logs = metrics.get("totalLogs", 0)
        error_count = metrics.get("errorCount", 0)
        warn_count = metrics.get("warnCount", 0)

        health_score = self._calculate_health_score(total_logs, error_count, warn_count)
        critical_issues = error_count

        metadata = AiDocumentMetadata(
            word_count=len(html_content.split()),
            estimated_reading_time=self._estimate_reading_time(html_content),
            sections_generated=self._extract_sections(html_content),
            charts_included=self._extract_charts(context),
            css_libraries_used=self._get_css_libraries(style),
            js_libraries_used=self._get_js_libraries(style),
            health_score=health_score,
            critical_issues=critical_issues,
            total_issues=error_count + warn_count,
            recommendations=self._calculate_recommendations(error_count, warn_count),
            ai_insights={
                "system_health_summary": ai_health_summary
            } if ai_health_summary else None,
        )

        return html_content, metadata

    async def _generate_error_analysis(
        self, request: AiHtmlDocumentRequest
    ) -> tuple[str, AiDocumentMetadata]:
        """
        Generate error analysis HTML document

        Args:
            request: Document generation request

        Returns:
            tuple: (html_content, metadata)
        """
        # Load template
        template = self.env.get_template("analysis/error_analysis.html")

        # Extract data from request
        data = request.data
        options = request.options or {}
        style = request.style_preferences

        # Prepare template context
        error_log = data.get("errorLog", {})
        existing_analysis = data.get("existingAnalysis", {})

        context = {
            "log_id": request.log_id,
            "error_log": error_log,
            "existing_analysis": existing_analysis,
            "related_logs": data.get("relatedLogs", []),
            "options": options,
            "style": {
                "css_framework": style.css_framework if style else "tailwind",
                "chart_library": style.chart_library if style else "chartjs",
                "color_scheme": style.color_scheme if style else "blue",
                "dark_mode": options.get("darkMode", False),
            },
        }

        # Render template
        html_content = template.render(**context)

        # Calculate metadata
        severity = self._determine_severity(error_log.get("level", "ERROR"))
        root_cause = existing_analysis.get("errorCause", "Unknown")[:100]

        metadata = AiDocumentMetadata(
            word_count=len(html_content.split()),
            estimated_reading_time=self._estimate_reading_time(html_content),
            sections_generated=self._extract_sections(html_content),
            charts_included=self._extract_charts(context),
            css_libraries_used=self._get_css_libraries(style),
            js_libraries_used=self._get_js_libraries(style),
            severity=severity,
            root_cause=root_cause,
            affected_users=len(data.get("relatedLogs", [])),
        )

        return html_content, metadata

    def _validate_html(
        self, html_content: str, document_type: DocumentType
    ) -> AiValidationStatus:
        """
        Validate generated HTML

        Args:
            html_content: Generated HTML
            document_type: Type of document

        Returns:
            AiValidationStatus: Validation result
        """
        warnings = []

        # Check required HTML tags
        html_lower = html_content.lower()
        is_valid_html = (
            "<html" in html_lower and "<body" in html_lower and "<head" in html_lower
        )

        if not is_valid_html:
            warnings.append("Missing required HTML tags (html, head, body)")

        # Check required sections based on document type
        has_required_sections = True
        if document_type == DocumentType.PROJECT_ANALYSIS:
            if "metrics" not in html_lower and "overview" not in html_lower:
                has_required_sections = False
                warnings.append("Missing required sections for project analysis")
        elif document_type == DocumentType.ERROR_ANALYSIS:
            if "error" not in html_lower and "analysis" not in html_lower:
                has_required_sections = False
                warnings.append("Missing required sections for error analysis")

        return AiValidationStatus(
            is_valid_html=is_valid_html,
            has_required_sections=has_required_sections,
            warnings=warnings,
        )

    @staticmethod
    def _calculate_health_score(
        total_logs: int, error_count: int, warn_count: int
    ) -> int:
        """Calculate project health score (0-100)"""
        if total_logs == 0:
            return 100

        error_rate = error_count / total_logs
        warn_rate = warn_count / total_logs

        # Deduct points based on error and warning rates
        score = 100
        score -= min(error_rate * 1000, 50)  # Max 50 points deduction for errors
        score -= min(warn_rate * 100, 30)  # Max 30 points deduction for warnings

        return max(0, int(score))

    @staticmethod
    def _determine_severity(log_level: str) -> str:
        """Determine error severity"""
        severity_map = {"ERROR": "HIGH", "WARN": "MEDIUM", "INFO": "LOW"}
        return severity_map.get(log_level.upper(), "MEDIUM")

    @staticmethod
    def _estimate_reading_time(html_content: str) -> str:
        """Estimate reading time based on word count"""
        word_count = len(html_content.split())
        minutes = max(1, word_count // 200)  # ~200 words per minute
        return f"{minutes} minutes"

    @staticmethod
    def _extract_sections(html_content: str) -> List[str]:
        """Extract section names from HTML"""
        sections = []
        html_lower = html_content.lower()

        # Common sections
        section_keywords = [
            "overview",
            "metrics",
            "errors",
            "warnings",
            "recommendations",
            "analysis",
            "timeline",
            "impact",
        ]

        for keyword in section_keywords:
            if keyword in html_lower:
                sections.append(keyword.capitalize())

        return sections

    @staticmethod
    def _extract_charts(context: Dict[str, Any]) -> List[str]:
        """Extract chart names from context"""
        charts = []

        if context.get("options", {}).get("includeCharts"):
            # Add default charts based on available data
            if "metrics" in context:
                charts.extend(["error_trend", "level_distribution"])
            if "error_log" in context:
                charts.append("error_timeline")

        return charts

    @staticmethod
    def _get_css_libraries(style) -> List[str]:
        """Get CSS libraries to use"""
        if not style:
            return ["tailwindcss"]

        css_framework = style.css_framework
        if css_framework == "tailwind":
            return ["tailwindcss"]
        elif css_framework == "bootstrap":
            return ["bootstrap"]
        else:
            return ["tailwindcss"]

    @staticmethod
    def _get_js_libraries(style) -> List[str]:
        """Get JS libraries to use"""
        if not style:
            return ["chart.js"]

        chart_library = style.chart_library
        if chart_library == "chartjs":
            return ["chart.js"]
        elif chart_library == "d3":
            return ["d3.js"]
        else:
            return ["chart.js"]

    @staticmethod
    def _calculate_recommendations(error_count: int, warn_count: int) -> int:
        """Calculate number of recommendations"""
        # Simple heuristic: 1 recommendation per 10 errors/warnings
        return min((error_count + warn_count) // 10, 10)

    @staticmethod
    def _format_number(value) -> str:
        """Format number with thousands separator"""
        if isinstance(value, (int, float)):
            return f"{value:,}"
        return str(value)

    @staticmethod
    def _format_percentage(value, total) -> str:
        """Format percentage"""
        if total == 0:
            return "0%"
        percentage = (value / total) * 100
        return f"{percentage:.1f}%"

    @staticmethod
    def _markdown_to_html(text: str) -> Markup:
        """
        Convert Markdown text to HTML

        Args:
            text: Markdown formatted text

        Returns:
            Markup: Safe HTML string (not escaped by Jinja2)
        """
        if not text:
            return Markup("")

        # Convert markdown to HTML with useful extensions
        html = markdown.markdown(
            text,
            extensions=[
                'extra',      # Tables, fenced code blocks, etc.
                'nl2br',      # Convert newlines to <br>
                'sane_lists', # Better list handling
            ]
        )

        # Return as Markup to prevent auto-escaping
        return Markup(html)

    async def _analyze_project_health(
        self, project_uuid: str, time_range: Dict[str, str], metrics: Dict[str, Any]
    ) -> Optional[str]:
        """
        Analyze project health using AI based on OpenSearch logs and metrics

        Args:
            project_uuid: Project UUID
            time_range: Time range for analysis
            metrics: Basic metrics (totalLogs, errorCount, etc.)

        Returns:
            AI-generated health summary (Korean) or None if analysis fails
        """
        try:
            # Query OpenSearch for recent logs
            uuid_formatted = project_uuid.replace('-', '_')
            index_pattern = f"{uuid_formatted}_*"

            # Query for error and warning logs (sample 100 most recent)
            response = opensearch_client.search(
                index=index_pattern,
                body={
                    "query": {
                        "bool": {
                            "must": [
                                {"term": {"project_uuid.keyword": project_uuid}},
                            ],
                            "filter": [
                                {
                                    "range": {
                                        "timestamp": {
                                            "gte": time_range.get("startTime"),
                                            "lte": time_range.get("endTime")
                                        }
                                    }
                                }
                            ],
                            "should": [
                                {"term": {"log_level.keyword": "ERROR"}},
                                {"term": {"log_level.keyword": "WARN"}}
                            ],
                            "minimum_should_match": 1
                        }
                    },
                    "size": 100,
                    "sort": [{"timestamp": {"order": "desc"}}]
                }
            )

            # Extract component names from error/warning logs
            component_errors = {}
            for hit in response["hits"]["hits"]:
                log = hit["_source"]
                component = log.get("service_name", log.get("component_name", "Unknown"))
                level = log.get("log_level", "UNKNOWN")

                if component not in component_errors:
                    component_errors[component] = {"ERROR": 0, "WARN": 0}

                if level in ["ERROR", "WARN"]:
                    component_errors[component][level] += 1

            # Find top error components
            top_components = sorted(
                component_errors.items(),
                key=lambda x: x[1]["ERROR"] + x[1]["WARN"],
                reverse=True
            )[:5]

            top_components_str = ", ".join([
                f"{comp} ({counts['ERROR']}E/{counts['WARN']}W)"
                for comp, counts in top_components
            ]) if top_components else "없음"

            # Calculate rates
            total_logs = metrics.get("totalLogs", 0)
            error_count = metrics.get("errorCount", 0)
            warn_count = metrics.get("warnCount", 0)

            error_rate = (error_count / total_logs * 100) if total_logs > 0 else 0
            warn_rate = (warn_count / total_logs * 100) if total_logs > 0 else 0

            # Create LLM prompt
            prompt = ChatPromptTemplate.from_template(
                """당신은 로그 분석 전문가입니다.
다음 시스템 메트릭을 분석하고 전체 건강도를 요약하세요:

- 총 로그 수: {total_logs:,}개
- 에러 로그: {error_count:,}개 ({error_rate:.2f}%)
- 경고 로그: {warn_count:,}개 ({warn_rate:.2f}%)
- 주요 에러 발생 컴포넌트: {top_components}

**요구사항:**
1. 3-5문장으로 시스템의 전체 건강 상태를 요약하세요
2. 에러율과 경고율을 기준으로 안정성을 평가하세요
3. 주의가 필요한 컴포넌트가 있다면 언급하세요
4. 발견된 이상 징후가 있다면 강조하세요
5. 전문적이고 명확한 한국어로 작성하세요

**출력 형식:** 순수 텍스트만 출력하세요 (마크다운이나 특수 기호 사용 금지)"""
            )

            # Initialize LLM
            llm = ChatOpenAI(
                model=settings.OPENAI_MODEL,
                temperature=0.3,  # Lower temperature for consistent analysis
                timeout=30
            )

            # Generate analysis
            chain = prompt | llm
            result = await chain.ainvoke({
                "total_logs": total_logs,
                "error_count": error_count,
                "warn_count": warn_count,
                "error_rate": error_rate,
                "warn_rate": warn_rate,
                "top_components": top_components_str
            })

            # Extract content
            summary = result.content.strip()

            print(f"✅ AI health analysis generated: {len(summary)} characters")
            return summary

        except Exception as e:
            print(f"❌ AI health analysis failed: {e}")
            # Graceful fallback - return None so document generation continues
            return None


# Global instance
html_document_service = HtmlDocumentService()
