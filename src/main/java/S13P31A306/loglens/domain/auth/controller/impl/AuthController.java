package S13P31A306.loglens.domain.auth.controller.impl;

import S13P31A306.loglens.domain.auth.constants.AuthErrorCode;
import S13P31A306.loglens.domain.auth.constants.AuthSuccessCode;
import S13P31A306.loglens.domain.auth.controller.AuthApi;
import S13P31A306.loglens.domain.auth.dto.request.UserSigninRequest;
import S13P31A306.loglens.domain.auth.dto.response.TokenRefreshResponse;
import S13P31A306.loglens.domain.auth.dto.response.UserSigninResponse;
import S13P31A306.loglens.domain.auth.jwt.Jwt;
import S13P31A306.loglens.domain.auth.mapper.AuthMapper;
import S13P31A306.loglens.domain.auth.service.AuthService;
import S13P31A306.loglens.global.constants.GlobalErrorCode;
import S13P31A306.loglens.global.dto.response.ApiResponseFactory;
import S13P31A306.loglens.global.dto.response.BaseResponse;
import S13P31A306.loglens.global.exception.BusinessException;
import S13P31A306.loglens.global.utils.CookieUtil;
import jakarta.validation.Valid;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private static final String LOG_PREFIX = "[AuthController]";
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;
    private final AuthMapper authMapper;
    private final CookieUtil cookieUtil;

    @Override
    @PostMapping("/tokens")
    public ResponseEntity<? extends BaseResponse> signIn(
            @Valid @RequestBody UserSigninRequest request) {
        log.info("{} 로그인 요청: email={}", LOG_PREFIX, request.email());

        // 1. 인증 처리 및 JWT 생성
        Jwt jwt = authService.signIn(request);
        UserSigninResponse data = authMapper.toUserSigninResponse(jwt);

        return ApiResponseFactory.success(AuthSuccessCode.SIGNIN_SUCCESS, data);
    }

    @Override
    @PostMapping("/tokens/refresh")
    public ResponseEntity<? extends BaseResponse> reissueToken(
            @RequestHeader(name = "Authorization", required = false) String authHeader,
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken) {
        log.info("{} 토큰 재발급 요청", LOG_PREFIX);

        // 1. Authorization 헤더 검증
        if (!StringUtils.hasText(authHeader)) {
            log.warn("{} Access Token 누락", LOG_PREFIX);
            throw new BusinessException(AuthErrorCode.ACCESS_TOKEN_MISSING);
        }

        if (!authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("{} 잘못된 토큰 형식: {}", LOG_PREFIX, authHeader);
            throw new BusinessException(AuthErrorCode.INVALID_TOKEN_FORMAT);
        }

        // 2. Refresh Token 쿠키 검증
        if (!StringUtils.hasText(refreshToken)) {
            log.warn("{} Refresh Token 누락", LOG_PREFIX);
            throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_MISSING);
        }

        // 3. Bearer 접두사 제거
        String accessToken = authHeader.substring(BEARER_PREFIX.length());

        // 4. 토큰 재발급
        Jwt newJwt = authService.reissueToken(accessToken, refreshToken);

        // 5. 새로운 Refresh Token을 쿠키로 설정 (토큰 회전)
        ResponseCookie cookie = cookieUtil.createRefreshTokenCookie(newJwt.getRefreshToken());

        // 6. 응답 생성
        TokenRefreshResponse data = authMapper.toTokenRefreshResponse(newJwt);

        log.info("{} 토큰 재발급 성공", LOG_PREFIX);
        return ApiResponseFactory.success(AuthSuccessCode.TOKEN_REFRESH_SUCCESS, data, cookie);
    }

    @Override
    @PostMapping("/signout")
    public ResponseEntity<? extends BaseResponse> signOut(
            Authentication authentication,
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken) {

        if (Objects.isNull(authentication)) {
            log.warn("{} 인증되지 않은 로그아웃 요청", LOG_PREFIX);
            throw new BusinessException(GlobalErrorCode.UNAUTHORIZED);
        }

        String userEmail = authentication.getName();
        log.info("{} 로그아웃 요청: email={}", LOG_PREFIX, userEmail);

        // Refresh Token 쿠키 검증
        if (!StringUtils.hasText(refreshToken)) {
            log.warn("{} Refresh Token 누락", LOG_PREFIX);
            throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_MISSING);
        }

        // 로그아웃 처리
        authService.signOut(authentication);

        // Refresh Token 쿠키 삭제
        ResponseCookie cookie = cookieUtil.expireRefreshTokenCookie();

        log.info("{} 로그아웃 성공: email={}", LOG_PREFIX, userEmail);
        return ApiResponseFactory.success(AuthSuccessCode.SIGNOUT_SUCCESS, cookie);
    }
}
