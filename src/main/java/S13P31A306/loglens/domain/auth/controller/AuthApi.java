package S13P31A306.loglens.domain.auth.controller;

import S13P31A306.loglens.domain.auth.dto.request.UserSigninRequest;
import S13P31A306.loglens.domain.auth.dto.response.TokenRefreshResponse;
import S13P31A306.loglens.global.config.swagger.annotation.ApiInternalServerError;
import S13P31A306.loglens.global.dto.response.BaseResponse;
import S13P31A306.loglens.global.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@ApiInternalServerError
@Tag(name = "Auth API", description = "사용자 인증 관련 API")
public interface AuthApi {

    @Operation(
            summary = "사용자 로그인",
            description = "사용자 로그인을 처리하고 JWT 토큰을 발급합니다. Access Token은 응답 본문으로, Refresh Token은 HttpOnly 쿠키로 전달됩니다.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "로그인 성공",
                            headers = @Header(
                                    name = "Set-Cookie",
                                    description = "refreshToken=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...; HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=604800",
                                    schema = @Schema(type = "string")
                            ),
                            content = @Content(mediaType = "application/json",
                                    examples = @ExampleObject(name = "SignInSuccess", summary = "로그인 성공", value = """
                                            {
                                              "code": 200,
                                              "message": "로그인에 성공했습니다.",
                                              "data": {
                                                "userId": 1,
                                                "email": "som@example.com",
                                                "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                                "tokenType": "Bearer",
                                                "expiresIn": 3600
                                              },
                                              "timestamp": "2025-10-17T13:12:00Z"
                                            }
                                            """
                                    ))),
                    @ApiResponse(responseCode = "400", description = "입력값 유효성 검증 실패",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @ExampleObject(name = "EmailRequired", summary = "이메일 필수 입력", value = """
                                                    {
                                                      "code": 400,
                                                      "message": "이메일은 필수 입력입니다.",
                                                      "errorCode": "EMAIL_REQUIRED",
                                                      "timestamp": "2025-10-17T13:12:00Z"
                                                    }
                                                    """
                                            ),
                                            @ExampleObject(name = "EmailInvalidFormat", summary = "이메일 형식 오류", value = """
                                                    {
                                                      "code": 400,
                                                      "message": "이메일 형식이 올바르지 않습니다.",
                                                      "errorCode": "EMAIL_INVALID_FORMAT",
                                                      "timestamp": "2025-10-17T13:12:00Z"
                                                    }
                                                    """
                                            ),
                                            @ExampleObject(name = "PasswordRequired", summary = "비밀번호 필수 입력", value = """
                                                    {
                                                      "code": 400,
                                                      "message": "비밀번호는 필수 입력입니다.",
                                                      "errorCode": "PASSWORD_REQUIRED",
                                                      "timestamp": "2025-10-17T13:12:00Z"
                                                    }
                                                    """
                                            ),
                                            @ExampleObject(name = "ValidationFailed", summary = "유효성 검증 실패", value = """
                                                    {
                                                      "code": 400,
                                                      "message": "입력값이 유효하지 않습니다.",
                                                      "errorCode": "VALIDATION_FAILED",
                                                      "data": {
                                                        "path": "/api/auth/tokens",
                                                        "errors": [
                                                          {
                                                            "field": "email",
                                                            "code": "EMAIL_INVALID_FORMAT",
                                                            "reason": "이메일 형식이 올바르지 않습니다.",
                                                            "rejectedValue": "som@"
                                                          },
                                                          {
                                                            "field": "password",
                                                            "code": "PASSWORD_REQUIRED",
                                                            "reason": "비밀번호는 필수 입력입니다.",
                                                            "rejectedValue": ""
                                                          }
                                                        ]
                                                      },
                                                      "timestamp": "2025-10-17T13:12:00Z"
                                                    }
                                                    """
                                            )
                                    })),
                    @ApiResponse(responseCode = "401", description = "인증 실패 (이메일 또는 비밀번호 불일치)",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "InvalidCredentials", summary = "인증 실패", value = """
                                            {
                                              "code": 401,
                                              "message": "이메일 또는 비밀번호가 일치하지 않습니다.",
                                              "errorCode": "INVALID_CREDENTIALS",
                                              "timestamp": "2025-10-17T13:12:00Z"
                                            }
                                            """
                                    )))
            }
    )
    ResponseEntity<? extends BaseResponse> signIn(@Valid @RequestBody UserSigninRequest request);

    @Operation(
            summary = "토큰 재발급",
            description = "만료된 Access Token과 유효한 Refresh Token(쿠키)을 사용하여 새로운 Access Token을 발급받고, Refresh Token을 갱신하여 쿠키로 전달합니다.",
            parameters = {
                    @Parameter(
                            in = ParameterIn.HEADER,
                            name = "Authorization",
                            description = "만료된 Access Token (Bearer 포함)",
                            required = true,
                            schema = @Schema(type = "string"),
                            example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                    ),
                    @Parameter(
                            in = ParameterIn.COOKIE,
                            name = "refreshToken",
                            description = "Refresh Token (HttpOnly, Secure 쿠키)",
                            required = true,
                            schema = @Schema(type = "string")
                    )
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "토큰 재발급 성공",
                            headers = @Header(
                                    name = "Set-Cookie",
                                    description = "refreshToken=eyNewRefreshToken...; HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=604800",
                                    schema = @Schema(type = "string")
                            ),
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = TokenRefreshResponse.class),
                                    examples = @ExampleObject(name = "TokenRefreshSuccess", summary = "토큰 재발급 성공", value = """
                                            {
                                              "code": 200,
                                              "message": "토큰이 갱신되었습니다.",
                                              "data": {
                                                "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwiaWF0IjoxNjk...",
                                                "tokenType": "Bearer",
                                                "expiresIn": 3600
                                              }
                                            }
                                            """
                                    ))),
                    @ApiResponse(responseCode = "400", description = "Refresh Token 누락 또는 형식 오류",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @ExampleObject(name = "RefreshTokenMissing", summary = "Refresh Token 누락", value = """
                                                    {
                                                      "code": 400,
                                                      "message": "Refresh Token이 누락되었습니다.",
                                                      "errorCode": "REFRESH_TOKEN_MISSING",
                                                      "timestamp": "2025-10-17T13:12:00Z"
                                                    }
                                                    """
                                            ),
                                            @ExampleObject(name = "InvalidTokenFormat", summary = "토큰 형식 오류", value = """
                                                    {
                                                      "code": 400,
                                                      "message": "토큰 형식이 올바르지 않습니다.",
                                                      "errorCode": "INVALID_TOKEN_FORMAT",
                                                      "timestamp": "2025-10-17T13:12:00Z"
                                                    }
                                                    """
                                            )
                                    })),
                    @ApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 리프레시 토큰",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @ExampleObject(name = "RefreshTokenExpired", summary = "Refresh Token 만료", value = """
                                                    {
                                                      "code": 401,
                                                      "message": "Refresh Token이 만료되었습니다. 다시 로그인해주세요.",
                                                      "errorCode": "REFRESH_TOKEN_EXPIRED",
                                                      "timestamp": "2025-10-17T13:12:00Z"
                                                    }
                                                    """
                                            ),
                                            @ExampleObject(name = "RefreshTokenInvalid", summary = "유효하지 않은 Refresh Token", value = """
                                                    {
                                                      "code": 401,
                                                      "message": "유효하지 않거나 취소된 Refresh Token입니다.",
                                                      "errorCode": "REFRESH_TOKEN_INVALID",
                                                      "timestamp": "2025-10-17T13:12:00Z"
                                                    }
                                                    """
                                            )
                                    }))
            }
    )
    ResponseEntity<? extends BaseResponse> reissueToken(
            @RequestHeader(name = "Authorization", required = true) String authHeader,
            @CookieValue(name = "refreshToken", required = true) String refreshToken
    );

    @Operation(
            summary = "로그아웃",
            description = "사용자 로그아웃을 처리하고 Access Token과 Refresh Token을 모두 무효화합니다. Access Token은 블랙리스트에 추가되고, Refresh Token은 서버에서 삭제됩니다.",
            parameters = {
                    @Parameter(
                            in = ParameterIn.HEADER,
                            name = "Authorization",
                            description = "Bearer {access_token}",
                            required = true,
                            schema = @Schema(type = "string"),
                            example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                    ),
                    @Parameter(
                            in = ParameterIn.COOKIE,
                            name = "refreshToken",
                            description = "Refresh Token (HttpOnly, Secure 쿠키)",
                            required = true,
                            schema = @Schema(type = "string")
                    )
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "로그아웃 성공",
                            headers = @Header(
                                    name = "Set-Cookie",
                                    description = "refreshToken=; HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=0",
                                    schema = @Schema(type = "string")
                            ),
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = BaseResponse.class),
                                    examples = {
                                            @ExampleObject(name = "SignOutSuccess", summary = "로그아웃 성공", value = """
                                                    {
                                                      "code": 200,
                                                      "message": "로그아웃이 완료되었습니다.",
                                                      "timestamp": "2025-10-17T13:12:00Z"
                                                    }
                                                    """
                                            ),
                                            @ExampleObject(name = "AlreadySignedOut", summary = "이미 로그아웃된 상태 (Idempotent)", value = """
                                                    {
                                                      "code": 200,
                                                      "message": "이미 로그아웃된 상태입니다.",
                                                      "errorCode": "ALREADY_SIGNED_OUT",
                                                      "timestamp": "2025-10-17T13:12:00Z"
                                                    }
                                                    """
                                            )
                                    })),
                    @ApiResponse(responseCode = "400", description = "토큰 누락 또는 형식 오류",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @ExampleObject(name = "AccessTokenMissing", summary = "Access Token 누락", value = """
                                                    {
                                                      "code": 400,
                                                      "message": "Access Token이 누락되었습니다.",
                                                      "errorCode": "ACCESS_TOKEN_MISSING",
                                                      "timestamp": "2025-10-17T13:12:00Z"
                                                    }
                                                    """
                                            ),
                                            @ExampleObject(name = "InvalidTokenFormat", summary = "토큰 형식 오류", value = """
                                                    {
                                                      "code": 400,
                                                      "message": "토큰 형식이 올바르지 않습니다. Bearer 토큰을 사용해주세요.",
                                                      "errorCode": "INVALID_TOKEN_FORMAT",
                                                      "timestamp": "2025-10-17T13:12:00Z"
                                                    }
                                                    """
                                            ),
                                            @ExampleObject(name = "RefreshTokenMissing", summary = "Refresh Token 누락", value = """
                                                    {
                                                      "code": 400,
                                                      "message": "Refresh Token이 누락되었습니다.",
                                                      "errorCode": "REFRESH_TOKEN_MISSING",
                                                      "timestamp": "2025-10-17T13:12:00Z"
                                                    }
                                                    """
                                            )
                                    })),
                    @ApiResponse(responseCode = "401", description = "유효하지 않은 토큰",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @ExampleObject(name = "AccessTokenInvalid", summary = "Access Token 무효", value = """
                                                    {
                                                      "code": 401,
                                                      "message": "유효하지 않은 Access Token입니다.",
                                                      "errorCode": "ACCESS_TOKEN_INVALID",
                                                      "timestamp": "2025-10-17T13:12:00Z"
                                                    }
                                                    """
                                            ),
                                            @ExampleObject(name = "RefreshTokenInvalid", summary = "Refresh Token 무효", value = """
                                                    {
                                                      "code": 401,
                                                      "message": "유효하지 않거나 이미 무효화된 Refresh Token입니다.",
                                                      "errorCode": "REFRESH_TOKEN_INVALID",
                                                      "timestamp": "2025-10-17T13:12:00Z"
                                                    }
                                                    """
                                            )
                                    }))
            }
    )
    ResponseEntity<? extends BaseResponse> signOut(
            Authentication authentication,
            @CookieValue(name = "refreshToken", required = true) String refreshToken
    );
}
