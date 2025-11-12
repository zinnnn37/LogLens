package S13P31A306.loglens.domain.component.service.impl;

import static S13P31A306.loglens.domain.component.constants.OpenSearchAggregation.Name.BY_COMPONENT;
import static S13P31A306.loglens.domain.component.constants.OpenSearchAggregation.Name.ERROR_COUNT;
import static S13P31A306.loglens.domain.component.constants.OpenSearchAggregation.Name.ERROR_LOGS;
import static S13P31A306.loglens.domain.component.constants.OpenSearchAggregation.Name.ERROR_TRACES;
import static S13P31A306.loglens.domain.component.constants.OpenSearchAggregation.Name.INFO_LOGS;
import static S13P31A306.loglens.domain.component.constants.OpenSearchAggregation.Name.TOTAL_CALLS;
import static S13P31A306.loglens.domain.component.constants.OpenSearchAggregation.Name.TOTAL_TRACES;
import static S13P31A306.loglens.domain.component.constants.OpenSearchAggregation.Name.WARN_COUNT;
import static S13P31A306.loglens.domain.component.constants.OpenSearchAggregation.Name.WARN_LOGS;
import static S13P31A306.loglens.domain.component.constants.OpenSearchAggregation.Name.WARN_TRACES;

import S13P31A306.loglens.domain.component.constants.LogLevel;
import S13P31A306.loglens.domain.component.constants.OpenSearchAggregation;
import S13P31A306.loglens.domain.component.constants.OpenSearchField;
import S13P31A306.loglens.domain.component.constants.SourceType;
import S13P31A306.loglens.domain.component.dto.MetricsData;
import S13P31A306.loglens.domain.component.service.OpenSearchMetricsService;
import S13P31A306.loglens.domain.dashboard.dto.FrontendMetricsSummary;
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
            SearchResponse<Void> response = openSearchClient.search(searchRequest, Void.class);

            Map<String, MetricsData> metricsMap = parseProjectMetricsResponse(response);

            log.debug("{} 프로젝트 메트릭 조회 완료: projectUuid={}, components={}",
                    LOG_PREFIX, projectUuid, metricsMap.size());

            return metricsMap;

        } catch (IOException e) {
            log.error("{} OpenSearch 조회 실패 - 커넥션 풀 상태 확인 필요: projectUuid={}, errorType={}, message={}",
                    LOG_PREFIX, projectUuid, e.getClass().getSimpleName(), e.getMessage());
            log.debug("{} OpenSearch IOException 상세 스택:", LOG_PREFIX, e);
            return new HashMap<>();
        } catch (Exception e) {
            log.error("{} 예상치 못한 오류 발생: projectUuid={}, errorType={}, message={}",
                    LOG_PREFIX, projectUuid, e.getClass().getSimpleName(), e.getMessage());
            log.debug("{} 예상치 못한 오류 상세 스택:", LOG_PREFIX, e);
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
            log.error("{} OpenSearch 조회 실패 - 커넥션 풀 상태 확인 필요: projectUuid={}, component={}, errorType={}, message={}",
                    LOG_PREFIX, projectUuid, componentName, e.getClass().getSimpleName(), e.getMessage());
            log.debug("{} OpenSearch IOException 상세 스택:", LOG_PREFIX, e);
            return MetricsData.empty();
        } catch (Exception e) {
            log.error("{} 예상치 못한 오류 발생: projectUuid={}, component={}, errorType={}, message={}",
                    LOG_PREFIX, projectUuid, componentName, e.getClass().getSimpleName(), e.getMessage());
            log.debug("{} 예상치 못한 오류 상세 스택:", LOG_PREFIX, e);
            return MetricsData.empty();
        }
    }

    @Override
    public FrontendMetricsSummary getFrontendMetrics(String projectUuid) {
        log.debug("{} Frontend 메트릭 조회 시작: projectUuid={}", LOG_PREFIX, projectUuid);

        try {
            SearchRequest searchRequest = buildFrontendMetricsRequest(projectUuid);
            SearchResponse<Void> response = openSearchClient.search(searchRequest, Void.class);

            FrontendMetricsSummary summary = parseFrontendMetricsResponse(response);

            log.debug("{} Frontend 메트릭 조회 완료: projectUuid={}, traces={}, errors={}",
                    LOG_PREFIX, projectUuid, summary.totalTraces(), summary.totalErrorLogs());

            return summary;

        } catch (IOException e) {
            log.error("{} ⚠️ Frontend 메트릭 조회 실패 - 커넥션 풀 상태 확인 필요: projectUuid={}, errorType={}, message={}",
                    LOG_PREFIX, projectUuid, e.getClass().getSimpleName(), e.getMessage());
            log.debug("{} OpenSearch IOException 상세 스택:", LOG_PREFIX, e);
            return FrontendMetricsSummary.empty();
        } catch (Exception e) {
            log.error("{} ⚠️ Frontend 메트릭 예상치 못한 오류: projectUuid={}, errorType={}, message={}",
                    LOG_PREFIX, projectUuid, e.getClass().getSimpleName(), e.getMessage());
            log.debug("{} 예상치 못한 오류 상세 스택:", LOG_PREFIX, e);
            return FrontendMetricsSummary.empty();
        }
    }

    /**
     * 프로젝트 전체 메트릭 조회 쿼리 생성 (Backend만)
     */
    private SearchRequest buildProjectMetricsRequest(String projectUuid) {
        Query boolQuery = Query.of(q -> q
                .bool(b -> b
                        .filter(f -> f.term(t -> t
                                .field(OpenSearchField.PROJECT_UUID_KEYWORD.getFieldName())
                                .value(v -> v.stringValue(projectUuid))
                        ))
                        .filter(f -> f.term(t -> t
                                .field(OpenSearchField.SOURCE_TYPE.getFieldName())
                                .value(v -> v.stringValue(SourceType.BACKEND.getType()))
                        ))
                )
        );

        Query errorFilter = Query.of(q -> q.term(t -> t
                .field(OpenSearchField.LOG_LEVEL.getFieldName())
                .value(v -> v.stringValue(LogLevel.ERROR.getLevel()))
        ));

        Query warnFilter = Query.of(q -> q.term(t -> t
                .field(OpenSearchField.LOG_LEVEL.getFieldName())
                .value(v -> v.stringValue(LogLevel.WARN.getLevel()))
        ));

        return SearchRequest.of(s -> s
                .index(getProjectIndexPattern(projectUuid))
                .size(0)
                .query(boolQuery)
                .aggregations(BY_COMPONENT, a -> a
                        .terms(t -> t
                                .field(OpenSearchField.COMPONENT_NAME.getFieldName())
                                .size(OpenSearchAggregation.MAX_SIZE)
                        )
                        .aggregations(TOTAL_CALLS, sub -> sub
                                .cardinality(c -> c.field(OpenSearchField.TRACE_ID.getFieldName()))
                        )
                        .aggregations(ERROR_TRACES, sub -> sub
                                .filter(errorFilter)
                                .aggregations(ERROR_COUNT, subsub -> subsub
                                        .cardinality(c -> c.field(OpenSearchField.TRACE_ID.getFieldName()))
                                )
                        )
                        .aggregations(WARN_TRACES, sub -> sub
                                .filter(warnFilter)
                                .aggregations(WARN_COUNT, subsub -> subsub
                                        .cardinality(c -> c.field(OpenSearchField.TRACE_ID.getFieldName()))
                                )
                        )
                )
        );
    }

    /**
     * 프로젝트별 인덱스 패턴을 반환
     *
     * @param projectUuid 프로젝트 UUID (하이픈 포함)
     * @return "{projectUuid_with_underscores}_*" 형식의 인덱스 패턴
     */
    private String getProjectIndexPattern(String projectUuid) {
        String sanitizedUuid = projectUuid.replace("-", "_");
        return sanitizedUuid + "_*";
    }

    /**
     * 단일 컴포넌트 메트릭 조회 쿼리 생성 (Backend)
     */
    private SearchRequest buildComponentMetricsRequest(String projectUuid, String componentName) {
        Query boolQuery = Query.of(q -> q
                .bool(b -> b
                        .filter(f -> f.term(t -> t
                                .field(OpenSearchField.PROJECT_UUID_KEYWORD.getFieldName())
                                .value(v -> v.stringValue(projectUuid))
                        ))
                        .filter(f -> f.term(t -> t
                                .field(OpenSearchField.SOURCE_TYPE.getFieldName())
                                .value(v -> v.stringValue(SourceType.BACKEND.getType()))
                        ))
                        .filter(f -> f.term(t -> t
                                .field(OpenSearchField.COMPONENT_NAME.getFieldName())
                                .value(v -> v.stringValue(componentName))
                        ))
                )
        );

        Query errorFilter = Query.of(q -> q.term(t -> t
                .field(OpenSearchField.LOG_LEVEL.getFieldName())
                .value(v -> v.stringValue(LogLevel.ERROR.getLevel()))
        ));

        Query warnFilter = Query.of(q -> q.term(t -> t
                .field(OpenSearchField.LOG_LEVEL.getFieldName())
                .value(v -> v.stringValue(LogLevel.WARN.getLevel()))
        ));

        return SearchRequest.of(s -> s
                .index(getProjectIndexPattern(projectUuid))  // ✅ 수정: 프로젝트별 인덱스 패턴 사용
                .size(0)
                .query(boolQuery)
                .aggregations(TOTAL_CALLS, a -> a
                        .cardinality(c -> c.field(OpenSearchField.TRACE_ID.getFieldName()))
                )
                .aggregations(ERROR_TRACES, a -> a
                        .filter(errorFilter)
                        .aggregations(ERROR_COUNT, sub -> sub
                                .cardinality(c -> c.field(OpenSearchField.TRACE_ID.getFieldName()))
                        )
                )
                .aggregations(WARN_TRACES, a -> a
                        .filter(warnFilter)
                        .aggregations(WARN_COUNT, sub -> sub
                                .cardinality(c -> c.field(OpenSearchField.TRACE_ID.getFieldName()))
                        )
                )
        );
    }

    /**
     * Frontend 메트릭 조회 쿼리 생성
     */
    private SearchRequest buildFrontendMetricsRequest(String projectUuid) {
        Query boolQuery = Query.of(q -> q
                .bool(b -> b
                        .filter(f -> f.term(t -> t
                                .field(OpenSearchField.PROJECT_UUID_KEYWORD.getFieldName())
                                .value(v -> v.stringValue(projectUuid))
                        ))
                        .filter(f -> f.term(t -> t
                                .field(OpenSearchField.SOURCE_TYPE.getFieldName())
                                .value(v -> v.stringValue(SourceType.FRONTEND.getType()))
                        ))
                )
        );

        Query infoFilter = Query.of(q -> q.term(t -> t
                .field(OpenSearchField.LOG_LEVEL.getFieldName())
                .value(v -> v.stringValue(LogLevel.INFO.getLevel()))
        ));

        Query warnFilter = Query.of(q -> q.term(t -> t
                .field(OpenSearchField.LOG_LEVEL.getFieldName())
                .value(v -> v.stringValue(LogLevel.WARN.getLevel()))
        ));

        Query errorFilter = Query.of(q -> q.term(t -> t
                .field(OpenSearchField.LOG_LEVEL.getFieldName())
                .value(v -> v.stringValue(LogLevel.ERROR.getLevel()))
        ));

        return SearchRequest.of(s -> s
                .index(getProjectIndexPattern(projectUuid))  // ✅ 수정: 프로젝트별 인덱스 패턴 사용
                .size(0)
                .query(boolQuery)
                .aggregations(TOTAL_TRACES, a -> a
                        .cardinality(c -> c.field(OpenSearchField.TRACE_ID.getFieldName()))
                )
                .aggregations(INFO_LOGS, a -> a
                        .filter(infoFilter)
                )
                .aggregations(WARN_LOGS, a -> a
                        .filter(warnFilter)
                )
                .aggregations(ERROR_LOGS, a -> a
                        .filter(errorFilter)
                )
        );
    }

    /**
     * 프로젝트 메트릭 응답 파싱
     */
    private Map<String, MetricsData> parseProjectMetricsResponse(SearchResponse<Void> response) {
        Map<String, MetricsData> metricsMap = new HashMap<>();

        Aggregate byComponentAgg = response.aggregations().get(BY_COMPONENT);
        if (byComponentAgg == null || !byComponentAgg.isSterms()) {
            log.warn("{} by_component aggregation이 없거나 타입이 맞지 않습니다", LOG_PREFIX);
            return metricsMap;
        }

        StringTermsAggregate termsAgg = byComponentAgg.sterms();

        for (StringTermsBucket bucket : termsAgg.buckets().array()) {
            String componentName = bucket.key();

            int totalCalls = extractCardinalityValue(bucket.aggregations(), TOTAL_CALLS);
            int errorCount = extractNestedCardinalityValue(
                    bucket.aggregations(), ERROR_TRACES, ERROR_COUNT
            );
            int warnCount = extractNestedCardinalityValue(
                    bucket.aggregations(), WARN_TRACES, WARN_COUNT
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

        int totalCalls = extractCardinalityValue(aggregations, TOTAL_CALLS);
        int errorCount = extractNestedCardinalityValue(aggregations, ERROR_TRACES, ERROR_COUNT);
        int warnCount = extractNestedCardinalityValue(aggregations, WARN_TRACES, WARN_COUNT);

        return MetricsData.of(totalCalls, errorCount, warnCount);
    }

    /**
     * Frontend 메트릭 응답 파싱
     */
    private FrontendMetricsSummary parseFrontendMetricsResponse(SearchResponse<Void> response) {
        Map<String, Aggregate> aggregations = response.aggregations();

        int totalTraces = extractCardinalityValue(aggregations, TOTAL_TRACES);
        int infoLogs = extractFilterDocCount(aggregations, INFO_LOGS);
        int warnLogs = extractFilterDocCount(aggregations, WARN_LOGS);
        int errorLogs = extractFilterDocCount(aggregations, ERROR_LOGS);

        return FrontendMetricsSummary.of(totalTraces, infoLogs, warnLogs, errorLogs);
    }

    /**
     * Cardinality aggregation 값 추출
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

    /**
     * Filter aggregation의 docCount 추출
     */
    private int extractFilterDocCount(Map<String, Aggregate> aggregations, String filterAggName) {
        Aggregate filterAgg = aggregations.get(filterAggName);
        if (filterAgg != null && filterAgg.isFilter()) {
            return (int) filterAgg.filter().docCount();
        }
        log.warn("{} {} filter aggregation을 찾을 수 없습니다", LOG_PREFIX, filterAggName);
        return 0;
    }
}
