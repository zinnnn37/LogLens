package S13P31A306.loglens.domain.auth.service;

import S13P31A306.loglens.domain.auth.dto.request.UserSignupRequest;
import S13P31A306.loglens.domain.auth.jwt.Jwt;
import org.springframework.security.core.Authentication;

public interface AuthService {

    /**
     * 사용자 로그인 처리
     *
     * @param request 로그인 요청 DTO (email, password)
     * @return JWT (Access Token, Refresh Token)
     */
    Jwt signIn(UserSignupRequest request);

    /**
     * Access Token 재발급
     *
     * @param accessToken  만료된 Access Token
     * @param refreshToken 유효한 Refresh Token
     * @return 새로운 JWT (Access Token, Refresh Token)
     */
    Jwt reissueToken(String accessToken, String refreshToken);

    /**
     * 사용자 로그아웃 처리
     *
     * @param authentication 현재 인증된 사용자 정보
     */
    void signOut(Authentication authentication);
}
