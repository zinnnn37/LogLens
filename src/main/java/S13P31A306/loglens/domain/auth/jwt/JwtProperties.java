package S13P31A306.loglens.domain.auth.jwt;

import S13P31A306.loglens.global.annotation.Sensitive;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 관련 설정값을 application.yml에서 매핑하는 record
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        @Sensitive
        String secret,
        long accessTokenValidityInSeconds,
        long refreshTokenValidityInSeconds
) {
}
