package S13P31A306.loglens.domain.statistics.mapper;

import S13P31A306.loglens.domain.statistics.dto.internal.LogTrendAggregation;
import S13P31A306.loglens.domain.statistics.dto.response.LogTrendResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import static S13P31A306.loglens.domain.statistics.constants.StatisticsConstants.*;

/**
 * 로그 추이 매핑 인터페이스
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface LogTrendMapper {

    /**
     * LogTrendAggregation을 DataPoint로 변환
     *
     * @param aggregation OpenSearch 집계 결과
     * @return DataPoint
     */
    @Mapping(target = "timestamp", expression = "java(formatTimestamp(aggregation.timestamp()))")
    @Mapping(target = "hour", expression = "java(formatHour(aggregation.timestamp()))")
    LogTrendResponse.DataPoint toDataPoint(LogTrendAggregation aggregation);

    /**
     * 타임스탬프를 ISO 8601 형식으로 변환
     *
     * @param timestamp LocalDateTime
     * @return ISO 8601 형식 문자열
     */
    default String formatTimestamp(LocalDateTime timestamp) {
        return timestamp.atZone(ZoneId.of(DEFAULT_TIMEZONE))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /**
     * 시간을 HH:mm 형식으로 변환
     *
     * @param timestamp LocalDateTime
     * @return HH:mm 형식 문자열
     */
    default String formatHour(LocalDateTime timestamp) {
        return timestamp.format(DateTimeFormatter.ofPattern(TIME_FORMAT));
    }

    /**
     * 전체 응답 생성
     *
     * @param projectUuid 프로젝트 UUID
     * @param startTime   시작 시간
     * @param endTime     종료 시간
     * @param aggregations OpenSearch 집계 결과 리스트
     * @return LogTrendResponse
     */
    default LogTrendResponse toLogTrendResponse(
            String projectUuid,
            LocalDateTime startTime,
            LocalDateTime endTime,
            List<LogTrendAggregation> aggregations
    ) {
        // DataPoint 변환
        List<LogTrendResponse.DataPoint> dataPoints = aggregations.stream()
                .map(this::toDataPoint)
                .toList();

        // Period 생성
        LogTrendResponse.Period period = new LogTrendResponse.Period(
                formatTimestamp(startTime),
                formatTimestamp(endTime)
        );

        // Summary 생성
        LogTrendResponse.Summary summary = buildSummary(aggregations);

        return new LogTrendResponse(
                projectUuid,
                period,
                INTERVAL_HOURS + "h",
                dataPoints,
                summary
        );
    }

    /**
     * Summary 통계 계산
     *
     * @param aggregations OpenSearch 집계 결과 리스트
     * @return Summary
     */
    default LogTrendResponse.Summary buildSummary(List<LogTrendAggregation> aggregations) {
        // 총 로그 수
        int totalLogs = aggregations.stream()
                .mapToInt(LogTrendAggregation::totalCount)
                .sum();

        // 평균 로그 수
        int avgLogsPerInterval = aggregations.isEmpty() ? 0 : totalLogs / aggregations.size();

        // 피크 시간 찾기
        LogTrendAggregation peak = aggregations.stream()
                .max(Comparator.comparing(LogTrendAggregation::totalCount))
                .orElse(new LogTrendAggregation(LocalDateTime.now(), 0, 0, 0, 0));

        String peakHour = formatHour(peak.timestamp());
        int peakCount = peak.totalCount();

        return new LogTrendResponse.Summary(
                totalLogs,
                avgLogsPerInterval,
                peakHour,
                peakCount
        );
    }
}
