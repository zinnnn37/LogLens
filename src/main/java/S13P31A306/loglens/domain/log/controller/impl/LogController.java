package S13P31A306.loglens.domain.log.controller.impl;

import S13P31A306.loglens.domain.log.constants.LogSuccessCode;
import S13P31A306.loglens.domain.log.controller.LogApi;
import S13P31A306.loglens.domain.log.dto.request.LogSearchRequest;
import S13P31A306.loglens.domain.log.dto.request.LogStreamRequest;
import S13P31A306.loglens.domain.log.dto.response.LogDetailResponse;
import S13P31A306.loglens.domain.log.dto.response.LogPageResponse;
import S13P31A306.loglens.domain.log.dto.response.TraceLogResponse;
import S13P31A306.loglens.domain.log.service.LogService;
import S13P31A306.loglens.domain.log.validator.LogValidator;
import S13P31A306.loglens.global.dto.response.ApiResponseFactory;
import S13P31A306.loglens.global.dto.response.BaseResponse;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController implements LogApi {

    private final LogService logService;
    private final LogValidator logValidator;

    @Override
    @GetMapping
    public ResponseEntity<? extends BaseResponse> getLogs(@ModelAttribute LogSearchRequest request) {
        logValidator.validate(request);

        if (!Objects.isNull(request.getTraceId()) && !request.getTraceId().trim().isEmpty()) {
            TraceLogResponse response = logService.getLogsByTraceId(request);
            return ApiResponseFactory.success(LogSuccessCode.TRACE_LOGS_READ_SUCCESS, response);
        } else {
            LogPageResponse response = logService.getLogs(request);
            return ApiResponseFactory.success(LogSuccessCode.LOGS_READ_SUCCESS, response);
        }
    }

    @Override
    @GetMapping("/{logId}")
    public ResponseEntity<? extends BaseResponse> getLogDetail(
            @PathVariable @NotNull Long logId,
            @RequestParam @NotNull String projectUuid
    ) {
        LogDetailResponse response = logService.getLogDetail(logId, projectUuid);
        return ApiResponseFactory.success(LogSuccessCode.LOG_DETAIL_READ_SUCCESS, response);
    }

    @Override
    @GetMapping("/stream")
    public SseEmitter streamLogs(@ModelAttribute LogStreamRequest request) {
        LogSearchRequest searchRequest = request.toLogSearchRequest();
        logValidator.validate(searchRequest);
        return logService.streamLogs(searchRequest);
    }
}
