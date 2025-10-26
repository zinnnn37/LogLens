package S13P31A306.loglens.global.utils;

import S13P31A306.loglens.domain.auth.jwt.JwtProperties;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CookieUtil {
    private final JwtProperties jwtProperties;

    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/api/auth")
                .maxAge(Duration.ofSeconds(jwtProperties.refreshTokenValidityInSeconds()))
                .sameSite("Lax")
                .build();
    }

    public ResponseCookie expireRefreshTokenCookie() {
        return ResponseCookie.from("refreshToken", "")
                .maxAge(0)
                .path("/api/auth")
                .secure(true)
                .httpOnly(true)
                .sameSite("Lax")
                .build();
    }
}
