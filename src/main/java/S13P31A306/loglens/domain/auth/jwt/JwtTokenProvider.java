package S13P31A306.loglens.domain.auth.jwt;

import S13P31A306.loglens.domain.auth.model.CustomUserDetails;
import S13P31A306.loglens.domain.auth.service.CustomUserDetailsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtTokenProvider {

    private static final String LOG_PREFIX = "[JwtTokenProvider]";
    private static final String AUTHORITIES_KEY = "auth";

    private final Key key;
    private final long accessTokenValidityInMilliseconds;
    private final long refreshTokenValidityInMilliseconds;
    private final CustomUserDetailsService customUserDetailsService;

    public JwtTokenProvider(final JwtProperties jwtProperties,
                            final CustomUserDetailsService customUserDetailsService) {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.secret());
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenValidityInMilliseconds = jwtProperties.accessTokenValidityInSeconds() * 1000L;
        this.refreshTokenValidityInMilliseconds = jwtProperties.refreshTokenValidityInSeconds() * 1000L;
        this.customUserDetailsService = customUserDetailsService;
    }

    public Jwt generateJwt(final Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Integer userId = userDetails.getUserId();
        String email = userDetails.getUsername();

        String accessToken = generateAccessToken(authentication);
        String refreshToken = generateRefreshToken(authentication);
        return Jwt.builder()
                .userId(userId)
                .email(email)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn((int) (accessTokenValidityInMilliseconds / 1000))
                .build();
    }


    // 인증 정보를 기반으로 Access Token 생성
    public String generateAccessToken(final Authentication authentication) {
        String authorities = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        long now = (new Date()).getTime();
        Date validity = new Date(now + this.accessTokenValidityInMilliseconds);

        return Jwts.builder()
                .setSubject(authentication.getName())
                .claim(AUTHORITIES_KEY, authorities)
                .claim("jti", UUID.randomUUID().toString())
                .signWith(key, SignatureAlgorithm.HS512)
                .setExpiration(validity)
                .compact();
    }

    // Refresh Token 생성 (Access Token보다 만료 시간이 길다)
    public String generateRefreshToken(final Authentication authentication) {
        long now = (new Date()).getTime();
        Date validity = new Date(now + this.refreshTokenValidityInMilliseconds);

        return Jwts.builder()
                .setSubject(authentication.getName()) // email 기반 식별
                .claim("jti", UUID.randomUUID().toString())
                .signWith(key, SignatureAlgorithm.HS512)
                .setExpiration(validity)
                .compact();
    }

    // JWT 토큰에서 인증 정보 조회
    public Authentication getAuthentication(final String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        Collection<? extends GrantedAuthority> authorities = Arrays.stream(
                        claims.get(AUTHORITIES_KEY, String.class).split(","))
                .filter(s -> !s.trim().isEmpty())
                .map(SimpleGrantedAuthority::new)
                .toList();

        // subject는 email이므로, email로 사용자 로드
        String email = claims.getSubject();
        CustomUserDetails principal = (CustomUserDetails) customUserDetailsService.loadUserByUsername(email);

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    // 토큰에서 사용자 Email(Subject) 추출
    public String getSubject(final String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // 만료된 토큰에서 사용자 Email 추출
    public String getSubjectFromExpiredToken(final String token) {
        try {
            return getSubject(token);
        } catch (ExpiredJwtException e) {
            return e.getClaims().getSubject();
        }
    }

    // 토큰 유효성 검증
    public boolean validateToken(final String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("{} 잘못된 JWT 서명입니다.", LOG_PREFIX);
        } catch (ExpiredJwtException e) {
            log.info("{} 만료된 JWT 토큰입니다.", LOG_PREFIX);
        } catch (UnsupportedJwtException e) {
            log.info("{} 지원되지 않는 JWT 토큰입니다.", LOG_PREFIX);
        } catch (IllegalArgumentException e) {
            log.info("{} JWT 토큰이 잘못되었습니다.", LOG_PREFIX);
        }
        return false;
    }
}
