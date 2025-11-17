package S13P31A306.loglens.domain.project.service.impl;

import S13P31A306.loglens.domain.dashboard.dto.opensearch.ApiEndpointStats;
import S13P31A306.loglens.domain.project.entity.ApiEndpoint;
import S13P31A306.loglens.domain.project.entity.Project;
import S13P31A306.loglens.domain.project.repository.ApiEndpointRepository;
import S13P31A306.loglens.domain.project.service.ApiEndpointTransactionalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.aggregations.CompositeAggregate;
import org.opensearch.client.opensearch._types.aggregations.CompositeAggregationSource;
import org.opensearch.client.opensearch._types.aggregations.CompositeBucket;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.json.JsonData;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

/**
 * API 엔드포인트 메트릭 트랜잭션 서비스 구현체
 * OpenSearch에서 API 호출 통계를 조회하여 DB에 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiEndpointTransactionalServiceImpl implements ApiEndpointTransactionalService {

    private static final String LOG_PREFIX = "[ApiEndpointTransactionalService]";
    private static final String DEFAULT_TIMEZONE = "Asia/Seoul";

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

    private SearchRequest buildApiEndpointRequest(String indexPattern, LocalDateTime from, LocalDateTime to, String projectUuid) {
        return SearchRequest.of(s -> s
                .index(indexPattern)
                .size(0)
                .query(q -> q
                        .bool(b -> b
                                .must(m -> m
                                        .range(r -> r
                                                .field("timestamp")
                                                .gte(JsonData.of(from.atZone(ZoneId.of(DEFAULT_TIMEZONE)).toInstant().toString()))
                                                .lt(JsonData.of(to.atZone(ZoneId.of(DEFAULT_TIMEZONE)).toInstant().toString()))
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
                .aggregations("api_endpoints", a -> a
                        .composite(c -> c
                                .size(1000)
                                .sources(
                                        Map.of(
                                                "uri", CompositeAggregationSource.of(cas -> cas
                                                        .terms(t -> t
                                                                .field("log_details.request_uri.keyword")
                                                        )
                                                ),
                                                "method", CompositeAggregationSource.of(cas -> cas
                                                        .terms(t -> t
                                                                .field("log_details.http_method")
                                                        )
                                                )
                                        )
                                )
                        )
                        .aggregations("error_count", sub -> sub
                                .filter(f -> f
                                        .range(r -> r
                                                .field("log_details.response_status")
                                                .gte(JsonData.of(400))
                                        )
                                )
                        )
                        .aggregations("avg_response_time", sub -> sub
                                .avg(avg -> avg
                                        .field("log_details.execution_time")
                                )
                        )
                        .aggregations("max_timestamp", sub -> sub
                                .max(max -> max
                                        .field("timestamp")
                                )
                        )
                )
        );
    }

    private Map<String, ApiEndpointStats> parseApiEndpointStatistics(SearchResponse<Void> response) {
        Map<String, ApiEndpointStats> statsMap = new HashMap<>();

        if (Objects.isNull(response.aggregations()) ||
                Objects.isNull(response.aggregations().get("api_endpoints"))) {
            return statsMap;
        }

        CompositeAggregate composite = response.aggregations()
                .get("api_endpoints")
                .composite();

        for (CompositeBucket bucket : composite.buckets().array()) {
            String uri = bucket.key().get("uri").stringValue();
            String method = bucket.key().get("method").stringValue();
            long totalRequests = bucket.docCount();

            long errorCount = bucket.aggregations()
                    .get("error_count")
                    .filter()
                    .docCount();

            Double avgResponseTime = bucket.aggregations()
                    .get("avg_response_time")
                    .avg()
                    .value();

            Double maxTimestamp = bucket.aggregations()
                    .get("max_timestamp")
                    .max()
                    .value();

            String key = method + ":" + uri;
            statsMap.put(key, new ApiEndpointStats(
                    uri,
                    method,
                    totalRequests,
                    errorCount,
                    avgResponseTime,
                    maxTimestamp
            ));
        }

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
