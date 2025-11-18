package S13P31A306.loglens.domain.component.service.impl;

import static S13P31A306.loglens.domain.component.constants.OpenSearchAggregation.Name.BY_COMPONENT;
import static S13P31A306.loglens.domain.component.constants.OpenSearchAggregation.Name.ERROR_COUNT;
import static S13P31A306.loglens.domain.component.constants.OpenSearchAggregation.Name.ERROR_TRACES;
import static S13P31A306.loglens.domain.component.constants.OpenSearchAggregation.Name.TOTAL_CALLS;
import static S13P31A306.loglens.domain.component.constants.OpenSearchAggregation.Name.TOTAL_TRACES;
import static S13P31A306.loglens.domain.component.constants.OpenSearchAggregation.Name.WARN_COUNT;
import static S13P31A306.loglens.domain.component.constants.OpenSearchAggregation.Name.WARN_TRACES;

import S13P31A306.loglens.domain.component.constants.LogLevel;
import S13P31A306.loglens.domain.component.constants.OpenSearchAggregation;
import S13P31A306.loglens.domain.component.constants.OpenSearchField;
import S13P31A306.loglens.domain.component.constants.SourceType;
import S13P31A306.loglens.domain.component.dto.MetricsData;
import S13P31A306.loglens.domain.component.service.OpenSearchMetricsService;
import S13P31A306.loglens.domain.dashboard.dto.FrontendMetricsSummary;
import S13P31A306.loglens.global.utils.OpenSearchUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.StringTermsAggregate;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenSearchMetricsServiceImpl implements OpenSearchMetricsService {

    private static final String LOG_PREFIX = "[OpenSearchMetricsService]";
    private final OpenSearchClient openSearchClient;

    @Override
    public Map<String, MetricsData> getProjectMetrics(String projectUuid) {
        log.debug("{} 프로젝트 메트릭 조회 시작: projectUuid={}", LOG_PREFIX, projectUuid);

        try {
            SearchRequest searchRequest = buildProjectMetricsRequest(projectUuid);

            log.debug("{} 🔍 Query built: {}", LOG_PREFIX, searchRequest);

            SearchResponse<Void> response = openSearchClient.search(searchRequest, Void.class);

            log.debug("{} 🔍 Raw aggregations: {}", LOG_PREFIX, response.aggregations().keySet());

            Map<String, MetricsData> metricsMap = parseProjectMetricsResponse(response);

            log.debug("{} 프로젝트 메트릭 조회 완료: projectUuid={}, components={}",
                    LOG_PREFIX, projectUuid, metricsMap.size());

            return metricsMap;

        } catch (IOException e) {
            log.error("{} OpenSearch 조회 실패: projectUuid={}, errorType={}, message={}",
                    LOG_PREFIX, projectUuid, e.getClass().getSimpleName(), e.getMessage());
            return new HashMap<>();
        }
    }

    @Override
    public MetricsData getComponentMetrics(String projectUuid, String componentName) {
        log.debug("{} 컴포넌트 메트릭 조회: projectUuid={}, component={}",
                LOG_PREFIX, projectUuid, componentName);

        try {
            SearchRequest searchRequest = buildComponentMetricsRequest(projectUuid, componentName);

            log.debug("{} 🔍 Component Query built: {}", LOG_PREFIX, searchRequest);

            SearchResponse<Void> response = openSearchClient.search(searchRequest, Void.class);

            log.debug("{} 🔍 Raw aggregations: {}", LOG_PREFIX, response.aggregations().keySet());

            MetricsData metricsData = parseComponentMetricsResponse(response);

            log.debug("{} 컴포넌트 메트릭 조회 완료: {} -> calls={}, errors={}, warns={}",
                    LOG_PREFIX, componentName,
                    metricsData.totalCalls(), metricsData.errorCount(), metricsData.warnCount());

            return metricsData;

        } catch (IOException e) {
            log.error("{} OpenSearch 조회 실패: projectUuid={}, component={}, error={}",
                    LOG_PREFIX, projectUuid, componentName, e.getMessage());
            return MetricsData.empty();
        }
    }

    @Override
    public FrontendMetricsSummary getFrontendMetrics(String projectUuid) {
        log.debug("{} Frontend 메트릭 조회 시작: projectUuid={}", LOG_PREFIX, projectUuid);

        try {
            SearchRequest searchRequest = buildFrontendMetricsRequest(projectUuid);

            log.debug("{} 🔍 Frontend Query built: {}", LOG_PREFIX, searchRequest);

            SearchResponse<Void> response = openSearchClient.search(searchRequest, Void.class);
            log.debug("{} 🔍 Raw aggregations: {}", LOG_PREFIX, response.aggregations().keySet());

            FrontendMetricsSummary summary = parseFrontendMetricsResponse(response);

            log.debug("{} Frontend 메트릭 조회 완료: total={}, error={}, warn={}",
                    LOG_PREFIX, summary.totalTraces(), summary.totalError(), summary.totalWarn());

            return summary;

        } catch (IOException e) {
            log.error("{} Frontend 메트릭 조회 실패: error={}", LOG_PREFIX, e.getMessage());
            return FrontendMetricsSummary.empty();
        }
    }

    // -------------------------------
    // Query Builders
    // -------------------------------

    private SearchRequest buildProjectMetricsRequest(String projectUuid) {
        Query boolQuery = Query.of(q -> q.bool(b -> b
                .filter(f -> f.term(t -> t
                        .field(OpenSearchField.PROJECT_UUID_KEYWORD.getFieldName())
                        .value(v -> v.stringValue(projectUuid))))
                .filter(f -> f.term(t -> t
                        .field(OpenSearchField.SOURCE_TYPE.getFieldName())
                        .value(v -> v.stringValue(SourceType.BACKEND.getType()))))
        ));

        return SearchRequest.of(s -> s
                .index(OpenSearchUtils.getProjectIndexPattern(projectUuid))
                .size(0)
                .query(boolQuery)
                .aggregations(BY_COMPONENT, a -> a
                        .terms(t -> t
                                .field(OpenSearchField.COMPONENT_NAME_KEYWORD.getFieldName())
                                .size(OpenSearchAggregation.MAX_SIZE))
                        .aggregations(TOTAL_CALLS, sub -> sub
                                .cardinality(c -> c.field(OpenSearchField.TRACE_ID.getFieldName())))
                        .aggregations(ERROR_TRACES, sub -> sub
                                .filter(f -> f.term(
                                        t -> t.field(OpenSearchField.LOG_LEVEL.getFieldName())
                                                .value(v -> v.stringValue(LogLevel.ERROR.getLevel()))))
                                .aggregations(ERROR_COUNT, subsub -> subsub
                                        .cardinality(c -> c.field(OpenSearchField.TRACE_ID.getFieldName()))))
                        .aggregations(WARN_TRACES, sub -> sub
                                .filter(f -> f.bool(b -> b
                                        .must(m -> m.term(t -> t.field(OpenSearchField.LOG_LEVEL.getFieldName())
                                                .value(v -> v.stringValue(LogLevel.WARN.getLevel()))))
                                        .mustNot(mn -> mn.term(t -> t.field(OpenSearchField.LOG_LEVEL.getFieldName())
                                                .value(v -> v.stringValue(LogLevel.ERROR.getLevel()))))))
                                .aggregations(WARN_COUNT, subsub -> subsub
                                        .cardinality(c -> c.field(OpenSearchField.TRACE_ID.getFieldName())))))
        );
    }

    private SearchRequest buildComponentMetricsRequest(String projectUuid, String componentName) {
        Query boolQuery = Query.of(q -> q.bool(b -> b
                .filter(f -> f.term(t -> t
                        .field(OpenSearchField.PROJECT_UUID_KEYWORD.getFieldName())
                        .value(v -> v.stringValue(projectUuid))))
                .filter(f -> f.term(t -> t
                        .field(OpenSearchField.SOURCE_TYPE.getFieldName())
                        .value(v -> v.stringValue(SourceType.BACKEND.getType()))))
                .filter(f -> f.term(t -> t
                        .field(OpenSearchField.COMPONENT_NAME_KEYWORD.getFieldName())
                        .value(v -> v.stringValue(componentName))))
        ));

        return SearchRequest.of(s -> s
                .index(OpenSearchUtils.getProjectIndexPattern(projectUuid))
                .size(0)
                .query(boolQuery)
                .aggregations(TOTAL_CALLS, a -> a
                        .cardinality(c -> c.field(OpenSearchField.TRACE_ID.getFieldName())))
                .aggregations(ERROR_TRACES, a -> a
                        .filter(f -> f.term(t -> t.field(OpenSearchField.LOG_LEVEL.getFieldName())
                                .value(v -> v.stringValue(LogLevel.ERROR.getLevel()))))
                        .aggregations(ERROR_COUNT, sub -> sub.cardinality(
                                c -> c.field(OpenSearchField.TRACE_ID.getFieldName()))))
                .aggregations(WARN_TRACES, a -> a
                        .filter(f -> f.term(t -> t.field(OpenSearchField.LOG_LEVEL.getFieldName())
                                .value(v -> v.stringValue(LogLevel.WARN.getLevel()))))
                        .aggregations(WARN_COUNT, sub -> sub.cardinality(
                                c -> c.field(OpenSearchField.TRACE_ID.getFieldName()))))
        );
    }

    private SearchRequest buildFrontendMetricsRequest(String projectUuid) {
        Query baseQuery = Query.of(q -> q.bool(b -> b
                .filter(f -> f.term(t -> t.field(OpenSearchField.PROJECT_UUID_KEYWORD.getFieldName())
                        .value(v -> v.stringValue(projectUuid))))
                .filter(f -> f.term(t -> t.field(OpenSearchField.SOURCE_TYPE.getFieldName())
                        .value(v -> v.stringValue(SourceType.FRONTEND.getType()))))
        ));

        return SearchRequest.of(s -> s
                .index(OpenSearchUtils.getProjectIndexPattern(projectUuid))
                .size(0)
                .query(baseQuery)
                .aggregations("total_traces", a -> a
                        .cardinality(c -> c.field(OpenSearchField.TRACE_ID.getFieldName())))
                .aggregations("error_traces", a -> a
                        .filter(f -> f.term(t -> t.field(OpenSearchField.LOG_LEVEL.getFieldName())
                                .value(v -> v.stringValue(LogLevel.ERROR.getLevel()))))
                        .aggregations("count", sub -> sub.cardinality(
                                c -> c.field(OpenSearchField.TRACE_ID.getFieldName()))))
                .aggregations("warn_traces", a -> a
                        .filter(f -> f.bool(b -> b
                                .must(m -> m.term(t -> t.field(OpenSearchField.LOG_LEVEL.getFieldName())
                                        .value(v -> v.stringValue(LogLevel.WARN.getLevel()))))
                                .mustNot(mn -> mn.term(t -> t.field(OpenSearchField.LOG_LEVEL.getFieldName())
                                        .value(v -> v.stringValue(LogLevel.ERROR.getLevel()))))))
                        .aggregations("count", sub -> sub.cardinality(
                                c -> c.field(OpenSearchField.TRACE_ID.getFieldName()))))
        );
    }

    // -------------------------------
    // Aggregation Parsing + Debug Logs
    // -------------------------------

    private Map<String, MetricsData> parseProjectMetricsResponse(SearchResponse<Void> response) {
        Map<String, MetricsData> metricsMap = new HashMap<>();

        Aggregate byComponentAgg = response.aggregations().get(BY_COMPONENT);

        if (byComponentAgg == null || !byComponentAgg.isSterms()) {
            log.warn("{} ❗ by_component aggregation 없음 또는 타입 불일치", LOG_PREFIX);
            return metricsMap;
        }

        StringTermsAggregate termsAgg = byComponentAgg.sterms();

        log.debug("{} 🔍 Component bucket count = {}", LOG_PREFIX, termsAgg.buckets().array().size());

        for (StringTermsBucket bucket : termsAgg.buckets().array()) {

            log.debug("{} ▶ Component: {}, docCount={}",
                    LOG_PREFIX, bucket.key(), bucket.docCount());

            bucket.aggregations().forEach((key, agg) ->
                    log.debug("{}    ├─ subAgg {}: type={}", LOG_PREFIX, key, agg._kind()));

            int totalCalls = extractCardinalityDebug(bucket.aggregations(), TOTAL_CALLS, bucket.key());
            int errorCount = extractNestedCardinalityDebug(bucket.aggregations(), ERROR_TRACES, ERROR_COUNT,
                    bucket.key());
            int warnCount = extractNestedCardinalityDebug(bucket.aggregations(), WARN_TRACES, WARN_COUNT, bucket.key());

            MetricsData metricsData = MetricsData.of(totalCalls, errorCount, warnCount);
            metricsMap.put(bucket.key(), metricsData);

            log.debug("{} Component Result → {}: calls={}, error={}, warn={}",
                    LOG_PREFIX, bucket.key(), totalCalls, errorCount, warnCount);
        }

        return metricsMap;
    }

    private MetricsData parseComponentMetricsResponse(SearchResponse<Void> response) {
        Map<String, Aggregate> aggs = response.aggregations();

        int totalCalls = extractCardinalityDebug(aggs, TOTAL_CALLS, "component");
        int errorCount = extractNestedCardinalityDebug(aggs, ERROR_TRACES, ERROR_COUNT, "component");
        int warnCount = extractNestedCardinalityDebug(aggs, WARN_TRACES, WARN_COUNT, "component");

        return MetricsData.of(totalCalls, errorCount, warnCount);
    }

    private FrontendMetricsSummary parseFrontendMetricsResponse(SearchResponse<Void> response) {
        Map<String, Aggregate> aggs = response.aggregations();

        int totalTraces = extractCardinalityDebug(aggs, TOTAL_TRACES, "FE");
        int errorTraces = extractNestedCardinalityDebug(aggs, "error_traces", "count", "FE");
        int warnTraces = extractNestedCardinalityDebug(aggs, "warn_traces", "count", "FE");

        int infoTraces = totalTraces - errorTraces - warnTraces;

        return FrontendMetricsSummary.of(totalTraces, infoTraces, warnTraces, errorTraces);
    }

    // -------------------------------
    // Debug Extractor Helpers
    // -------------------------------

    private int extractCardinalityDebug(Map<String, Aggregate> aggs, String name, String comp) {
        Aggregate agg = aggs.get(name);

        if (agg == null) {
            log.warn("{} ❗ [{}] '{}' agg 없음", LOG_PREFIX, comp, name);
            return 0;
        }

        log.debug("{}    → [{}] '{}' agg type={}", LOG_PREFIX, comp, name, agg._kind());

        if (agg.isCardinality()) {
            int value = (int) agg.cardinality().value();
            log.debug("{}    ✔ [{}] '{}' = {}", LOG_PREFIX, comp, name, value);
            return value;
        }

        log.warn("{} ❗ '{}' cardinality 아님: type={}", LOG_PREFIX, name, agg._kind());
        return 0;
    }

    private int extractNestedCardinalityDebug(
            Map<String, Aggregate> aggs, String filterName, String innerName, String comp) {

        Aggregate filter = aggs.get(filterName);

        if (filter == null) {
            log.warn("{} ❗ [{}] filter '{}' 없음", LOG_PREFIX, comp, filterName);
            return 0;
        }

        log.debug("{}    → [{}] '{}' filter type={}", LOG_PREFIX, comp, filterName, filter._kind());

        if (!filter.isFilter()) {
            log.warn("{} ❗ '{}' filter 아님", LOG_PREFIX, filterName);
            return 0;
        }

        Map<String, Aggregate> nested = filter.filter().aggregations();

        log.debug("{}      └ Nested keys under '{}': {}", LOG_PREFIX, filterName, nested.keySet());

        return extractCardinalityDebug(nested, innerName, comp);
    }
}
