package S13P31A306.loglens.domain.statistics.mapper;

import S13P31A306.loglens.domain.statistics.dto.internal.LogTrendAggregation;
import S13P31A306.loglens.domain.statistics.dto.response.LogTrendResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        // 전체 시간 슬롯 생성 (24시간 / 3시간 = 8개)
        List<LocalDateTime> timeSlots = generateTimeSlots(startTime, INTERVAL_HOURS, TREND_HOURS / INTERVAL_HOURS);

        // OpenSearch 결과를 Map으로 변환 (timestamp를 시간 단위로 truncate하여 매칭)
        Map<LocalDateTime, LogTrendAggregation> aggMap = aggregations.stream()
                .collect(Collectors.toMap(
                        agg -> agg.timestamp().truncatedTo(ChronoUnit.HOURS),
                        agg -> agg,
                        (existing, replacement) -> existing  // 중복 시 첫 번째 값 사용
                ));

        // 각 시간 슬롯에 대해 데이터 있으면 사용, 없으면 0으로 채움
        List<LogTrendResponse.DataPoint> dataPoints = timeSlots.stream()
                .map(ts -> aggMap.getOrDefault(ts.truncatedTo(ChronoUnit.HOURS), createEmptyAggregation(ts)))
                .map(this::toDataPoint)
                .toList();

        // Period 생성
        LogTrendResponse.Period period = new LogTrendResponse.Period(
                formatTimestamp(startTime),
                formatTimestamp(endTime)
        );

        // Summary 생성 (실제 aggregations 기반)
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
     * 시간 슬롯 생성
     *
     * @param startTime 시작 시간
     * @param intervalHours 간격 (시간)
     * @param count 생성할 슬롯 개수
     * @return 시간 슬롯 리스트
     */
    default List<LocalDateTime> generateTimeSlots(LocalDateTime startTime, int intervalHours, int count) {
        List<LocalDateTime> timeSlots = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            timeSlots.add(startTime.plusHours((long) i * intervalHours));
        }
        return timeSlots;
    }

    /**
     * 빈 집계 결과 생성
     *
     * @param timestamp 타임스탬프
     * @return 0으로 채워진 LogTrendAggregation
     */
    default LogTrendAggregation createEmptyAggregation(LocalDateTime timestamp) {
        return new LogTrendAggregation(timestamp, 0, 0, 0, 0);
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
