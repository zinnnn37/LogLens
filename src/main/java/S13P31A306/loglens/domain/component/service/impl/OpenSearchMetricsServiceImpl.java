package S13P31A306.loglens.domain.component.service.impl;

import S13P31A306.loglens.domain.component.dto.MetricsData;
import S13P31A306.loglens.domain.component.service.OpenSearchMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.bucket.filter.Filter;
import org.opensearch.search.aggregations.bucket.terms.Terms;
import org.opensearch.search.aggregations.metrics.Cardinality;
import org.opensearch.search.builder.SearchSourceBuilder;
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

    private final RestHighLevelClient openSearchClient;

    @Value("${opensearch.index.logs:logs-*}")
    private String logsIndexPattern;

    @Override
    public Map<String, MetricsData> getProjectMetrics(String projectUuid) {
        log.debug("{} 프로젝트 메트릭 조회 시작: projectUuid={}", LOG_PREFIX, projectUuid);

        try {
            SearchRequest searchRequest = buildProjectMetricsRequest(projectUuid);
            SearchResponse response = openSearchClient.search(searchRequest, RequestOptions.DEFAULT);

            Map<String, MetricsData> metricsMap = parseProjectMetricsResponse(response);

            log.debug("{} 프로젝트 메트릭 조회 완료: projectUuid={}, components={}",
                    LOG_PREFIX, projectUuid, metricsMap.size());

            return metricsMap;

        } catch (IOException e) {
            log.error("{} OpenSearch 조회 실패: projectUuid={}", LOG_PREFIX, projectUuid, e);
            return new HashMap<>();
        }
    }

    @Override
    public MetricsData getComponentMetrics(String projectUuid, String componentName) {
        log.debug("{} 컴포넌트 메트릭 조회: projectUuid={}, component={}",
                LOG_PREFIX, projectUuid, componentName);

        try {
            SearchRequest searchRequest = buildComponentMetricsRequest(projectUuid, componentName);
            SearchResponse response = openSearchClient.search(searchRequest, RequestOptions.DEFAULT);

            MetricsData metricsData = parseComponentMetricsResponse(response);

            log.debug("{} 컴포넌트 메트릭 조회 완료: component={}, calls={}, errors={}",
                    LOG_PREFIX, componentName, metricsData.totalCalls(), metricsData.errorCount());

            return metricsData;

        } catch (IOException e) {
            log.error("{} OpenSearch 조회 실패: projectUuid={}, component={}",
                    LOG_PREFIX, projectUuid, componentName, e);
            return MetricsData.empty();
        }
    }

    /**
     * 프로젝트 전체 메트릭 조회 쿼리 생성
     */
    private SearchRequest buildProjectMetricsRequest(String projectUuid) {
        BoolQueryBuilder baseQuery = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termQuery(PROJECT_UUID_FIELD, projectUuid));

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                .query(baseQuery)
                .size(0)  // 집계만 필요
                // 컴포넌트별로 그룹화
                .aggregation(
                        AggregationBuilders.terms("by_component")
                                .field(COMPONENT_NAME_FIELD)
                                .size(10000)  // 충분히 큰 값
                                // 각 컴포넌트의 총 호출 수 (고유 trace_id)
                                .subAggregation(
                                        AggregationBuilders.cardinality("total_calls")
                                                .field(TRACE_ID_FIELD)
                                )
                                // ERROR를 포함한 trace_id 개수
                                .subAggregation(
                                        AggregationBuilders.filter("error_traces",
                                                        QueryBuilders.termQuery(LOG_LEVEL_FIELD, ERROR_LEVEL))
                                                .subAggregation(
                                                        AggregationBuilders.cardinality("error_count")
                                                                .field(TRACE_ID_FIELD)
                                                )
                                )
                                // WARN을 포함한 trace_id 개수
                                .subAggregation(
                                        AggregationBuilders.filter("warn_traces",
                                                        QueryBuilders.termQuery(LOG_LEVEL_FIELD, WARN_LEVEL))
                                                .subAggregation(
                                                        AggregationBuilders.cardinality("warn_count")
                                                                .field(TRACE_ID_FIELD)
                                                )
                                )
                );

        SearchRequest searchRequest = new SearchRequest(logsIndexPattern);
        searchRequest.source(sourceBuilder);

        return searchRequest;
    }

    /**
     * 단일 컴포넌트 메트릭 조회 쿼리 생성
     */
    private SearchRequest buildComponentMetricsRequest(String projectUuid, String componentName) {
        BoolQueryBuilder baseQuery = QueryBuilders.boolQuery()
                .filter(QueryBuilders.termQuery(PROJECT_UUID_FIELD, projectUuid))
                .filter(QueryBuilders.termQuery(COMPONENT_NAME_FIELD, componentName));

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                .query(baseQuery)
                .size(0)
                // 총 호출 수
                .aggregation(
                        AggregationBuilders.cardinality("total_calls")
                                .field(TRACE_ID_FIELD)
                )
                // ERROR 포함 trace
                .aggregation(
                        AggregationBuilders.filter("error_traces",
                                        QueryBuilders.termQuery(LOG_LEVEL_FIELD, ERROR_LEVEL))
                                .subAggregation(
                                        AggregationBuilders.cardinality("error_count")
                                                .field(TRACE_ID_FIELD)
                                )
                )
                // WARN 포함 trace
                .aggregation(
                        AggregationBuilders.filter("warn_traces",
                                        QueryBuilders.termQuery(LOG_LEVEL_FIELD, WARN_LEVEL))
                                .subAggregation(
                                        AggregationBuilders.cardinality("warn_count")
                                                .field(TRACE_ID_FIELD)
                                )
                );

        SearchRequest searchRequest = new SearchRequest(logsIndexPattern);
        searchRequest.source(sourceBuilder);

        return searchRequest;
    }

    /**
     * 프로젝트 메트릭 응답 파싱
     */
    private Map<String, MetricsData> parseProjectMetricsResponse(SearchResponse response) {
        Map<String, MetricsData> metricsMap = new HashMap<>();

        Terms byComponent = response.getAggregations().get("by_component");

        for (Terms.Bucket bucket : byComponent.getBuckets()) {
            String componentName = bucket.getKeyAsString();

            // 총 호출 수
            Cardinality totalCallsAgg = bucket.getAggregations().get("total_calls");
            int totalCalls = (int) totalCallsAgg.getValue();

            // ERROR 개수
            Filter errorTracesAgg = bucket.getAggregations().get("error_traces");
            Cardinality errorCountAgg = errorTracesAgg.getAggregations().get("error_count");
            int errorCount = (int) errorCountAgg.getValue();

            // WARN 개수
            Filter warnTracesAgg = bucket.getAggregations().get("warn_traces");
            Cardinality warnCountAgg = warnTracesAgg.getAggregations().get("warn_count");
            int warnCount = (int) warnCountAgg.getValue();

            MetricsData metricsData = MetricsData.of(totalCalls, errorCount, warnCount);
            metricsMap.put(componentName, metricsData);
        }

        return metricsMap;
    }

    /**
     * 단일 컴포넌트 메트릭 응답 파싱
     */
    private MetricsData parseComponentMetricsResponse(SearchResponse response) {
        // 총 호출 수
        Cardinality totalCallsAgg = response.getAggregations().get("total_calls");
        int totalCalls = (int) totalCallsAgg.getValue();

        // ERROR 개수
        Filter errorTracesAgg = response.getAggregations().get("error_traces");
        Cardinality errorCountAgg = errorTracesAgg.getAggregations().get("error_count");
        int errorCount = (int) errorCountAgg.getValue();

        // WARN 개수
        Filter warnTracesAgg = response.getAggregations().get("warn_traces");
        Cardinality warnCountAgg = warnTracesAgg.getAggregations().get("warn_count");
        int warnCount = (int) warnCountAgg.getValue();

        return MetricsData.of(totalCalls, errorCount, warnCount);
    }
}
