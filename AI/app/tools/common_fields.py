"""
OpenSearch _source 공통 필드 정의

모든 도구에서 일관된 필드를 요청하여 trace_id, request_id 등 추적 정보 누락 방지
"""

# 기본 필드 (모든 도구에서 필수)
BASE_FIELDS = [
    "log_id",
    "trace_id",           # 분산 추적 ID
    "request_id",         # 요청 ID
    "message",
    "level",
    "service_name",
    "timestamp",
    "layer",
    "component_name",
    "requester_ip",       # 사용자 IP
    "stacktrace"          # 스택 트레이스 (최상위 필드)
]

# 로그 상세 필드 (log_details nested object)
LOG_DETAILS_FIELDS = [
    "log_details.exception_type",
    "log_details.class_name",
    "log_details.method_name",
    "log_details.http_method",
    "log_details.request_uri",
    "log_details.response_status",
    "log_details.execution_time",
    "log_details.stacktrace"     # nested 스택 트레이스
]

# AI 분석 필드 (ai_analysis nested object)
AI_ANALYSIS_FIELDS = [
    "ai_analysis.summary",
    "ai_analysis.error_cause",
    "ai_analysis.solution",
    "ai_analysis.tags",
    "ai_analysis.analysis_type"
]

# 전체 필드 (필요시 조합 사용)
ALL_FIELDS = BASE_FIELDS + LOG_DETAILS_FIELDS + AI_ANALYSIS_FIELDS
