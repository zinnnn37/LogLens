package S13P31A306.loglens.domain.dashboard.dto.response;

import S13P31A306.loglens.global.annotation.Sensitive;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "자주 발생하는 에러 top N 응답")
public record TopFrequentErrorsResponse(
        @Sensitive
        @Schema(description = "프로젝트 UUID", example = "48d96cd7-bf8d-38f5-891c-9c2f6430d871")
        String projectUuid,

        @Schema(description = "조회 기간")
        Period period,
        
        @Schema(description = "에러 목록")
        List<ErrorInfo> errors,

        @Schema(description = "에러 통계 요약")
        ErrorSummary summary
) {

    public record Period(
            @Schema(description = "조회 시작 시간", example = "2025-10-10T00:00:00Z")
            LocalDateTime startTime,

            @Schema(description = "조회 종료 시간", example = "2025-10-17T23:59:59Z")
            LocalDateTime endTime
    ) {
    }

    public record ErrorInfo(

    ) {
    }

    public record ErrorSummary(

    ) {
    }

}
