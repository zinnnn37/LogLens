package S13P31A306.loglens.global.constants;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum SwaggerMessages {
    // Security
    SECURITY_SCHEME_NAME("bearerAuth"),
    BEARER_SCHEME("bearer"),
    BEARER_FORMAT("JWT"),

    // API Info
    API_TITLE("LogLens API"),
    API_DESCRIPTION("LogLens 시스템 API"),
    API_VERSION("v1.0.0"),

    // Server URLs
    LOCAL_SERVER_URL("http://localhost:8080"),
    PROD_SERVER_URL("https://api.loglens.co.kr");

    private final String message;

    public String message() {
        return this.message;
    }
}
