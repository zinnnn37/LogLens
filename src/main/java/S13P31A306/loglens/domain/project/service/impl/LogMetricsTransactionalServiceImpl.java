package S13P31A306.loglens.domain.project.service.impl;

import S13P31A306.loglens.domain.project.entity.HeatmapMetrics;
import S13P31A306.loglens.domain.project.entity.LogMetrics;
import S13P31A306.loglens.domain.project.entity.Project;
import S13P31A306.loglens.domain.project.service.LogMetricsTransactionalService;
import S13P31A306.loglens.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.Script;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.CompositeAggregationSource;
import org.opensearch.client.opensearch._types.aggregations.CompositeBucket;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.json.JsonData;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static S13P31A306.loglens.global.constants.GlobalErrorCode.OPENSEARCH_OPERATION_FAILED;

/**
 * 로그 메트릭 집계의 트랜잭션 처리를 담당하는 서비스
 * OpenSearch 조회는 트랜잭션 밖에서 수행하고, DB 저장만 트랜잭션으로 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogMetricsTransactionalServiceImpl implements LogMetricsTransactionalService {

    private static final String LOG_PREFIX = "[LogMetricsTransactionalService]";
    private static final String DEFAULT_TIMEZONE = "Asia/Seoul";
    private static final int HEATMAP_AGGREGATION_SIZE = 168;  // 7일 * 24시간

    private final OpenSearchClient openSearchClient;
    private final LogMetricsTransactionHelper transactionHelper;

    @Override
    public void aggregateProjectMetricsIncremental(
            Project project,
            LocalDateTime from,
            LocalDateTime to,
            LogMetrics previous) {

        long startTime = System.currentTimeMillis();

        try {
            String indexPattern = getProjectIndexPattern(project.getProjectUuid());

            // 1. LogMetrics 집계
            SearchRequest logMetricsRequest = buildLogMetricsRequest(indexPattern, from, to);
            SearchResponse<Void> logMetricsResponse = openSearchClient.search(logMetricsRequest, Void.class);
            LogMetrics metrics = calculateCumulativeMetrics(logMetricsResponse, project, to, previous);

            // 2. HeatmapMetrics 집계
            SearchRequest heatmapRequest = buildHeatmapRequest(indexPattern, from, to, project.getId());
            SearchResponse<Void> heatmapResponse = openSearchClient.search(heatmapRequest, Void.class);
            List<HeatmapMetrics> heatmapMetrics = calculateHeatmapMetrics(heatmapResponse, project, to);

            // 3. DB 저장 (한 번에)
            transactionHelper.saveMetrics(metrics, heatmapMetrics);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("{} 집계 완료: projectId={}, 소요시간={}ms, 증분로그={}, 히트맵셀={}",
                    LOG_PREFIX, project.getId(), elapsed,
                    metrics.getTotalLogs() - (previous != null ? previous.getTotalLogs() : 0),
                    heatmapMetrics.size());

        } catch (Exception e) {
            log.error("{} OpenSearch 집계 실패: projectId={}, from={}, to={}",
                    LOG_PREFIX, project.getId(), from, to, e);
            throw new BusinessException(OPENSEARCH_OPERATION_FAILED);
        }
    }

    /**
     * LogMetrics용 OpenSearch 검색 요청 생성
     */
    private SearchRequest buildLogMetricsRequest(String indexPattern, LocalDateTime from, LocalDateTime to) {
        return SearchRequest.of(s -> s
                .index(indexPattern)
                .size(0)
                .query(q -> q
                        .range(r -> r
                                .field("timestamp")
                                .gte(JsonData.of(from.atZone(ZoneId.of(DEFAULT_TIMEZONE)).toInstant().toString()))
                                .lt(JsonData.of(to.atZone(ZoneId.of(DEFAULT_TIMEZONE)).toInstant().toString()))
                        )
                )
                .aggregations("total_logs", a -> a
                        .valueCount(v -> v.field("_id"))
                )
                .aggregations("error_logs", a -> a
                        .filter(f -> f
                                .term(t -> t.field("log_level").value(FieldValue.of("ERROR")))
                        )
                        .aggregations("count", sub -> sub
                                .valueCount(v -> v.field("_id"))
                        )
                )
                .aggregations("warn_logs", a -> a
                        .filter(f -> f
                                .term(t -> t.field("log_level").value(FieldValue.of("WARN")))
                        )
                        .aggregations("count", sub -> sub
                                .valueCount(v -> v.field("_id"))
                        )
                )
                .aggregations("info_logs", a -> a
                        .filter(f -> f
                                .term(t -> t.field("log_level").value(FieldValue.of("INFO")))
                        )
                        .aggregations("count", sub -> sub
                                .valueCount(v -> v.field("_id"))
                        )
                )
                .aggregations("sum_response_time", a -> a
                        .sum(sum -> sum.field("duration"))
                )
        );
    }

    /**
     * HeatmapMetrics용 OpenSearch 검색 요청 생성
     */
    private SearchRequest buildHeatmapRequest(String indexPattern, LocalDateTime from, LocalDateTime to, Integer projectId) {
        return SearchRequest.of(s -> s
                .index(indexPattern)
                .size(0)
                .query(q -> q.bool(b -> {
                    b.must(m -> m.term(t -> t.field("project_id").value(FieldValue.of(projectId))));
                    b.must(m -> m.range(r -> r
                            .field("timestamp")
                            .gte(JsonData.of(from.atZone(ZoneId.of(DEFAULT_TIMEZONE)).toInstant().toString()))
                            .lt(JsonData.of(to.atZone(ZoneId.of(DEFAULT_TIMEZONE)).toInstant().toString()))
                    ));
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
    }

    /**
     * HeatmapMetrics 계산
     */
    private List<HeatmapMetrics> calculateHeatmapMetrics(
            SearchResponse<Void> response,
            Project project,
            LocalDateTime aggregatedAt) {

        List<HeatmapMetrics> result = new ArrayList<>();

        Aggregate byDayAndHour = response.aggregations().get("by_day_and_hour");
        if (Objects.isNull(byDayAndHour) || Objects.isNull(byDayAndHour.composite())) {
            return result;
        }

        for (CompositeBucket bucket : byDayAndHour.composite().buckets().array()) {
            Integer dayOfWeek = Integer.parseInt(bucket.key().get("day_of_week").toString());
            Integer hour = Integer.parseInt(bucket.key().get("hour_of_day").toString());

            Double totalCountDouble = bucket.aggregations().get("total_count").valueCount().value();
            Integer totalCount = (totalCountDouble != null && !totalCountDouble.isNaN())
                    ? totalCountDouble.intValue() : 0;

            Map<String, Integer> levelCounts = parseLevelCounts(bucket.aggregations().get("by_level"));

            HeatmapMetrics heatmap = HeatmapMetrics.builder()
                    .project(project)
                    .dayOfWeek(dayOfWeek)
                    .hour(hour)
                    .totalCount(totalCount)
                    .errorCount(levelCounts.getOrDefault("ERROR", 0))
                    .warnCount(levelCounts.getOrDefault("WARN", 0))
                    .infoCount(levelCounts.getOrDefault("INFO", 0))
                    .aggregatedAt(aggregatedAt)
                    .build();

            result.add(heatmap);
        }

        return result;
    }

    /**
     * 로그 레벨별 카운트 파싱
     */
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

    private String getProjectIndexPattern(String projectUuid) {
        String sanitizedUuid = projectUuid.replace("-", "_");
        return sanitizedUuid + "_*";
    }

    private LogMetrics calculateCumulativeMetrics(
            SearchResponse<Void> response,
            Project project,
            LocalDateTime aggregatedAt,
            LogMetrics previous) {

        Map<String, Aggregate> aggs = response.aggregations();

        long incrementalTotal = extractValueCount(aggs, "total_logs");
        long incrementalErrors = extractNestedValueCount(aggs, "error_logs");
        long incrementalWarns = extractNestedValueCount(aggs, "warn_logs");
        long incrementalInfos = extractNestedValueCount(aggs, "info_logs");
        long incrementalSumResponseTime = extractSumValue(aggs);

        long newTotalLogs = (previous != null ? previous.getTotalLogs() : 0) + incrementalTotal;
        long newErrorLogs = (previous != null ? previous.getErrorLogs() : 0) + incrementalErrors;
        long newWarnLogs = (previous != null ? previous.getWarnLogs() : 0) + incrementalWarns;
        long newInfoLogs = (previous != null ? previous.getInfoLogs() : 0) + incrementalInfos;
        long newSumResponseTime = (previous != null ? previous.getSumResponseTime() : 0L) + incrementalSumResponseTime;

        int newAvgResponseTime = newTotalLogs > 0
                ? (int) (newSumResponseTime / newTotalLogs)
                : 0;

        return LogMetrics.builder()
                .project(project)
                .totalLogs((int) newTotalLogs)
                .errorLogs((int) newErrorLogs)
                .warnLogs((int) newWarnLogs)
                .infoLogs((int) newInfoLogs)
                .sumResponseTime(newSumResponseTime)
                .avgResponseTime(newAvgResponseTime)
                .aggregatedAt(aggregatedAt)
                .build();
    }

    private long extractValueCount(Map<String, Aggregate> aggregations, String aggName) {
        Aggregate agg = aggregations.get(aggName);
        if (agg != null && agg.isValueCount()) {
            Double value = agg.valueCount().value();
            if (value != null && !value.isNaN()) {
                return value.longValue();
            }
        }
        log.warn("{} {} aggregation not found or invalid type", LOG_PREFIX, aggName);
        return 0L;
    }

    private long extractNestedValueCount(Map<String, Aggregate> aggregations, String filterName) {
        Aggregate filterAgg = aggregations.get(filterName);
        if (filterAgg != null && filterAgg.isFilter()) {
            Map<String, Aggregate> subAggs = filterAgg.filter().aggregations();
            return extractValueCount(subAggs, "count");
        }
        log.warn("{} {} filter aggregation not found", LOG_PREFIX, filterName);
        return 0L;
    }

    private long extractSumValue(Map<String, Aggregate> aggregations) {
        Aggregate agg = aggregations.get("sum_response_time");
        if (agg != null && agg.isSum()) {
            Double value = agg.sum().value();
            if (value != null && !value.isNaN()) {
                return value.longValue();
            }
        }
        log.warn("{} sum_response_time aggregation not found or invalid", LOG_PREFIX);
        return 0L;
    }
}
