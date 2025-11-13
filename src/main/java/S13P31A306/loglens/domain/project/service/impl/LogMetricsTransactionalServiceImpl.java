package S13P31A306.loglens.domain.project.service.impl;

import S13P31A306.loglens.domain.project.entity.LogMetrics;
import S13P31A306.loglens.domain.project.entity.Project;
import S13P31A306.loglens.domain.project.repository.LogMetricsRepository;
import S13P31A306.loglens.domain.project.service.LogMetricsTransactionalService;
import S13P31A306.loglens.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.json.JsonData;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import static S13P31A306.loglens.global.constants.GlobalErrorCode.OPENSEARCH_OPERATION_FAILED;

/**
 * 로그 메트릭 집계의 트랜잭션 처리를 담당하는 서비스
 * OpenSearch 조회는 트랜잭션 밖에서 수행하고, DB 저장만 트랜잭션으로 처리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogMetricsTransactionalServiceImpl implements LogMetricsTransactionalService {

    private static final String LOG_PREFIX = "[LogMetricsTransactionalService]";

    private final LogMetricsRepository logMetricsRepository;
    private final OpenSearchClient openSearchClient;

    @Override
    public void aggregateProjectMetricsIncremental(
            Project project,
            LocalDateTime from,
            LocalDateTime to,
            LogMetrics previous) {

        long startTime = System.currentTimeMillis();

        try {
            // 1. OpenSearch 조회 (트랜잭션 밖에서 수행)
            String indexPattern = getProjectIndexPattern(project.getProjectUuid());
            SearchRequest searchRequest = buildSearchRequest(indexPattern, from, to);
            SearchResponse<Void> response = openSearchClient.search(searchRequest, Void.class);

            // 2. 메트릭 계산 (트랜잭션 밖에서 수행)
            LogMetrics metrics = calculateCumulativeMetrics(response, project, to, previous);

            // 3. DB 저장만 트랜잭션으로 처리
            saveMetrics(metrics);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("{} 집계 완료: projectId={}, 소요시간={}ms, 증분로그={}",
                    LOG_PREFIX, project.getId(), elapsed, metrics.getTotalLogs() - (previous != null ? previous.getTotalLogs() : 0));

        } catch (Exception e) {
            log.error("{} OpenSearch 집계 실패: projectId={}, from={}, to={}",
                    LOG_PREFIX, project.getId(), from, to, e);
            throw new BusinessException(OPENSEARCH_OPERATION_FAILED);
        }
    }

    /**
     * DB 저장만 트랜잭션으로 처리 (커넥션 점유 시간 최소화)
     *
     * @param metrics 저장할 메트릭
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void saveMetrics(LogMetrics metrics) {
        logMetricsRepository.save(metrics);
    }

    /**
     * OpenSearch 검색 요청을 생성합니다.
     *
     * @param indexPattern 인덱스 패턴
     * @param from 시작 시간
     * @param to 종료 시간
     * @return SearchRequest
     */
    private SearchRequest buildSearchRequest(String indexPattern, LocalDateTime from, LocalDateTime to) {
        return SearchRequest.of(s -> s
                .index(indexPattern)
                .size(0)
                .query(q -> q
                        .range(r -> r
                                .field("timestamp")
                                .gte(JsonData.of(from.atZone(ZoneId.of("Asia/Seoul")).toInstant().toString()))
                                .lt(JsonData.of(to.atZone(ZoneId.of("Asia/Seoul")).toInstant().toString()))
                        )
                )
                .aggregations("total_logs", a -> a
                        .valueCount(v -> v.field("_id"))
                )
                .aggregations("error_logs", a -> a
                        .filter(f -> f
                                .term(t -> t.field("log_level").value(FieldValue.of("ERROR")))
                        )
                        .aggregations("count", sub -> sub
                                .valueCount(v -> v.field("_id"))
                        )
                )
                .aggregations("warn_logs", a -> a
                        .filter(f -> f
                                .term(t -> t.field("log_level").value(FieldValue.of("WARN")))
                        )
                        .aggregations("count", sub -> sub
                                .valueCount(v -> v.field("_id"))
                        )
                )
                .aggregations("info_logs", a -> a
                        .filter(f -> f
                                .term(t -> t.field("log_level").value(FieldValue.of("INFO")))
                        )
                        .aggregations("count", sub -> sub
                                .valueCount(v -> v.field("_id"))
                        )
                )
                .aggregations("sum_response_time", a -> a
                        .sum(sum -> sum.field("duration"))
                )
        );
    }

    /**
     * 프로젝트별 인덱스 패턴을 반환합니다.
     *
     * @param projectUuid 프로젝트 UUID (하이픈 포함)
     * @return "{projectUuid_with_underscores}_*" 형식의 인덱스 패턴
     */
    private String getProjectIndexPattern(String projectUuid) {
        String sanitizedUuid = projectUuid.replace("-", "_");
        return sanitizedUuid + "_*";
    }

    /**
     * OpenSearch 집계 결과를 파싱하여 누적 메트릭을 계산합니다.
     * Null 안전성을 보장하기 위해 모든 집계 값에 대해 null 체크를 수행합니다.
     *
     * @param response OpenSearch 응답
     * @param project 프로젝트
     * @param aggregatedAt 집계 시점
     * @param previous 이전 누적 메트릭 (null 가능)
     * @return 새로운 누적 메트릭
     */
    private LogMetrics calculateCumulativeMetrics(
            SearchResponse<Void> response,
            Project project,
            LocalDateTime aggregatedAt,
            LogMetrics previous) {

        Map<String, Aggregate> aggs = response.aggregations();

        // 증분 값 추출 (Null 안전 처리)
        long incrementalTotal = extractValueCount(aggs, "total_logs");
        long incrementalErrors = extractNestedValueCount(aggs, "error_logs");
        long incrementalWarns = extractNestedValueCount(aggs, "warn_logs");
        long incrementalInfos = extractNestedValueCount(aggs, "info_logs");
        long incrementalSumResponseTime = extractSumValue(aggs);

        // 누적 계산 (단순 합산)
        long newTotalLogs = (previous != null ? previous.getTotalLogs() : 0) + incrementalTotal;
        long newErrorLogs = (previous != null ? previous.getErrorLogs() : 0) + incrementalErrors;
        long newWarnLogs = (previous != null ? previous.getWarnLogs() : 0) + incrementalWarns;
        long newInfoLogs = (previous != null ? previous.getInfoLogs() : 0) + incrementalInfos;
        long newSumResponseTime = (previous != null ? previous.getSumResponseTime() : 0L) + incrementalSumResponseTime;

        int newAvgResponseTime = newTotalLogs > 0
                ? (int) (newSumResponseTime / newTotalLogs)
                : 0;

        return LogMetrics.builder()
                .project(project)
                .totalLogs((int) newTotalLogs)
                .errorLogs((int) newErrorLogs)
                .warnLogs((int) newWarnLogs)
                .infoLogs((int) newInfoLogs)
                .sumResponseTime(newSumResponseTime)
                .avgResponseTime(newAvgResponseTime)
                .aggregatedAt(aggregatedAt)
                .build();
    }

    /**
     * ValueCount 집계 값을 안전하게 추출합니다.
     *
     * @param aggregations 집계 맵
     * @param aggName 집계 이름
     * @return 추출된 값 (없으면 0)
     */
    private long extractValueCount(Map<String, Aggregate> aggregations, String aggName) {
        Aggregate agg = aggregations.get(aggName);
        if (agg != null && agg.isValueCount()) {
            Double value = agg.valueCount().value();
            if (value != null && !value.isNaN()) {
                return value.longValue();
            }
        }
        log.warn("{} {} aggregation not found or invalid type", LOG_PREFIX, aggName);
        return 0L;
    }

    /**
     * Filter 내부의 ValueCount 집계 값을 안전하게 추출합니다.
     *
     * @param aggregations 집계 맵
     * @param filterName 필터 이름
     * @return 추출된 값 (없으면 0)
     */
    private long extractNestedValueCount(Map<String, Aggregate> aggregations, String filterName) {
        Aggregate filterAgg = aggregations.get(filterName);
        if (filterAgg != null && filterAgg.isFilter()) {
            Map<String, Aggregate> subAggs = filterAgg.filter().aggregations();
            return extractValueCount(subAggs, "count");
        }
        log.warn("{} {} filter aggregation not found", LOG_PREFIX, filterName);
        return 0L;
    }

    /**
     * Sum 집계 값을 안전하게 추출합니다.
     *
     * @param aggregations 집계 맵
     * @return 추출된 값 (없거나 NaN이면 0)
     */
    private long extractSumValue(Map<String, Aggregate> aggregations) {
        Aggregate agg = aggregations.get("sum_response_time");
        if (agg != null && agg.isSum()) {
            Double value = agg.sum().value();
            if (value != null && !value.isNaN()) {
                return value.longValue();
            }
        }
        log.warn("{} sum_response_time aggregation not found or invalid", LOG_PREFIX);
        return 0L;
    }
}
