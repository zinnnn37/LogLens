package S13P31A306.loglens.domain.auth.controller.impl;

import S13P31A306.loglens.domain.auth.constants.AuthSuccessCode;
import S13P31A306.loglens.domain.auth.controller.AuthApi;
import S13P31A306.loglens.domain.auth.dto.request.UserSigninRequest;
import S13P31A306.loglens.domain.auth.dto.response.TokenRefreshResponse;
import S13P31A306.loglens.domain.auth.dto.response.UserSigninResponse;
import S13P31A306.loglens.domain.auth.jwt.Jwt;
import S13P31A306.loglens.domain.auth.mapper.AuthMapper;
import S13P31A306.loglens.domain.auth.service.AuthService;
import S13P31A306.loglens.global.dto.response.ApiResponseFactory;
import S13P31A306.loglens.global.dto.response.BaseResponse;
import S13P31A306.loglens.global.utils.CookieUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    private final AuthService authService;
    private final AuthMapper authMapper;
    private final CookieUtil cookieUtil;

    @Override
    @PostMapping("/tokens")
    public ResponseEntity<? extends BaseResponse> signIn(
            @Valid @RequestBody UserSigninRequest request) {
        Jwt jwt = authService.signIn(request);
        UserSigninResponse data = authMapper.toUserSigninResponse(jwt);

        return ApiResponseFactory.success(AuthSuccessCode.SIGNIN_SUCCESS, data);
    }

    @Override
    @PostMapping("/tokens/refresh")
    public ResponseEntity<? extends BaseResponse> reissueToken(
            @RequestHeader(name = "Authorization", required = false) String authHeader,
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken) {
        Jwt newJwt = authService.reissueToken(authHeader, refreshToken);
        ResponseCookie cookie = cookieUtil.createRefreshTokenCookie(newJwt.getRefreshToken());
        TokenRefreshResponse data = authMapper.toTokenRefreshResponse(newJwt);

        return ApiResponseFactory.success(AuthSuccessCode.TOKEN_REFRESH_SUCCESS, data, cookie);
    }

    @Override
    @DeleteMapping("/tokens")
    public ResponseEntity<? extends BaseResponse> signOut(
            Authentication authentication,
            @CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken) {
        authService.signOut(authentication, refreshToken);
        ResponseCookie cookie = cookieUtil.expireRefreshTokenCookie();

        return ApiResponseFactory.success(AuthSuccessCode.SIGNOUT_SUCCESS, cookie);
    }
}
