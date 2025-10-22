package S13P31A306.loglens.global.constants;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum Messages {
    // 유효성 검증 메시지
    INVALID_INPUT("잘못된 입력입니다."),

    // API 문서 관련 메시지
    API_TITLE("OSM API Document"),
    API_DESCRIPTION("우리들의 비밀 장터 API 명세서"),
    API_VERSION("1.0.0"),

    // 서버 URL 정보
    LOCAL_SERVER_URL("http://localhost:8080"),
    PROD_SERVER_URL("https://api.fintech-osm.store"),

    // 보안 인증 관련
    SECURITY_SCHEME_NAME("BearerAuthentication"),
    BEARER_FORMAT("JWT"),
    BEARER_SCHEME("bearer"),

    // 시스템 패키지 필터링
    SPRING_PACKAGE("org.springframework"),
    APACHE_PACKAGE("org.apache"),

    // 로깅 관련 메시지
    LOG_ARG_CONVERSION_FAILED("Failed to convert argument"),
    LOG_START_PREFIX("-->"),
    LOG_COMPLETE_PREFIX("<--"),
    LOG_EXCEPTION_PREFIX("<X-"),
    LOG_UNSUPPORTED_LEVEL("Unsupported log level: {}"),
    LOG_MASKED_VALUE("****"),
    LOG_ERROR_VALUE("<error>"),
    LOG_EXCLUDED_VALUE("<excluded>"),

    // OAuth 관련 메시지
    PROVIDER_ID_KEY("providerId"),
    PROVIDER_KEY("provider"),
    NAME_KEY("name"),
    IMAGE_URL_KEY("imageUrl"),

    // Utility 클래스 관련 메시지
    UTILITY_CLASS_ERROR("Utility class");

    private final String message;

    public String message() {  // getMessage() 대신 더 간단한 이름으로 변경
        return this.message;
    }
}
