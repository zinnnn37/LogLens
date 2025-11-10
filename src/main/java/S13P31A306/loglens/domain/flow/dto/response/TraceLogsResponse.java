package S13P31A306.loglens.domain.flow.dto.response;

import S13P31A306.loglens.domain.log.dto.response.LogResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Trace 로그 목록 응답")
public record TraceLogsResponse(
        @Schema(description = "추적 ID", example = "e78e7203-b81c-43c5-9611-571163183411")
        String traceId,

        @Schema(description = "프로젝트 UUID", example = "9f8c4c75-a936-3ab6-92a5-d1309cd9f87e")
        String projectUuid,

        @Schema(description = "요청 로그 (가장 빠른 timestamp)")
        LogResponse request,

        @Schema(description = "응답 로그 (가장 느린 timestamp)")
        LogResponse response,

        @Schema(description = "총 소요 시간 (ms)", example = "1333")
        Long duration,

        @Schema(description = "전체 상태", example = "SUCCESS", allowableValues = {"SUCCESS", "ERROR"})
        String status,

        @Schema(description = "시간순 정렬된 모든 로그")
        List<LogResponse> logs
) {
}
