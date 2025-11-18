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
            LocalDateTime startTimeUtc,   // 요청 start
            LocalDateTime endTimeUtc,     // 요청 end
            List<LogTrendAggregation> aggregations // OS bucket 결과
    ) {
        log.info("=== [Mapper] toLogTrendResponse START ===");
        log.info("[Mapper] startTimeUtc={}", startTimeUtc);
        log.info("[Mapper] endTimeUtc={}", endTimeUtc);

        if (aggregations.isEmpty()) {
            log.warn("[Mapper] No aggregations returned from OpenSearch — returning empty response.");
            return new LogTrendResponse(
                    projectUuid,
                    new LogTrendResponse.Period(formatTimestamp(startTimeUtc), formatTimestamp(endTimeUtc)),
                    INTERVAL_HOURS + "h",
                    List.of(),
                    new LogTrendResponse.Summary(0, 0, "00:00", 0)
            );
        }

        // === 1) 요청된 시간을 KST로 변환 ===
        LocalDateTime startTimeKst = startTimeUtc.atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(ZoneId.of(DEFAULT_TIMEZONE))
                .toLocalDateTime();

        LocalDateTime endTimeKst = endTimeUtc.atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(ZoneId.of(DEFAULT_TIMEZONE))
                .toLocalDateTime();

        log.info("[Mapper] startTimeKst={}", startTimeKst);
        log.info("[Mapper] endTimeKst={}", endTimeKst);

        // === 2) Bucket의 첫 timestamp(KST 기준)를 slot 시작 기준으로 사용 ===
        LocalDateTime slotBaseKst = aggregations.getFirst().timestamp()
                .atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(ZoneId.of(DEFAULT_TIMEZONE))
                .toLocalDateTime()
                .truncatedTo(ChronoUnit.HOURS);

        log.info("[Mapper] slotBaseKst(첫 bucket KST 기준)={}", slotBaseKst);

        // === 3) timeSlots 생성 ===
        List<LocalDateTime> timeSlots = generateTimeSlots(
                slotBaseKst,
                INTERVAL_HOURS,
                TREND_HOURS / INTERVAL_HOURS
        );

        log.info("=== [Mapper] Generated Time Slots (KST) ===");
        timeSlots.forEach(ts -> log.info("[Slot] {}", ts));

        // === 4) Aggregations을 KST hour-truncated 기준으로 Map으로 변환 ===
        log.info("=== [Mapper] Aggregations (Original UTC) ===");
        aggregations.forEach(a -> log.info("[Agg-UTC] {}", a.timestamp()));

        Map<LocalDateTime, LogTrendAggregation> aggMap = aggregations.stream()
                .collect(Collectors.toMap(
                        a -> a.timestamp()
                                .atOffset(ZoneOffset.UTC)
                                .atZoneSameInstant(ZoneId.of(DEFAULT_TIMEZONE))
                                .toLocalDateTime()
                                .truncatedTo(ChronoUnit.HOURS),
                        a -> a,
                        (existing, replacement) -> existing
                ));

        log.info("=== [Mapper] Aggregation Map Keys (KST truncated) ===");
        aggMap.forEach((k, v) ->
                log.info("[AggMap] key={} | total={}", k, v.totalCount())
        );

        // === 5) timeSlots와 aggMap 매칭 ===
        log.info("=== [Mapper] Slot Matching Result ===");
        List<LogTrendResponse.DataPoint> dataPoints =
                timeSlots.stream()
                        .map(ts -> {
                            boolean match = aggMap.containsKey(ts);
                            log.info("[MatchCheck] slot={} | match={} ({})",
                                    ts, match,
                                    match ? "USE aggregation" : "EMPTY → createEmptyAggregation"
                            );
                            return aggMap.getOrDefault(ts, createEmptyAggregation(ts));
                        })
                        .map(this::toDataPoint)
                        .toList();

        // === 6) Period는 요청 시간 기준으로 출력(KST) ===
        LogTrendResponse.Period period = new LogTrendResponse.Period(
                formatTimestamp(startTimeUtc),  // formatTimestamp는 자동 KST 변환됨
                formatTimestamp(endTimeUtc)
        );

        // === 7) Summary 계산 ===
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
