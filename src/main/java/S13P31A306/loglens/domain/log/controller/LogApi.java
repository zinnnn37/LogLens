package S13P31A306.loglens.domain.log.controller;

import S13P31A306.loglens.domain.log.dto.request.LogSearchRequest;
import S13P31A306.loglens.domain.log.dto.request.LogStreamRequest;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
                                                              "data": {
                                                                "logs": [
                                                                  {
                                                                    "logId": 2261174186,
                                                                    "traceId": "fde75789-e22f-4f9f-b139-877115810ba7",
                                                                    "logLevel": "INFO",
                                                                    "sourceType": "BE",
                                                                    "message": "Response completed: existsByEmail",
                                                                    "timestamp": "2025-11-13T14:08:42.574Z",
                                                                    "logger": "com.example.demo.domain.user.repository.UserJpaRepository",
                                                                    "layer": "Repository",
                                                                    "comment": "thread: http-nio-8081-exec-3, app: demo, pid: 85986",
                                                                    "serviceName": "Loglens",
                                                                    "methodName": "existsByEmail",
                                                                    "threadName": "http-nio-8081-exec-3",
                                                                    "requesterIp": "127.0.0.1",
                                                                    "duration": 5,
                                                                    "logDetails": {
                                                                      "response_status": 200,
                                                                      "response_body": {
                                                                        "result": "true",
                                                                        "method": "existsByEmail",
                                                                        "http": {
                                                                          "endpoint": "/users",
                                                                          "method": "POST",
                                                                          "statusCode": 200
                                                                        }
                                                                      }
                                                                    }
                                                                  },
                                                                  {
                                                                    "logId": 450494601,
                                                                    "traceId": "fde75789-e22f-4f9f-b139-877115810ba7",
                                                                    "logLevel": "INFO",
                                                                    "sourceType": "BE",
                                                                    "message": "Request received: existsByEmail",
                                                                    "timestamp": "2025-11-13T14:08:42.569Z",
                                                                    "logger": "com.example.demo.domain.user.repository.UserJpaRepository",
                                                                    "layer": "Repository",
                                                                    "comment": "thread: http-nio-8081-exec-3, app: demo, pid: 85986",
                                                                    "serviceName": "Loglens",
                                                                    "methodName": "existsByEmail",
                                                                    "threadName": "http-nio-8081-exec-3",
                                                                    "requesterIp": "127.0.0.1",
                                                                    "duration": null,
                                                                    "logDetails": {
                                                                      "request_body": {
                                                                        "parameters": {
                                                                          "email": "developer2@example.com"
                                                                        },
                                                                        "http": {
                                                                          "endpoint": "/users",
                                                                          "method": "POST"
                                                                        },
                                                                        "method": "existsByEmail"
                                                                      }
                                                                    }
                                                                  }
                                                                ],
                                                                "pagination": {
                                                                  "nextCursor": "WzE3NjMwNDI5MjI1NjksIjVkZjdlYWUwNDNlNGJiNWY3N2FjODQ2ODJjMjkwMmFhNzhkMzgxZGY3MzU3YWZjZGIwNjQzN2M0MmQ3Y2RjYzYiXQ==",
                                                                  "hasNext": true,
                                                                  "size": 2
                                                                }
                                                              },
                                                              "timestamp": "2025-11-13T06:50:08.493Z"
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
                                                              "data": {
                                                                "traceId": "fde75789-e22f-4f9f-b139-877115810ba7",
                                                                "summary": {
                                                                  "totalLogs": 9,
                                                                  "durationMs": 32,
                                                                  "startTime": "2025-11-13T14:08:42.560Z",
                                                                  "endTime": "2025-11-13T14:08:42.592Z",
                                                                  "errorCount": 3,
                                                                  "warnCount": 0,
                                                                  "infoCount": 6
                                                                },
                                                                "logs": [
                                                                  {
                                                                    "logId": 1258064169,
                                                                    "traceId": "fde75789-e22f-4f9f-b139-877115810ba7",
                                                                    "logLevel": "INFO",
                                                                    "sourceType": "BE",
                                                                    "message": "Request received: doFilter",
                                                                    "timestamp": "2025-11-13T14:08:42.560Z",
                                                                    "logger": "com.example.demo.global.filter.CorsFilter",
                                                                    "layer": "Other",
                                                                    "comment": "thread: http-nio-8081-exec-3, app: demo, pid: 85986",
                                                                    "serviceName": "Loglens",
                                                                    "methodName": "doFilter",
                                                                    "threadName": "http-nio-8081-exec-3",
                                                                    "requesterIp": "127.0.0.1",
                                                                    "duration": null,
                                                                    "logDetails": {
                                                                      "request_body": {
                                                                        "method": "doFilter",
                                                                        "parameters": {}
                                                                      }
                                                                    }
                                                                  },
                                                                  {
                                                                    "logId": 4009327824,
                                                                    "traceId": "fde75789-e22f-4f9f-b139-877115810ba7",
                                                                    "logLevel": "INFO",
                                                                    "sourceType": "BE",
                                                                    "message": "Request received: createUser",
                                                                    "timestamp": "2025-11-13T14:08:42.565Z",
                                                                    "logger": "com.example.demo.domain.user.controller.UserController",
                                                                    "layer": "Controller",
                                                                    "comment": "thread: http-nio-8081-exec-3, app: demo, pid: 85986",
                                                                    "serviceName": "Loglens",
                                                                    "methodName": "createUser",
                                                                    "threadName": "http-nio-8081-exec-3",
                                                                    "requesterIp": "127.0.0.1",
                                                                    "duration": null,
                                                                    "logDetails": {
                                                                      "request_body": {
                                                                        "parameters": {
                                                                          "request": {
                                                                            "name": "홍길동",
                                                                            "email": "developer2@example.com",
                                                                            "secret": "<excluded>",
                                                                            "password": "****"
                                                                          }
                                                                        },
                                                                        "http": {
                                                                          "endpoint": "/users",
                                                                          "method": "POST"
                                                                        },
                                                                        "method": "createUser"
                                                                      }
                                                                    }
                                                                  }
                                                                ]
                                                              },
                                                              "timestamp": "2025-11-13T06:52:02.117Z"
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
                                                              "message": "projectUuid는 필수입니다.",
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
            summary = "로그 상세 조회 (AI 분석만 포함)",
            description = """
                    특정 로그의 AI 분석 결과를 포함합니다.
                    - OpenSearch에 AI 분석 결과가 저장되어 있으면 해당 결과를 반환합니다.
                    - AI 분석 결과가 없으면 AI 서비스를 호출하여 새로 분석합니다.
                    - AI 서비스 호출이 실패해도 로그 기본 정보는 반환됩니다 (analysis 필드가 null).
                    """,
            parameters = {
                    @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {access_token}", required = true, schema = @Schema(type = "string")),
                    @Parameter(in = ParameterIn.PATH, name = "logId", description = "로그 ID", required = true, schema = @Schema(type = "integer", format = "int64"), example = "2845913357"),
                    @Parameter(in = ParameterIn.QUERY, name = "projectUuid", description = "프로젝트 UUID", required = true, schema = @Schema(type = "string"), example = "9911573f-8a1d-3b96-98b4-5a0def93513b")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "로그 상세 조회(AI 분석만) 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "LogDetailSuccess",
                                            summary = "로그 상세 조회 성공 (AI 분석만)",
                                            value = """
                                                    {
                                                      "code": "LG200-4",
                                                      "message": "로그 상세 정보를 성공적으로 조회했습니다.",
                                                      "status": 200,
                                                      "data": {
                                                        "analysis": {
                                                          "summary": "사용자가 비밀번호 변경 요청을 시도했으나, 기존 비밀번호가 일치하지 않아 **BusinessException** 발생",
                                                          "solution": "### 사용자 조치 (완료 예상: 즉시)\\n- [ ] 올바른 기존 비밀번호를 입력하여 다시 시도\\n- [ ] 비밀번호를 잊으셨다면 비밀번호 찾기 기능 사용\\n\\n### 선택적 프론트엔드 개선 (완료 예상: 1-2일)\\n- [ ] 비밀번호 입력 시 실시간 검증 기능 추가\\n- [ ] 더 명확한 에러 메시지 표시 (예: \\"입력한 기존 비밀번호가 일치하지 않습니다. 다시 확인해주세요.\\")",
                                                          "tags": [
                                                            "USER_ERROR",
                                                            "SEVERITY_LOW",
                                                            "BusinessException",
                                                            "UserService"
                                                          ],
                                                          "error_cause": "사용자가 비밀번호 변경을 시도했으나, 입력한 기존 비밀번호가 데이터베이스에 저장된 비밀번호와 일치하지 않아 **BusinessException**이 발생했습니다. 이는 사용자가 잘못된 비밀번호를 입력했거나, 비밀번호 변경 과정에서의 사용자 실수로 인한 것입니다.\\n\\n### 근거 데이터\\n- **에러 메시지**: 기존 비밀번호가 일치하지 않습니다\\n- **발생 시각**: 2025-11-09 02:09:23 UTC\\n- **관련 메서드**: `UserServiceImpl.changePassword()`에서 비밀번호 검증 로직이 실행됨",
                                                          "analysis_type": "SINGLE",
                                                          "target_type": "LOG",
                                                          "analyzed_at": "2025-11-10T04:48:49.551"
                                                        },
                                                        "fromCache": true,
                                                        "similarLogId": null,
                                                        "similarityScore": null
                                                      },
                                                      "timestamp": "2025-11-13T06:52:48.010Z"
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

    @Operation(
            summary = "실시간 로그 스트리밍 (SSE)",
            description = """
                    Server-Sent Events를 통해 실시간으로 로그를 스트리밍합니다.
                    - 5초 간격으로 새로운 로그를 조회하여 클라이언트에 전송합니다.
                    - 새 로그가 없을 때는 heartbeat 이벤트를 전송합니다.
                    - 연결 유지 시간: 1시간
                    - 이벤트 타입:
                      - `log-update`: 새로운 로그 데이터
                      - `heartbeat`: 연결 유지 확인
                    """,
            parameters = {
                    @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Bearer {access_token}", required = true, schema = @Schema(type = "string"))
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "SSE 스트림 연결 성공",
                            content = @Content(
                                    mediaType = "text/event-stream",
                                    examples = @ExampleObject(
                                            name = "SseStreamExample",
                                            summary = "SSE 스트림 예시",
                                            description = "실시간으로 전송되는 로그 데이터 예시입니다.",
                                            value = """
                                                    event: log-update
                                                    data: [{"logId":"abc123xyz789","traceId":"trace-abc-123","logLevel":"ERROR","sourceType":"BE","message":"NullPointerException occurred","timestamp":"2024-01-15T10:30:45.123Z","logger":"com.example.UserService","layer":"Service","comment":null}]
                                                    
                                                    event: heartbeat
                                                    data: No new logs
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
                                    examples = @ExampleObject(
                                            name = "ProjectUuidRequired",
                                            value = """
                                                    {
                                                      "code": "LG400-09",
                                                      "message": "projectUuid는 필수입니다.",
                                                      "status": 400,
                                                      "timestamp": "2025-11-03T15:02:00Z"
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
    SseEmitter streamLogs(@ParameterObject @ModelAttribute LogStreamRequest request);
}
