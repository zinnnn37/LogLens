package S13P31A306.loglens.domain.alert.controller.impl;

import static S13P31A306.loglens.domain.alert.constants.AlertSuccessCode.ALERT_CONFIG_CREATED;
import static S13P31A306.loglens.domain.alert.constants.AlertSuccessCode.ALERT_CONFIG_RETRIEVED;
import static S13P31A306.loglens.domain.alert.constants.AlertSuccessCode.ALERT_CONFIG_UPDATED;

import S13P31A306.loglens.domain.alert.controller.AlertConfigApi;
import S13P31A306.loglens.domain.alert.dto.AlertConfigCreateRequest;
import S13P31A306.loglens.domain.alert.dto.AlertConfigResponse;
import S13P31A306.loglens.domain.alert.dto.AlertConfigUpdateRequest;
import S13P31A306.loglens.domain.alert.service.AlertConfigService;
import S13P31A306.loglens.domain.auth.util.AuthenticationHelper;
import S13P31A306.loglens.global.dto.response.ApiResponseFactory;
import S13P31A306.loglens.global.dto.response.BaseResponse;
import a306.dependency_logger_starter.logging.annotation.NoLogging;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 알림 설정 컨트롤러
 */
@RestController
@RequestMapping("/api/alerts/config")
@RequiredArgsConstructor
@Validated
public class AlertConfigController implements AlertConfigApi {

    private final AlertConfigService alertConfigService;
    private final AuthenticationHelper authHelper;

    @PostMapping
    @Override
    public ResponseEntity<? extends BaseResponse> createAlertConfig(
            @Valid @RequestBody AlertConfigCreateRequest request) {
        Integer userId = authHelper.getCurrentUserId();
        AlertConfigResponse response = alertConfigService.createAlertConfig(request, userId);
        return ApiResponseFactory.success(ALERT_CONFIG_CREATED, response);
    }

    @GetMapping
    @NoLogging
    @Override
    public ResponseEntity<? extends BaseResponse> getAlertConfig(
            @RequestParam String projectUuid) {
        Integer userId = authHelper.getCurrentUserId();
        AlertConfigResponse response = alertConfigService.getAlertConfig(projectUuid, userId);
        return ApiResponseFactory.success(ALERT_CONFIG_RETRIEVED, response);
    }

    @PutMapping
    @Override
    public ResponseEntity<? extends BaseResponse> updateAlertConfig(
            @Valid @RequestBody AlertConfigUpdateRequest request) {
        Integer userId = authHelper.getCurrentUserId();
        AlertConfigResponse response = alertConfigService.updateAlertConfig(request, userId);
        return ApiResponseFactory.success(ALERT_CONFIG_UPDATED, response);
    }
}

