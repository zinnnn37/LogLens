package S13P31A306.loglens.domain.dashboard.service.impl;

import S13P31A306.loglens.domain.auth.util.AuthenticationHelper;
import S13P31A306.loglens.domain.dashboard.constants.DayOfWeek;
import S13P31A306.loglens.domain.dashboard.dto.HeatmapAggregation;
import S13P31A306.loglens.domain.dashboard.dto.response.HeatmapResponse;
import S13P31A306.loglens.domain.dashboard.service.HeatmapService;
import S13P31A306.loglens.domain.dashboard.validator.DashboardValidator;
import S13P31A306.loglens.domain.project.entity.HeatmapMetrics;
import S13P31A306.loglens.domain.project.repository.HeatmapMetricsRepository;
import S13P31A306.loglens.domain.project.validator.ProjectValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static S13P31A306.loglens.domain.dashboard.constants.DashboardConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class HeatmapServiceImpl implements HeatmapService {

    private static final String LOG_PREFIX = "[HeatmapService]";

    private final ProjectValidator projectValidator;
    private final DashboardValidator dashboardValidator;
    private final AuthenticationHelper authHelper;
    private final HeatmapMetricsRepository heatmapMetricsRepository;

    @Override
    public HeatmapResponse getLogHeatmap(String projectUuid, String startTime, String endTime, String logLevel) {
        log.info("{} 히트맵 정보 조회 시작: projUuid={}, logLevel={}, start={}, end={}",
                LOG_PREFIX, projectUuid, logLevel, startTime, endTime);

        Integer userId = authHelper.getCurrentUserId();
        Integer projectId = projectValidator.validateProjectExists(projectUuid).getId();
        projectValidator.validateMemberExists(projectId, userId);

        LocalDateTime parsedEnd = dashboardValidator.validateAndParseTime(endTime);
        LocalDateTime parsedStart = dashboardValidator.validateAndParseTime(startTime);

        LocalDateTime end;
        if (parsedEnd != null) {
            end = parsedEnd;
        } else if (parsedStart != null) {
            end = parsedStart.plusDays(HEATMAP_DEFAULT_DAYS);
        } else {
            end = LocalDateTime.now();
        }
        LocalDateTime start = parsedStart != null ? parsedStart : end.minusDays(HEATMAP_DEFAULT_DAYS);

        dashboardValidator.validateTimeRange(start, end, HEATMAP_MAX_DAYS);

        String level = dashboardValidator.validateLogLevel(logLevel);

        List<HeatmapMetrics> heatmapMetrics = heatmapMetricsRepository
                .findByProjectIdAndDateBetween(projectId, start.toLocalDate(), end.toLocalDate());

        return buildHeatmapResponse(projectId, start, end, level, heatmapMetrics);
    }

    private HeatmapResponse buildHeatmapResponse(
            Integer projectId,
            LocalDateTime start,
            LocalDateTime end,
            String logLevel,
            List<HeatmapMetrics> heatmapMetrics) {

        // date 포함한 고유 키 사용
        Map<String, HeatmapAggregation> aggregations = new HashMap<>();

        for (HeatmapMetrics metric : heatmapMetrics) {
            int dayOfWeek = metric.getDate().getDayOfWeek().getValue();
            // date를 키에 포함 → 날짜별로 구분됨
            String key = metric.getDate() + "_" + metric.getHour();

            int count;
            if ("ERROR".equalsIgnoreCase(logLevel)) {
                count = metric.getErrorCount();
            } else if ("WARN".equalsIgnoreCase(logLevel)) {
                count = metric.getWarnCount();
            } else if ("INFO".equalsIgnoreCase(logLevel)) {
                count = metric.getInfoCount();
            } else {
                count = metric.getTotalCount();
            }

            aggregations.put(key, new HeatmapAggregation(
                    dayOfWeek,
                    metric.getHour(),
                    count,
                    metric.getErrorCount(),
                    metric.getWarnCount(),
                    metric.getInfoCount()
            ));
        }

        int maxCount = aggregations.values().stream()
                .mapToInt(HeatmapAggregation::totalCount)
                .max()
                .orElse(1);

        List<HeatmapResponse.DayData> heatmap = buildDayDataList(aggregations, maxCount);
        HeatmapResponse.Summary summary = buildSummary(aggregations, start, end);

        return new HeatmapResponse(
                projectId,
                new HeatmapResponse.Period(
                        start.format(DateTimeFormatter.ISO_DATE_TIME),
                        end.format(DateTimeFormatter.ISO_DATE_TIME)
                ),
                heatmap,
                summary,
                new HeatmapResponse.Metadata(logLevel, DEFAULT_TIMEZONE)
        );
    }

    private List<HeatmapResponse.DayData> buildDayDataList(Map<String, HeatmapAggregation> aggregations, int maxCount) {
        Map<Integer, List<HeatmapResponse.HourData>> dayMap = new HashMap<>();

        aggregations.values().forEach(agg -> {
            HeatmapResponse.HourData hourData = new HeatmapResponse.HourData(
                    agg.hour(),
                    agg.totalCount(),
                    agg.errorCount(),
                    agg.warnCount(),
                    agg.infoCount(),
                    maxCount == 0 ? 0.0 : (double) agg.totalCount() / maxCount
            );

            dayMap.computeIfAbsent(agg.dayOfWeek(), k -> new ArrayList<>()).add(hourData);
        });

        return dayMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    Integer dayNum = entry.getKey();
                    DayOfWeek day = DayOfWeek.fromValue(dayNum);
                    List<HeatmapResponse.HourData> hourlyData = entry.getValue().stream()
                            .sorted(Comparator.comparing(HeatmapResponse.HourData::hour))
                            .collect(Collectors.toList());

                    int totalCount = hourlyData.stream()
                            .mapToInt(HeatmapResponse.HourData::count)
                            .sum();

                    return new HeatmapResponse.DayData(
                            day.name(),
                            day.getKoreanName(),
                            hourlyData,
                            totalCount
                    );
                })
                .collect(Collectors.toList());
    }

    private HeatmapResponse.Summary buildSummary(Map<String, HeatmapAggregation> aggregations,
                                                 LocalDateTime start, LocalDateTime end) {
        int totalLogs = aggregations.values().stream()
                .mapToInt(HeatmapAggregation::totalCount)
                .sum();

        HeatmapAggregation peakAgg = aggregations.values().stream()
                .max(Comparator.comparing(HeatmapAggregation::totalCount))
                .orElse(new HeatmapAggregation(1, 0, 0, 0, 0, 0));

        Map<Integer, Integer> dayTotals = aggregations.values().stream()
                .collect(Collectors.groupingBy(
                        HeatmapAggregation::dayOfWeek,
                        Collectors.summingInt(HeatmapAggregation::totalCount)
                ));

        String peakDay = dayTotals.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> DayOfWeek.fromValue(e.getKey()).name())
                .orElse("MONDAY");

        long days = ChronoUnit.DAYS.between(start, end);
        int avgDailyCount = days == 0 ? totalLogs : (int) (totalLogs / days);

        return new HeatmapResponse.Summary(
                totalLogs,
                peakDay,
                peakAgg.hour(),
                peakAgg.totalCount(),
                avgDailyCount
        );
    }
}
