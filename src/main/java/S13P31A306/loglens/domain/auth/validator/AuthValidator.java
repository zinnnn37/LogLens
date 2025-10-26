package S13P31A306.loglens.domain.auth.validator;

import S13P31A306.loglens.domain.auth.constants.AuthErrorCode;
import S13P31A306.loglens.domain.auth.jwt.JwtTokenProvider;
import S13P31A306.loglens.domain.auth.respository.AuthRepository;
import S13P31A306.loglens.global.exception.BusinessException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthValidator {

    private static final String LOG_PREFIX = "[AuthValidator]";

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
}
