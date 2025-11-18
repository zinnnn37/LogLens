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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface LogTrendMapper {

    Logger log = LoggerFactory.getLogger(LogTrendMapper.class);

    @Mapping(target = "timestamp", expression = "java(formatTimestamp(aggregation.timestamp()))")
    @Mapping(target = "hour", expression = "java(formatHour(aggregation.timestamp()))")
    LogTrendResponse.DataPoint toDataPoint(LogTrendAggregation aggregation);

    default String formatTimestamp(LocalDateTime timestamp) {
        return timestamp.atZone(ZoneId.of(DEFAULT_TIMEZONE))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    default String formatHour(LocalDateTime timestamp) {
        return timestamp.format(DateTimeFormatter.ofPattern(TIME_FORMAT));
    }

    default LogTrendResponse toLogTrendResponse(
            String projectUuid,
            LocalDateTime startTimeUtc,
            LocalDateTime endTimeUtc,
            List<LogTrendAggregation> aggregations
    ) {

        log.info("=== [Mapper] toLogTrendResponse START ===");
        log.info("[Mapper] startTimeUtc={}", startTimeUtc);
        log.info("[Mapper] endTimeUtc={}", endTimeUtc);

        // UTC → KST 변환
        LocalDateTime startTimeKst = startTimeUtc.atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(ZoneId.of(DEFAULT_TIMEZONE))
                .toLocalDateTime();

        LocalDateTime endTimeKst = endTimeUtc.atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(ZoneId.of(DEFAULT_TIMEZONE))
                .toLocalDateTime();

        log.info("[Mapper] startTimeKst={}", startTimeKst);
        log.info("[Mapper] endTimeKst={}", endTimeKst);

        // KST 기준 슬롯 생성
        List<LocalDateTime> timeSlots = generateTimeSlots(
                startTimeKst, INTERVAL_HOURS, TREND_HOURS / INTERVAL_HOURS
        );

        log.info("=== [Mapper] Generated Time Slots (KST) ===");
        for (LocalDateTime ts : timeSlots) {
            log.info("[Slot] {}", ts);
        }

        // Aggregation timestamp도 모두 KST로 변환해서 log
        log.info("=== [Mapper] Aggregations (Original UTC) ===");
        for (LogTrendAggregation agg : aggregations) {
            log.info("[Agg-UTC] {}", agg.timestamp());
        }

        // OpenSearch 결과 → KST로 변환하여 Map에 저장
        Map<LocalDateTime, LogTrendAggregation> aggMap = new HashMap<>();
        for (LogTrendAggregation agg : aggregations) {
            LocalDateTime kstKey = agg.timestamp()
                    .atOffset(ZoneOffset.UTC)
                    .atZoneSameInstant(ZoneId.of(DEFAULT_TIMEZONE))
                    .toLocalDateTime()
                    .truncatedTo(ChronoUnit.HOURS);

            aggMap.putIfAbsent(kstKey, agg);
        }

        log.info("=== [Mapper] Aggregation Map Keys (KST truncated) ===");
        for (LocalDateTime key : aggMap.keySet()) {
            LogTrendAggregation a = aggMap.get(key);
            log.info("[AggMap] key={} | total={}", key, a.totalCount());
        }

        // 슬롯 매핑
        log.info("=== [Mapper] Slot Matching Result ===");
        List<LogTrendResponse.DataPoint> dataPoints = timeSlots.stream()
                .map(ts -> {
                    boolean match = aggMap.containsKey(ts);
                    log.info("[MatchCheck] slot={} | match={} {}", ts, match,
                            match ? "(FOUND)" : "(EMPTY → createEmptyAggregation)");

                    return aggMap.getOrDefault(ts, createEmptyAggregation(ts));
                })
                .map(this::toDataPoint)
                .toList();

        LogTrendResponse.Period period = new LogTrendResponse.Period(
                formatTimestamp(startTimeUtc),
                formatTimestamp(endTimeUtc)
        );

        LogTrendResponse.Summary summary = buildSummary(aggregations);

        log.info("=== [Mapper] toLogTrendResponse END ===");

        return new LogTrendResponse(
                projectUuid,
                period,
                INTERVAL_HOURS + "h",
                dataPoints,
                summary
        );
    }

    default List<LocalDateTime> generateTimeSlots(LocalDateTime startTime, int intervalHours, int count) {
        List<LocalDateTime> timeSlots = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            timeSlots.add(startTime.plusHours((long) i * intervalHours));
        }
        return timeSlots;
    }

    default LogTrendAggregation createEmptyAggregation(LocalDateTime timestamp) {
        return new LogTrendAggregation(timestamp, 0, 0, 0, 0);
    }

    default LogTrendResponse.Summary buildSummary(List<LogTrendAggregation> aggregations) {
        int totalLogs = aggregations.stream()
                .mapToInt(LogTrendAggregation::totalCount)
                .sum();

        int avgLogsPerInterval = aggregations.isEmpty() ? 0 : totalLogs / aggregations.size();

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
