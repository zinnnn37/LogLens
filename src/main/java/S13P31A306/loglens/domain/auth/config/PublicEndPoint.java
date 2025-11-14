package S13P31A306.loglens.domain.auth.config;

import java.util.Arrays;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum PublicEndPoint {

    // Auth
    AUTH_SIGN_IN("/api/auth/users"),
    AUTH_REFRESH("/api/auth/tokens"),
    AUTH_CHECK_EMAIL("/api/auth/emails"),

    // Swagger
    SWAGGER_UI("/swagger-ui/**"),
    SWAGGER_DOCS("/v3/api-docs/**"),
    SWAGGER_RESOURCES("/swagger-resources/**"),
    WEBJARS("/webjars/**"),

    // Monitoring
    ACTUATOR_HEALTH("/actuator/health"),
    ACTUATOR_PROMETHEUS("/actuator/prometheus"),

    // Health Check
    HEALTH_CHECK("/health-check"),

    // Maven Repository
    MAVEN_REPO("/maven-repo/**"),

    // npm Library files
    NPM_LIBRARY("/static/libs/**"),

    // Component test
    COMPONENT("/api/components/**"),
    DEPENDENCY("/api/dependencies/**"),
    H2("/h2-console/**"),

    SUB_DOMAIN("https://*.loglens.co.kr"),
    ALB_DOMAIN("https://*.ap-northeast-2.elb.amazonaws.com"),
    BLUE_PORT("http://localhost:8080"),
    GREEN_PORT("http://localhost:8081"),

    // Root
    ROOT("/");

    private final String path;

    public static String[] getPublicEndpoints() {
        return Arrays.stream(values())
                .map(endpoint -> endpoint.path)
                .toArray(String[]::new);
    }
}
