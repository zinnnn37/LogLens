package S13P31A306.loglens.domain.dashboard.repository.impl;

import static S13P31A306.loglens.global.constants.GlobalErrorCode.OPENSEARCH_OPERATION_FAILED;

import S13P31A306.loglens.domain.component.constants.OpenSearchField;
import S13P31A306.loglens.domain.dashboard.dto.opensearch.ErrorAggregation;
import S13P31A306.loglens.domain.dashboard.dto.opensearch.ErrorStatistics;
import S13P31A306.loglens.domain.dashboard.repository.DashboardRepository;
import S13P31A306.loglens.global.exception.BusinessException;
import S13P31A306.loglens.global.utils.OpenSearchUtils;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DashboardRepositoryImpl implements DashboardRepository {

    private static final String LOG_PREFIX = "[DashboardRepository]";
    private final OpenSearchClient openSearchClient;

    // ================================================================================================================ 
    // Public Methods
    // ================================================================================================================ 

    @Override
    public List<ErrorAggregation> findTopErrors(String projectUuid, LocalDateTime start, LocalDateTime end,
                                                Integer limit) {
        log.info("{} Top {} 에러 집계 쿼리 시작: projectUuid={}", LOG_PREFIX, limit, projectUuid);
        try {
            SearchRequest request = buildTopErrorsSearchRequest(projectUuid, start, end, limit);
            SearchResponse<Void> response = openSearchClient.search(request, Void.class);
            List<ErrorAggregation> result = parseTopErrorsResponse(response);
            log.debug("{} Top {} 에러 집계 완료: {}개 조회", LOG_PREFIX, limit, result.size());
            return result;
        } catch (IOException e) {
            log.error("{} OpenSearch 에러 집계 쿼리 실패", LOG_PREFIX, e);
            throw new BusinessException(OPENSEARCH_OPERATION_FAILED);
        }
    }

    @Override
    public ErrorStatistics findErrorStatistics(String projectUuid, LocalDateTime start, LocalDateTime end) {
        log.debug("{} 에러 통계 쿼리 시작: projectUuid={}", LOG_PREFIX, projectUuid);
        try {
            SearchRequest request = buildErrorStatisticsSearchRequest(projectUuid, start, end);
            SearchResponse<Void> response = openSearchClient.search(request, Void.class);
            ErrorStatistics statistics = parseErrorStatisticsResponse(response);
            log.debug("{} 에러 통계 조회 완료: totalErrors={}, uniqueErrorTypes={}",
                    LOG_PREFIX, statistics.totalErrors(), statistics.uniqueErrorTypes());
            return statistics;
        } catch (IOException e) {
            log.error("{} OpenSearch 에러 통계 조회 실패", LOG_PREFIX, e);
            throw new BusinessException(OPENSEARCH_OPERATION_FAILED);
        }
    }

    // ================================================================================================================ 
    // Request Builders
    // ================================================================================================================ 

    private SearchRequest buildTopErrorsSearchRequest(String projectUuid, LocalDateTime start, LocalDateTime end,
                                                      Integer limit) {
        return SearchRequest.of(s -> s
                .index(OpenSearchUtils.getProjectIndexPattern(projectUuid))
                .size(0)
                .query(buildErrorLogQuery(start, end))
                .aggregations("by_error_type", a -> a
                        .terms(t -> t
                                .field(OpenSearchField.LOGGER.getFieldName())
                                .size(limit))
                        .aggregations("first_occurrence", a2 -> a2
                                .min(m -> m.field(OpenSearchField.TIMESTAMP.getFieldName())))
                        .aggregations("last_occurrence", a2 -> a2
                                .max(m -> m.field(OpenSearchField.TIMESTAMP.getFieldName())))
                        .aggregations("sample_data", a2 -> a2
                                .topHits(th -> th
                                        .size(1)
                                        .source(src -> src.filter(f -> f
                                                .includes(List.of(
                                                        OpenSearchField.MESSAGE.getFieldName(),
                                                        OpenSearchField.LOGGER.getFieldName(),
                                                        OpenSearchField.STACKTRACE.getFieldName()
                                                )))))))
        );
    }

    private SearchRequest buildErrorStatisticsSearchRequest(String projectUuid, LocalDateTime start,
                                                            LocalDateTime end) {
        return SearchRequest.of(s -> s
                .index(OpenSearchUtils.getProjectIndexPattern(projectUuid))
                .size(0)
                .trackTotalHits(t -> t.enabled(true))  // 10,000건 제한 해제
                .query(buildErrorLogQuery(start, end))
                .aggregations("unique_types", a -> a
                        .cardinality(c -> c.field(OpenSearchField.LOGGER.getFieldName())))
        );
    }

    private Query buildErrorLogQuery(LocalDateTime start, LocalDateTime end) {
        String startStr = start.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        String endStr = end.atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);

        return Query.of(q -> q.bool(b -> b
                .must(m -> m.term(t -> t
                        .field(OpenSearchField.LOG_LEVEL.getFieldName())
                        .value(FieldValue.of("ERROR"))))
                .must(m -> m.range(r -> r
                        .field(OpenSearchField.TIMESTAMP.getFieldName())
                        .gte(JsonData.of(startStr))
                        .lte(JsonData.of(endStr))))));
    }

    // ================================================================================================================ 
    // Response Parsers
    // ================================================================================================================ 

    private List<ErrorAggregation> parseTopErrorsResponse(SearchResponse<Void> response) {
        log.debug("{} Top Errors 응답 파싱 시작", LOG_PREFIX);
        List<ErrorAggregation> result = new ArrayList<>();
        Map<String, Aggregate> aggregations = response.aggregations();

        if (Objects.isNull(aggregations) || !aggregations.containsKey("by_error_type")) {
            log.debug("{} 'by_error_type' aggregation 결과 없음", LOG_PREFIX);
            return result;
        }

        Aggregate aggregation = aggregations.get("by_error_type");
        if (Objects.isNull(aggregation) || Objects.isNull(aggregation.sterms())) {
            log.debug("{} sterms aggregation 결과 없음", LOG_PREFIX);
            return result;
        }

        List<StringTermsBucket> buckets = aggregation.sterms().buckets().array();
        if (buckets.isEmpty()) {
            log.debug("{} buckets 비어있음", LOG_PREFIX);
            return result;
        }

        for (StringTermsBucket bucket : buckets) {
            buildErrorAggregationFromBucket(bucket).ifPresent(result::add);
        }

        log.debug("{} Top Errors 응답 파싱 완료: {}개", LOG_PREFIX, result.size());
        return result;
    }

    private ErrorStatistics parseErrorStatisticsResponse(SearchResponse<Void> response) {
        int totalErrors = 0;
        if (response.hits().total() != null) {
            totalErrors = Math.toIntExact(response.hits().total().value());
        }

        int uniqueTypes = 0;
        Map<String, Aggregate> aggregations = response.aggregations();
        if (aggregations != null && aggregations.containsKey("unique_types")) {
            Aggregate uniqueTypesAgg = aggregations.get("unique_types");
            if (uniqueTypesAgg != null && uniqueTypesAgg.cardinality() != null) {
                uniqueTypes = (int) uniqueTypesAgg.cardinality().value();
            }
        }
        return new ErrorStatistics(totalErrors, uniqueTypes);
    }

    private java.util.Optional<ErrorAggregation> buildErrorAggregationFromBucket(StringTermsBucket bucket) {
        try {
            String exceptionType = bucket.key();
            Integer count = (int) bucket.docCount();

            LocalDateTime firstOccurrence = parseTimestamp(
                    bucket.aggregations().get("first_occurrence").min().valueAsString());
            LocalDateTime lastOccurrence = parseTimestamp(
                    bucket.aggregations().get("last_occurrence").max().valueAsString());

            if (Objects.isNull(firstOccurrence) || Objects.isNull(lastOccurrence)) {
                log.warn("{} timestamp 파싱 실패로 버킷 건너뜀: key={}", LOG_PREFIX, bucket.key());
                return java.util.Optional.empty();
            }

            List<Hit<JsonData>> hits = bucket.aggregations().get("sample_data").topHits().hits().hits();
            if (hits.isEmpty()) {
                return java.util.Optional.empty();
            }

            JsonData source = hits.get(0).source();
            String message = extractField(source, OpenSearchField.MESSAGE.getFieldName());
            String stackTrace = extractStackTraceFirstLine(source);
            String logger = extractField(source, OpenSearchField.LOGGER.getFieldName());

            return java.util.Optional.of(new ErrorAggregation(
                    exceptionType, message, count, firstOccurrence, lastOccurrence, stackTrace, logger
            ));
        } catch (Exception e) {
            log.error("{} 버킷 파싱 중 오류 발생: key={}", LOG_PREFIX, bucket.key(), e);
            return java.util.Optional.empty();
        }
    }

    // ================================================================================================================ 
    // Utility Methods
    // ================================================================================================================
    private LocalDateTime parseTimestamp(String timestamp) {
        if (timestamp == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            log.error("{} timestamp 파싱 실패: time={}", LOG_PREFIX, timestamp, e);
            return null;
        }
    }

    private String extractField(JsonData source, String fieldName) {
        if (source == null || fieldName == null) {
            return "";
        }
        try {
            return source.toJson().asJsonObject().getString(fieldName, "");
        } catch (Exception e) {
            log.error("{} 필드 추출 실패: field={}", LOG_PREFIX, fieldName, e);
            return "";
        }
    }

    private String extractStackTraceFirstLine(JsonData source) {
        if (source == null) {
            return "";
        }
        try {
            String fullStackTrace = source.toJson().asJsonObject()
                    .getString(OpenSearchField.STACKTRACE.getFieldName(), "");
            if (fullStackTrace.isBlank()) {
                return "";
            }
            int firstNewLine = fullStackTrace.indexOf('\n');
            return firstNewLine > 0 ? fullStackTrace.substring(0, firstNewLine) : fullStackTrace;
        } catch (Exception e) {
            log.error("{} Stack Trace 추출 실패", LOG_PREFIX, e);
            return "";
        }
    }
}
