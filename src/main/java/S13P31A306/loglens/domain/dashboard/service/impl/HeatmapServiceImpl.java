package S13P31A306.loglens.domain.dashboard.service.impl;

import S13P31A306.loglens.domain.auth.util.AuthenticationHelper;
import S13P31A306.loglens.domain.dashboard.constants.DayOfWeek;
import S13P31A306.loglens.domain.dashboard.dto.HeatmapAggregation;
import S13P31A306.loglens.domain.dashboard.dto.response.HeatmapResponse;
import S13P31A306.loglens.domain.dashboard.service.HeatmapService;
import S13P31A306.loglens.domain.dashboard.validator.DashboardValidator;
import S13P31A306.loglens.domain.project.validator.ProjectValidator;
import S13P31A306.loglens.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.aggregations.CompositeAggregationSource;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.Script;
import org.opensearch.client.opensearch._types.aggregations.*;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static S13P31A306.loglens.domain.dashboard.constants.DashboardConstants.*;
import static S13P31A306.loglens.global.constants.GlobalErrorCode.OPENSEARCH_OPERATION_FAILED;

@Slf4j
@Service
@RequiredArgsConstructor
public class HeatmapServiceImpl implements HeatmapService {

    private static final String LOG_PREFIX = "[HeatmapService]";
    private static final String INDEX_PATTERN = "logs-*";

    private final OpenSearchClient openSearchClient;
    private final ProjectValidator projectValidator;
    private final DashboardValidator dashboardValidator;
    private final AuthenticationHelper authHelper;

    @Override
    public HeatmapResponse getLogHeatmap(String projectUuid, String startTime, String endTime, String logLevel) {
        log.info("{} 히트맵 정보 조회 시작: projUuid={}, logLevel={}, start={}, end={}",
                LOG_PREFIX, projectUuid, logLevel, startTime, endTime);

        Integer userId = authHelper.getCurrentUserId();

        // 프로젝트 조회
        Integer projectId = projectValidator.validateProjectExists(projectUuid).getId();

        // 사용자가 프로젝트에 존재하는지 확인
        projectValidator.validateMemberExists(projectId, userId);

        // 시간 범위 설정
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

        // set default log level
        String level = dashboardValidator.validateLogLevel(logLevel);

        // Query
        Map<String, HeatmapAggregation> aggregations = aggregateLogsByDayAndHour(
                projectId, start, end, level
        );

        return buildHeatmapResponse(projectId, start, end, level, aggregations);
    }

    private HeatmapResponse buildHeatmapResponse(Integer projectId, LocalDateTime start,
                                                 LocalDateTime end, String logLevel,
                                                 Map<String, HeatmapAggregation> aggregations) {
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

    private Map<String, HeatmapAggregation> aggregateLogsByDayAndHour(
            Integer projectId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String logLevel
    ) {
        log.info("{} OpenSearch 쿼리 실행: projId={}", LOG_PREFIX, projectId);

        try {
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(INDEX_PATTERN)
                    .size(0)
                    .query(q -> q.bool(b -> {
                        b.must(m -> m.term(t -> t.field("project_id").value(FieldValue.of(projectId))));
                        b.must(m -> m.range(r -> r
                                .field("timestamp")
                                .gte(JsonData.of(startTime.atZone(ZoneId.of(DEFAULT_TIMEZONE)).toInstant().toString()))
                                .lte(JsonData.of(endTime.atZone(ZoneId.of(DEFAULT_TIMEZONE)).toInstant().toString()))
                        ));

                        if (!"ALL".equalsIgnoreCase(logLevel)) {
                            b.must(m -> m.term(t -> t.field("log_level").value(FieldValue.of(logLevel))));
                        }

                        return b;
                    }))
                    .aggregations("by_day_and_hour", a -> a
                            .composite(c -> {
                                Map<String, CompositeAggregationSource> sources = new LinkedHashMap<>();
                                sources.put("day_of_week", CompositeAggregationSource.of(cas -> cas
                                        .terms(t -> t.script(Script.of(sc -> sc
                                                .inline(i -> i.source(
                                                        "doc['timestamp'].value.withZoneSameInstant(ZoneId.of('" + DEFAULT_TIMEZONE + "')).getDayOfWeek()"
                                                ))
                                        )))
                                ));
                                sources.put("hour_of_day", CompositeAggregationSource.of(cas -> cas
                                        .terms(t -> t.script(Script.of(sc -> sc
                                                .inline(i -> i.source(
                                                        "doc['timestamp'].value.withZoneSameInstant(ZoneId.of('" + DEFAULT_TIMEZONE + "')).getHour()"
                                                ))
                                        )))
                                ));

                                return c.size(HEATMAP_AGGREGATION_SIZE).sources(sources);
                            })
                            .aggregations("total_count", agg -> agg
                                    .valueCount(v -> v.field("_id"))
                            )
                            .aggregations("by_level", agg -> agg
                                    .terms(t -> t.field("log_level.keyword"))
                            )
                    )
            );

            SearchResponse<Void> response = openSearchClient.search(searchRequest, Void.class);
            return parseAggregationResponse(response);
        } catch (Exception e) {
            log.error("{} OpenSearch 쿼리 실행 중 오류 발생: e", LOG_PREFIX, e);
            throw new BusinessException(OPENSEARCH_OPERATION_FAILED);
        }
    }

    private Map<String, HeatmapAggregation> parseAggregationResponse(SearchResponse<Void> response) {
        Map<String, HeatmapAggregation> resultMap = new HashMap<>();

        Aggregate byDayAndHour = response.aggregations().get("by_day_and_hour");
        if (Objects.isNull(byDayAndHour) || Objects.isNull(byDayAndHour.composite())) {
            return resultMap;
        }

        for (CompositeBucket bucket : byDayAndHour.composite().buckets().array()) {
            Integer dayOfWeek = Integer.parseInt(bucket.key().get("day_of_week").toString());
            Integer hour = Integer.parseInt(bucket.key().get("hour_of_day").toString());
            Integer totalCount = bucket.aggregations().get("total_count").valueCount().value().intValue();

            Map<String, Integer> levelCounts = parseLevelCounts(bucket.aggregations().get("by_level"));

            HeatmapAggregation aggregation = new HeatmapAggregation(
                    dayOfWeek, hour, totalCount,
                    levelCounts.getOrDefault("ERROR", 0),
                    levelCounts.getOrDefault("WARN", 0),
                    levelCounts.getOrDefault("INFO", 0)
            );

            resultMap.put(dayOfWeek + "_" + hour, aggregation);
        }

        return resultMap;
    }

    private Map<String, Integer> parseLevelCounts(Aggregate byLevel) {
        Map<String, Integer> counts = new HashMap<>();
        if (Objects.isNull(byLevel) || Objects.isNull(byLevel.sterms())) {
            return counts;
        }
        for (StringTermsBucket bucket : byLevel.sterms().buckets().array()) {
            counts.put(bucket.key(), (int) bucket.docCount());
        }
        return counts;
    }

}
