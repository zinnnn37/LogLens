package S13P31A306.loglens.domain.project.service.impl;

import S13P31A306.loglens.domain.dashboard.dto.opensearch.ApiEndpointStats;
import S13P31A306.loglens.domain.project.entity.ApiEndpoint;
import S13P31A306.loglens.domain.project.entity.Project;
import S13P31A306.loglens.domain.project.repository.ApiEndpointRepository;
import S13P31A306.loglens.domain.project.service.ApiEndpointTransactionalService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

//@formatter:off
/**
 * API 엔드포인트 메트릭 트랜잭션 서비스 구현체
 * OpenSearch에서 API 호출 통계를 조회하여 DB에 저장
 */
//@formatter:on
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiEndpointTransactionalServiceImpl implements ApiEndpointTransactionalService {

    private static final String LOG_PREFIX = "[ApiEndpointTransactionalService]";
    private static final String DEFAULT_TIMEZONE = "Asia/Seoul";
    private static final String TIMESTAMP_FIELD = "timestamp";  // ✨ 추가

    private final OpenSearchClient openSearchClient;
    private final ApiEndpointRepository apiEndpointRepository;

    @Override
    public void aggregateApiEndpointMetrics(
            Project project,
            LocalDateTime from,
            LocalDateTime to) {

        long startTime = System.currentTimeMillis();

        try {
            String indexPattern = getProjectIndexPattern(project.getProjectUuid());

            // 1. OpenSearch에서 API 엔드포인트 통계 조회
            SearchRequest searchRequest = buildApiEndpointRequest(indexPattern, from, to, project.getProjectUuid());
            SearchResponse<Void> response = openSearchClient.search(searchRequest, Void.class);

            // 2. 결과 파싱
            Map<String, ApiEndpointStats> statsMap = parseApiEndpointStatistics(response);

            if (statsMap.isEmpty()) {
                log.debug("{} API 호출 데이터 없음: projectId={}", LOG_PREFIX, project.getId());
                return;
            }

            // 3. DB에 저장 (독립 트랜잭션)
            saveApiEndpointMetrics(project.getId(), statsMap);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("{} API 엔드포인트 메트릭 집계 완료: projectId={}, count={}, 소요시간={}ms",
                    LOG_PREFIX, project.getId(), statsMap.size(), elapsed);

        } catch (Exception e) {
            log.error("{} API 엔드포인트 메트릭 집계 실패: projectId={}, from={}, to={}",
                    LOG_PREFIX, project.getId(), from, to, e);
        }
    }

    private SearchRequest buildApiEndpointRequest(String indexPattern, LocalDateTime from, LocalDateTime to,
                                                  String projectUuid) {
        return SearchRequest.of(s -> s
                .index(indexPattern)
                .size(0)
                .query(q -> q
                        .bool(b -> b
                                .must(m -> m
                                        .range(r -> r
                                                .field(TIMESTAMP_FIELD)
                                                .gte(JsonData.of(from.atZone(ZoneId.of(DEFAULT_TIMEZONE)).toInstant()
                                                        .toString()))
                                                .lt(JsonData.of(
                                                        to.atZone(ZoneId.of(DEFAULT_TIMEZONE)).toInstant().toString()))
                                        )
                                )
                                .must(m -> m
                                        .term(t -> t
                                                .field("project_uuid.keyword")
                                                .value(FieldValue.of(projectUuid))
                                        )
                                )
                                .must(m -> m
                                        .exists(e -> e
                                                .field("log_details.request_uri")
                                        )
                                )
                        )
                )
                .aggregations("by_endpoint", a -> a
                        .terms(t -> t
                                .field("log_details.request_uri.keyword")
                                .size(1000)
                        )
                        .aggregations("by_method", sub -> sub
                                .terms(t -> t
                                        .field("log_details.http_method")
                                        .size(10)
                                )
                                .aggregations("error_count", subsub -> subsub
                                        .filter(f -> f
                                                .range(r -> r
                                                        .field("log_details.response_status")
                                                        .gte(JsonData.of(400))
                                                )
                                        )
                                )
                                .aggregations("avg_response_time", subsub -> subsub
                                        .avg(avg -> avg
                                                .field("log_details.execution_time")
                                        )
                                )
                                .aggregations("max_timestamp", subsub -> subsub
                                        .max(max -> max
                                                .field(TIMESTAMP_FIELD)
                                        )
                                )
                        )
                )
        );
    }

    private Map<String, ApiEndpointStats> parseApiEndpointStatistics(SearchResponse<Void> response) {
        Map<String, ApiEndpointStats> statsMap = new HashMap<>();

        if (Objects.isNull(response.aggregations())) {
            log.warn("{} aggregations가 null입니다", LOG_PREFIX);
            return statsMap;
        }

        Aggregate byEndpointAgg = response.aggregations().get("by_endpoint");
        if (Objects.isNull(byEndpointAgg) || !byEndpointAgg.isSterms()) {
            log.warn("{} by_endpoint aggregation이 없습니다", LOG_PREFIX);
            return statsMap;
        }

        for (var endpointBucket : byEndpointAgg.sterms().buckets().array()) {
            String uri = endpointBucket.key();

            Aggregate byMethodAgg = endpointBucket.aggregations().get("by_method");
            if (Objects.isNull(byMethodAgg) || !byMethodAgg.isSterms()) {
                continue;
            }

            for (var methodBucket : byMethodAgg.sterms().buckets().array()) {
                try {
                    String method = methodBucket.key();
                    long totalRequests = methodBucket.docCount();

                    // error_count aggregation 안전하게 추출
                    long errorCount = 0;
                    Aggregate errorAgg = methodBucket.aggregations().get("error_count");
                    if (errorAgg != null && errorAgg.isFilter()) {
                        errorCount = errorAgg.filter().docCount();
                    }

                    // avg_response_time aggregation 안전하게 추출
                    Double avgResponseTime = null;
                    Aggregate avgAgg = methodBucket.aggregations().get("avg_response_time");
                    if (avgAgg != null && avgAgg.isAvg()) {
                        avgResponseTime = avgAgg.avg().value();
                    }

                    // max_timestamp aggregation 안전하게 추출
                    Double maxTimestamp = null;
                    Aggregate maxAgg = methodBucket.aggregations().get("max_timestamp");
                    if (maxAgg != null && maxAgg.isMax()) {
                        maxTimestamp = maxAgg.max().value();
                    }

                    String key = method + ":" + uri;
                    statsMap.put(key, new ApiEndpointStats(
                            uri,
                            method,
                            totalRequests,
                            errorCount,
                            avgResponseTime,
                            maxTimestamp
                    ));

                    log.debug("{} 파싱 완료: {}:{}, requests={}, errors={}",
                            LOG_PREFIX, method, uri, totalRequests, errorCount);

                } catch (Exception e) {
                    log.error("{} 메서드 버킷 파싱 실패", LOG_PREFIX, e);
                }
            }
        }

        log.info("{} 총 {}개 엔드포인트 파싱 완료", LOG_PREFIX, statsMap.size());
        return statsMap;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void saveApiEndpointMetrics(Integer projectId, Map<String, ApiEndpointStats> statsMap) {
        int savedCount = 0;
        int updatedCount = 0;

        for (ApiEndpointStats stats : statsMap.values()) {
            LocalTime lastAccessed = stats.lastAccessedTimestamp() != null
                    ? LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(stats.lastAccessedTimestamp().longValue()),
                    ZoneId.of(DEFAULT_TIMEZONE)
            ).toLocalTime()
                    : null;

            Optional<ApiEndpoint> existing = apiEndpointRepository
                    .findByProjectIdAndEndpointPathAndHttpMethod(
                            projectId,
                            stats.endpointPath(),
                            stats.httpMethod()
                    );

            if (existing.isPresent()) {
                ApiEndpoint entity = existing.get();
                entity.updateMetrics(
                        (int) stats.totalRequests(),
                        (int) stats.errorCount(),
                        stats.avgResponseTime(),
                        lastAccessed
                );
                updatedCount++;
            } else {
                ApiEndpoint endpoint = ApiEndpoint.builder()
                        .projectId(projectId)
                        .endpointPath(stats.endpointPath())
                        .httpMethod(stats.httpMethod())
                        .totalRequests((int) stats.totalRequests())
                        .errorCount((int) stats.errorCount())
                        .avgResponseTime(stats.avgResponseTime() != null
                                ? BigDecimal.valueOf(stats.avgResponseTime()).setScale(2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO)
                        .anomalyCount(0)
                        .lastAccessed(lastAccessed)
                        .componentId(0)
                        .build();

                apiEndpointRepository.save(endpoint);
                savedCount++;
            }
        }

        log.info("{} API 엔드포인트 메트릭 저장 완료: projectId={}, 신규={}, 업데이트={}",
                LOG_PREFIX, projectId, savedCount, updatedCount);
    }

    private String getProjectIndexPattern(String projectUuid) {
        String sanitizedUuid = projectUuid.replace("-", "_");
        return sanitizedUuid + "_*";
    }

}
