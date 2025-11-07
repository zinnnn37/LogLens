package S13P31A306.loglens.domain.dashboard.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record TopFrequentErrorsRequest(
        @Schema(description = "프로젝트 UUID", example = "48d96cd7-bf8d-38f5-891c-9c2f6430d871")
        String projectUuid,

        @Schema(description = "조회 시작 시간", example = "2025-11-02T10:30:00Z")
        String startTime,

        @Schema(description = "조회 종료 시간", example = "2025-11-02T10:30:00Z")
        String endTime,

        @Schema(description = "반환할 에러 개수 (1~50)", example = "10", defaultValue = "10")
        Integer limit
) {

    public TopFrequentErrorsRequest {
        limit = limit != null ? limit : 10;
    }

}
