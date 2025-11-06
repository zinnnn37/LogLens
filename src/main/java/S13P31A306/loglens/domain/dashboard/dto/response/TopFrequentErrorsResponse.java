package S13P31A306.loglens.domain.dashboard.dto.response;

import S13P31A306.loglens.global.annotation.Sensitive;
import io.swagger.v3.oas.annotations.media.Schema;

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

    public record ErrorInfo(

    ) {
    }

    public record ErrorSummary(

    ) {
    }

}
