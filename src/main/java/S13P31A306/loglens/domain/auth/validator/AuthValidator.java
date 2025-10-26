package S13P31A306.loglens.domain.auth.validator;

import S13P31A306.loglens.domain.auth.jwt.JwtTokenProvider;
import S13P31A306.loglens.domain.auth.respository.AuthRepository;
import S13P31A306.loglens.global.constants.GlobalErrorCode;
import S13P31A306.loglens.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthValidator {

    private static final String LOG_PREFIX = "[AuthValidator] ";

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthRepository authRepository;

    public String validateRefreshToken(final String accessToken, final String refreshToken) {
        // 1. Refresh Token 자체 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            log.warn("{} 토큰 재발급 실패: 유효하지 않은 Refresh Token", LOG_PREFIX);
            throw new BusinessException(GlobalErrorCode.REFRESH_TOKEN_INVALID);
        }

        // 2. Access Token과 Refresh Token의 소유자 일치 여부 확인
        String userEmailFromRt = jwtTokenProvider.getSubject(refreshToken);
        String userEmailFromAt = jwtTokenProvider.getSubjectFromExpiredToken(accessToken);

        if (!userEmailFromRt.equals(userEmailFromAt)) {
            log.warn("{} 토큰 재발급 실패: 토큰 소유자 불일치. RT sub: {}, AT sub: {}",
                    LOG_PREFIX,
                    userEmailFromRt,
                    userEmailFromAt);
            throw new BusinessException(GlobalErrorCode.REFRESH_TOKEN_INVALID);
        }

        // 3. Repository(Redis)에 저장된 Refresh Token과 일치하는지 확인
        String storedRefreshToken = authRepository.findRefreshTokenByEmail(userEmailFromRt)
                .orElseThrow(() -> {
                    log.warn("{} 토큰 재발급 실패: 저장된 Refresh Token 없음 - 사용자 email: {}",
                            LOG_PREFIX,
                            userEmailFromRt);
                    return new BusinessException(GlobalErrorCode.REFRESH_TOKEN_INVALID);
                });

        if (!storedRefreshToken.equals(refreshToken)) {
            log.warn("{} 토큰 재발급 실패: 저장된 토큰과 불일치 - 사용자 email: {}",
                    LOG_PREFIX,
                    userEmailFromRt);
            throw new BusinessException(GlobalErrorCode.REFRESH_TOKEN_INVALID);
        }

        // 모든 검증 통과 시, 사용자 email 반환
        return userEmailFromRt;
    }
}
