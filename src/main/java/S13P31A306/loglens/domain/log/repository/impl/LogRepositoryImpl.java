package S13P31A306.loglens.domain.log.repository.impl;

import S13P31A306.loglens.domain.component.constants.OpenSearchField;
import S13P31A306.loglens.domain.log.constants.LogErrorCode;
import S13P31A306.loglens.domain.log.dto.internal.LogSearchResult;
import S13P31A306.loglens.domain.log.dto.internal.TraceLogSearchResult;
import S13P31A306.loglens.domain.log.dto.request.LogSearchRequest;
import S13P31A306.loglens.domain.log.dto.response.LogSummaryResponse;
import S13P31A306.loglens.domain.log.entity.Log;
import S13P31A306.loglens.domain.log.repository.LogRepository;
import S13P31A306.loglens.domain.statistics.dto.internal.LogTrendAggregation;
import S13P31A306.loglens.domain.statistics.dto.internal.TrafficAggregation;
import S13P31A306.loglens.global.constants.GlobalErrorCode;
import S13P31A306.loglens.global.exception.BusinessException;
import S13P31A306.loglens.global.utils.OpenSearchUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
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
import org.opensearch.client.opensearch._types.Time;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.DateHistogramBucket;
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
            log.debug("{} 검색 인덱스: {}", LOG_PREFIX, OpenSearchUtils.getProjectIndexPattern(projectUuid));
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

        // ============================================================
        // 🔍 상세 디버깅 로그 시작
        // ============================================================
        log.debug("{} ============================================", LOG_PREFIX);
        log.debug("{} Trace ID 검색 상세 디버깅 정보", LOG_PREFIX);
        log.debug("{} ============================================", LOG_PREFIX);
        log.debug("{} [기본 정보]", LOG_PREFIX);
        log.debug("{}   - 인덱스 패턴: {}", LOG_PREFIX, OpenSearchUtils.getProjectIndexPattern(projectUuid));
        log.debug("{}   - Project UUID (원본): {}", LOG_PREFIX, projectUuid);
        log.debug("{}   - Project UUID (변환): {}", LOG_PREFIX, projectUuid.replace("-", "_"));
        log.debug("{}   - Trace ID: {}", LOG_PREFIX, request.getTraceId());
        log.debug("{}   - 시작 시간: {}", LOG_PREFIX, request.getStartTime());
        log.debug("{}   - 종료 시간: {}", LOG_PREFIX, request.getEndTime());
        log.debug("{}   - 키워드: {}", LOG_PREFIX, request.getKeyword());

        log.debug("{} [SearchRequest 정보]", LOG_PREFIX);
        log.debug("{}   - 인덱스: {}", LOG_PREFIX, searchRequest.index());
        log.debug("{}   - Size: {}", LOG_PREFIX, searchRequest.size());
        log.debug("{}   - Sort: {}", LOG_PREFIX, searchRequest.sort());

        log.debug("{} [OpenSearchField 필드명]", LOG_PREFIX);
        log.debug("{}   - PROJECT_UUID_KEYWORD: {}", LOG_PREFIX, OpenSearchField.PROJECT_UUID_KEYWORD.getFieldName());
        log.debug("{}   - TRACE_ID: {}", LOG_PREFIX, OpenSearchField.TRACE_ID.getFieldName());
        log.debug("{}   - LOG_LEVEL: {}", LOG_PREFIX, OpenSearchField.LOG_LEVEL.getFieldName());
        log.debug("{}   - SOURCE_TYPE: {}", LOG_PREFIX, OpenSearchField.SOURCE_TYPE.getFieldName());

        // Query 상세 분석
        if (query.isBool()) {
            BoolQuery boolQuery = query.bool();
            log.debug("{} [Bool Query 구조]", LOG_PREFIX);
            log.debug("{}   - Filter 개수: {}", LOG_PREFIX, boolQuery.filter().size());
            log.debug("{}   - Must 개수: {}", LOG_PREFIX, boolQuery.must().size());
            log.debug("{}   - Should 개수: {}", LOG_PREFIX, boolQuery.should().size());
            log.debug("{}   - MustNot 개수: {}", LOG_PREFIX, boolQuery.mustNot().size());

            // 각 필터 상세 출력
            for (int i = 0; i < boolQuery.filter().size(); i++) {
                Query filter = boolQuery.filter().get(i);
                log.debug("{} [Filter[{}]]", LOG_PREFIX, i);
                log.debug("{}   - Type: term={}, terms={}, range={}, match={}",
                        LOG_PREFIX, filter.isTerm(), filter.isTerms(), filter.isRange(), filter.isMatch());

                if (filter.isTerm()) {
                    String field = filter.term().field();
                    FieldValue fieldValue = filter.term().value();
                    log.debug("{}   - Term Field: {}", LOG_PREFIX, field);

                    String value = null;
                    if (fieldValue.isString()) {
                        value = fieldValue.stringValue();
                    } else if (fieldValue.isLong()) {
                        value = String.valueOf(fieldValue.longValue());
                    } else if (fieldValue.isDouble()) {
                        value = String.valueOf(fieldValue.doubleValue());
                    } else if (fieldValue.isBoolean()) {
                        value = String.valueOf(fieldValue.booleanValue());
                    }
                    log.debug("{}   - Term Value: {}", LOG_PREFIX, value);
                }

                if (filter.isTerms()) {
                    log.debug("{}   - Terms Field: {}", LOG_PREFIX, filter.terms().field());
                    log.debug("{}   - Terms Values: {}", LOG_PREFIX, filter.terms().terms());
                }

                if (filter.isRange()) {
                    log.debug("{}   - Range Field: {}", LOG_PREFIX, filter.range().field());
                    log.debug("{}   - Range GTE: {}", LOG_PREFIX, filter.range().gte());
                    log.debug("{}   - Range LTE: {}", LOG_PREFIX, filter.range().lte());
                }

                if (filter.isMatch()) {
                    log.debug("{}   - Match Field: {}", LOG_PREFIX, filter.match().field());
                    log.debug("{}   - Match Query: {}", LOG_PREFIX, filter.match().query());
                }
            }

            // Must 쿼리 출력
            for (int i = 0; i < boolQuery.must().size(); i++) {
                Query must = boolQuery.must().get(i);
                log.debug("{} [Must[{}]]", LOG_PREFIX, i);
                log.debug("{}   - Type: term={}, terms={}, range={}, match={}",
                        LOG_PREFIX, must.isTerm(), must.isTerms(), must.isRange(), must.isMatch());

                if (must.isMatch()) {
                    log.debug("{}   - Match Field: {}", LOG_PREFIX, must.match().field());
                    log.debug("{}   - Match Query: {}", LOG_PREFIX, must.match().query());
                }
            }
        }

        log.debug("{} ============================================", LOG_PREFIX);
        // ============================================================
        // 🔍 상세 디버깅 로그 끝
        // ============================================================

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
            log.error("{} 에러 상세: {}", LOG_PREFIX, e.getMessage());
            if (e.getCause() != null) {
                log.error("{} 에러 원인: {}", LOG_PREFIX, e.getCause().getMessage());
            }
            throw new BusinessException(GlobalErrorCode.OPENSEARCH_OPERATION_FAILED, null, e);
        }
    }

    @Override
    public java.util.Optional<Log> findByLogId(Long logId, String projectUuid) {
        log.debug("{} OpenSearch에서 로그 ID로 조회: logId={}, projectUuid={}", LOG_PREFIX, logId, projectUuid);

        try {
            // 쿼리 생성: log_id와 project_uuid 매칭
            Query query = Query.of(q -> q.bool(b -> b
                    .filter(f -> f.term(t -> t
                            .field(OpenSearchField.LOG_ID.getFieldName())
                            .value(FieldValue.of(logId))))
                    .filter(f -> f.term(t -> t
                            .field(OpenSearchField.PROJECT_UUID_KEYWORD.getFieldName())
                            .value(FieldValue.of(projectUuid))))));

            SearchRequest searchRequest = new SearchRequest.Builder()
                    .index(OpenSearchUtils.getProjectIndexPattern(projectUuid))
                    .query(query)
                    .size(1)
                    .build();

            SearchResponse<Log> response = openSearchClient.search(searchRequest, Log.class);

            if (response.hits().hits().isEmpty()) {
                log.debug("{} 로그를 찾을 수 없음: logId={}, projectUuid={}", LOG_PREFIX, logId, projectUuid);
                return java.util.Optional.empty();
            }

            Hit<Log> hit = response.hits().hits().get(0);
            Log logEntity = hit.source();
            if (logEntity != null) {
                logEntity.setId(hit.id()); // OpenSearch document _id 설정
                log.debug("{} 로그 조회 성공: logId={}, _id={}", LOG_PREFIX, logId, hit.id());
                return java.util.Optional.of(logEntity);
            }

            return java.util.Optional.empty();

        } catch (IOException e) {
            log.error("{} OpenSearch findByLogId 중 에러 발생: logId={}, projectUuid={}",
                    LOG_PREFIX, logId, projectUuid, e);
            throw new BusinessException(GlobalErrorCode.OPENSEARCH_OPERATION_FAILED, null, e);
        }
    }

    @Override
    public boolean existsByProjectUuid(String projectUuid) {
        log.debug("{} 프로젝트 UUID로 로그 존재 확인: projectUuid={}", LOG_PREFIX, projectUuid);
        try {
            SearchRequest searchRequest = new SearchRequest.Builder()
                    .index(OpenSearchUtils.getProjectIndexPattern(projectUuid))
                    .query(q -> q.term(t -> t.field(OpenSearchField.PROJECT_UUID_KEYWORD.getFieldName())
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
                .index(OpenSearchUtils.getProjectIndexPattern(projectUuid))
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
                .index(OpenSearchUtils.getProjectIndexPattern(projectUuid))
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
        if (Objects.isNull(searchAfterValues)) {
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
                t -> t.field(OpenSearchField.PROJECT_UUID_KEYWORD.getFieldName()).value(FieldValue.of(projectUuid))));
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

    @Override
    public long countErrorLogsByProjectUuidAndTimeRange(
            String projectUuid,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        log.debug("{} ERROR 로그 개수 조회: projectUuid={}, startTime={}, endTime={}",
                LOG_PREFIX, projectUuid, startTime, endTime);

        try {
            // 1. Bool Query 생성: project_uuid + log_level=ERROR + timestamp 범위
            Query query = Query.of(q -> q.bool(b -> b
                    .filter(f -> f.term(t -> t
                            .field(OpenSearchField.PROJECT_UUID_KEYWORD.getFieldName())
                            .value(FieldValue.of(projectUuid))))
                    .filter(f -> f.term(t -> t
                            .field(OpenSearchField.LOG_LEVEL.getFieldName())
                            .value(FieldValue.of("ERROR"))))
                    .filter(f -> f.range(r -> r
                            .field(TIMESTAMP_FIELD)
                            .gte(JsonData.of(startTime.atOffset(ZoneOffset.UTC).toString()))
                            .lte(JsonData.of(endTime.atOffset(ZoneOffset.UTC).toString()))
                    ))
            ));

            // 2. SearchRequest 생성 (size=0, 집계만 수행)
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(OpenSearchUtils.getProjectIndexPattern(projectUuid))
                    .query(query)
                    .size(0)  // 문서는 반환하지 않음
            );

            // 3. OpenSearch 쿼리 실행
            SearchResponse<Void> response = openSearchClient.search(searchRequest, Void.class);

            long errorCount = Objects.requireNonNull(response.hits().total()).value();

            log.debug("{} ERROR 로그 개수 조회 완료: projectUuid={}, errorCount={}",
                    LOG_PREFIX, projectUuid, errorCount);

            return errorCount;

        } catch (IOException e) {
            log.error("{} ERROR 로그 개수 조회 실패: projectUuid={}", LOG_PREFIX, projectUuid, e);
            throw new BusinessException(GlobalErrorCode.OPENSEARCH_OPERATION_FAILED, null, e);
        }
    }

    @Override
    public List<LogTrendAggregation> aggregateLogTrendByTimeRange(
            String projectUuid,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String interval
    ) {
        log.info("{} 로그 추이 집계 시작: projectUuid={}, start={}, end={}, interval={}",
                LOG_PREFIX, projectUuid, startTime, endTime, interval);

        try {
            // OpenSearchUtils로 인덱스 패턴 생성
            String indexPattern = OpenSearchUtils.getProjectIndexPattern(projectUuid);
            log.debug("{} 인덱스 패턴: {}", LOG_PREFIX, indexPattern);

            // SearchRequest 생성
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(indexPattern)
                    .size(0)  // 집계만 수행
                    .query(q -> q.bool(b -> b
                            // project_uuid 필터 (불필요하지만 명시적으로 추가)
                            .filter(f -> f.term(t -> t
                                    .field(OpenSearchField.PROJECT_UUID_KEYWORD.getFieldName())
                                    .value(FieldValue.of(projectUuid))
                            ))
                            // 시간 범위 필터
                            .filter(f -> f.range(r -> r
                                    .field(TIMESTAMP_FIELD)
                                    .gte(JsonData.of(startTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toString()))
                                    .lt(JsonData.of(endTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toString()))
                            ))
                    ))
                    .aggregations("logs_over_time", a -> a
                            // Date Histogram aggregation
                            .dateHistogram(dh -> dh
                                    .field(TIMESTAMP_FIELD)
                                    .fixedInterval(Time.of(t -> t.time(interval)))
                                    .timeZone("Asia/Seoul")
                                    .minDocCount(0)  // 로그가 없는 시간대도 포함
                            )
                            // log_level별 집계 (sub-aggregation)
                            .aggregations("by_level", sub -> sub
                                    .terms(t -> t
                                            .field(OpenSearchField.LOG_LEVEL.getFieldName())
                                    )
                            )
                    )
            );

            // OpenSearch 쿼리 실행
            SearchResponse<Void> response = openSearchClient.search(searchRequest, Void.class);

            // 결과 파싱
            List<LogTrendAggregation> result = parseLogTrendAggregation(response);

            log.info("{} 로그 추이 집계 완료: projectUuid={}, 결과개수={}",
                    LOG_PREFIX, projectUuid, result.size());

            return result;

        } catch (IOException e) {
            log.error("{} 로그 추이 집계 중 오류 발생: projectUuid={}", LOG_PREFIX, projectUuid, e);
            throw new BusinessException(GlobalErrorCode.OPENSEARCH_OPERATION_FAILED, null, e);
        }
    }

    /**
     * OpenSearch 집계 결과를 LogTrendAggregation 리스트로 파싱
     */
    private List<LogTrendAggregation> parseLogTrendAggregation(SearchResponse<Void> response) {
        List<LogTrendAggregation> result = new ArrayList<>();

        Aggregate logsOverTime = response.aggregations().get("logs_over_time");
        if (Objects.isNull(logsOverTime) || Objects.isNull(logsOverTime.dateHistogram())) {
            return result;
        }

        for (DateHistogramBucket bucket : logsOverTime.dateHistogram().buckets().array()) {
            // 타임스탬프 파싱
            String timestampStr = bucket.keyAsString();
            LocalDateTime timestamp = OffsetDateTime.parse(
                    timestampStr,
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME
            ).toLocalDateTime();

            // 전체 로그 수
            int totalCount = (int) bucket.docCount();

            // log_level별 집계 파싱
            Map<String, Integer> levelCounts = parseLevelCountsForTrend(bucket.aggregations().get("by_level"));

            LogTrendAggregation aggregation = new LogTrendAggregation(
                    timestamp,
                    totalCount,
                    levelCounts.getOrDefault("INFO", 0),
                    levelCounts.getOrDefault("WARN", 0),
                    levelCounts.getOrDefault("ERROR", 0)
            );

            result.add(aggregation);
        }

        return result;
    }

    /**
     * log_level별 집계 결과 파싱 (로그 추이용)
     */
    private Map<String, Integer> parseLevelCountsForTrend(Aggregate byLevel) {
        Map<String, Integer> counts = new HashMap<>();
        if (Objects.isNull(byLevel) || Objects.isNull(byLevel.sterms())) {
            return counts;
        }

        for (StringTermsBucket bucket : byLevel.sterms().buckets().array()) {
            counts.put(bucket.key(), (int) bucket.docCount());
        }

        return counts;
    }

    @Override
    public List<TrafficAggregation> aggregateTrafficByTimeRange(
            String projectUuid,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String interval
    ) {
        log.info("{} Traffic 집계 시작: projectUuid={}, start={}, end={}, interval={}",
                LOG_PREFIX, projectUuid, startTime, endTime, interval);

        try {
            // OpenSearchUtils로 인덱스 패턴 생성
            String indexPattern = OpenSearchUtils.getProjectIndexPattern(projectUuid);
            log.debug("{} 인덱스 패턴: {}", LOG_PREFIX, indexPattern);

            // SearchRequest 생성
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(indexPattern)
                    .size(0)  // 집계만 수행
                    .query(q -> q.bool(b -> b
                            // project_uuid 필터
                            .filter(f -> f.term(t -> t
                                    .field(OpenSearchField.PROJECT_UUID_KEYWORD.getFieldName())
                                    .value(FieldValue.of(projectUuid))
                            ))
                            // 시간 범위 필터
                            .filter(f -> f.range(r -> r
                                    .field(TIMESTAMP_FIELD)
                                    .gte(JsonData.of(startTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toString()))
                                    .lt(JsonData.of(endTime.atZone(ZoneId.of("Asia/Seoul")).toInstant().toString()))
                            ))
                    ))
                    .aggregations("traffic_over_time", a -> a
                            // Date Histogram aggregation
                            .dateHistogram(dh -> dh
                                    .field(TIMESTAMP_FIELD)
                                    .fixedInterval(Time.of(t -> t.time(interval)))
                                    .timeZone("Asia/Seoul")
                                    .minDocCount(0)  // 로그가 없는 시간대도 포함
                            )
                            // source_type별 집계 (sub-aggregation)
                            .aggregations("by_source_type", sub -> sub
                                    .terms(t -> t
                                            .field(OpenSearchField.SOURCE_TYPE.getFieldName())
                                    )
                            )
                    )
            );

            // OpenSearch 쿼리 실행
            SearchResponse<Void> response = openSearchClient.search(searchRequest, Void.class);

            // 결과 파싱
            List<TrafficAggregation> result = parseTrafficAggregation(response);

            log.info("{} Traffic 집계 완료: projectUuid={}, 결과개수={}",
                    LOG_PREFIX, projectUuid, result.size());

            return result;

        } catch (IOException e) {
            log.error("{} Traffic 집계 중 오류 발생: projectUuid={}", LOG_PREFIX, projectUuid, e);
            throw new BusinessException(GlobalErrorCode.OPENSEARCH_OPERATION_FAILED, null, e);
        }
    }

    /**
     * OpenSearch 집계 결과를 TrafficAggregation 리스트로 파싱
     */
    private List<TrafficAggregation> parseTrafficAggregation(SearchResponse<Void> response) {
        List<TrafficAggregation> result = new ArrayList<>();

        Aggregate trafficOverTime = response.aggregations().get("traffic_over_time");
        if (Objects.isNull(trafficOverTime) || Objects.isNull(trafficOverTime.dateHistogram())) {
            return result;
        }

        for (DateHistogramBucket bucket : trafficOverTime.dateHistogram().buckets().array()) {
            // 타임스탬프 파싱
            String timestampStr = bucket.keyAsString();
            LocalDateTime timestamp = OffsetDateTime.parse(
                    timestampStr,
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME
            ).toLocalDateTime();

            // 전체 로그 수
            int totalCount = (int) bucket.docCount();

            // source_type별 집계 파싱
            Map<String, Integer> sourceTypeCounts = parseSourceTypeCounts(bucket.aggregations().get("by_source_type"));

            TrafficAggregation aggregation = new TrafficAggregation(
                    timestamp,
                    totalCount,
                    sourceTypeCounts.getOrDefault("FE", 0),
                    sourceTypeCounts.getOrDefault("BE", 0)
            );

            result.add(aggregation);
        }

        return result;
    }

    /**
     * source_type별 집계 결과 파싱 (Traffic용)
     */
    private Map<String, Integer> parseSourceTypeCounts(Aggregate bySourceType) {
        Map<String, Integer> counts = new HashMap<>();
        if (Objects.isNull(bySourceType) || Objects.isNull(bySourceType.sterms())) {
            return counts;
        }

        for (StringTermsBucket bucket : bySourceType.sterms().buckets().array()) {
            counts.put(bucket.key(), (int) bucket.docCount());
        }

        return counts;
    }
}
