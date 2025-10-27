package S13P31A306.loglens.domain.auth.controller;

import S13P31A306.loglens.domain.auth.dto.request.UserSignupRequest;
import S13P31A306.loglens.domain.auth.dto.response.EmailValidateResponse;
import S13P31A306.loglens.domain.auth.dto.response.UserSignupResponse;
import S13P31A306.loglens.global.config.swagger.annotation.ApiInternalServerError;
import S13P31A306.loglens.global.dto.response.BaseResponse;
import S13P31A306.loglens.global.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@ApiInternalServerError
@Tag(name = "User API", description = "사용자 회원가입 및 이메일 중복 확인 API")
public interface UserApi {

    @Operation(
            summary = "회원가입",
            description = "새로운 사용자를 등록합니다. 회원가입 시 자동으로 사용자 토큰이 발급됩니다.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "회원가입 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = UserSignupResponse.class),
                                    examples = @ExampleObject(
                                            name = "SignupSuccess",
                                            value = """
                                                    {
                                                      "code": "U201-1",
                                                      "message": "회원가입이 완료되었습니다.",
                                                      "status": 201,
                                                      "timestamp": "2025-10-17T12:30:00Z",
                                                      "data": {
                                                        "userId": 1,
                                                        "name": "홍길동",
                                                        "email": "developer@example.com",
                                                        "createdAt": "2025-10-17T12:30:00Z"
                                                      }
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "입력값 유효성 검증 실패",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "EmailInvalidFormat",
                                                    value = """
                                                            {
                                                              "code": "U400-3",
                                                              "message": "이메일 형식이 올바르지 않습니다.",
                                                              "status": 400,
                                                              "timestamp": "2025-10-17T12:31:00Z"
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "PasswordMismatch",
                                                    value = """
                                                            {
                                                              "code": "U400-1",
                                                              "message": "비밀번호가 일치하지 않습니다.",
                                                              "status": 400,
                                                              "timestamp": "2025-10-17T12:31:00Z"
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "EmailAlreadyExists",
                                                    value = """
                                                            {
                                                              "code": "U400-2",
                                                              "message": "이미 사용 중인 이메일입니다.",
                                                              "status": 400,
                                                              "timestamp": "2025-10-17T12:31:00Z"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    )
            }
    )
    ResponseEntity<? extends BaseResponse> signup(
            @Valid @RequestBody UserSignupRequest request
    );

    @Operation(
            summary = "이메일 중복 확인",
            description = "회원가입 시 이메일 중복 여부를 확인합니다.",
            parameters = {
                    @Parameter(
                            in = ParameterIn.QUERY,
                            name = "email",
                            description = "확인할 이메일 주소",
                            required = true,
                            schema = @Schema(type = "string", example = "developer@example.com")
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "이메일 사용 가능 또는 중복",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = EmailValidateResponse.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "EmailAvailable",
                                                    summary = "사용 가능한 이메일",
                                                    value = """
                                                            {
                                                              "code": "U200-1",
                                                              "message": "사용 가능한 이메일입니다.",
                                                              "status": 200,
                                                              "timestamp": "2025-10-17T10:30:00Z",
                                                              "data": {
                                                                "email": "developer@example.com",
                                                                "available": true
                                                              }
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "EmailDuplicate",
                                                    summary = "이메일 중복",
                                                    value = """
                                                            {
                                                              "code": "U200-2",
                                                              "message": "이미 사용 중인 이메일입니다.",
                                                              "status": 200,
                                                              "timestamp": "2025-10-17T10:30:00Z",
                                                              "data": {
                                                                "email": "developer@example.com",
                                                                "available": false
                                                              }
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "유효성 검증 실패",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "InvalidEmailFormat",
                                            value = """
                                                    {
                                                      "code": "U400-3",
                                                      "message": "이메일 형식이 올바르지 않습니다.",
                                                      "status": 400,
                                                      "timestamp": "2025-10-17T10:30:00Z",
                                                      "data": {
                                                        "errors": [
                                                          {
                                                            "field": "email",
                                                            "reason": "이메일 형식이 올바르지 않습니다."
                                                          }
                                                        ]
                                                      }
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<? extends BaseResponse> checkEmailAvailability(
            @RequestParam(name = "email") String email
    );
}
