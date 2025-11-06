package S13P31A306.loglens.domain.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "대시보드 통계 데이터 응답")
public record DashboardOverviewResponse(
        @Schema(description = "프로젝트 UUID", example = "pk_1a2b3c4d5e6f")
        String projectUuid,

        @Schema(description = "필터링 시간대")
        Period period,

        @Schema(description = "로그 개요")
        Summary summary
) {

    public record Period(
        @Schema(description = "필터링 시작 시간", example = "2025-10-29T10:30:00")
        String startTime,

        @Schema(description = "필터링 종료 시간", example = "2025-10-29T10:30:00")
        String endTime
    ) {
    }

    public record Summary(
        @Schema(description = "전체 로그 갯수", example = "24500")
        int totalLogs,

        @Schema(description = "에러 로그 갯수", example = "5000")
        int errorCount,

        @Schema(description = "경고 로그 갯수", example = "12300")
        int warnCount,

        @Schema(description = "정보 로그 갯수", example = "7200")
        int infoCount,

        @Schema(description = "평균 응답 시간", example = "245")
        int avgResponseTime
    ) {
    }

}
