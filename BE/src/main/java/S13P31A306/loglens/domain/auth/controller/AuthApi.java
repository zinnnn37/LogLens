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
                    @ApiResponse(
                            responseCode = "200",
                            description = "로그인 성공",
                            headers = @Header(
                                    name = "Set-Cookie",
                                    description = "refreshToken=eyJ...; HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=604800",
                                    schema = @Schema(type = "string")
                            ),
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(name = "SignInSuccess", value = """
                                            {
                                              "code": "A200-1",
                                              "message": "로그인에 성공했습니다.",
                                              "status": 200,
                                              "timestamp": "2025-10-17T13:12:00Z",
                                              "data": {
                                                "userId": 1,
                                                "email": "som@example.com",
                                                "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                                "tokenType": "Bearer",
                                                "expiresIn": 3600
                                              }
                                            }
                                            """)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "입력값 유효성 검증 실패",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @ExampleObject(name = "EmailRequired", value = """
                                                    {
                                                      "code": "A400-2",
                                                      "message": "이메일은 필수 입력입니다.",
                                                      "status": 400,
                                                      "timestamp": "2025-10-17T13:12:00Z"
                                                    }
                                                    """),
                                            @ExampleObject(name = "EmailInvalidFormat", value = """
                                                    {
                                                      "code": "A400-3",
                                                      "message": "이메일 형식이 올바르지 않습니다.",
                                                      "status": 400,
                                                      "timestamp": "2025-10-17T13:12:00Z"
                                                    }
                                                    """),
                                            @ExampleObject(name = "PasswordRequired", value = """
                                                    {
                                                      "code": "A400-4",
                                                      "message": "비밀번호는 필수 입력입니다.",
                                                      "status": 400,
                                                      "timestamp": "2025-10-17T13:12:00Z"
                                                    }
                                                    """)
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패 (이메일 또는 비밀번호 불일치)",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "InvalidCredentials", value = """
                                            {
                                              "code": "A401-1",
                                              "message": "이메일 또는 비밀번호가 일치하지 않습니다.",
                                              "status": 401,
                                              "timestamp": "2025-10-17T13:12:00Z"
                                            }
                                            """)
                            )
                    )
            }
    )
    ResponseEntity<? extends BaseResponse> signIn(@Valid @RequestBody UserSigninRequest request);

    @Operation(
            summary = "토큰 재발급",
            description = "만료된 Access Token과 유효한 Refresh Token(쿠키)을 사용하여 새로운 Access Token을 발급받습니다.",
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
                    @ApiResponse(
                            responseCode = "200",
                            description = "토큰 재발급 성공",
                            headers = @Header(
                                    name = "Set-Cookie",
                                    description = "refreshToken=eyNewRefreshToken...; HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=604800",
                                    schema = @Schema(type = "string")
                            ),
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TokenRefreshResponse.class),
                                    examples = @ExampleObject(name = "TokenRefreshSuccess", value = """
                                            {
                                              "code": "A201-1",
                                              "message": "토큰이 갱신되었습니다.",
                                              "status": 200,
                                              "timestamp": "2025-10-17T13:12:00Z",
                                              "data": {
                                                "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                                "tokenType": "Bearer",
                                                "expiresIn": 3600
                                              }
                                            }
                                            """)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Refresh Token 누락 또는 형식 오류",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @ExampleObject(name = "RefreshTokenMissing", value = """
                                                    {
                                                      "code": "A400-5",
                                                      "message": "Refresh Token이 누락되었습니다.",
                                                      "status": 400,
                                                      "timestamp": "2025-10-17T13:12:00Z"
                                                    }
                                                    """),
                                            @ExampleObject(name = "InvalidTokenFormat", value = """
                                                    {
                                                      "code": "A400-7",
                                                      "message": "토큰 형식이 올바르지 않습니다.",
                                                      "status": 400,
                                                      "timestamp": "2025-10-17T13:12:00Z"
                                                    }
                                                    """)
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "유효하지 않거나 만료된 Refresh Token",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @ExampleObject(name = "RefreshTokenExpired", value = """
                                                    {
                                                      "code": "A401-3",
                                                      "message": "Refresh Token이 만료되었습니다. 다시 로그인해주세요.",
                                                      "status": 401,
                                                      "timestamp": "2025-10-17T13:12:00Z"
                                                    }
                                                    """),
                                            @ExampleObject(name = "RefreshTokenInvalid", value = """
                                                    {
                                                      "code": "A401-2",
                                                      "message": "유효하지 않거나 취소된 Refresh Token입니다.",
                                                      "status": 401,
                                                      "timestamp": "2025-10-17T13:12:00Z"
                                                    }
                                                    """)
                                    }
                            )
                    )
            }
    )
    ResponseEntity<? extends BaseResponse> reissueToken(
            @RequestHeader(name = "Authorization") String authHeader,
            @CookieValue(name = "refreshToken") String refreshToken
    );
    
    @Operation(
            summary = "로그아웃",
            description = "Access Token과 Refresh Token을 모두 무효화합니다.",
            parameters = {
                    @Parameter(
                            in = ParameterIn.HEADER,
                            name = "Authorization",
                            description = "Bearer {access_token}",
                            required = true,
                            schema = @Schema(type = "string")
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
                    @ApiResponse(
                            responseCode = "200",
                            description = "로그아웃 성공",
                            headers = @Header(
                                    name = "Set-Cookie",
                                    description = "refreshToken=; HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=0",
                                    schema = @Schema(type = "string")
                            ),
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(name = "SignOutSuccess", value = """
                                            {
                                              "code": "A200-2",
                                              "message": "로그아웃이 완료되었습니다.",
                                              "status": 200,
                                              "timestamp": "2025-10-17T13:12:00Z"
                                            }
                                            """)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "토큰 누락 또는 형식 오류",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(name = "AccessTokenMissing", value = """
                                                    {
                                                      "code": "A400-6",
                                                      "message": "Access Token이 누락되었습니다.",
                                                      "status": 400,
                                                      "timestamp": "2025-10-17T13:12:00Z"
                                                    }
                                                    """),
                                            @ExampleObject(name = "InvalidTokenFormat", value = """
                                                    {
                                                      "code": "A400-7",
                                                      "message": "토큰 형식이 올바르지 않습니다.",
                                                      "status": 400,
                                                      "timestamp": "2025-10-17T13:12:00Z"
                                                    }
                                                    """)
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "유효하지 않은 토큰",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(name = "AccessTokenInvalid", value = """
                                            {
                                              "code": "A401-4",
                                              "message": "유효하지 않은 Access Token입니다.",
                                              "status": 401,
                                              "timestamp": "2025-10-17T13:12:00Z"
                                            }
                                            """)
                            )
                    )
            }
    )
    ResponseEntity<? extends BaseResponse> signOut(
            Authentication authentication,
            @CookieValue(name = "refreshToken") String refreshToken
    );
}
