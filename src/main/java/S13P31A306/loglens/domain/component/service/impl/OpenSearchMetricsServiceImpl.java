package S13P31A306.loglens.domain.component.service.impl;

import S13P31A306.loglens.domain.component.dto.MetricsData;
import S13P31A306.loglens.domain.component.service.OpenSearchMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.aggregations.*;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenSearchMetricsServiceImpl implements OpenSearchMetricsService {

    private static final String LOG_PREFIX = "[OpenSearchMetricsService]";
    private static final String TRACE_ID_FIELD = "trace_id.keyword";
    private static final String COMPONENT_NAME_FIELD = "name.keyword";
    private static final String PROJECT_UUID_FIELD = "project_uuid.keyword";
    private static final String LOG_LEVEL_FIELD = "level.keyword";
    private static final String ERROR_LEVEL = "ERROR";
    private static final String WARN_LEVEL = "WARN";
    private static final int MAX_AGGREGATION_SIZE = 10000;

    private final OpenSearchClient openSearchClient;

    @Value("${opensearch.index.logs:logs-*}")
    private String logsIndexPattern;

    @Override
    public Map<String, MetricsData> getProjectMetrics(String projectUuid) {
        log.debug("{} 프로젝트 메트릭 조회 시작: projectUuid={}", LOG_PREFIX, projectUuid);

        try {
            SearchRequest searchRequest = buildProjectMetricsRequest(projectUuid);
            SearchResponse<Void> response = openSearchClient.search(searchRequest, Void.class);

            Map<String, MetricsData> metricsMap = parseProjectMetricsResponse(response);

            log.debug("{} 프로젝트 메트릭 조회 완료: projectUuid={}, components={}",
                    LOG_PREFIX, projectUuid, metricsMap.size());

            return metricsMap;

        } catch (IOException e) {
            log.error("{} OpenSearch 조회 실패: projectUuid={}", LOG_PREFIX, projectUuid, e);
            return new HashMap<>();
        } catch (Exception e) {
            log.error("{} 예상치 못한 오류 발생: projectUuid={}", LOG_PREFIX, projectUuid, e);
            return new HashMap<>();
        }
    }

    @Override
    public MetricsData getComponentMetrics(String projectUuid, String componentName) {
        log.debug("{} 컴포넌트 메트릭 조회: projectUuid={}, component={}",
                LOG_PREFIX, projectUuid, componentName);

        try {
            SearchRequest searchRequest = buildComponentMetricsRequest(projectUuid, componentName);
            SearchResponse<Void> response = openSearchClient.search(searchRequest, Void.class);

            MetricsData metricsData = parseComponentMetricsResponse(response);

            log.debug("{} 컴포넌트 메트릭 조회 완료: component={}, calls={}, errors={}",
                    LOG_PREFIX, componentName, metricsData.totalCalls(), metricsData.errorCount());

            return metricsData;

        } catch (IOException e) {
            log.error("{} OpenSearch 조회 실패: projectUuid={}, component={}",
                    LOG_PREFIX, projectUuid, componentName, e);
            return MetricsData.empty();
        } catch (Exception e) {
            log.error("{} 예상치 못한 오류 발생: projectUuid={}, component={}",
                    LOG_PREFIX, projectUuid, componentName, e);
            return MetricsData.empty();
        }
    }

    /**
     * 프로젝트 전체 메트릭 조회 쿼리 생성
     */
    private SearchRequest buildProjectMetricsRequest(String projectUuid) {
        // 프로젝트 필터 쿼리
        Query projectFilter = Query.of(q -> q
                .term(t -> t
                        .field(PROJECT_UUID_FIELD)
                        .value(v -> v.stringValue(projectUuid))
                )
        );

        Query errorFilter = Query.of(q -> q
                .term(t -> t
                        .field(LOG_LEVEL_FIELD)
                        .value(v -> v.stringValue(ERROR_LEVEL))
                )
        );

        Query warnFilter = Query.of(q -> q
                .term(t -> t
                        .field(LOG_LEVEL_FIELD)
                        .value(v -> v.stringValue(WARN_LEVEL))
                )
        );

        return SearchRequest.of(s -> s
                .index(logsIndexPattern)
                .size(0)
                .query(projectFilter)  // ✅ 직접 사용
                .aggregations("by_component", a -> a
                        .terms(t -> t
                                .field(COMPONENT_NAME_FIELD)
                                .size(MAX_AGGREGATION_SIZE)
                        )
                        .aggregations("total_calls", sub -> sub
                                .cardinality(c -> c.field(TRACE_ID_FIELD))
                        )
                        .aggregations("error_traces", sub -> sub
                                .filter(errorFilter)  // ✅ Query 객체 직접 전달
                                .aggregations("error_count", subsub -> subsub
                                        .cardinality(c -> c.field(TRACE_ID_FIELD))
                                )
                        )
                        .aggregations("warn_traces", sub -> sub
                                .filter(warnFilter)  // ✅ Query 객체 직접 전달
                                .aggregations("warn_count", subsub -> subsub
                                        .cardinality(c -> c.field(TRACE_ID_FIELD))
                                )
                        )
                )
        );
    }

    /**
     * 단일 컴포넌트 메트릭 조회 쿼리 생성
     */
    private SearchRequest buildComponentMetricsRequest(String projectUuid, String componentName) {
        Query boolQuery = Query.of(q -> q
                .bool(b -> b
                        .filter(f -> f
                                .term(t -> t
                                        .field(PROJECT_UUID_FIELD)
                                        .value(v -> v.stringValue(projectUuid))
                                )
                        )
                        .filter(f -> f
                                .term(t -> t
                                        .field(COMPONENT_NAME_FIELD)
                                        .value(v -> v.stringValue(componentName))
                                )
                        )
                )
        );

        Query errorFilter = Query.of(q -> q
                .term(t -> t
                        .field(LOG_LEVEL_FIELD)
                        .value(v -> v.stringValue(ERROR_LEVEL))
                )
        );

        Query warnFilter = Query.of(q -> q
                .term(t -> t
                        .field(LOG_LEVEL_FIELD)
                        .value(v -> v.stringValue(WARN_LEVEL))
                )
        );

        return SearchRequest.of(s -> s
                .index(logsIndexPattern)
                .size(0)
                .query(boolQuery)
                .aggregations("total_calls", a -> a
                        .cardinality(c -> c.field(TRACE_ID_FIELD))
                )
                .aggregations("error_traces", a -> a
                        .filter(errorFilter)  // ✅ Query 객체 직접 전달
                        .aggregations("error_count", sub -> sub
                                .cardinality(c -> c.field(TRACE_ID_FIELD))
                        )
                )
                .aggregations("warn_traces", a -> a
                        .filter(warnFilter)  // ✅ Query 객체 직접 전달
                        .aggregations("warn_count", sub -> sub
                                .cardinality(c -> c.field(TRACE_ID_FIELD))
                        )
                )
        );
    }

    /**
     * 프로젝트 메트릭 응답 파싱
     */
    private Map<String, MetricsData> parseProjectMetricsResponse(SearchResponse<Void> response) {
        Map<String, MetricsData> metricsMap = new HashMap<>();

        // by_component aggregation 파싱
        Aggregate byComponentAgg = response.aggregations().get("by_component");
        if (byComponentAgg == null || !byComponentAgg.isSterms()) {
            log.warn("{} by_component aggregation이 없거나 타입이 맞지 않습니다", LOG_PREFIX);
            return metricsMap;
        }

        StringTermsAggregate termsAgg = byComponentAgg.sterms();

        for (StringTermsBucket bucket : termsAgg.buckets().array()) {
            String componentName = bucket.key();

            // 총 호출 수
            int totalCalls = extractCardinalityValue(bucket.aggregations(), "total_calls");

            // ERROR 개수
            int errorCount = extractNestedCardinalityValue(
                    bucket.aggregations(), "error_traces", "error_count"
            );

            // WARN 개수
            int warnCount = extractNestedCardinalityValue(
                    bucket.aggregations(), "warn_traces", "warn_count"
            );

            MetricsData metricsData = MetricsData.of(totalCalls, errorCount, warnCount);
            metricsMap.put(componentName, metricsData);

            log.trace("{} 컴포넌트 메트릭 파싱: {} - calls={}, errors={}, warns={}",
                    LOG_PREFIX, componentName, totalCalls, errorCount, warnCount);
        }

        return metricsMap;
    }

    /**
     * 단일 컴포넌트 메트릭 응답 파싱
     */
    private MetricsData parseComponentMetricsResponse(SearchResponse<Void> response) {
        Map<String, Aggregate> aggregations = response.aggregations();

        // 총 호출 수
        int totalCalls = extractCardinalityValue(aggregations, "total_calls");

        // ERROR 개수
        int errorCount = extractNestedCardinalityValue(aggregations, "error_traces", "error_count");

        // WARN 개수
        int warnCount = extractNestedCardinalityValue(aggregations, "warn_traces", "warn_count");

        return MetricsData.of(totalCalls, errorCount, warnCount);
    }

    /**
     * Cardinality aggregation 값 추출 (직접)
     */
    private int extractCardinalityValue(Map<String, Aggregate> aggregations, String aggName) {
        Aggregate agg = aggregations.get(aggName);
        if (agg != null && agg.isCardinality()) {
            return (int) agg.cardinality().value();
        }
        log.warn("{} {} aggregation을 찾을 수 없거나 타입이 맞지 않습니다", LOG_PREFIX, aggName);
        return 0;
    }

    /**
     * Nested cardinality aggregation 값 추출 (filter > cardinality)
     */
    private int extractNestedCardinalityValue(
            Map<String, Aggregate> aggregations,
            String filterAggName,
            String cardinalityAggName) {

        Aggregate filterAgg = aggregations.get(filterAggName);
        if (filterAgg == null || !filterAgg.isFilter()) {
            log.warn("{} {} filter aggregation을 찾을 수 없습니다", LOG_PREFIX, filterAggName);
            return 0;
        }

        Map<String, Aggregate> subAggregations = filterAgg.filter().aggregations();
        return extractCardinalityValue(subAggregations, cardinalityAggName);
    }
}
