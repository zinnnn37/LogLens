package S13P31A306.loglens.domain.flow.controller.impl;

import S13P31A306.loglens.domain.flow.constants.FlowSuccessCode;
import S13P31A306.loglens.domain.flow.controller.FlowApi;
import S13P31A306.loglens.domain.flow.dto.response.TraceLogsResponse;
import S13P31A306.loglens.domain.flow.service.FlowService;
import S13P31A306.loglens.global.annotation.ValidUuid;
import S13P31A306.loglens.global.dto.response.ApiResponseFactory;
import S13P31A306.loglens.global.dto.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/traces")
@RequiredArgsConstructor
public class FlowController implements FlowApi {

    private final FlowService flowService;

    @Override
    @GetMapping("/{traceId}/logs")
    public ResponseEntity<? extends BaseResponse> getTraceLogs(
            @PathVariable String traceId,
            @ValidUuid @RequestParam String projectUuid
    ) {
        TraceLogsResponse response = flowService.getTraceLogsById(traceId, projectUuid);
        return ApiResponseFactory.success(FlowSuccessCode.TRACE_LOGS_RETRIEVED, response);
    }
}
