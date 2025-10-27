package S13P31A306.loglens.domain.auth.validator;

import S13P31A306.loglens.domain.auth.constants.AuthErrorCode;
import S13P31A306.loglens.domain.auth.jwt.JwtTokenProvider;
import S13P31A306.loglens.domain.auth.respository.AuthRepository;
import S13P31A306.loglens.global.constants.GlobalErrorCode;
import S13P31A306.loglens.global.exception.BusinessException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthValidator {

    private static final String LOG_PREFIX = "[AuthValidator]";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthRepository authRepository;

    public String validateRefreshToken(final String accessToken, final String refreshToken) {
        // 1. Refresh Token 자체 유효성 검증 (만료, 형식 오류 등 구분)
        String userEmailFromRt;
        try {
            userEmailFromRt = jwtTokenProvider.getSubject(refreshToken);
        } catch (ExpiredJwtException e) {
            log.warn("{} 토큰 재발급 실패: Refresh Token 만료", LOG_PREFIX);
            throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.warn("{} 토큰 재발급 실패: 잘못된 Refresh Token 서명 또는 형식", LOG_PREFIX);
            throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_INVALID);
        } catch (UnsupportedJwtException | IllegalArgumentException e) {
            log.warn("{} 토큰 재발급 실패: 지원되지 않거나 잘못된 Refresh Token", LOG_PREFIX);
            throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_INVALID);
        }

        // 2. Access Token과 Refresh Token의 소유자 일치 여부 확인
        String userEmailFromAt = jwtTokenProvider.getSubjectFromExpiredToken(accessToken);
        if (!userEmailFromRt.equals(userEmailFromAt)) {
            log.warn("{} 토큰 재발급 실패: 토큰 소유자 불일치. RT sub: {}, AT sub: {}",
                    LOG_PREFIX,
                    userEmailFromRt,
                    userEmailFromAt);
            throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_INVALID);
        }

        // 3. Repository(Redis)에 저장된 Refresh Token과 일치하는지 확인
        String storedRefreshToken = authRepository.findRefreshTokenByEmail(userEmailFromRt)
                .orElseThrow(() -> {
                    log.warn("{} 토큰 재발급 실패: 저장된 Refresh Token 없음 - 사용자 email: {}",
                            LOG_PREFIX,
                            userEmailFromRt);
                    return new BusinessException(AuthErrorCode.REFRESH_TOKEN_INVALID);
                });

        if (!storedRefreshToken.equals(refreshToken)) {
            log.warn("{} 토큰 재발급 실패: 저장된 토큰과 불일치 - 사용자 email: {}",
                    LOG_PREFIX,
                    userEmailFromRt);
            throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_INVALID);
        }

        // 모든 검증 통과 시, 사용자 email 반환
        return userEmailFromRt;
    }

    /**
     * Authorization 헤더를 검증하고 Access Token을 추출합니다.
     *
     * @param authHeader Authorization 헤더
     * @return Bearer 접두사가 제거된 Access Token
     * @throws BusinessException Authorization 헤더가 없거나 형식이 잘못된 경우
     */
    public String validateAndExtractAccessToken(final String authHeader) {
        // 1. Authorization 헤더 검증
        if (!StringUtils.hasText(authHeader)) {
            log.warn("{} Access Token 누락", LOG_PREFIX);
            throw new BusinessException(AuthErrorCode.ACCESS_TOKEN_MISSING);
        }

        // 2. Bearer 접두사 검증
        if (!authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("{} 잘못된 토큰 형식: {}", LOG_PREFIX, authHeader);
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN_FORMAT);
        }

        // 3. Bearer 접두사 제거 후 반환
        return authHeader.substring(BEARER_PREFIX.length());
    }

    /**
     * Refresh Token 쿠키를 검증합니다.
     *
     * @param refreshToken Refresh Token 쿠키 값
     * @throws BusinessException Refresh Token이 없는 경우
     */
    public void validateRefreshTokenCookie(final String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            log.warn("{} Refresh Token 누락", LOG_PREFIX);
            throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_MISSING);
        }
    }

    /**
     * Authentication 객체를 검증합니다.
     *
     * @param authentication Authentication 객체
     * @throws BusinessException Authentication이 null인 경우
     */
    public void validateAuthentication(final Authentication authentication) {
        if (Objects.isNull(authentication)) {
            log.warn("{} 인증되지 않은 요청", LOG_PREFIX);
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }
    }
}
