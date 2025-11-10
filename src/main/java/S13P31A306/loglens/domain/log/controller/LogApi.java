package S13P31A306.loglens.domain.log.controller;

import S13P31A306.loglens.domain.log.dto.request.LogSearchRequest;
import S13P31A306.loglens.global.config.swagger.annotation.ApiInternalServerError;
import S13P31A306.loglens.global.config.swagger.annotation.ApiUnauthorizedError;
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
import jakarta.validation.constraints.NotNull;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@ApiInternalServerError
@ApiUnauthorizedError
@Tag(name = "Log API", description = "로그 조회 API")
public interface LogApi {

    @Operation(
            summary = "로그 목록 조회 및 Trace ID 기반 조회",
            description = """
                    다양한 조건으로 로그를 필터링하고 커서 기반 페이지네이션을 통해 조회합니다.
                    `traceId` 파라미터 유무에 따라 두 가지 모드로 동작합니다.
                    - **`traceId`가 없는 경우**: 일반적인 로그 목록을 페이지네이션하여 반환합니다.
                    - **`traceId`가 있는 경우**: 해당 Trace ID에 속한 모든 로그 목록과 요약 정보를 반환합니다.
                    """,
            parameters = {
                    @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {access_token}", required = true, schema = @Schema(type = "string"))
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "로그 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(
                                                    name = "LogListSuccess",
                                                    summary = "일반 로그 목록 조회 성공",
                                                    description = "`traceId` 없이 요청 시, 페이지네이션된 로그 목록을 반환합니다.",
                                                    value = """
                                                            {
                                                              "code": "LG200-2",
                                                              "message": "로그 목록을 성공적으로 조회했습니다.",
                                                              "status": 200,
                                                              "timestamp": "2025-11-03T15:00:00Z",
                                                              "data": {
                                                                "logs": [
                                                                  {
                                                                    "logId": "abc123xyz789",
                                                                    "traceId": "trace-abc-123",
                                                                    "logLevel": "ERROR",
                                                                    "sourceType": "BE",
                                                                    "message": "NullPointerException occurred in UserService",
                                                                    "timestamp": "2024-01-15T10:30:45.123Z",
                                                                    "logger": "com.example.UserService",
                                                                    "layer": "Service",
                                                                    "comment": null
                                                                  }
                                                                ],
                                                                "pagination": {
                                                                  "nextCursor": "eyJzb3J0IjpbMTcwNTMxMjgwMDAwMCwiYWJjMTIzIl19",
                                                                  "hasNext": true,
                                                                  "size": 1
                                                                }
                                                              }
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "TraceLogSuccess",
                                                    summary = "Trace ID 로그 조회 성공",
                                                    description = "`traceId` 지정 요청 시, 해당 Trace에 속한 로그 목록과 요약 정보를 반환합니다.",
                                                    value = """
                                                            {
                                                              "code": "LG200-3",
                                                              "message": "TraceID로 로그를 성공적으로 조회했습니다.",
                                                              "status": 200,
                                                              "timestamp": "2025-11-03T15:01:00Z",
                                                              "data": {
                                                                "traceId": "trace-abc-123",
                                                                "summary": {
                                                                  "totalLogs": 5,
                                                                  "durationMs": 123,
                                                                  "startTime": "2024-01-15T10:30:45.000Z",
                                                                  "endTime": "2024-01-15T10:30:45.123Z",
                                                                  "errorCount": 1,
                                                                  "warnCount": 2,
                                                                  "infoCount": 2
                                                                },
                                                                "logs": [
                                                                  {
                                                                    "logId": "abc123xyz789",
                                                                    "traceId": "trace-abc-123",
                                                                    "logLevel": "ERROR",
                                                                    "sourceType": "BE",
                                                                    "message": "NullPointerException occurred in UserService",
                                                                    "timestamp": "2024-01-15T10:30:45.123Z",
                                                                    "logger": "com.example.UserService",
                                                                    "layer": "Service",
                                                                    "comment": null
                                                                  }
                                                                ]
                                                              }
                                                            }
                                                            """
                                            )
                                    }
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
                                                    name = "ProjectIdRequired",
                                                    value = """
                                                            {
                                                              "code": "LG400-09",
                                                              "message": "projectId는 필수입니다.",
                                                              "status": 400,
                                                              "timestamp": "2025-11-03T15:02:00Z"
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "InvalidTimeRange",
                                                    value = """
                                                            {
                                                              "code": "LG400-14",
                                                              "message": "startTime은 endTime보다 이전이어야 합니다.",
                                                              "status": 400,
                                                              "timestamp": "2025-11-03T15:03:00Z"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "프로젝트 접근 권한 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "ProjectForbidden",
                                            value = """
                                                    {
                                                      "code": "LG403-01",
                                                      "message": "해당 프로젝트에 대한 접근 권한이 없습니다.",
                                                      "status": 403,
                                                      "timestamp": "2025-11-03T15:04:00Z"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<? extends BaseResponse> getLogs(@ParameterObject @ModelAttribute LogSearchRequest request);

    @Operation(
            summary = "로그 상세 조회 (AI 분석 포함)",
            description = """
                    특정 로그의 상세 정보를 조회하고, AI 분석 결과를 포함합니다.
                    - OpenSearch에 AI 분석 결과가 저장되어 있으면 해당 결과를 반환합니다.
                    - AI 분석 결과가 없으면 AI 서비스를 호출하여 새로 분석합니다.
                    - AI 서비스 호출이 실패해도 로그 기본 정보는 반환됩니다 (analysis 필드가 null).
                    """,
            parameters = {
                    @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {access_token}", required = true, schema = @Schema(type = "string")),
                    @Parameter(in = ParameterIn.PATH, name = "logId", description = "로그 ID", required = true, schema = @Schema(type = "integer", format = "int64")),
                    @Parameter(in = ParameterIn.QUERY, name = "projectUuid", description = "프로젝트 UUID", required = true, schema = @Schema(type = "string"))
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "로그 상세 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "LogDetailSuccess",
                                            summary = "로그 상세 조회 성공 (AI 분석 포함)",
                                            value = """
                                                    {
                                                      "code": "LG200-4",
                                                      "message": "로그 상세 정보를 성공적으로 조회했습니다.",
                                                      "status": 200,
                                                      "timestamp": "2025-11-07T15:00:00Z",
                                                      "data": {
                                                        "logId": 1234567890,
                                                        "traceId": "trace-abc-123",
                                                        "logLevel": "ERROR",
                                                        "sourceType": "BE",
                                                        "message": "NullPointerException occurred in UserService",
                                                        "timestamp": "2024-01-15T10:30:45.123Z",
                                                        "logger": "com.example.UserService",
                                                        "layer": "Service",
                                                        "comment": null,
                                                        "serviceName": "loglens-api",
                                                        "className": "com.example.UserServiceImpl",
                                                        "methodName": "getUserById",
                                                        "threadName": "http-nio-8080-exec-5",
                                                        "requesterIp": "192.168.1.100",
                                                        "duration": 1250,
                                                        "stackTrace": "java.lang.NullPointerException\\n\\tat com.example.UserServiceImpl.getUserById(UserServiceImpl.java:42)",
                                                        "logDetails": {"userId": "123", "action": "getUser"},
                                                        "analysis": {
                                                          "summary": "사용자 ID 조회 중 NULL 참조 에러가 발생했습니다.",
                                                          "errorCause": "사용자가 존재하지 않는 경우 NULL 체크 없이 메서드를 호출하여 발생했습니다.",
                                                          "solution": "1. [우선순위: 높음] 사용자 조회 전 NULL 체크 추가\\n2. [우선순위: 중간] Optional 사용 고려",
                                                          "tags": ["NULL_POINTER", "USER_SERVICE", "ERROR"],
                                                          "analysisType": "TRACE_BASED",
                                                          "targetType": "LOG",
                                                          "analyzedAt": "2025-11-07T15:00:45.123Z"
                                                        },
                                                        "fromCache": true,
                                                        "similarLogId": 1234567800,
                                                        "similarityScore": 0.92
                                                      }
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "로그를 찾을 수 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "LogNotFound",
                                            value = """
                                                    {
                                                      "code": "LG404-01",
                                                      "message": "로그를 찾을 수 없습니다.",
                                                      "status": 404,
                                                      "timestamp": "2025-11-07T15:01:00Z"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<? extends BaseResponse> getLogDetail(
            @PathVariable @NotNull Long logId,
            @RequestParam @NotNull String projectUuid
    );
}
