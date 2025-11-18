package S13P31A306.loglens.domain.statistics.mapper;

import static S13P31A306.loglens.domain.statistics.constants.StatisticsConstants.DEFAULT_TIMEZONE;
import static S13P31A306.loglens.domain.statistics.constants.StatisticsConstants.INTERVAL_HOURS;
import static S13P31A306.loglens.domain.statistics.constants.StatisticsConstants.TIME_FORMAT;
import static S13P31A306.loglens.domain.statistics.constants.StatisticsConstants.TREND_HOURS;

import S13P31A306.loglens.domain.statistics.dto.internal.LogTrendAggregation;
import S13P31A306.loglens.domain.statistics.dto.response.LogTrendResponse;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

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

    default LogTrendResponse toLogTrendResponse(
            String projectUuid,
            LocalDateTime startKst,
            LocalDateTime endKst,
            List<LogTrendAggregation> aggregations
    ) {
        ZoneId KST = ZoneId.of("Asia/Seoul");

        // KST 기준으로 슬롯 생성
        List<LocalDateTime> timeSlots = generateTimeSlots(startKst, INTERVAL_HOURS, TREND_HOURS / INTERVAL_HOURS);

        // Repo에서 내려온 timestamp(UTC)를 반드시 KST로 변환 후 매칭
        Map<LocalDateTime, LogTrendAggregation> aggMap = aggregations.stream()
                .collect(Collectors.toMap(
                        agg -> agg.timestamp()
                                .atOffset(ZoneOffset.UTC)
                                .atZoneSameInstant(KST)
                                .toLocalDateTime()
                                .truncatedTo(ChronoUnit.HOURS),
                        agg -> agg,
                        (existing, replacement) -> existing
                ));

        // 출력 DataPoint도 KST 기준으로 생성
        List<LogTrendResponse.DataPoint> dataPoints = timeSlots.stream()
                .map(slot -> {
                    LogTrendAggregation agg = aggMap.get(slot);
                    if (agg == null) {
                        return toDataPoint(createEmptyAggregation(slot));
                    }

                    // 로그의 timestamp(UTC)를 slot(KST)로 변환해서 DataPoint 생성
                    LocalDateTime timestampKst = agg.timestamp()
                            .atOffset(ZoneOffset.UTC)
                            .atZoneSameInstant(KST)
                            .toLocalDateTime();

                    return new LogTrendResponse.DataPoint(
                            timestampKst.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),   // ✔ String 변환
                            timestampKst.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                            agg.totalCount(),
                            agg.infoCount(),
                            agg.warnCount(),
                            agg.errorCount()
                    );
                })
                .toList();

        return new LogTrendResponse(
                projectUuid,
                new LogTrendResponse.Period(formatTimestamp(startKst), formatTimestamp(endKst)),
                INTERVAL_HOURS + "h",
                dataPoints,
                buildSummary(aggregations)
        );
    }

    /**
     * 시간 슬롯 생성
     *
     * @param startTime     시작 시간
     * @param intervalHours 간격 (시간)
     * @param count         생성할 슬롯 개수
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
