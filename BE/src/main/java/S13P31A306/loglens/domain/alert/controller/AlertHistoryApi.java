package S13P31A306.loglens.domain.alert.controller;

import S13P31A306.loglens.domain.alert.dto.AlertHistoryResponse;
import S13P31A306.loglens.global.config.swagger.annotation.ApiInternalServerError;
import S13P31A306.loglens.global.config.swagger.annotation.ApiUnauthorizedError;
import S13P31A306.loglens.global.constants.SwaggerMessages;
import S13P31A306.loglens.global.dto.response.BaseResponse;
import S13P31A306.loglens.global.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 알림 이력 API 인터페이스
 */
@ApiInternalServerError
@ApiUnauthorizedError
@Tag(name = "Alert History API", description = "알림 이력 관련 API")
public interface AlertHistoryApi {

    @Operation(
            summary = "알림 이력 조회",
            description = "프로젝트의 알림 이력을 조회합니다. resolvedYN 파라미터로 읽음 여부를 필터링할 수 있습니다.",
            security = @SecurityRequirement(name = SwaggerMessages.BEARER_AUTH),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "알림 이력 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(
                                                    name = "AlertHistoriesSuccess",
                                                    summary = "알림 이력 조회 성공 (데이터 있음)",
                                                    value = """
                                                            {
                                                              "code": "AL200-3",
                                                              "message": "알림 이력을 성공적으로 조회했습니다.",
                                                              "status": 200,
                                                              "data": [
                                                                {
                                                                  "id": 1,
                                                                  "alertMessage": "에러 발생 건수가 임계값(10건)을 초과했습니다.",
                                                                  "alertTime": "2025-11-12T13:25:00",
                                                                  "resolvedYN": "N",
                                                                  "logReference": "{\\"logId\\": 12345, \\"traceId\\": \\"abc-123\\", \\"errorCount\\": 15}",
                                                                  "projectUuid": "9911573f-8a1d-3b96-98b4-5a0def93513b"
                                                                },
                                                                {
                                                                  "id": 2,
                                                                  "alertMessage": "응답 시간이 임계값(100ms)을 초과했습니다.",
                                                                  "alertTime": "2025-11-12T14:30:00",
                                                                  "resolvedYN": "Y",
                                                                  "logReference": "{\\"logId\\": 12346, \\"traceId\\": \\"def-456\\", \\"latency\\": 150}",
                                                                  "projectUuid": "9911573f-8a1d-3b96-98b4-5a0def93513b"
                                                                }
                                                              ],
                                                              "timestamp": "2025-11-13T10:30:00Z"
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "AlertHistoriesEmpty",
                                                    summary = "알림 이력 조회 성공 (데이터 없음)",
                                                    value = """
                                                            {
                                                              "code": "AL200-3",
                                                              "message": "알림 이력을 성공적으로 조회했습니다.",
                                                              "status": 200,
                                                              "data": [],
                                                              "timestamp": "2025-11-13T10:30:00Z"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 (프로젝트 UUID 누락 또는 잘못된 resolvedYN 값)",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "InvalidResolvedYN",
                                            summary = "잘못된 resolvedYN 파라미터",
                                            value = """
                                                    {
                                                      "code": "G400",
                                                      "message": "입력값이 유효하지 않습니다.",
                                                      "status": 400,
                                                      "timestamp": "2025-11-13T10:30:00Z"
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
                                            name = "ProjectAccessDenied",
                                            summary = "프로젝트 접근 권한 없음",
                                            value = """
                                                    {
                                                      "code": "PJ403",
                                                      "message": "해당 프로젝트에 접근 권한이 없습니다.",
                                                      "status": 403,
                                                      "timestamp": "2025-11-13T10:30:00Z"
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
                                            summary = "프로젝트 미존재",
                                            value = """
                                                    {
                                                      "code": "PJ404",
                                                      "message": "프로젝트를 찾을 수 없습니다.",
                                                      "status": 404,
                                                      "timestamp": "2025-11-13T10:30:00Z"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<? extends BaseResponse> getAlertHistories(
            @Parameter(description = "프로젝트 UUID", required = true, example = "9911573f-8a1d-3b96-98b4-5a0def93513b")
            @RequestParam String projectUuid,
            @Parameter(description = "읽음 여부 필터 (Y: 읽음, N: 읽지 않음, 미입력: 전체)", example = "N")
            @RequestParam(required = false) String resolvedYN);

    @Operation(
            summary = "알림 읽음 처리",
            description = "알림을 읽음 상태로 변경합니다. 이미 읽은 알림도 재요청 가능합니다 (멱등성).",
            security = @SecurityRequirement(name = SwaggerMessages.BEARER_AUTH),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "알림 읽음 처리 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "MarkAsReadSuccess",
                                            summary = "알림 읽음 처리 성공",
                                            value = """
                                                    {
                                                      "code": "AL200-4",
                                                      "message": "알림을 읽음 처리했습니다.",
                                                      "status": 200,
                                                      "data": null,
                                                      "timestamp": "2025-11-13T10:30:00Z"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "알림 접근 권한 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "AlertAccessDenied",
                                            summary = "알림 접근 권한 없음",
                                            value = """
                                                    {
                                                      "code": "AL403",
                                                      "message": "해당 알림에 접근 권한이 없습니다.",
                                                      "status": 403,
                                                      "timestamp": "2025-11-13T10:30:00Z"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "알림 이력을 찾을 수 없음",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "AlertHistoryNotFound",
                                            summary = "알림 이력 미존재",
                                            value = """
                                                    {
                                                      "code": "AL404-1",
                                                      "message": "알림 이력을 찾을 수 없습니다.",
                                                      "status": 404,
                                                      "timestamp": "2025-11-13T10:30:00Z"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<? extends BaseResponse> markAsRead(
            @Parameter(description = "알림 이력 ID", required = true, example = "1")
            @PathVariable Integer alertId);

    @Operation(
            summary = "읽지 않은 알림 개수 조회",
            description = "프로젝트의 읽지 않은 알림 개수를 조회합니다.",
            security = @SecurityRequirement(name = SwaggerMessages.BEARER_AUTH),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "읽지 않은 알림 개수 조회 성공",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            name = "UnreadCountSuccess",
                                            summary = "읽지 않은 알림 개수 조회 성공",
                                            value = """
                                                    {
                                                      "code": "AL200-5",
                                                      "message": "읽지 않은 알림 개수를 성공적으로 조회했습니다.",
                                                      "status": 200,
                                                      "data": {
                                                        "unreadCount": 5
                                                      },
                                                      "timestamp": "2025-11-13T10:30:00Z"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "프로젝트 UUID 누락",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "ProjectUuidMissing",
                                            summary = "프로젝트 UUID 누락",
                                            value = """
                                                    {
                                                      "code": "G400",
                                                      "message": "입력값이 유효하지 않습니다.",
                                                      "status": 400,
                                                      "timestamp": "2025-11-13T10:30:00Z"
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
                                            name = "ProjectAccessDenied",
                                            summary = "프로젝트 접근 권한 없음",
                                            value = """
                                                    {
                                                      "code": "PJ403",
                                                      "message": "해당 프로젝트에 접근 권한이 없습니다.",
                                                      "status": 403,
                                                      "timestamp": "2025-11-13T10:30:00Z"
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
                                            summary = "프로젝트 미존재",
                                            value = """
                                                    {
                                                      "code": "PJ404",
                                                      "message": "프로젝트를 찾을 수 없습니다.",
                                                      "status": 404,
                                                      "timestamp": "2025-11-13T10:30:00Z"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<? extends BaseResponse> getUnreadCount(
            @Parameter(description = "프로젝트 UUID", required = true, example = "9911573f-8a1d-3b96-98b4-5a0def93513b")
            @RequestParam String projectUuid);

    @Operation(
            summary = "실시간 알림 스트리밍",
            description = "SSE를 통해 프로젝트의 새로운 알림을 실시간으로 받습니다. 5초 간격으로 새 알림을 확인하고 전송합니다.",
            security = @SecurityRequirement(name = SwaggerMessages.BEARER_AUTH),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "SSE 연결 성공 (스트리밍 시작)",
                            content = @Content(
                                    mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                                    examples = @ExampleObject(
                                            name = "SSEStreamExample",
                                            summary = "SSE 스트리밍 예시",
                                            value = """
                                                    data: {"id":1,"alertMessage":"에러 발생 건수가 임계값(10건)을 초과했습니다.","alertTime":"2025-11-13T10:30:00","resolvedYN":"N","logReference":"{\\"logId\\": 12345}","projectUuid":"9911573f-8a1d-3b96-98b4-5a0def93513b"}

                                                    data: {"id":2,"alertMessage":"응답 시간이 임계값(100ms)을 초과했습니다.","alertTime":"2025-11-13T10:35:00","resolvedYN":"N","logReference":"{\\"logId\\": 12346}","projectUuid":"9911573f-8a1d-3b96-98b4-5a0def93513b"}
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "프로젝트 UUID 누락",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(
                                            name = "ProjectUuidMissing",
                                            summary = "프로젝트 UUID 누락",
                                            value = """
                                                    {
                                                      "code": "G400",
                                                      "message": "입력값이 유효하지 않습니다.",
                                                      "status": 400,
                                                      "timestamp": "2025-11-13T10:30:00Z"
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
                                            name = "ProjectAccessDenied",
                                            summary = "프로젝트 접근 권한 없음",
                                            value = """
                                                    {
                                                      "code": "PJ403",
                                                      "message": "해당 프로젝트에 접근 권한이 없습니다.",
                                                      "status": 403,
                                                      "timestamp": "2025-11-13T10:30:00Z"
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
                                            summary = "프로젝트 미존재",
                                            value = """
                                                    {
                                                      "code": "PJ404",
                                                      "message": "프로젝트를 찾을 수 없습니다.",
                                                      "status": 404,
                                                      "timestamp": "2025-11-13T10:30:00Z"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter streamAlerts(
            @Parameter(description = "프로젝트 UUID", required = true, example = "9911573f-8a1d-3b96-98b4-5a0def93513b")
            @RequestParam String projectUuid);
}
