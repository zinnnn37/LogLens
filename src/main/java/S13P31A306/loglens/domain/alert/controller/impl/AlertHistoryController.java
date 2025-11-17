package S13P31A306.loglens.domain.alert.controller.impl;

import S13P31A306.loglens.domain.alert.controller.AlertHistoryApi;
import S13P31A306.loglens.domain.alert.dto.AlertHistoryResponse;
import S13P31A306.loglens.domain.alert.service.AlertHistoryService;
import S13P31A306.loglens.domain.auth.util.AuthenticationHelper;
import S13P31A306.loglens.global.dto.response.ApiResponseFactory;
import S13P31A306.loglens.global.dto.response.BaseResponse;
import a306.dependency_logger_starter.logging.annotation.NoLogging;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

import static S13P31A306.loglens.domain.alert.constants.AlertSuccessCode.*;

/**
 * 알림 이력 컨트롤러
 */
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Validated
public class AlertHistoryController implements AlertHistoryApi {

    private final AlertHistoryService alertHistoryService;
    private final AuthenticationHelper authHelper;

    @GetMapping("/histories")
    @Override
    public ResponseEntity<? extends BaseResponse> getAlertHistories(
            @RequestParam String projectUuid,
            @RequestParam(required = false) String resolvedYN) {
        Integer userId = authHelper.getCurrentUserId();
        List<AlertHistoryResponse> responses = alertHistoryService
                .getAlertHistories(projectUuid, userId, resolvedYN);
        return ApiResponseFactory.success(ALERT_HISTORIES_RETRIEVED, responses);
    }

    @PatchMapping("/{alertId}/read")
    @Override
    public ResponseEntity<? extends BaseResponse> markAsRead(
            @PathVariable Integer alertId) {
        Integer userId = authHelper.getCurrentUserId();
        AlertHistoryResponse response = alertHistoryService.markAsRead(alertId, userId);
        return ApiResponseFactory.success(ALERT_MARKED_AS_READ, response);
    }

    @GetMapping("/unread-count")
    @Override
    public ResponseEntity<? extends BaseResponse> getUnreadCount(
            @RequestParam String projectUuid) {
        Integer userId = authHelper.getCurrentUserId();
        long count = alertHistoryService.getUnreadCount(projectUuid, userId);
        return ApiResponseFactory.success(ALERT_UNREAD_COUNT_RETRIEVED, Map.of("unreadCount", count));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @NoLogging
    @Override
    public SseEmitter streamAlerts(@RequestParam String projectUuid) {
        return alertHistoryService.streamAlerts(projectUuid);
    }
}
