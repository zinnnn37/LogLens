package S13P31A306.loglens.domain.project.service.impl;

import S13P31A306.loglens.domain.project.entity.LogMetrics;
import S13P31A306.loglens.domain.project.entity.Project;
import S13P31A306.loglens.domain.project.repository.LogMetricsRepository;
import S13P31A306.loglens.domain.project.repository.ProjectRepository;
import S13P31A306.loglens.domain.project.service.LogMetricsBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.aggregations.Aggregation;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.json.JsonData;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static S13P31A306.loglens.domain.project.constants.LogMetricsConstants.AGGREGATION_INTERVAL_MINUTES;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LogMetricsBatchServiceImpl implements LogMetricsBatchService {

    private static final String LOG_PREFIX = "[LogMetricsBatchService]";
    private static final String INDEX_NAME = "logs-*";

    private final ProjectRepository projectRepository;
    private final LogMetricsRepository logMetricsRepository;
    private final OpenSearchClient openSearchClient;

    @Override
    @Transactional
    public void aggregateAllProjects() {
        log.info("{} 전체 프로젝트 로그 메트릭 집계 시작", LOG_PREFIX);

        List<Project> projects = projectRepository.findAll();
        LocalDateTime aggregatedAt = getCurrentAggregationTime();

        int successCount = 0;
        int failCount = 0;

        for (Project project : projects) {
            try {
                aggregateProjectMetrics(project, aggregatedAt);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("{} 프로젝트 {} 집계 실패", LOG_PREFIX, project.getProjectUuid(), e);
            }
        }

        log.info("{} 로그 메트릭 집계 완료 - 성공: {}, 실패: {}", LOG_PREFIX, successCount, failCount);
    }

    /**
     * 특정 프로젝트의 로그 메트릭 집계
     *
     * @param project 집계 대상 프로젝트
     * @param aggregatedAt 집계 시간
     */
    private void aggregateProjectMetrics(Project project, LocalDateTime aggregatedAt) throws Exception {
        // 중복 집계 방지
        if (logMetricsRepository.existsByProjectIdAndAggregatedAt(project.getId(), aggregatedAt)) {
            log.info("{} 프로젝트 {} - 이미 집계된 데이터 존재", LOG_PREFIX, project.getProjectUuid());
            return;
        }

        // 최근 10분간 데이터 조회
        LocalDateTime startTime = aggregatedAt.minusMinutes(AGGREGATION_INTERVAL_MINUTES);

        // OpenSearch 쿼리 실행
        SearchResponse<Void> response = executeOpenSearchQuery(project.getProjectUuid(), startTime, aggregatedAt);

        // 집계 결과 추출
        Map<String, Long> logLevelCounts = extractLogLevelCounts(response);
        Integer avgResponseTime = extractAvgResponseTime(response);

        // LogMetrics 저장
        LogMetrics metrics = LogMetrics.builder()
                .project(project)
                .totalLogs(logLevelCounts.values().stream().mapToInt(Long::intValue).sum())
                .errorLogs(logLevelCounts.getOrDefault("ERROR", 0L).intValue())
                .warnLogs(logLevelCounts.getOrDefault("WARN", 0L).intValue())
                .infoLogs(logLevelCounts.getOrDefault("INFO", 0L).intValue())
                .avgResponseTime(avgResponseTime)
                .aggregatedAt(aggregatedAt)
                .build();

        logMetricsRepository.save(metrics);
        log.info("{} 프로젝트 {} 집계 완료 - 총 로그: {}",
                LOG_PREFIX, project.getProjectUuid(), metrics.getTotalLogs());
    }

    /**
     * OpenSearch 쿼리 실행
     *
     * @param projectUuid 프로젝트 UUID
     * @param startTime 조회 시작 시간
     * @param endTime 조회 종료 시간
     * @return SearchResponse OpenSearch 응답
     */
    private SearchResponse<Void> executeOpenSearchQuery(
            String projectUuid,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) throws Exception {
        // 시간 범위 쿼리
        Query timeRangeQuery = Query.of(q -> q
                .range(r -> r
                        .field("timestamp")
                        .gte(JsonData.of(startTime.toInstant(ZoneOffset.UTC).toString()))
                        .lt(JsonData.of(endTime.toInstant(ZoneOffset.UTC).toString()))
                )
        );

        // 프로젝트 UUID 필터
        Query projectQuery = Query.of(q -> q
                .term(t -> t
                        .field("project_uuid")
                        .value(v -> v.stringValue(projectUuid))
                )
        );

        // 조합 쿼리
        Query combinedQuery = Query.of(q -> q
                .bool(b -> b
                        .must(timeRangeQuery, projectQuery)
                )
        );

        // Aggregation: log_level별 count
        Aggregation logLevelAgg = Aggregation.of(a -> a
                .terms(t -> t
                        .field("log_level")
                        .size(10)
                )
        );

        // Aggregation: duration 평균
        Aggregation avgDurationAgg = Aggregation.of(a -> a
                .avg(avg -> avg
                        .field("duration")
                )
        );

        // SearchRequest 생성
        SearchRequest request = SearchRequest.of(s -> s
                .index(INDEX_NAME)
                .query(combinedQuery)
                .aggregations("log_level_count", logLevelAgg)
                .aggregations("avg_duration", avgDurationAgg)
                .size(0)  // 문서는 필요 없고 집계 결과만
        );

        return openSearchClient.search(request, Void.class);
    }

    /**
     * OpenSearch 응답에서 log_level별 카운트 추출
     *
     * @param response OpenSearch 응답
     * @return Map<String, Long> log_level별 카운트
     */
    private Map<String, Long> extractLogLevelCounts(SearchResponse<Void> response) {
        return response.aggregations()
                .get("log_level_count")
                .sterms()
                .buckets()
                .array()
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        StringTermsBucket::key,
                        StringTermsBucket::docCount
                ));
    }

    /**
     * OpenSearch 응답에서 평균 응답시간 추출
     *
     * @param response OpenSearch 응답
     * @return Integer 평균 응답시간 (ms)
     */
    private Integer extractAvgResponseTime(SearchResponse<Void> response) {
        Double avgDuration = response.aggregations()
                .get("avg_duration")
                .avg()
                .value();

        if (Objects.isNull(avgDuration) || avgDuration.isNaN()) {
            return 0;
        }

        return (int) Math.round(avgDuration);
    }

    /**
     * 현재 집계 시간 계산 (10분 단위로 절사)
     *
     * @return LocalDateTime 집계 기준 시간
     */
    private LocalDateTime getCurrentAggregationTime() {
        LocalDateTime now = LocalDateTime.now();
        int minute = (now.getMinute() / AGGREGATION_INTERVAL_MINUTES) * AGGREGATION_INTERVAL_MINUTES;
        return now.withMinute(minute).withSecond(0).withNano(0);
    }
}
