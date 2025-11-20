package S13P31A306.loglens.domain.flow.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "흐름 요약 정보")
public record FlowSummary(
        @Schema(description = "총 소요 시간 (ms)", example = "1333")
        Long totalDuration,

        @Schema(description = "전체 상태", example = "SUCCESS", allowableValues = {"SUCCESS", "ERROR"})
        String status,

        @Schema(description = "시작 시간")
        LocalDateTime startTime,

        @Schema(description = "종료 시간")
        LocalDateTime endTime
) {
}
