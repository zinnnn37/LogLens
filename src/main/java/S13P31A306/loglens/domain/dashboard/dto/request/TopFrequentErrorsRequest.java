package S13P31A306.loglens.domain.dashboard.dto.request;

import S13P31A306.loglens.global.annotation.Sensitive;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Range;

public record TopFrequentErrorsRequest(
        @NotBlank(message = "PROJECT_UUID_REQUIRED")
        @Schema(description = "프로젝트 UUID", example = "48d96cd7-bf8d-38f5-891c-9c2f6430d871")
        @Sensitive
        String projectUuid,

        @Schema(description = "조회 시작 시간", example = "2025-11-02T10:30:00Z")
        String startTime,

        @Schema(description = "조회 종료 시간", example = "2025-11-02T10:30:00Z")
        String endTime,

        @Range(min = 1, max = 50, message = "INVALID_LIMIT")
        @Schema(description = "반환할 에러 개수 (1~50)", example = "10", defaultValue = "10")
        Integer limit
) {

    public TopFrequentErrorsRequest {
        limit = limit != null ? limit : 10;
    }

}
