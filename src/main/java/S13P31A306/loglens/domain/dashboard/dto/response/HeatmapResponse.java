package S13P31A306.loglens.domain.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "히트맵 조회 응답")
public record HeatmapResponse(
        @Schema(description = "프로젝트 ID", example = "12345")
        Integer projectId,

        @Schema(description = "조회 기간")
        Period period,

        @Schema(description = "요일별 히트맵 데이터")
        List<DayData> heatmap,

        @Schema(description = "요약 통계")
        Summary summary,

        @Schema(description = "메타데이터")
        Metadata metadata
) {

    public record Period(
            @Schema(description = "시작 시간", example = "2025-10-10T00:00:00Z")
            String startTime,

            @Schema(description = "종료 시간", example = "2025-10-17T23:59:59Z")
            String endTime
    ) {}

    public record DayData(
            @Schema(description = "요일 (MONDAY~SUNDAY)", example = "MONDAY")
            String dayOfWeek,

            @Schema(description = "요일 한글명", example = "월요일")
            String dayName,

            @Schema(description = "시간대별 데이터 (0~23시)")
            List<HourData> hourlyData,

            @Schema(description = "해당 요일 총 로그 수", example = "28945")
            Integer totalCount
    ) {}

    public record HourData(
            @Schema(description = "시간대 (0~23)", example = "14")
            Integer hour,

            @Schema(description = "해당 시간대 총 로그 수", example = "1234")
            Integer count,

            @Schema(description = "ERROR 로그 수", example = "45")
            Integer errorCount,

            @Schema(description = "WARN 로그 수", example = "120")
            Integer warnCount,

            @Schema(description = "INFO 로그 수", example = "1069")
            Integer infoCount,

            @Schema(description = "강도 (0.0~1.0, 정규화된 값)", example = "0.62")
            Double intensity
    ) {}

    public record Summary(
            @Schema(description = "조회 기간 총 로그 수", example = "185643")
            Integer totalLogs,

            @Schema(description = "로그가 가장 많이 발생한 요일", example = "WEDNESDAY")
            String peakDay,

            @Schema(description = "로그가 가장 많이 발생한 시간", example = "14")
            Integer peakHour,

            @Schema(description = "피크 시간대 로그 수", example = "4567")
            Integer peakCount,

            @Schema(description = "일평균 로그 수", example = "26520")
            Integer avgDailyCount
    ) {}

    public record Metadata(
            @Schema(description = "조회에 사용된 로그 레벨 필터", example = "ALL")
            String logLevel,

            @Schema(description = "타임존", example = "Asia/Seoul")
            String timezone
    ) {}
}
