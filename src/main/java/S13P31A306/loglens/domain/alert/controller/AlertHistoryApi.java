package S13P31A306.loglens.domain.alert.controller;

import S13P31A306.loglens.domain.alert.dto.AlertHistoryResponse;
import S13P31A306.loglens.global.config.swagger.annotation.ApiInternalServerError;
import S13P31A306.loglens.global.config.swagger.annotation.ApiUnauthorizedError;
import S13P31A306.loglens.global.constants.SwaggerMessages;
import S13P31A306.loglens.global.dto.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
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
            security = @SecurityRequirement(name = SwaggerMessages.BEARER_AUTH)
    )
    ResponseEntity<? extends BaseResponse> getAlertHistories(
            @RequestParam String projectUuid,
            @RequestParam(required = false) String resolvedYN);

    @Operation(
            summary = "알림 읽음 처리",
            description = "알림을 읽음 상태로 변경합니다. 이미 읽은 알림도 재요청 가능합니다 (멱등성).",
            security = @SecurityRequirement(name = SwaggerMessages.BEARER_AUTH)
    )
    ResponseEntity<? extends BaseResponse> markAsRead(
            @PathVariable Integer alertId);

    @Operation(
            summary = "읽지 않은 알림 개수 조회",
            description = "프로젝트의 읽지 않은 알림 개수를 조회합니다.",
            security = @SecurityRequirement(name = SwaggerMessages.BEARER_AUTH)
    )
    ResponseEntity<? extends BaseResponse> getUnreadCount(
            @RequestParam String projectUuid);

    @Operation(
            summary = "실시간 알림 스트리밍",
            description = "SSE를 통해 프로젝트의 새로운 알림을 실시간으로 받습니다. 5초 간격으로 새 알림을 확인하고 전송합니다.",
            security = @SecurityRequirement(name = SwaggerMessages.BEARER_AUTH)
    )
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter streamAlerts(
            @RequestParam String projectUuid);
}
