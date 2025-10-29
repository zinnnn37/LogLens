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
@Tag(name = "User API", description = "사용자 회원가입, 이메일 중복 확인 및 사용자 검색 API")
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

    @Operation(
            summary = "이름으로 멤버 검색",
            description = "입력한 이름(name)을 기준으로 가입된 사용자를 검색합니다. 결과는 페이지네이션 및 정렬이 적용되어 반환됩니다.",
            parameters = {
                    @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {access_token}", required = true, schema = @Schema(type = "string")),
                    @Parameter(in = ParameterIn.QUERY, name = "name", description = "검색할 사용자 이름 (최소 1자, 최대 50자)", required = true, schema = @Schema(type = "string", example = "홍길동")),
                    @Parameter(in = ParameterIn.QUERY, name = "page", description = "페이지 번호 (0부터 시작)", schema = @Schema(type = "integer", defaultValue = "0")),
                    @Parameter(in = ParameterIn.QUERY, name = "size", description = "페이지당 항목 수 (1~100)", schema = @Schema(type = "integer", defaultValue = "20")),
                    @Parameter(in = ParameterIn.QUERY, name = "sort", description = "정렬 기준 (CREATED_AT, NAME, EMAIL)", schema = @Schema(type = "string", defaultValue = "CREATED_AT")),
                    @Parameter(in = ParameterIn.QUERY, name = "order", description = "정렬 방향 (ASC, DESC)", schema = @Schema(type = "string", defaultValue = "DESC"))
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "사용자 검색 성공",
                            content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "UserSearchSuccess", value = """
                                    {
                                      "code": "U200-3",
                                      "message": "사용자 검색이 완료되었습니다.",
                                      "status": 200,
                                      "timestamp": "2025-10-27T13:00:00Z",
                                      "data": {
                                        "content": [
                                          {
                                            "userId": 101,
                                            "username": "홍길동",
                                            "email": "hong1@example.com"
                                          },
                                          {
                                            "userId": 104,
                                            "username": "홍길동",
                                            "email": "hong2@example.com"
                                          }
                                        ],
                                        "pageable": {
                                          "page": 0,
                                          "size": 20,
                                          "sort": "CREATED_AT,DESC"
                                        },
                                        "totalElements": 2,
                                        "totalPages": 1,
                                        "first": true,
                                        "last": true
                                      }
                                    }
                                    """))
                    ),
                    @ApiResponse(responseCode = "400", description = "입력값 유효성 검증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "검색 결과 없음", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    ResponseEntity<? extends BaseResponse> findUsersByName(
            @RequestParam(name = "name") String name,
            @RequestParam(name = "page", defaultValue = "0") Integer page,
            @RequestParam(name = "size", defaultValue = "20") Integer size,
            @RequestParam(name = "sort", defaultValue = "CREATED_AT") String sort,
            @RequestParam(name = "order", defaultValue = "DESC") String order
    );
}
