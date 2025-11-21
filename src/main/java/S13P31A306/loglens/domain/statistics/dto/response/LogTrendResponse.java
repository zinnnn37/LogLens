package S13P31A306.loglens.domain.statistics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 로그 발생 추이 조회 응답
 */
@Schema(description = "로그 발생 추이 조회 응답")
public record LogTrendResponse(

        @Schema(description = "프로젝트 UUID", example = "3a73c7d4-8176-3929-b72f-d5b921daae67")
        String projectUuid,

        @Schema(description = "조회 기간")
        Period period,

        @Schema(description = "시간 간격", example = "3h")
        String interval,

        @Schema(description = "시계열 데이터 포인트 (8개)")
        List<DataPoint> dataPoints,

        @Schema(description = "요약 통계")
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
    ) {}

    /**
     * 데이터 포인트 (3시간 단위)
     */
    @Schema(description = "데이터 포인트 (3시간 단위)")
    public record DataPoint(
            @Schema(description = "타임스탬프 (ISO 8601)", example = "2025-11-13T15:00:00+09:00")
            String timestamp,

            @Schema(description = "시각 (HH:mm)", example = "15:00")
            String hour,

            @Schema(description = "전체 로그 수", example = "1523")
            Integer totalCount,

            @Schema(description = "INFO 로그 수", example = "1200")
            Integer infoCount,

            @Schema(description = "WARN 로그 수", example = "250")
            Integer warnCount,

            @Schema(description = "ERROR 로그 수", example = "73")
            Integer errorCount
    ) {}

    /**
     * 요약 통계
     */
    @Schema(description = "요약 통계")
    public record Summary(
            @Schema(description = "전체 로그 수 (24시간)", example = "12000")
            Integer totalLogs,

            @Schema(description = "시간대별 평균 로그 수", example = "1500")
            Integer avgLogsPerInterval,

            @Schema(description = "피크 시간 (HH:mm)", example = "12:00")
            String peakHour,

            @Schema(description = "피크 시간의 로그 수", example = "2100")
            Integer peakCount
    ) {}
}
