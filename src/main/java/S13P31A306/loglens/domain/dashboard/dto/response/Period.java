package S13P31A306.loglens.domain.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record Period(
        @Schema(description = "조회 시작 시간", example = "2025-10-10T00:00:00Z")
        LocalDateTime startTime,

        @Schema(description = "조회 종료 시간", example = "2025-10-17T23:59:59Z")
        LocalDateTime endTime
) {
}