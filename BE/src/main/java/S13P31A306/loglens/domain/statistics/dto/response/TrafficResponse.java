package S13P31A306.loglens.domain.statistics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Traffic 그래프 응답 DTO
 */
@Schema(description = "Traffic 그래프 응답")
public record TrafficResponse(
        @Schema(description = "프로젝트 UUID", example = "3a73c7d4-8176-3929-b72f-d5b921daae67")
        String projectUuid,

        @Schema(description = "조회 기간")
        Period period,

        @Schema(description = "시간 간격", example = "3h")
        String interval,

        @Schema(description = "시간대별 데이터 포인트 (8개)")
        List<DataPoint> dataPoints,

        @Schema(description = "전체 요약 통계")
        Summary summary
) {
    /**
     * 조회 기간
     */
    @Schema(description = "조회 기간")
    public record Period(
            @Schema(description = "시작 시간", example = "2025-11-13T15:00:00+09:00")
            String startTime,

            @Schema(description = "종료 시간", example = "2025-11-14T15:00:00+09:00")
            String endTime
    ) {
    }

    /**
     * 시간대별 데이터 포인트
     */
    @Schema(description = "시간대별 데이터 포인트")
    public record DataPoint(
            @Schema(description = "타임스탬프", example = "2025-11-13T15:00:00+09:00")
            String timestamp,

            @Schema(description = "시각 (HH:mm 형식)", example = "15:00")
            String hour,

            @Schema(description = "전체 로그 수", example = "1500")
            Integer totalCount,

            @Schema(description = "FE 로그 수", example = "800")
            Integer feCount,

            @Schema(description = "BE 로그 수", example = "700")
            Integer beCount
    ) {
    }

    /**
     * 전체 요약 통계
     */
    @Schema(description = "전체 요약 통계")
    public record Summary(
            @Schema(description = "전체 로그 수", example = "12000")
            Integer totalLogs,

            @Schema(description = "전체 FE 로그 수", example = "6400")
            Integer totalFeCount,

            @Schema(description = "전체 BE 로그 수", example = "5600")
            Integer totalBeCount,

            @Schema(description = "시간 간격당 평균 로그 수", example = "1500")
            Integer avgLogsPerInterval,

            @Schema(description = "피크 시간대", example = "12:00")
            String peakHour,

            @Schema(description = "피크 시간대 로그 수", example = "2650")
            Integer peakCount
    ) {
    }
}
