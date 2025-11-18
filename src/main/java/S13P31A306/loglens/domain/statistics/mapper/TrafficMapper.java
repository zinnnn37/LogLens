package S13P31A306.loglens.domain.statistics.mapper;

import static S13P31A306.loglens.domain.statistics.constants.StatisticsConstants.INTERVAL_HOURS;
import static S13P31A306.loglens.domain.statistics.constants.StatisticsConstants.TREND_HOURS;

import S13P31A306.loglens.domain.statistics.dto.internal.TrafficAggregation;
import S13P31A306.loglens.domain.statistics.dto.response.TrafficResponse;
import S13P31A306.loglens.domain.statistics.dto.response.TrafficResponse.DataPoint;
import S13P31A306.loglens.domain.statistics.dto.response.TrafficResponse.Period;
import S13P31A306.loglens.domain.statistics.dto.response.TrafficResponse.Summary;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

/**
 * Traffic 관련 Mapper
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TrafficMapper {

    Logger log = LoggerFactory.getLogger(LogTrendMapper.class);

    /**
     * TrafficAggregation을 DataPoint로 변환
     */
    @Mapping(target = "timestamp", expression = "java(formatTimestamp(agg.timestamp()))")
    @Mapping(target = "hour", expression = "java(formatHour(agg.timestamp()))")
    DataPoint toDataPoint(TrafficAggregation agg);

    default TrafficResponse toTrafficResponse(
            String projectUuid,
            LocalDateTime startTime,
            LocalDateTime endTime,
            List<TrafficAggregation> aggregations
    ) {
        log.info("[TrafficMapper] ==================================================");
        log.info("[TrafficMapper] TrafficResponse 매핑 시작");
        log.info("[TrafficMapper] projectUuid={}", projectUuid);
        log.info("[TrafficMapper] start(KST)={}, end(KST)={}", startTime, endTime);
        log.info("[TrafficMapper] 입력 Aggregation 개수={}", aggregations.size());

        // 1. 전체 시간 슬롯 생성 (24시간 / 3시간 = 8개)
        // bucket 첫 timestamp(KST) → bucket end timestamp 기준
        LocalDateTime slotBaseKst = aggregations.getFirst().timestamp()
                .plusHours(INTERVAL_HOURS)
                .atZone(ZoneId.of("Asia/Seoul"))
                .toLocalDateTime()
                .truncatedTo(ChronoUnit.HOURS);

        List<LocalDateTime> timeSlots = generateTimeSlots(
                slotBaseKst,
                INTERVAL_HOURS,
                TREND_HOURS / INTERVAL_HOURS
        );
        log.info("[TrafficMapper] 생성된 timeSlots(KST)={}", timeSlots);

        // 2. OpenSearch bucket(timestamp + 3h)를 END 기준으로 변환
        Map<LocalDateTime, TrafficAggregation> aggMap = aggregations.stream()
                .collect(Collectors.toMap(
                        agg -> {
                            LocalDateTime endTs = agg.timestamp().plusHours(INTERVAL_HOURS);
                            log.info("[TrafficMapper] Bucket START={}, END(after interval)={}", agg.timestamp(), endTs);
                            return endTs.truncatedTo(ChronoUnit.HOURS);
                        },
                        agg -> agg,
                        (existing, replacement) -> existing
                ));

        // DEBUG: 매핑 테이블 출력
        log.info("[TrafficMapper] =========== Bucket 매핑 테이블 (END 시각 기준) ===========");
        aggMap.forEach((k, v) -> log.info("[BucketMap] {} => total={}, FE={}, BE={}",
                k, v.totalCount(), v.feCount(), v.beCount()));
        log.info("[TrafficMapper] =======================================================");

        // 3. 최종 DataPoint 생성
        List<DataPoint> dataPoints = timeSlots.stream()
                .map(ts -> {
                    LocalDateTime truncated = ts.truncatedTo(ChronoUnit.HOURS);
                    TrafficAggregation matched = aggMap.get(truncated);

                    if (matched != null) {
                        log.info("[TrafficMapper] 슬롯 {} → 매칭된 bucket END {} (total={})",
                                ts, truncated, matched.totalCount());
                    } else {
                        log.info("[TrafficMapper] 슬롯 {} → 매칭 없음 → 0으로 채움", ts);
                    }

                    return matched != null ? matched : createEmptyAggregation(ts);
                })
                .map(this::toDataPoint)
                .toList();

        // 4. Period 생성
        Period period = new Period(
                formatTimestamp(startTime),
                formatTimestamp(endTime)
        );

        // 5. Summary 생성
        Summary summary = buildSummary(aggregations);

        log.info("[TrafficMapper] 최종 dataPoints 개수={}", dataPoints.size());
        log.info("[TrafficMapper] TrafficResponse 매핑 완료");
        log.info("[TrafficMapper] ==================================================");

        return new TrafficResponse(
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
     * @return 0으로 채워진 TrafficAggregation
     */
    default TrafficAggregation createEmptyAggregation(LocalDateTime timestamp) {
        return new TrafficAggregation(timestamp, 0, 0, 0);
    }

    /**
     * Summary 생성 (전체 요약 통계 - 실제 aggregations 기반)
     */
    default Summary buildSummary(List<TrafficAggregation> aggregations) {
        int totalLogs = aggregations.stream()
                .mapToInt(TrafficAggregation::totalCount)
                .sum();

        int totalFeCount = aggregations.stream()
                .mapToInt(TrafficAggregation::feCount)
                .sum();

        int totalBeCount = aggregations.stream()
                .mapToInt(TrafficAggregation::beCount)
                .sum();

        int avgLogsPerInterval = aggregations.isEmpty() ? 0 : totalLogs / aggregations.size();

        // 피크 시간대 찾기
        TrafficAggregation peakAggregation = aggregations.stream()
                .max(Comparator.comparing(TrafficAggregation::totalCount))
                .orElse(null);

        String peakHour = peakAggregation != null ? formatHour(peakAggregation.timestamp()) : "00:00";
        Integer peakCount = peakAggregation != null ? peakAggregation.totalCount() : 0;

        return new Summary(
                totalLogs,
                totalFeCount,
                totalBeCount,
                avgLogsPerInterval,
                peakHour,
                peakCount
        );
    }

    /**
     * LocalDateTime을 ISO-8601 문자열로 포맷 (Asia/Seoul 타임존 적용)
     */
    default String formatTimestamp(LocalDateTime timestamp) {
        return timestamp.atZone(ZoneId.of("Asia/Seoul"))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /**
     * LocalDateTime을 HH:mm 형식으로 포맷
     */
    default String formatHour(LocalDateTime timestamp) {
        return timestamp.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}
