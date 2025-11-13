package S13P31A306.loglens.domain.component.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * OpenSearch 필드명 Enum
 */
@Getter
@RequiredArgsConstructor
public enum OpenSearchField {

    // ──────────────── 트레이싱 / 식별자 ────────────────
    TRACE_ID("trace_id.keyword"),
    PROJECT_UUID("project_uuid"),
    PROJECT_UUID_KEYWORD("project_uuid.keyword"),
    LOG_ID("log_id"),

    // ──────────────── 컨텍스트 / 메타데이터 ────────────────
    COMPONENT_NAME("component_name"),
    SERVICE_NAME("service_name"),
    SOURCE_TYPE("source_type"),
    LOGGER("logger"),

    // ──────────────── 로그 레벨 및 내용 ────────────────
    LOG_LEVEL("log_level"),
    LEVEL("level"),
    MESSAGE("message"),
    MESSAGE_KEYWORD("message.keyword"),
    COMMENT("comment"),

    // ──────────────── 클래스 / 메서드 / 스레드 ────────────────
    CLASS_NAME("class_name"),
    METHOD_NAME("method_name"),
    THREAD_NAME("thread_name"),

    // ──────────────── 시간 관련 ────────────────
    TIMESTAMP("timestamp"),
    INDEXED_AT("indexed_at"),
    AT_TIMESTAMP("@timestamp"),

    // ──────────────── 요청 / 응답 / IP ────────────────
    REQUESTER_IP("requester_ip"),
    DURATION("duration"),
    STACKTRACE("stacktrace"),

    // ──────────────── AI 분석 / 벡터 ────────────────
    AI_ANALYSIS("ai_analysis"),
    LOG_VECTOR("log_vector"),

    // ──────────────── 상세 로그 (log_details 내부 필드) ────────────────
    LOG_DETAILS("log_details"),
    EXCEPTION_TYPE("log_details.exception_type"),
    EXECUTION_TIME("log_details.execution_time"),
    RESPONSE_STATUS("log_details.response_status"),
    HTTP_METHOD("log_details.http_method"),
    REQUEST_URI("log_details.request_uri"),
    REQUEST_BODY("log_details.request_body"),
    RESPONSE_BODY("log_details.response_body"),
    LOG_DETAILS_CLASS_NAME("log_details.class_name"),
    LOG_DETAILS_METHOD_NAME("log_details.method_name"),
    LOG_DETAILS_STACKTRACE("log_details.stacktrace");

    private final String fieldName;
}
