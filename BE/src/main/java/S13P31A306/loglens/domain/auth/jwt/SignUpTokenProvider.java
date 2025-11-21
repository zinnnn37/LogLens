package S13P31A306.loglens.domain.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SignUpTokenProvider {

    private static final String LOG_PREFIX = "[SignUpTokenProvider]";

    private final Key key;
    private final long validityInMilliseconds;

    public SignUpTokenProvider(
            @Value("${jwt.secret}") final String secretKey,
            @Value("${jwt.access-token-validity-in-seconds}") final long validityInSeconds) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.validityInMilliseconds = validityInSeconds * 1000;
    }

    public Claims getClaims(final String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(final String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info(LOG_PREFIX + "잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.info(LOG_PREFIX + "만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.info(LOG_PREFIX + "지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.info(LOG_PREFIX + "JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }
}
