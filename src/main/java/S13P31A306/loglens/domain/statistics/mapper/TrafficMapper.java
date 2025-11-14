package S13P31A306.loglens.domain.statistics.mapper;

import S13P31A306.loglens.domain.statistics.dto.internal.TrafficAggregation;
import S13P31A306.loglens.domain.statistics.dto.response.TrafficResponse;
import S13P31A306.loglens.domain.statistics.dto.response.TrafficResponse.DataPoint;
import S13P31A306.loglens.domain.statistics.dto.response.TrafficResponse.Period;
import S13P31A306.loglens.domain.statistics.dto.response.TrafficResponse.Summary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static S13P31A306.loglens.domain.statistics.constants.StatisticsConstants.*;

/**
 * Traffic 관련 Mapper
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TrafficMapper {

    /**
     * TrafficAggregation을 DataPoint로 변환
     */
    @Mapping(target = "timestamp", expression = "java(formatTimestamp(agg.timestamp()))")
    @Mapping(target = "hour", expression = "java(formatHour(agg.timestamp()))")
    DataPoint toDataPoint(TrafficAggregation agg);

    /**
     * TrafficAggregation 리스트를 TrafficResponse로 변환
     */
    default TrafficResponse toTrafficResponse(
            String projectUuid,
            LocalDateTime startTime,
            LocalDateTime endTime,
            List<TrafficAggregation> aggregations
    ) {
        // 전체 시간 슬롯 생성 (24시간 / 3시간 = 8개)
        List<LocalDateTime> timeSlots = generateTimeSlots(startTime, INTERVAL_HOURS, TREND_HOURS / INTERVAL_HOURS);

        // OpenSearch 결과를 Map으로 변환 (timestamp -> aggregation)
        Map<LocalDateTime, TrafficAggregation> aggMap = aggregations.stream()
                .collect(Collectors.toMap(
                        TrafficAggregation::timestamp,
                        agg -> agg
                ));

        // 각 시간 슬롯에 대해 데이터 있으면 사용, 없으면 0으로 채움
        List<DataPoint> dataPoints = timeSlots.stream()
                .map(ts -> aggMap.getOrDefault(ts, createEmptyAggregation(ts)))
                .map(this::toDataPoint)
                .toList();

        // Period 생성
        Period period = new Period(
                formatTimestamp(startTime),
                formatTimestamp(endTime)
        );

        // Summary 생성
        Summary summary = buildSummary(dataPoints);

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
     * @return 0으로 채워진 TrafficAggregation
     */
    default TrafficAggregation createEmptyAggregation(LocalDateTime timestamp) {
        return new TrafficAggregation(timestamp, 0, 0, 0);
    }

    /**
     * Summary 생성 (전체 요약 통계)
     */
    default Summary buildSummary(List<DataPoint> dataPoints) {
        int totalLogs = dataPoints.stream()
                .mapToInt(DataPoint::totalCount)
                .sum();

        int totalFeCount = dataPoints.stream()
                .mapToInt(DataPoint::feCount)
                .sum();

        int totalBeCount = dataPoints.stream()
                .mapToInt(DataPoint::beCount)
                .sum();

        int avgLogsPerInterval = dataPoints.isEmpty() ? 0 : totalLogs / dataPoints.size();

        // 피크 시간대 찾기
        DataPoint peakDataPoint = dataPoints.stream()
                .max((d1, d2) -> Integer.compare(d1.totalCount(), d2.totalCount()))
                .orElse(null);

        String peakHour = peakDataPoint != null ? peakDataPoint.hour() : "00:00";
        Integer peakCount = peakDataPoint != null ? peakDataPoint.totalCount() : 0;

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
