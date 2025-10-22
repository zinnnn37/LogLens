package S13P31A306.loglens.global.constants;

import lombok.RequiredArgsConstructor;

/**
 * 로깅 관련 메시지 상수
 */
@RequiredArgsConstructor
public enum LogMessages {
    // 로그 접두사
    LOG_START_PREFIX("-->"),
    LOG_COMPLETE_PREFIX("<--"),
    LOG_EXCEPTION_PREFIX("<X-"),

    // 로그 레벨
    LOG_UNSUPPORTED_LEVEL("Unsupported log level: {}"),

    // 로그 값 표시
    LOG_MASKED_VALUE("****"),
    LOG_ERROR_VALUE("<error>"),
    LOG_EXCLUDED_VALUE("<excluded>"),
    LOG_ARG_CONVERSION_FAILED("Failed to convert argument"),

    // 패키지 필터링
    SPRING_PACKAGE("org.springframework"),
    APACHE_PACKAGE("org.apache");

    private final String message;

    public String message() {
        return this.message;
    }
}