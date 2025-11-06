package S13P31A306.loglens.domain.log.repository.impl;

import S13P31A306.loglens.domain.component.constants.OpenSearchField;
import S13P31A306.loglens.domain.log.constants.LogErrorCode;
import S13P31A306.loglens.domain.log.dto.internal.LogSearchResult;
import S13P31A306.loglens.domain.log.dto.internal.TraceLogSearchResult;
import S13P31A306.loglens.domain.log.dto.request.LogSearchRequest;
import S13P31A306.loglens.domain.log.dto.response.LogSummaryResponse;
import S13P31A306.loglens.domain.log.entity.Log;
import S13P31A306.loglens.domain.log.repository.LogRepository;
import S13P31A306.loglens.global.constants.GlobalErrorCode;
import S13P31A306.loglens.global.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch._types.query_dsl.BoolQuery;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.TermsQueryField;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class LogRepositoryImpl implements LogRepository {

    private static final String LOG_PREFIX = "[LogRepository]";

    private final OpenSearchClient openSearchClient;
    private final ObjectMapper objectMapper;
    private static final String TIMESTAMP_FIELD = "timestamp";
    private static final String ID_FIELD = "_id";
    private static final int MAX_TRACE_LOGS = 1000;

    /**
     * 프로젝트별 인덱스 패턴을 반환
     *
     * @param projectUuid 프로젝트 UUID (하이픈 포함)
     * @return "{projectUuid_with_underscores}-*" 형식의 인덱스 패턴
     */
    private String getProjectIndexPattern(String projectUuid) {
        // Logstash에서 하이픈을 언더스코어로 변환하므로 동일하게 변환
        String sanitizedUuid = projectUuid.replace("-", "_");
        return sanitizedUuid + "_*";
    }

    @Override
    public LogSearchResult findWithCursor(String projectUuid, LogSearchRequest request) {
        log.debug("{} OpenSearch에서 커서 기반 로그 조회 시작: projectUuid={}, request={}", LOG_PREFIX, projectUuid, request);
        int requestSize = request.getSize();
        int querySize = requestSize + 1;

        // 1. 검색 쿼리 생성
        Query query = buildSearchQuery(projectUuid, request);

        // 2. 정렬 옵션 생성
        List<SortOptions> sortOptions = buildSortOptions(request);

        // 3. SearchRequest 빌드
        SearchRequest searchRequest = buildSearchRequestWithCursor(projectUuid, query, sortOptions, querySize,
                request.getCursor());

        // 4. OpenSearch 쿼리 실행
        try {
            // 쿼리 디버깅을 위한 상세 로그
            log.debug("{} 실제 projectUuid 값: [{}]", LOG_PREFIX, projectUuid);
            log.debug("{} 검색 인덱스: {}", LOG_PREFIX, getProjectIndexPattern(projectUuid));
            log.debug("{} 쿼리 크기: {}", LOG_PREFIX, querySize);

            // OpenSearch 쿼리를 JSON으로 직렬화하여 출력
            try {
                String queryJson = objectMapper.writeValueAsString(searchRequest);
                log.debug("{} OpenSearch 쿼리 JSON: {}", LOG_PREFIX, queryJson);
            } catch (Exception e) {
                log.warn("{} 쿼리 JSON 직렬화 실패", LOG_PREFIX, e);
            }

            log.debug("{} OpenSearch에 검색 요청 실행", LOG_PREFIX);
            SearchResponse<Log> response = openSearchClient.search(searchRequest, Log.class);
            log.debug("{} OpenSearch 응답 수신: {} hits", LOG_PREFIX, response.hits().total().value());

            // 5. 응답 처리
            LogSearchResult result = processSearchResponse(response, requestSize);
            log.debug("{} 커서 기반 로그 조회 완료: {} logs, hasNext={}", LOG_PREFIX, result.logs().size(), result.hasNext());
            return result;
        } catch (IOException e) {
            log.error("{} OpenSearch findWithCursor 중 에러 발생", LOG_PREFIX, e);
            throw new BusinessException(GlobalErrorCode.OPENSEARCH_OPERATION_FAILED, null, e);
        }
    }

    @Override
    public TraceLogSearchResult findByTraceId(String projectUuid, LogSearchRequest request) {
        log.debug("{} OpenSearch에서 Trace ID 기반 로그 조회 시작: projectUuid={}, request={}", LOG_PREFIX, projectUuid, request);
        // 1. 검색 쿼리 생성
        Query query = buildSearchQuery(projectUuid, request);

        // 2. SearchRequest 빌드 (Aggregation 포함)
        SearchRequest searchRequest = buildTraceSearchRequest(projectUuid, query);

        // 3. OpenSearch 쿼리 실행
        try {
            log.debug("{} OpenSearch에 Trace ID 검색 요청 실행", LOG_PREFIX);
            SearchResponse<Log> response = openSearchClient.search(searchRequest, Log.class);
            log.debug("{} OpenSearch 응답 수신: {} hits", LOG_PREFIX, response.hits().total().value());

            // 4. 응답 처리
            List<Log> logs = extractLogsFromHits(response.hits().hits());
            LogSummaryResponse summary = buildSummaryFromAggregations(response, logs.size());

            TraceLogSearchResult result = new TraceLogSearchResult(logs, summary);
            log.debug("{} Trace ID 기반 로그 조회 완료: {} logs", LOG_PREFIX, result.logs().size());
            return result;
        } catch (IOException e) {
            log.error("{} OpenSearch findByTraceId 중 에러 발생", LOG_PREFIX, e);
            throw new BusinessException(GlobalErrorCode.OPENSEARCH_OPERATION_FAILED, null, e);
        }
    }

    @Override
    public boolean existsByProjectUuid(String projectUuid) {
        log.debug("{} 프로젝트 UUID로 로그 존재 확인: projectUuid={}", LOG_PREFIX, projectUuid);
        try {
            SearchRequest searchRequest = new SearchRequest.Builder()
                    .index(getProjectIndexPattern(projectUuid))
                    .query(q -> q.term(t -> t.field(OpenSearchField.PROJECT_UUID.getFieldName())
                            .value(FieldValue.of(projectUuid))))
                    .size(1)
                    .build();

            SearchResponse<Log> response = openSearchClient.search(searchRequest, Log.class);

            long totalHits = Objects.requireNonNull(response.hits().total()).value();
            log.debug("{} OpenSearch 검색 결과: projectUuid={}, totalHits={}", LOG_PREFIX, projectUuid, totalHits);

            return totalHits > 0;
        } catch (IOException e) {
            log.error("{} OpenSearch 검색 중 오류 발생: projectUuid={}", LOG_PREFIX, projectUuid, e);
            return false;
        }
    }

    // ============================================================
    // Query Building Methods
    // ============================================================

    /**
     * 검색 쿼리 생성
     */
    private Query buildSearchQuery(String projectUuid, LogSearchRequest request) {
        log.debug("{} 검색 쿼리 빌드 시작", LOG_PREFIX);
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
        buildFilterClauses(projectUuid, request, boolQueryBuilder);
        return new Query.Builder().bool(boolQueryBuilder.build()).build();
    }

    /**
     * 정렬 옵션 생성
     */
    private List<SortOptions> buildSortOptions(LogSearchRequest request) {
        String[] sortParams = request.getSort().split(",");
        SortOrder sortOrder = "asc".equalsIgnoreCase(sortParams[1]) ? SortOrder.Asc : SortOrder.Desc;

        return List.of(
                SortOptions.of(s -> s.field(f -> f.field(TIMESTAMP_FIELD).order(sortOrder))),
                SortOptions.of(s -> s.field(f -> f.field(ID_FIELD).order(sortOrder)))
        );
    }

    /**
     * 커서 기반 페이지네이션 SearchRequest 생성
     */
    private SearchRequest buildSearchRequestWithCursor(String projectUuid, Query query, List<SortOptions> sortOptions,
                                                       int size, String cursor) {
        SearchRequest.Builder builder = new SearchRequest.Builder()
                .index(getProjectIndexPattern(projectUuid))
                .query(query)
                .size(size)
                .sort(sortOptions);

        if (Objects.nonNull(cursor) && !cursor.isEmpty()) {
            List<FieldValue> searchAfterValues = convertCursorToFieldValues(cursor);
            builder.searchAfter(searchAfterValues);
        }

        return builder.build();
    }

    /**
     * TraceId 조회용 SearchRequest 생성 (Aggregation 포함)
     */
    private SearchRequest buildTraceSearchRequest(String projectUuid, Query query) {
        return new SearchRequest.Builder()
                .index(getProjectIndexPattern(projectUuid))
                .query(query)
                .size(MAX_TRACE_LOGS)
                .sort(s -> s.field(f -> f.field(TIMESTAMP_FIELD).order(SortOrder.Asc)))
                .aggregations("min_timestamp", a -> a.min(m -> m.field(TIMESTAMP_FIELD)))
                .aggregations("max_timestamp", a -> a.max(m -> m.field(TIMESTAMP_FIELD)))
                .aggregations("level_counts", a -> a.terms(t -> t.field(OpenSearchField.LOG_LEVEL.getFieldName())))
                .build();
    }

    // ============================================================
    // Response Processing Methods
    // ============================================================

    /**
     * 검색 응답을 LogSearchResult로 변환
     */
    private LogSearchResult processSearchResponse(SearchResponse<Log> response, int requestSize) {
        log.debug("{} 검색 응답 처리 시작", LOG_PREFIX);
        List<Hit<Log>> hits = response.hits().hits();
        List<Log> logs = extractLogsFromHits(hits);

        boolean hasNext = logs.size() > requestSize;
        Object[] nextSortValues = null;

        if (hasNext) {
            logs.remove(requestSize);
            nextSortValues = extractSortValues(hits.get(requestSize - 1));
        }

        LogSearchResult result = new LogSearchResult(logs, hasNext, nextSortValues);
        log.debug("{} 검색 응답 처리 완료: {} logs, hasNext={}", LOG_PREFIX, result.logs().size(), result.hasNext());
        return result;
    }

    /**
     * Hit 목록에서 Log 엔티티 추출
     */
    private List<Log> extractLogsFromHits(List<Hit<Log>> hits) {
        List<Log> logs = new ArrayList<>();
        for (Hit<Log> hit : hits) {
            Log logEntity = hit.source();
            if (Objects.nonNull(logEntity)) {
                logEntity.setId(hit.id());
                logs.add(logEntity);
            }
        }
        return logs;
    }

    /**
     * Hit에서 Sort 값 추출
     */
    private Object[] extractSortValues(Hit<Log> hit) {
        return hit.sort().stream()
                .map(this::convertFieldValueToObject)
                .toArray();
    }

    // ============================================================
    // Aggregation Processing Methods
    // ============================================================

    /**
     * Aggregation 결과로부터 LogSummaryResponse 생성
     */
    private LogSummaryResponse buildSummaryFromAggregations(SearchResponse<Log> response, int totalLogs) {
        log.debug("{} Aggregation 기반 요약 정보 빌드 시작", LOG_PREFIX);
        if (totalLogs == 0) {
            log.debug("{} 로그가 없어 빈 요약 정보 반환", LOG_PREFIX);
            return LogSummaryResponse.builder().totalLogs(0).build();
        }

        Map<String, Aggregate> aggs = response.aggregations();

        LocalDateTime startTime = getDateTimeFromAgg(aggs, "min_timestamp");
        LocalDateTime endTime = getDateTimeFromAgg(aggs, "max_timestamp");
        long durationMs = calculateDuration(startTime, endTime);

        Map<String, Long> levelCounts = extractLevelCounts(aggs);

        LogSummaryResponse summary = LogSummaryResponse.builder()
                .totalLogs(totalLogs)
                .durationMs(durationMs)
                .startTime(startTime)
                .endTime(endTime)
                .errorCount(levelCounts.getOrDefault("ERROR", 0L))
                .warnCount(levelCounts.getOrDefault("WARN", 0L))
                .infoCount(levelCounts.getOrDefault("INFO", 0L))
                .build();
        log.debug("{} 요약 정보 빌드 완료: totalLogs={}", LOG_PREFIX, totalLogs);
        return summary;
    }

    /**
     * Aggregation에서 날짜 추출
     */
    private LocalDateTime getDateTimeFromAgg(Map<String, Aggregate> aggs, String aggName) {
        if (!aggs.containsKey(aggName)) {
            return null;
        }

        Aggregate agg = aggs.get(aggName);
        Double millis = null;

        if (agg.isMin() && Objects.nonNull(agg.min())) {
            millis = agg.min().value();
        } else if (agg.isMax() && Objects.nonNull(agg.max())) {
            millis = agg.max().value();
        }

        if (!Objects.isNull(millis) && millis > 0 && !Double.isInfinite(millis)) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis.longValue()), ZoneOffset.UTC);
        }
        return null;
    }

    /**
     * 시간 차이 계산 (밀리초)
     */
    private long calculateDuration(LocalDateTime startTime, LocalDateTime endTime) {
        if (Objects.nonNull(startTime) && Objects.nonNull(endTime)) {
            return java.time.Duration.between(startTime, endTime).toMillis();
        }
        return 0;
    }

    /**
     * Level별 카운트 추출
     */
    private Map<String, Long> extractLevelCounts(Map<String, Aggregate> aggs) {
        Map<String, Long> levelCounts = new java.util.HashMap<>();

        if (!aggs.containsKey("level_counts")) {
            return levelCounts;
        }

        List<StringTermsBucket> buckets = aggs.get("level_counts").sterms().buckets().array();
        for (StringTermsBucket bucket : buckets) {
            String level = bucket.key();
            levelCounts.put(level.toUpperCase(), bucket.docCount());
        }

        return levelCounts;
    }

    // ============================================================
    // Cursor & Conversion Utility Methods
    // ============================================================

    /**
     * 커서를 FieldValue 리스트로 변환
     */
    private List<FieldValue> convertCursorToFieldValues(String cursor) {
        Object[] searchAfterValues = decodeCursor(cursor);
        if (searchAfterValues == null) {
            return List.of();
        }
        return Arrays.stream(searchAfterValues)
                .map(String::valueOf)
                .map(FieldValue::of)
                .collect(Collectors.toList());
    }

    /**
     * 커서 디코딩
     */
    private Object[] decodeCursor(String cursor) {
        log.debug("{} 커서 디코딩 시작", LOG_PREFIX);
        if (Objects.isNull(cursor) || cursor.isEmpty()) {
            return null;
        }
        try {
            Object[] decoded = objectMapper.readValue(Base64.getDecoder().decode(cursor), Object[].class);
            log.debug("{} 커서 디코딩 완료", LOG_PREFIX);
            return decoded;
        } catch (Exception e) {
            log.warn("{} 커서 디코딩 실패: cursor={}", LOG_PREFIX, cursor, e);
            throw new BusinessException(LogErrorCode.INVALID_CURSOR);
        }
    }

    /**
     * FieldValue를 Object로 변환
     */
    private Object convertFieldValueToObject(FieldValue fieldValue) {
        if (fieldValue.isLong()) {
            return fieldValue.longValue();
        } else if (fieldValue.isString()) {
            return fieldValue.stringValue();
        } else if (fieldValue.isDouble()) {
            return fieldValue.doubleValue();
        } else if (fieldValue.isBoolean()) {
            return fieldValue.booleanValue();
        }
        return null;
    }

    // ============================================================
    // Filter Building Methods
    // ============================================================

    /**
     * OpenSearch 쿼리 필터 조건 생성
     */
    private void buildFilterClauses(String projectUuid, LogSearchRequest request, BoolQuery.Builder boolQueryBuilder) {
        addProjectFilter(boolQueryBuilder, projectUuid);
        addTraceIdFilter(boolQueryBuilder, request.getTraceId());
        addLogLevelFilter(boolQueryBuilder, request.getLogLevel());
        addSourceTypeFilter(boolQueryBuilder, request.getSourceType());
        addTimeRangeFilter(boolQueryBuilder, request.getStartTime(), request.getEndTime());
        addKeywordFilter(boolQueryBuilder, request.getKeyword());
    }

    /**
     * 프로젝트 UUID 필터 추가
     */
    private void addProjectFilter(BoolQuery.Builder builder, String projectUuid) {
        builder.filter(q -> q.term(
                t -> t.field(OpenSearchField.PROJECT_UUID.getFieldName()).value(FieldValue.of(projectUuid))));
    }

    /**
     * TraceId 필터 추가
     */
    private void addTraceIdFilter(BoolQuery.Builder builder, String traceId) {
        if (Objects.nonNull(traceId) && !traceId.isEmpty()) {
            builder.filter(
                    q -> q.term(t -> t.field(OpenSearchField.TRACE_ID.getFieldName()).value(FieldValue.of(traceId))));
        }
    }

    /**
     * 로그 레벨 필터 추가
     */
    private void addLogLevelFilter(BoolQuery.Builder builder, List<String> logLevels) {
        if (Objects.isNull(logLevels) || logLevels.isEmpty()) {
            return;
        }

        List<FieldValue> levels = logLevels.stream()
                .map(String::toUpperCase)
                .map(FieldValue::of)
                .collect(Collectors.toList());

        builder.filter(q -> q.terms(t -> t
                .field(OpenSearchField.LOG_LEVEL.getFieldName())
                .terms(new TermsQueryField.Builder().value(levels).build())));
    }

    /**
     * 소스 타입 필터 추가
     */
    private void addSourceTypeFilter(BoolQuery.Builder builder, List<String> sourceTypes) {
        if (Objects.isNull(sourceTypes) || sourceTypes.isEmpty()) {
            return;
        }

        List<FieldValue> sources = sourceTypes.stream()
                .map(String::toUpperCase)
                .map(FieldValue::of)
                .toList();

        builder.filter(q -> q.terms(t -> t
                .field(OpenSearchField.SOURCE_TYPE.getFieldName())
                .terms(new TermsQueryField.Builder().value(sources).build())));
    }

    /**
     * 시간 범위 필터 추가
     */
    private void addTimeRangeFilter(BoolQuery.Builder builder, LocalDateTime startTime, LocalDateTime endTime) {
        if (Objects.isNull(startTime) && Objects.isNull(endTime)) {
            return;
        }

        builder.filter(q -> q.range(r -> {
            r.field(TIMESTAMP_FIELD);
            if (Objects.nonNull(startTime)) {
                r.gte(JsonData.of(startTime.atOffset(ZoneOffset.UTC).toString()));
            }
            if (Objects.nonNull(endTime)) {
                r.lte(JsonData.of(endTime.atOffset(ZoneOffset.UTC).toString()));
            }
            return r;
        }));
    }

    /**
     * 키워드 필터 추가 (message 필드 검색)
     */
    private void addKeywordFilter(BoolQuery.Builder builder, String keyword) {
        if (Objects.nonNull(keyword) && !keyword.isEmpty()) {
            builder.must(q -> q.match(m -> m.field("message").query(FieldValue.of(keyword))));
        }
    }
}
