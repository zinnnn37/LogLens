package S13P31A306.loglens.domain.dashboard.repository.impl;

import static S13P31A306.loglens.global.constants.GlobalErrorCode.OPENSEARCH_OPERATION_FAILED;

import S13P31A306.loglens.domain.component.constants.OpenSearchField;
import S13P31A306.loglens.domain.dashboard.dto.opensearch.ErrorAggregation;
import S13P31A306.loglens.domain.dashboard.dto.opensearch.ErrorStatistics;
import S13P31A306.loglens.domain.dashboard.repository.DashboardRepository;
import S13P31A306.loglens.global.exception.BusinessException;
import java.io.IOException;
import java.time.LocalDateTime;
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
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DashboardRepositoryImpl implements DashboardRepository {

    private static final String LOG_PREFIX = "[DashboardRepository]";
    private final OpenSearchClient openSearchClient;

    @Override
    public List<ErrorAggregation> findTopErrors(String projectUuid, LocalDateTime start, LocalDateTime end, Integer limit) {
        log.info("{} Top {} 에러 집계 쿼리 시작: projectUuid={}", LOG_PREFIX, limit, projectUuid);
        try {
            SearchRequest request = SearchRequest.of(s -> s
                    .index(projectUuid.replace('-', '_') + "_*")
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
                                                    .includes(List.of(OpenSearchField.MESSAGE.getFieldName(), OpenSearchField.LOGGER.getFieldName()))))))) 
            );
            SearchResponse<Void> response = openSearchClient.search(request, Void.class);
            List<ErrorAggregation> result = parseErrorAggregations(response);
            log.debug("{} Top {} 에러 집계 완료: {}개 조회", LOG_PREFIX, limit, result.size());
            return result;
        } catch (Exception e) {
            log.error("{} OpenSearch 에러 집계 쿼리 실패", LOG_PREFIX, e);
            throw new BusinessException(OPENSEARCH_OPERATION_FAILED);
        }
    }

    @Override
    public ErrorStatistics findErrorStatistics(String projectUuid, LocalDateTime start, LocalDateTime end) {
        log.debug("{} 에러 통계 쿼리 시작: projectUuid={}", LOG_PREFIX, projectUuid);
        try {
            SearchRequest request = SearchRequest.of(s -> s
                    .index(projectUuid.replace('-', '_') + "_*")
                    .size(0)
                    .query(buildErrorLogQuery(start, end))
                    .aggregations("unique_types", a -> a
                            .cardinality(c -> c.field(OpenSearchField.LOGGER.getFieldName())))
            );
            SearchResponse<Void> response = openSearchClient.search(request, Void.class);
            int totalErrors = 0;
            int uniqueTypes = 0;
            if (response.hits().total() != null) {
                totalErrors = Math.toIntExact(response.hits().total().value());
            }
            Map<String, Aggregate> aggregations = response.aggregations();
            if (aggregations != null && aggregations.containsKey("unique_types")) {
                Aggregate uniqueTypesAgg = aggregations.get("unique_types");
                if (uniqueTypesAgg != null && uniqueTypesAgg.cardinality() != null) {
                    uniqueTypes = (int) uniqueTypesAgg.cardinality().value();
                }
            }
            log.debug("{} 에러 통계 조회 완료: totalErrors={}, uniqueTypes={}",
                    LOG_PREFIX, totalErrors, uniqueTypes);
            return new ErrorStatistics(totalErrors, uniqueTypes);
        } catch (IOException e) {
            log.error("{} OpenSearch 에러 통계 조회 실패", LOG_PREFIX, e);
            throw new BusinessException(OPENSEARCH_OPERATION_FAILED);
        }
    }

    private Query buildErrorLogQuery(LocalDateTime start, LocalDateTime end) {
        String startStr = start.atZone(java.time.ZoneId.systemDefault())
                .withZoneSameInstant(java.time.ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT);
        String endStr = end.atZone(java.time.ZoneId.systemDefault())
                .withZoneSameInstant(java.time.ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_INSTANT);
        return Query.of(q -> q.bool(b -> b
                .must(m -> m.term(t -> t
                        .field(OpenSearchField.LOG_LEVEL.getFieldName())
                        .value(FieldValue.of("ERROR"))))
                .must(m -> m.range(r -> r
                        .field(OpenSearchField.TIMESTAMP.getFieldName())
                        .gte(JsonData.of(startStr))
                        .lte(JsonData.of(endStr))))));
    }

    private List<ErrorAggregation> parseErrorAggregations(SearchResponse<Void> response) {
        log.debug("{} OpenSearch 쿼리 결과 변환", LOG_PREFIX);
        List<ErrorAggregation> result = new ArrayList<>();
        try {
            Map<String, Aggregate> aggregations = response.aggregations();
            if (Objects.isNull(aggregations) || !aggregations.containsKey("by_error_type")) {
                log.debug("{} by_error_type aggregation 결과 없음", LOG_PREFIX);
                return result;
            }
            Aggregate aggregation = aggregations.get("by_error_type");
            if (Objects.isNull(aggregation) || Objects.isNull(aggregation.sterms())) {
                log.debug("{} sterms aggregation 결과 없음", LOG_PREFIX);
                return result;
            }
            var buckets = aggregation.sterms().buckets().array();
            if (Objects.isNull(buckets) || buckets.isEmpty()) {
                log.debug("{} buckets 비어있음", LOG_PREFIX);
                return result;
            }
            for (var bucket : buckets) {
                String exceptionType = bucket.key();
                Integer count = (int) bucket.docCount();
                LocalDateTime firstOccurrence = parseTimestamp(
                        bucket.aggregations().get("first_occurrence").min().valueAsString());
                LocalDateTime lastOccurrence = parseTimestamp(
                        bucket.aggregations().get("last_occurrence").max().valueAsString());
                if (Objects.isNull(firstOccurrence) || Objects.isNull(lastOccurrence)) {
                    log.warn("{} timeStamp 형식 이상", LOG_PREFIX);
                    continue;
                }
                var hits = bucket.aggregations()
                        .get("sample_data")
                        .topHits()
                        .hits()
                        .hits();
                if (hits.isEmpty()) {
                    continue;
                }
                var source = hits.getFirst().source();
                String message = extractField(source, OpenSearchField.MESSAGE.getFieldName());
                String stackTrace = extractStackTraceFirstLine(source);
                String logger = extractField(source, OpenSearchField.LOGGER.getFieldName());
                result.add(new ErrorAggregation(
                        exceptionType,
                        message,
                        count,
                        firstOccurrence,
                        lastOccurrence,
                        stackTrace,
                        logger
                ));
            }
            log.debug("{} OpenSearch 쿼리 결과 변환 성공", LOG_PREFIX);
            return result;
        } catch (NullPointerException e) {
            log.warn("{} aggregation 파싱 중 NullPointerException 발생: {}", LOG_PREFIX, e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("{} aggregation 파싱 중 예상치 못한 오류 발생", LOG_PREFIX, e);
            throw new BusinessException(OPENSEARCH_OPERATION_FAILED);
        }
    }

    private LocalDateTime parseTimestamp(String timestamp) {
        log.info("{} timestamp 파싱 시작: time={}", LOG_PREFIX, timestamp);
        try {
            return LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            log.error("{} timestamp 파싱 실패: time={}", LOG_PREFIX, timestamp);
            return null;
        }
    }

    private String extractField(JsonData source, String fieldName) {
        log.info("{} json 데이터에서 필드 추출: source={}, field={}", LOG_PREFIX, source, fieldName);
        try {
            return source.toJson().asJsonObject().getString(fieldName, "");
        } catch (Exception e) {
            log.error("{} 필드 추출 실패: source={}, field={}", LOG_PREFIX, source, fieldName);
            return "";
        }
    }

    private String extractStackTraceFirstLine(JsonData source) {
        log.info("{} Stack Trace의 첫 라인 추출 시작", LOG_PREFIX);
        try {
            String fullStackTrace = source.toJson().asJsonObject().getString(OpenSearchField.STACKTRACE.getFieldName(), "");
            if (fullStackTrace.isBlank()) {
                return "";
            }
            int firstNewLine = fullStackTrace.indexOf('\n');
            log.info("{} Stack Trace의 첫 라인 추출 성공", LOG_PREFIX);
            return firstNewLine > 0 ? fullStackTrace.substring(0, firstNewLine) : fullStackTrace;
        } catch (Exception e) {
            log.info("{} Stack Trace 추출 실패", LOG_PREFIX);
            return "";
        }
    }
}