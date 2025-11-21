"""
Input/Output Sanitization Utilities

Prevents XSS and injection attacks by sanitizing user input in responses.
"""

import html
import re


def sanitize_html(text: str) -> str:
    """
    HTML-escape dangerous characters to prevent XSS.

    Converts:
        < → &lt;
        > → &gt;
        & → &amp;
        " → &quot;
        ' → &#x27;

    Args:
        text: User-provided text

    Returns:
        HTML-safe string
    """
    if not text:
        return ""
    return html.escape(str(text), quote=True)


def sanitize_for_display(text: str, max_length: int = 100) -> str:
    """
    Prepare text for safe display in response messages.

    Use this when embedding user input (keywords, queries, etc.) in response strings.

    1. Truncate to max length
    2. HTML-escape special characters

    Args:
        text: User-provided text
        max_length: Maximum display length (default 100)

    Returns:
        Safe string for display
    """
    if not text:
        return ""

    text = str(text).strip()

    # Truncate long text
    if len(text) > max_length:
        text = text[:max_length - 3] + "..."

    # HTML escape
    return sanitize_html(text)


def detect_sql_injection_pattern(text: str) -> bool:
    """
    Detect common SQL injection patterns.

    Args:
        text: User input text

    Returns:
        True if SQL injection pattern detected
    """
    if not text:
        return False

    # Common SQL injection patterns
    sql_patterns = [
        r"(\s|^)(SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|TRUNCATE)(\s|$)",
        r"(--|#|/\*)",  # SQL comments
        r"(;)\s*(DROP|DELETE|UPDATE|INSERT)",  # Command chaining
        r"(')\s*(OR|AND)\s*('|[0-9])",  # Boolean injection
        r"UNION\s+(ALL\s+)?SELECT",  # UNION injection
    ]

    for pattern in sql_patterns:
        if re.search(pattern, text, re.IGNORECASE):
            return True

    return False


def detect_xss_pattern(text: str) -> bool:
    """
    Detect common XSS attack patterns.

    Args:
        text: User input text

    Returns:
        True if XSS pattern detected
    """
    if not text:
        return False

    xss_patterns = [
        r"<\s*script",  # <script> tags
        r"javascript\s*:",  # javascript: protocol
        r"on\w+\s*=",  # Event handlers (onclick=, onerror=, etc.)
        r"<\s*iframe",  # iframe injection
        r"<\s*img\s+[^>]*on\w+",  # img with event handlers
        r"eval\s*\(",  # eval() calls
        r"document\.(cookie|write|location)",  # DOM manipulation
    ]

    for pattern in xss_patterns:
        if re.search(pattern, text, re.IGNORECASE):
            return True

    return False


def log_security_warning(text: str, context: str = "input") -> None:
    """
    Log security pattern detection for monitoring.

    Args:
        text: Text that was checked
        context: Where the text came from (e.g., "keyword", "query")
    """
    has_sql = detect_sql_injection_pattern(text)
    has_xss = detect_xss_pattern(text)

    if has_sql or has_xss:
        print(f"⚠️ Security pattern in {context} - SQL: {has_sql}, XSS: {has_xss}")
