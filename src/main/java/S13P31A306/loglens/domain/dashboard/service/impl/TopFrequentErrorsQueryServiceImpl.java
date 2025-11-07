package S13P31A306.loglens.domain.dashboard.service.impl;

import S13P31A306.loglens.domain.dashboard.dto.opensearch.ErrorAggregation;
import S13P31A306.loglens.domain.dashboard.dto.opensearch.ErrorStatistics;
import S13P31A306.loglens.domain.dashboard.service.TopFrequentErrorsQueryService;
import S13P31A306.loglens.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static S13P31A306.loglens.global.constants.GlobalErrorCode.OPENSEARCH_OPERATION_FAILED;

/**
 * 자주 발생하는 에러 조회를 위한 OpenSearch 쿼리 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TopFrequentErrorsQueryServiceImpl implements TopFrequentErrorsQueryService {

    private static final String LOG_PREFIX = "[TopFrequentErrorsQueryService]";

    private final OpenSearchClient openSearchClient;

    /**
     * Top N 에러 집계 조회
     * OpenSearch에서 예외 타입별로 그룹핑하여 발생 횟수, 최초/최근 발생 시각, 샘플 데이터를 조회
     *
     * @param projectUuid 프로젝트 UUID
     * @param start 조회 시작 시간
     * @param end 조회 종료 시간
     * @param limit 조회할 에러 개수 (1~50)
     * @return 에러 집계 결과 리스트 (발생 횟수 내림차순)
     * @throws BusinessException OpenSearch 쿼리 실행 중 I/O 오류 발생 시 (OPENSEARCH_OPERATION_FAILED)
     */
    @Override
    public List<ErrorAggregation> queryTopErrors(
            String projectUuid,
            LocalDateTime start,
            LocalDateTime end,
            Integer limit) {

        log.info("{} Top {} 에러 집계 쿼리 시작: projectUuid={}", LOG_PREFIX, limit, projectUuid);

        try {
            SearchRequest request = SearchRequest.of(s -> s
                    .index("logs")
                    .size(0)
                    .query(buildErrorLogQuery(projectUuid, start, end))
                    .aggregations("by_error_type", a -> a
                            .terms(t -> t
                                    .field("log_details.exception_type.keyword")
                                    .size(limit))
                            .aggregations("first_occurrence", a2 -> a2
                                    .min(m -> m.field("timestamp")))
                            .aggregations("last_occurrence", a2 -> a2
                                    .max(m -> m.field("timestamp")))
                            .aggregations("sample_data", a2 -> a2
                                    .topHits(th -> th
                                            .size(1)
                                            .source(src -> src.filter(f -> f
                                                    .includes(List.of("message", "stack_trace", "logger")))))))
            );

            SearchResponse<Void> response = openSearchClient.search(request, Void.class);

            List<ErrorAggregation> result = parseErrorAggregations(response);

            log.debug("{} Top {} 에러 집계 완료: {}개 조회", LOG_PREFIX, limit, result.size());

            return result;

        } catch (IOException e) {
            log.error("{} OpenSearch 에러 집계 쿼리 실패", LOG_PREFIX, e);
            throw new BusinessException(OPENSEARCH_OPERATION_FAILED);
        }
    }

    /**
     * 전체 에러 통계 조회
     * 조회 기간 내 전체 에러 수와 고유 예외 타입 수를 조회
     *
     * @param projectUuid 프로젝트 UUID
     * @param start 조회 시작 시간
     * @param end 조회 종료 시간
     * @return 에러 통계 (전체 에러 수, 고유 타입 수)
     */
    @Override
    public ErrorStatistics queryErrorStatistics(
            String projectUuid,
            LocalDateTime start,
            LocalDateTime end) {

        log.debug("{} 에러 통계 쿼리 시작: projectUuid={}", LOG_PREFIX, projectUuid);

        try {
            SearchRequest request = SearchRequest.of(s -> s
                    .index("logs")
                    .size(0)
                    .query(buildErrorLogQuery(projectUuid, start, end))
                    .aggregations("unique_types", a -> a
                            .cardinality(c -> c.field("log_details.exception_type.keyword")))
            );

            SearchResponse<Void> response = openSearchClient.search(request, Void.class);

            long totalErrors = response.hits().total().value();
            int uniqueTypes = (int) response.aggregations()
                    .get("unique_types")
                    .cardinality()
                    .value();

            log.debug("{} 에러 통계 조회 완료: totalErrors={}, uniqueTypes={}",
                    LOG_PREFIX, totalErrors, uniqueTypes);

            return new ErrorStatistics(totalErrors, uniqueTypes);

        } catch (IOException e) {
            log.error("{} OpenSearch 에러 통계 조회 실패", LOG_PREFIX, e);
            return new ErrorStatistics(0L, 0);
        }
    }

    /**
     * 프로젝트의 ERROR 로그 기본 쿼리 생성
     * project_uuid, log_level(ERROR), timestamp 범위 조건을 포함한 bool 쿼리 생성
     *
     * @param projectUuid 프로젝트 UUID
     * @param start 조회 시작 시간
     * @param end 조회 종료 시간
     * @return OpenSearch Query 객체
     */
    private Query buildErrorLogQuery(String projectUuid, LocalDateTime start, LocalDateTime end) {
        return Query.of(q -> q.bool(b -> b
                .must(m -> m.term(t -> t
                        .field("project_uuid.keyword")
                        .value(FieldValue.of(projectUuid))))
                .must(m -> m.term(t -> t
                        .field("log_level.keyword")
                        .value(FieldValue.of("ERROR"))))
                .must(m -> m.range(r -> r
                        .field("timestamp")
                        .gte(JsonData.of(start.toString()))
                        .lte(JsonData.of(end.toString()))))));
    }

    /**
     * OpenSearch 응답을 ErrorAggregation 리스트로 변환
     * terms aggregation 버킷을 순회하며 예외 타입, 발생 횟수, 시간 정보, 샘플 데이터를 추출
     *
     * @param response OpenSearch 검색 응답
     * @return ErrorAggregation 리스트
     */
    private List<ErrorAggregation> parseErrorAggregations(SearchResponse<Void> response) {
        List<ErrorAggregation> result = new ArrayList<>();

        var buckets = response.aggregations()
                .get("by_error_type")
                .sterms()
                .buckets()
                .array();

        for (var bucket : buckets) {
            String exceptionType = bucket.key();
            Long count = bucket.docCount();

            // first/last occurrence
            LocalDateTime firstOccurrence = parseTimestamp(
                    bucket.aggregations().get("first_occurrence").min().valueAsString());
            LocalDateTime lastOccurrence = parseTimestamp(
                    bucket.aggregations().get("last_occurrence").max().valueAsString());

            // sample_data에서 message, stack_trace, logger 추출
            var hits = bucket.aggregations()
                    .get("sample_data")
                    .topHits()
                    .hits()
                    .hits();

            if (hits.isEmpty()) {
                continue;
            }

            var source = hits.getFirst().source();
            String message = extractField(source, "message");
            String stackTrace = extractStackTraceFirstLine(source);
            String logger = extractField(source, "logger");

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

        return result;
    }

    /**
     * ISO 8601 형식의 timestamp 문자열을 LocalDateTime으로 변환
     * 파싱 실패 시 현재 시간을 반환
     *
     * @param timestamp ISO 8601 형식의 시간 문자열
     * @return 변환된 LocalDateTime 또는 현재 시간
     */
    private LocalDateTime parseTimestamp(String timestamp) {
        log.info("{} timestamp 파싱 시작: time={}", LOG_PREFIX, timestamp);
        try {
            return LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            log.error("{} timestamp 파싱 실패: time={}", LOG_PREFIX, timestamp);
            return LocalDateTime.now();
        }
    }

    /**
     * JsonData 객체에서 특정 필드 값을 문자열로 추출
     * 추출 실패 시 빈 문자열 반환
     *
     * @param source JsonData 소스 객체
     * @param fieldName 추출할 필드명
     * @return 필드 값 또는 빈 문자열
     */
    private String extractField(JsonData source, String fieldName) {
        log.info("{} json 데이터에서 필드 추출: source={}, field={}", LOG_PREFIX, source, fieldName);
        try {
            return source.toJson().asJsonObject().getString(fieldName, "");
        } catch (Exception e) {
            log.error("{} 필드 추출 실패: source={}, field={}", LOG_PREFIX, source, fieldName);
            return "";
        }
    }

    /**
     * 전체 스택 트레이스에서 첫 라인만 추출
     * 첫 라인은 실제 에러 발생 지점을 나타내며, 전체 스택을 반환하면 응답 크기가 커지므로 첫 라인만 추출
     * 추출 실패 시 빈 문자열 반환
     *
     * @param source JsonData 소스 객체
     * @return 스택 트레이스 첫 라인 또는 빈 문자열
     */
    private String extractStackTraceFirstLine(JsonData source) {
        try {
            String fullStackTrace = source.toJson().asJsonObject().getString("stack_trace", "");
            if (fullStackTrace.isBlank()) {
                return "";
            }
            int firstNewLine = fullStackTrace.indexOf('\n');
            return firstNewLine > 0 ? fullStackTrace.substring(0, firstNewLine) : fullStackTrace;
        } catch (Exception e) {
            return "";
        }
    }
}
