package S13P31A306.loglens.domain.dashboard.controller;

import S13P31A306.loglens.domain.dashboard.dto.response.*;
import S13P31A306.loglens.global.annotation.ValidUuid;
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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@ApiInternalServerError
@Tag(name = "Dashboard API", description = "대시보드 관련 API")
public interface DashboardApi {

    @Operation(
            summary = "대시보드 통계 개요 조회",
            description = "프로젝트의 전체 로그 통계를 조회합니다. 총 로그 수, 에러/경고/정보 로그 수, 평균 응답 시간 등을 제공합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            parameters = {
                    @Parameter(
                            in = ParameterIn.QUERY,
                            name = "projectUuid",
                            description = "프로젝트 UUID",
                            required = true,
                            schema = @Schema(type = "string", example = "48d96cd7-bf8d-38f5-891c-9c2f6430d871")
                    ),
                    @Parameter(
                            in = ParameterIn.QUERY,
                            name = "startTime",
                            description = "조회 시작 시간 (ISO 8601 형식). 미입력 시 7일 전",
                            required = false,
                            schema = @Schema(type = "string", format = "date-time", example = "2025-10-10T14:20:35Z")
                    ),
                    @Parameter(
                            in = ParameterIn.QUERY,
                            name = "endTime",
                            description = "조회 종료 시간 (ISO 8601 형식). 미입력 시 현재 시간",
                            required = false,
                            schema = @Schema(type = "string", format = "date-time", example = "2025-10-17T14:20:35Z")
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "대시보드 통계 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = DashboardOverviewResponse.class),
                                    examples = @ExampleObject(
                                            name = "OverviewRetrieved",
                                            summary = "통계 개요 조회 성공 예시",
                                            value = """
                                                {
                                                  "code": "DSB200-1",
                                                  "message": "대시보드 통계를 성공적으로 조회했습니다.",
                                                  "status": 200,
                                                  "data": {
                                                    "projectUuid": "48d96cd7-bf8d-38f5-891c-9c2f6430d871",
                                                    "period": {
                                                      "startTime": "2025-10-10T14:20:35Z",
                                                      "endTime": "2025-10-17T14:20:35Z"
                                                    },
                                                    "summary": {
                                                      "totalLogs": 24500,
                                                      "errorCount": 5000,
                                                      "warnCount": 12300,
                                                      "infoCount": 7200,
                                                      "avgResponseTime": 245
                                                    }
                                                  },
                                                  "timestamp": "2025-10-17T14:20:35Z"
                                                }
                                                """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 (UUID 형식, 시간 형식, 시간 범위 등)",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "InvalidUuid",
                                                    summary = "잘못된 UUID 형식",
                                                    value = """
                                                        {
                                                            "code": "G400",
                                                            "message": "입력값이 유효하지 않습니다",
                                                            "status": 400,
                                                            "timestamp": "2025-10-17T14:20:35Z"
                                                        }
                                                        """
                                            ),
                                            @ExampleObject(
                                                    name = "InvalidTimeFormat",
                                                    summary = "잘못된 시간 형식",
                                                    value = """
                                                        {
                                                            "code": "DSB400-3",
                                                            "message": "유효하지 않은 시간 형식입니다. (ISO 8601 형식 사용)",
                                                            "status": 400,
                                                            "timestamp": "2025-10-17T14:20:35Z"
                                                        }
                                                        """
                                            ),
                                            @ExampleObject(
                                                    name = "InvalidTimeRange",
                                                    summary = "잘못된 시간 범위",
                                                    value = """
                                                        {
                                                            "code": "DSB400-4",
                                                            "message": "종료 시간은 시작 시간보다 늦어야 합니다.",
                                                            "status": 400,
                                                            "timestamp": "2025-10-17T14:20:35Z"
                                                        }
                                                        """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "Unauthorized",
                                            summary = "인증 필요",
                                            value = """
                                                {
                                                    "code": "G401",
                                                    "message": "인증이 필요합니다.",
                                                    "status": 401,
                                                    "timestamp": "2025-10-17T14:20:35Z"
                                                }
                                                """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "프로젝트 접근 권한 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "AccessDenied",
                                            summary = "접근 권한 없음",
                                            value = """
                                                {
                                                    "code": "PJ403-1",
                                                    "message": "해당 프로젝트에 대한 접근 권한이 없습니다.",
                                                    "status": 403,
                                                    "timestamp": "2025-10-17T14:20:35Z"
                                                }
                                                """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "프로젝트를 찾을 수 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "ProjectNotFound",
                                            summary = "프로젝트 없음",
                                            value = """
                                                {
                                                    "code": "PJ404-1",
                                                    "message": "해당 프로젝트를 찾을 수 없습니다.",
                                                    "status": 404,
                                                    "timestamp": "2025-10-17T14:20:35Z"
                                                }
                                                """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "서버 내부 오류",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "InternalServerError",
                                            summary = "서버 오류",
                                            value = """
                                                {
                                                    "code": "G500",
                                                    "message": "서버 내부 오류가 발생했습니다.",
                                                    "status": 500,
                                                    "timestamp": "2025-10-17T14:20:35Z"
                                                }
                                                """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<? extends BaseResponse> getStatisticsOverview(
            @ValidUuid @RequestParam String projectUuid,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    );

    @Operation(
            summary = "자주 발생하는 에러 Top N 조회",
            description = "프로젝트에서 자주 발생하는 에러를 빈도순으로 조회합니다. 에러 타입, 발생 횟수, 영향 받은 컴포넌트 등의 정보를 제공합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            parameters = {
                    @Parameter(
                            in = ParameterIn.QUERY,
                            name = "projectUuid",
                            description = "프로젝트 UUID",
                            required = true,
                            schema = @Schema(type = "string", example = "48d96cd7-bf8d-38f5-891c-9c2f6430d871")
                    ),
                    @Parameter(
                            in = ParameterIn.QUERY,
                            name = "limit",
                            description = "조회할 에러 개수 (1~50). 기본값 10",
                            required = false,
                            schema = @Schema(type = "integer", minimum = "1", maximum = "50", defaultValue = "10", example = "10")
                    ),
                    @Parameter(
                            in = ParameterIn.QUERY,
                            name = "startTime",
                            description = "조회 시작 시간 (ISO 8601 형식). 미입력 시 endTime 기준 -1일",
                            required = false,
                            schema = @Schema(type = "string", format = "date-time", example = "2025-10-10T00:00:00Z")
                    ),
                    @Parameter(
                            in = ParameterIn.QUERY,
                            name = "endTime",
                            description = "조회 종료 시간 (ISO 8601 형식). 미입력 시 startTime 기준 +1일",
                            required = false,
                            schema = @Schema(type = "string", format = "date-time", example = "2025-10-17T23:59:59Z")
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "자주 발생하는 에러 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TopFrequentErrorsResponse.class),
                                    examples = @ExampleObject(
                                            name = "FrequentErrorsRetrieved",
                                            summary = "에러 Top 10 조회 성공 예시",
                                            value = """
                                                {
                                                  "code": "DSB200-2",
                                                  "message": "자주 발생하는 에러를 성공적으로 조회했습니다.",
                                                  "status": 200,
                                                  "data": {
                                                    "projectUuid": "48d96cd7-bf8d-38f5-891c-9c2f6430d871",
                                                    "period": {
                                                      "startTime": "2025-10-10T00:00:00Z",
                                                      "endTime": "2025-10-17T23:59:59Z"
                                                    },
                                                    "errors": [
                                                      {
                                                        "rank": 1,
                                                        "exceptionType": "java.sql.SQLException",
                                                        "message": "Database connection timeout after 5000ms",
                                                        "count": 3456,
                                                        "percentage": 23.4,
                                                        "firstOccurrence": "2025-10-10T08:23:15Z",
                                                        "lastOccurrence": "2025-10-17T14:52:30Z",
                                                        "stackTrace": "at com.loglens.db.ConnectionPool.getConnection(ConnectionPool.java:145)",
                                                        "components": [
                                                          {
                                                            "id": 1,
                                                            "name": "auth-service"
                                                          },
                                                          {
                                                            "id": 4,
                                                            "name": "auth-repository"
                                                          }
                                                        ]
                                                      },
                                                      {
                                                        "rank": 2,
                                                        "exceptionType": "java.lang.NullPointerException",
                                                        "message": "Null value in required field",
                                                        "count": 2134,
                                                        "percentage": 14.5,
                                                        "firstOccurrence": "2025-10-11T10:15:22Z",
                                                        "lastOccurrence": "2025-10-17T15:30:12Z",
                                                        "stackTrace": "at com.loglens.service.UserService.validateUser(UserService.java:78)",
                                                        "components": [
                                                          {
                                                            "id": 1,
                                                            "name": "auth-service"
                                                          },
                                                          {
                                                            "id": 4,
                                                            "name": "auth-repository"
                                                          }
                                                        ]
                                                      },
                                                      {
                                                        "rank": 3,
                                                        "exceptionType": "com.loglens.exception.AuthenticationException",
                                                        "message": "Invalid JWT token: signature verification failed",
                                                        "count": 1876,
                                                        "percentage": 12.7,
                                                        "firstOccurrence": "2025-10-10T09:45:01Z",
                                                        "lastOccurrence": "2025-10-17T15:20:45Z",
                                                        "stackTrace": "at com.loglens.auth.JwtValidator.validate(JwtValidator.java:92)",
                                                        "components": [
                                                          {
                                                            "id": 1,
                                                            "name": "auth-service"
                                                          },
                                                          {
                                                            "id": 4,
                                                            "name": "auth-repository"
                                                          }
                                                        ]
                                                      }
                                                    ],
                                                    "summary": {
                                                      "totalErrors": 14765,
                                                      "uniqueErrorTypes": 47,
                                                      "top10Percentage": 68.3
                                                    }
                                                  },
                                                  "timestamp": "2025-10-17T15:45:30Z"
                                                }
                                                """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 (UUID 형식, 시간 형식, limit 범위 등)",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "InvalidUuid",
                                                    summary = "잘못된 UUID 형식",
                                                    value = """
                                                        {
                                                            "code": "G400",
                                                            "message": "입력값이 유효하지 않습니다",
                                                            "status": 400,
                                                            "timestamp": "2025-10-17T15:45:30Z"
                                                        }
                                                        """
                                            ),
                                            @ExampleObject(
                                                    name = "InvalidTimeFormat",
                                                    summary = "잘못된 시간 형식",
                                                    value = """
                                                        {
                                                            "code": "DSB400-3",
                                                            "message": "유효하지 않은 시간 형식입니다. (ISO 8601 형식 사용)",
                                                            "status": 400,
                                                            "timestamp": "2025-10-17T15:45:30Z"
                                                        }
                                                        """
                                            ),
                                            @ExampleObject(
                                                    name = "InvalidTimeRange",
                                                    summary = "잘못된 시간 범위",
                                                    value = """
                                                        {
                                                            "code": "DSB400-4",
                                                            "message": "종료 시간은 시작 시간보다 늦어야 합니다.",
                                                            "status": 400,
                                                            "timestamp": "2025-10-17T15:45:30Z"
                                                        }
                                                        """
                                            ),
                                            @ExampleObject(
                                                    name = "InvalidLimit",
                                                    summary = "limit 범위 초과",
                                                    value = """
                                                        {
                                                            "code": "DSB400-5",
                                                            "message": "limit은 1에서 50 사이의 값이어야 합니다.",
                                                            "status": 400,
                                                            "timestamp": "2025-10-17T15:45:30Z"
                                                        }
                                                        """
                                            ),
                                            @ExampleObject(
                                                    name = "PeriodExceedsLimit",
                                                    summary = "조회 기간 초과",
                                                    value = """
                                                        {
                                                            "code": "DSB400-6",
                                                            "message": "조회 기간은 최대 90일을 초과할 수 없습니다.",
                                                            "status": 400,
                                                            "timestamp": "2025-10-17T15:45:30Z"
                                                        }
                                                        """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "Unauthorized",
                                            summary = "인증 필요",
                                            value = """
                                                {
                                                    "code": "G401",
                                                    "message": "인증이 필요합니다.",
                                                    "status": 401,
                                                    "timestamp": "2025-10-17T15:45:30Z"
                                                }
                                                """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "프로젝트 접근 권한 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "AccessDenied",
                                            summary = "접근 권한 없음",
                                            value = """
                                                {
                                                    "code": "PJ403-1",
                                                    "message": "해당 프로젝트에 대한 접근 권한이 없습니다.",
                                                    "status": 403,
                                                    "timestamp": "2025-10-17T15:45:30Z"
                                                }
                                                """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "프로젝트를 찾을 수 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "ProjectNotFound",
                                            summary = "프로젝트 없음",
                                            value = """
                                                {
                                                    "code": "PJ404-1",
                                                    "message": "해당 프로젝트를 찾을 수 없습니다.",
                                                    "status": 404,
                                                    "timestamp": "2025-10-17T15:45:30Z"
                                                }
                                                """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "서버 내부 오류",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "InternalServerError",
                                            summary = "서버 오류",
                                            value = """
                                                {
                                                    "code": "G500",
                                                    "message": "서버 내부 오류가 발생했습니다.",
                                                    "status": 500,
                                                    "timestamp": "2025-10-17T15:45:30Z"
                                                }
                                                """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<? extends BaseResponse> getTopFrequentErrors(
            @ValidUuid @RequestParam String projectUuid,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    );

    @Operation(
            summary = "API 호출 통계 조회",
            description = "프로젝트의 API 엔드포인트별 호출 통계를 조회합니다. 엔드포인트별 호출 건수, 평균 응답 시간, 성공률 등을 제공합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            parameters = {
                    @Parameter(
                            in = ParameterIn.QUERY,
                            name = "projectUuid",
                            description = "프로젝트 UUID",
                            required = true,
                            schema = @Schema(type = "string", example = "48d96cd7-bf8d-38f5-891c-9c2f6430d871")
                    ),
                    @Parameter(
                            in = ParameterIn.QUERY,
                            name = "startTime",
                            description = "조회 시작 시간 (ISO 8601 형식). 미입력 시 endTime 기준 -1일",
                            required = false,
                            schema = @Schema(type = "string", format = "date-time", example = "2025-10-01T00:00:00Z")
                    ),
                    @Parameter(
                            in = ParameterIn.QUERY,
                            name = "endTime",
                            description = "조회 종료 시간 (ISO 8601 형식). 미입력 시 startTime 기준 +1일",
                            required = false,
                            schema = @Schema(type = "string", format = "date-time", example = "2025-10-17T23:59:59Z")
                    ),
                    @Parameter(
                            in = ParameterIn.QUERY,
                            name = "limit",
                            description = "조회할 API 개수 (1~50). 기본값 10",
                            required = false,
                            schema = @Schema(type = "integer", minimum = "1", maximum = "50", example = "10")
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "API 통계 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ApiEndpointResponse.class),
                                    examples = @ExampleObject(
                                            name = "ApiStatsRetrieved",
                                            summary = "API 통계 조회 성공 예시",
                                            value = """
                                                {
                                                  "code": "DSB200-3",
                                                  "message": "API 통계를 성공적으로 조회했습니다.",
                                                  "status": 200,
                                                  "data": {
                                                    "projectId": 12345,
                                                    "period": {
                                                      "startTime": "2025-10-01T00:00:00",
                                                      "endTime": "2025-10-17T23:59:59"
                                                    },
                                                    "endpoints": [
                                                      {
                                                        "id": 1,
                                                        "endpointPath": "/api/logs/search",
                                                        "httpMethod": "GET",
                                                        "totalRequests": 45230,
                                                        "errorCount": 678,
                                                        "errorRate": 1.5,
                                                        "avgResponseTime": 320,
                                                        "anomalyCount": 12,
                                                        "lastAccessed": "2025-10-17T15:30:00"
                                                      },
                                                      {
                                                        "id": 2,
                                                        "endpointPath": "/api/projects",
                                                        "httpMethod": "GET",
                                                        "totalRequests": 32150,
                                                        "errorCount": 257,
                                                        "errorRate": 0.8,
                                                        "avgResponseTime": 180,
                                                        "anomalyCount": 3,
                                                        "lastAccessed": "2025-10-17T15:28:00"
                                                      }
                                                    ],
                                                    "summary": {
                                                      "totalEndpoints": 35,
                                                      "totalRequests": 125430,
                                                      "totalErrors": 1254,
                                                      "overallErrorRate": 1.0,
                                                      "avgResponseTime": 245,
                                                      "criticalEndpoints": 2
                                                    }
                                                  },
                                                  "timestamp": "2025-10-17T10:30:00Z"
                                                }
                                                """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 (UUID 형식, 시간 형식, limit 범위 등)",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "InvalidUuid",
                                                    summary = "잘못된 UUID 형식",
                                                    value = """
                                                        {
                                                            "code": "G400",
                                                            "message": "입력값이 유효하지 않습니다",
                                                            "status": 400,
                                                            "timestamp": "2025-10-17T10:30:00Z"
                                                        }
                                                        """
                                            ),
                                            @ExampleObject(
                                                    name = "InvalidTimeFormat",
                                                    summary = "잘못된 시간 형식",
                                                    value = """
                                                        {
                                                            "code": "DSB400-3",
                                                            "message": "유효하지 않은 시간 형식입니다. (ISO 8601 형식 사용)",
                                                            "status": 400,
                                                            "timestamp": "2025-10-17T10:30:00Z"
                                                        }
                                                        """
                                            ),
                                            @ExampleObject(
                                                    name = "InvalidTimeRange",
                                                    summary = "잘못된 시간 범위",
                                                    value = """
                                                        {
                                                            "code": "DSB400-4",
                                                            "message": "종료 시간은 시작 시간보다 늦어야 합니다.",
                                                            "status": 400,
                                                            "timestamp": "2025-10-17T10:30:00Z"
                                                        }
                                                        """
                                            ),
                                            @ExampleObject(
                                                    name = "InvalidLimit",
                                                    summary = "limit 범위 초과",
                                                    value = """
                                                        {
                                                            "code": "DSB400-5",
                                                            "message": "limit은 1에서 50 사이의 값이어야 합니다.",
                                                            "status": 400,
                                                            "timestamp": "2025-10-17T10:30:00Z"
                                                        }
                                                        """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "인증 실패",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "Unauthorized",
                                            summary = "인증 필요",
                                            value = """
                                                {
                                                    "code": "G401",
                                                    "message": "인증이 필요합니다.",
                                                    "status": 401,
                                                    "timestamp": "2025-10-17T10:30:00Z"
                                                }
                                                """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "프로젝트 접근 권한 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "AccessDenied",
                                            summary = "접근 권한 없음",
                                            value = """
                                                {
                                                    "code": "PJ403-1",
                                                    "message": "해당 프로젝트에 대한 접근 권한이 없습니다.",
                                                    "status": 403,
                                                    "timestamp": "2025-10-17T10:30:00Z"
                                                }
                                                """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "프로젝트를 찾을 수 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "ProjectNotFound",
                                            summary = "프로젝트 없음",
                                            value = """
                                                {
                                                    "code": "PJ404-1",
                                                    "message": "해당 프로젝트를 찾을 수 없습니다.",
                                                    "status": 404,
                                                    "timestamp": "2025-10-17T10:30:00Z"
                                                }
                                                """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "서버 내부 오류",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "InternalServerError",
                                            summary = "서버 오류",
                                            value = """
                                                {
                                                    "code": "G500",
                                                    "message": "서버 내부 오류가 발생했습니다.",
                                                    "status": 500,
                                                    "timestamp": "2025-10-17T10:30:00Z"
                                                }
                                                """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<? extends BaseResponse> getApiCallStatistics(
            @ValidUuid @RequestParam String projectUuid,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) Integer limit
    );

    @Operation(
            summary = "로그 히트맵 조회",
            description = "특정 프로젝트의 로그 발생 패턴을 요일별, 시간대별로 집계하여 히트맵 데이터를 제공합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "히트맵 데이터 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = HeatmapResponse.class),
                                    examples = @ExampleObject(
                                            name = "HeatmapRetrieved",
                                            summary = "히트맵 조회 성공 예시",
                                            value = """
                                                {
                                                    "code": "DB200-1",
                                                    "message": "요일별 로그 히트맵 조회가 완료되었습니다.",
                                                    "status": 200,
                                                    "data": {
                                                        "projectId": 12345,
                                                        "period": {
                                                            "startTime": "2025-10-10T00:00:00Z",
                                                            "endTime": "2025-10-17T23:59:59Z"
                                                        },
                                                        "heatmap": [
                                                            {
                                                                "dayOfWeek": "MONDAY",
                                                                "dayName": "월요일",
                                                                "hourlyData": [
                                                                    {
                                                                        "hour": 0,
                                                                        "count": 1234,
                                                                        "errorCount": 45,
                                                                        "warnCount": 120,
                                                                        "infoCount": 1069,
                                                                        "intensity": 0.62
                                                                    }
                                                                ],
                                                                "totalCount": 28945
                                                            }
                                                        ],
                                                        "summary": {
                                                            "totalLogs": 185643,
                                                            "peakDay": "WEDNESDAY",
                                                            "peakHour": 14,
                                                            "peakCount": 4567,
                                                            "avgDailyCount": 26520
                                                        },
                                                        "metadata": {
                                                            "logLevel": "ALL",
                                                            "timezone": "Asia/Seoul"
                                                        }
                                                    },
                                                    "timestamp": "2025-10-17T15:30:45Z"
                                                }
                                                """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 파라미터",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = {
                                            @ExampleObject(
                                                    name = "InvalidTimeFormat",
                                                    summary = "잘못된 시간 형식",
                                                    value = """
                                                        {
                                                            "code": "DB400-1",
                                                            "message": "유효하지 않은 시간 형식입니다.",
                                                            "status": 400,
                                                            "timestamp": "2025-10-17T15:30:45Z"
                                                        }
                                                        """
                                            ),
                                            @ExampleObject(
                                                    name = "InvalidTimeRange",
                                                    summary = "잘못된 시간 범위",
                                                    value = """
                                                        {
                                                            "code": "DB400-2",
                                                            "message": "종료 시간은 시작 시간보다 늦어야 합니다.",
                                                            "status": 400,
                                                            "timestamp": "2025-10-17T15:30:45Z"
                                                        }
                                                        """
                                            ),
                                            @ExampleObject(
                                                    name = "InvalidLogLevel",
                                                    summary = "잘못된 로그 레벨",
                                                    value = """
                                                        {
                                                            "code": "DB400-3",
                                                            "message": "유효하지 않은 로그 레벨입니다.",
                                                            "status": 400,
                                                            "timestamp": "2025-10-17T15:30:45Z"
                                                        }
                                                        """
                                            ),
                                            @ExampleObject(
                                                    name = "PeriodExceedsLimit",
                                                    summary = "조회 기간 초과",
                                                    value = """
                                                        {
                                                            "code": "DB400-4",
                                                            "message": "조회 기간은 최대 90일을 초과할 수 없습니다.",
                                                            "status": 400,
                                                            "timestamp": "2025-10-17T15:30:45Z"
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
                                            name = "ProjectAccessForbidden",
                                            summary = "프로젝트 접근 권한 없음",
                                            value = """
                                                {
                                                    "code": "PJ403-1",
                                                    "message": "해당 프로젝트에 대한 접근 권한이 없습니다.",
                                                    "status": 403,
                                                    "timestamp": "2025-10-17T15:30:45Z"
                                                }
                                                """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "프로젝트를 찾을 수 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "ProjectNotFound",
                                            summary = "존재하지 않는 프로젝트",
                                            value = """
                                                {
                                                    "code": "PJ404-1",
                                                    "message": "해당 프로젝트를 찾을 수 없습니다.",
                                                    "status": 404,
                                                    "timestamp": "2025-10-17T15:30:45Z"
                                                }
                                                """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<? extends BaseResponse> getHeatmap(
            @PathVariable String projectUuid,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "ALL") String logLevel
    );

    /**
     * 알림 피드 조회
     *
     * @param projectUuid 프로젝트 UUID
     * @param severity 심각도 필터 (critical, warning, info)
     * @param isRead 읽음 여부 필터
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 알림 목록 (페이징)
     */
    ResponseEntity<? extends BaseResponse> getAlertFeed(
            @RequestParam String projectUuid,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    );

    @Operation(
            summary = "프로젝트 컴포넌트 목록 조회",
            description = "특정 프로젝트의 모든 컴포넌트 목록을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            parameters = {
                    @Parameter(in = ParameterIn.QUERY, name = "projectUuid", description = "프로젝트 UUID", required = true, schema = @Schema(type = "string"))
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "컴포넌트 목록 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ProjectComponentsResponse.class),
                                    examples = @ExampleObject(
                                            name = "ComponentsRetrieved",
                                            summary = "컴포넌트 목록 조회 성공 예시",
                                            value = """
                                                    {
                                                       "code": "DASH200-1",
                                                       "message": "컴포넌트 목록을 성공적으로 조회했습니다.",
                                                       "status": 200,
                                                       "data": {
                                                         "projectId": 1,
                                                         "components": [
                                                           {
                                                             "id": 1,
                                                             "name": "UserController",
                                                             "type": "BE",
                                                             "classType": "UserController",
                                                             "layer": "CONTROLLER",
                                                             "packageName": "com.example.demo.domain.user.controller",
                                                             "technology": "Spring Boot",
                                                             "metrics": null
                                                           },
                                                           {
                                                             "id": 2,
                                                             "name": "UserServiceImpl",
                                                             "type": "BE",
                                                             "classType": "UserServiceImpl",
                                                             "layer": "SERVICE",
                                                             "packageName": "com.example.demo.domain.user.service.impl",
                                                             "technology": "Spring Boot",
                                                             "metrics": null
                                                           },
                                                           {
                                                             "id": 3,
                                                             "name": "UserJpaRepository",
                                                             "type": "BE",
                                                             "classType": "UserJpaRepository",
                                                             "layer": "REPOSITORY",
                                                             "packageName": "com.example.demo.domain.user.repository",
                                                             "technology": "Spring Boot",
                                                             "metrics": null
                                                           },
                                                           {
                                                             "id": 4,
                                                             "name": "AsyncNotificationService",
                                                             "type": "BE",
                                                             "classType": "AsyncNotificationService",
                                                             "layer": "SERVICE",
                                                             "packageName": "com.example.demo.global.service",
                                                             "technology": "Spring Boot",
                                                             "metrics": null
                                                           },
                                                           {
                                                             "id": 5,
                                                             "name": "UserJdbcRepositoryImpl",
                                                             "type": "BE",
                                                             "classType": "UserJdbcRepositoryImpl",
                                                             "layer": "REPOSITORY",
                                                             "packageName": "com.example.demo.domain.user.repository.impl",
                                                             "technology": "Spring Boot",
                                                             "metrics": null
                                                           }
                                                         ]
                                                       },
                                                       "timestamp": "2025-11-05T08:09:11.291Z"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 UUID 형식",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "InvalidUuid",
                                            summary = "잘못된 UUID 형식",
                                            value = """
                                                    {
                                                        "code": "G400",
                                                        "message": "입력값이 유효하지 않습니다",
                                                        "status": 400,
                                                        "timestamp": "2025-01-10T10:30:00Z"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "프로젝트 접근 권한 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "AccessForbidden",
                                            summary = "접근 권한 없음",
                                            value = """
                                                    {
                                                        "code": "PJ403-1",
                                                        "message": "해당 프로젝트에 대한 접근 권한이 없습니다.",
                                                        "status": 403,
                                                        "timestamp": "2025-01-10T10:30:00Z"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "프로젝트를 찾을 수 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "ProjectNotFound",
                                            summary = "프로젝트 없음",
                                            value = """
                                                    {
                                                        "code": "PJ404-1",
                                                        "message": "해당 프로젝트를 찾을 수 없습니다.",
                                                        "status": 404,
                                                        "timestamp": "2025-01-10T10:30:00Z"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<? extends BaseResponse> getComponentDependencies(
            @ValidUuid @RequestParam String projectUuid,
            @AuthenticationPrincipal UserDetails userDetails
    );

    @Operation(
            summary = "컴포넌트 의존성 그래프 조회",
            description = "특정 컴포넌트의 의존성 그래프와 메트릭 정보를 조회합니다. 프로젝트의 전체 의존성 그래프를 반환합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            parameters = {
                    @Parameter(in = ParameterIn.PATH, name = "componentId", description = "컴포넌트 ID", required = true, schema = @Schema(type = "integer")),
                    @Parameter(in = ParameterIn.QUERY, name = "projectUuid", description = "프로젝트 UUID", required = true, schema = @Schema(type = "string"))
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "의존성 그래프 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ComponentDependencyResponse.class),
                                    examples = @ExampleObject(
                                            name = "DependencyGraphRetrieved",
                                            summary = "의존성 그래프 조회 성공 예시",
                                            value = """
                                                    {
                                                      "code": "DASH200-2",
                                                      "message": "컴포넌트 의존성 목록을 성공적으로 조회했습니다.",
                                                      "status": 200,
                                                      "data": {
                                                        "components": [
                                                          {
                                                            "id": 1,
                                                            "name": "UserController",
                                                            "type": "BE",
                                                            "classType": "UserController",
                                                            "layer": "CONTROLLER",
                                                            "packageName": "com.example.demo.domain.user.controller",
                                                            "technology": "Spring Boot",
                                                            "metrics": {
                                                              "callCount": 0,
                                                              "errorCount": 0,
                                                              "warnCount": 0,
                                                              "errorRate": 0,
                                                              "lastMeasuredAt": "2025-11-05T17:09:58.4915028"
                                                            }
                                                          },
                                                          {
                                                            "id": 2,
                                                            "name": "UserServiceImpl",
                                                            "type": "BE",
                                                            "classType": "UserServiceImpl",
                                                            "layer": "SERVICE",
                                                            "packageName": "com.example.demo.domain.user.service.impl",
                                                            "technology": "Spring Boot",
                                                            "metrics": {
                                                              "callCount": 0,
                                                              "errorCount": 0,
                                                              "warnCount": 0,
                                                              "errorRate": 0,
                                                              "lastMeasuredAt": "2025-11-05T17:09:58.4979604"
                                                            }
                                                          },
                                                          {
                                                            "id": 3,
                                                            "name": "UserJpaRepository",
                                                            "type": "BE",
                                                            "classType": "UserJpaRepository",
                                                            "layer": "REPOSITORY",
                                                            "packageName": "com.example.demo.domain.user.repository",
                                                            "technology": "Spring Boot",
                                                            "metrics": {
                                                              "callCount": 0,
                                                              "errorCount": 0,
                                                              "warnCount": 0,
                                                              "errorRate": 0,
                                                              "lastMeasuredAt": "2025-11-05T17:09:58.4997273"
                                                            }
                                                          },
                                                          {
                                                            "id": 4,
                                                            "name": "AsyncNotificationService",
                                                            "type": "BE",
                                                            "classType": "AsyncNotificationService",
                                                            "layer": "SERVICE",
                                                            "packageName": "com.example.demo.global.service",
                                                            "technology": "Spring Boot",
                                                            "metrics": {
                                                              "callCount": 0,
                                                              "errorCount": 0,
                                                              "warnCount": 0,
                                                              "errorRate": 0,
                                                              "lastMeasuredAt": "2025-11-05T17:09:58.4997273"
                                                            }
                                                          },
                                                          {
                                                            "id": 5,
                                                            "name": "UserJdbcRepositoryImpl",
                                                            "type": "BE",
                                                            "classType": "UserJdbcRepositoryImpl",
                                                            "layer": "REPOSITORY",
                                                            "packageName": "com.example.demo.domain.user.repository.impl",
                                                            "technology": "Spring Boot",
                                                            "metrics": {
                                                              "callCount": 0,
                                                              "errorCount": 0,
                                                              "warnCount": 0,
                                                              "errorRate": 0,
                                                              "lastMeasuredAt": "2025-11-05T17:09:58.4997273"
                                                            }
                                                          }
                                                        ],
                                                        "graph": {
                                                          "edges": [
                                                            {
                                                              "from": 1,
                                                              "to": 2
                                                            },
                                                            {
                                                              "from": 2,
                                                              "to": 3
                                                            },
                                                            {
                                                              "from": 2,
                                                              "to": 4
                                                            }
                                                          ]
                                                        },
                                                        "summary": {
                                                          "totalComponents": 5,
                                                          "totalCalls": 0,
                                                          "totalErrors": 0,
                                                          "totalWarns": 0,
                                                          "averageErrorRate": 0,
                                                          "highestErrorComponent": null,
                                                          "highestCallComponent": null
                                                        }
                                                      },
                                                      "timestamp": "2025-11-05T08:09:58.499Z"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "BadRequest",
                                            summary = "잘못된 요청",
                                            value = """
                                                    {
                                                        "code": "G400",
                                                        "message": "입력값이 유효하지 않습니다",
                                                        "status": 400,
                                                        "timestamp": "2025-01-10T10:30:00Z"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "접근 권한 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "Forbidden",
                                            summary = "접근 권한 없음",
                                            value = """
                                                    {
                                                        "code": "G403",
                                                        "message": "접근 권한이 없습니다",
                                                        "status": 403,
                                                        "timestamp": "2025-01-10T10:30:00Z"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "컴포넌트 또는 프로젝트를 찾을 수 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "NotFound",
                                            summary = "리소스 없음",
                                            value = """
                                                    {
                                                        "code": "G404",
                                                        "message": "요청한 리소스를 찾을 수 없습니다",
                                                        "status": 404,
                                                        "timestamp": "2025-01-10T10:30:00Z"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<? extends BaseResponse> getComponentDependencies(
            @PathVariable Integer componentId,
            @ValidUuid @RequestParam String projectUuid,
            @AuthenticationPrincipal UserDetails userDetails
    );

    @Operation(
            summary = "데이터베이스 컴포넌트 목록 조회",
            description = "프로젝트에서 사용하는 데이터베이스 목록을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"),
            parameters = {
                    @Parameter(in = ParameterIn.QUERY, name = "projectUuid", description = "프로젝트 UUID", required = true, schema = @Schema(type = "string"))
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "데이터베이스 목록 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = DatabaseComponentResponse.class),
                                    examples = @ExampleObject(
                                            name = "DatabasesRetrieved",
                                            summary = "데이터베이스 목록 조회 성공 예시",
                                            value = """
                                                    {
                                                       "code": "DASH200-9",
                                                       "message": "데이터베이스 목록을 성공적으로 조회했습니다.",
                                                       "status": 200,
                                                       "data": {
                                                         "databases": [
                                                           "MySQL",
                                                           "PostgreSQL",
                                                           "H2"
                                                         ]
                                                       },
                                                       "timestamp": "2025-11-05T08:09:11.291Z"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 UUID 형식",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "접근 권한 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "프로젝트를 찾을 수 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class)
                            )
                    )
            }
    )
    ResponseEntity<? extends BaseResponse> getDatabaseComponents(
            @ValidUuid @RequestParam String projectUuid,
            @AuthenticationPrincipal UserDetails userDetails
    );
}
