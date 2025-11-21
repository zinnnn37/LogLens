package S13P31A306.loglens.domain.project.scheduler;

import S13P31A306.loglens.domain.dashboard.dto.opensearch.ApiEndpointStats;
import S13P31A306.loglens.domain.project.entity.LogMetrics;
import S13P31A306.loglens.domain.project.entity.Project;
import S13P31A306.loglens.domain.project.repository.LogMetricsRepository;
import S13P31A306.loglens.domain.project.repository.ProjectRepository;
import S13P31A306.loglens.domain.project.service.ApiEndpointService;
import S13P31A306.loglens.domain.project.service.LogMetricsTransactionalService;
import S13P31A306.loglens.domain.project.service.ApiEndpointTransactionalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static S13P31A306.loglens.domain.project.constants.LogMetricsConstants.LOG_METRICS_AGGREGATION_CRON;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogMetricsBatchScheduler {

    private static final String LOG_PREFIX = "[LogMetricsBatchScheduler]";

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private final ProjectRepository projectRepository;
    private final LogMetricsRepository logMetricsRepository;
    private final LogMetricsTransactionalService logMetricsTransactionalService;
    private final ApiEndpointTransactionalService apiEndpointTransactionalService;


    @Scheduled(cron = LOG_METRICS_AGGREGATION_CRON)
    public void aggregateAllProjectsMetrics() {
        // 이미 실행 중이면 스킵
        if (!isRunning.compareAndSet(false, true)) {
            log.warn("{} 이전 배치가 아직 실행 중입니다. 스킵합니다.", LOG_PREFIX);
            return;
        }

        try {
            log.info("{} 전체 프로젝트 메트릭 배치 집계 시작", LOG_PREFIX);
            long startTime = System.currentTimeMillis();

            List<Project> projects = projectRepository.findAll();
            int successCount = 0;
            int skipCount = 0;
            int failCount = 0;

            for (Project project : projects) {
                try {
                    boolean aggregated = aggregateProjectIncremental(project);
                    if (aggregated) {
                        successCount++;
                        // LogMetrics 집계 성공 시 API 엔드포인트 메트릭도 집계
                        aggregateApiEndpointMetrics(project);
                    } else {
                        skipCount++;
                    }
                } catch (Exception e) {
                    failCount++;
                    log.error("{} 프로젝트 집계 실패: projectId={}", LOG_PREFIX, project.getId(), e);
                }
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("{} 배치 집계 완료 - 전체: {}, 성공: {}, 스킵: {}, 실패: {}, 소요시간: {}ms",
                    LOG_PREFIX, projects.size(), successCount, skipCount, failCount, elapsed);

        } finally {
            isRunning.set(false);
        }
    }

    private boolean aggregateProjectIncremental(Project project) {
        LogMetrics previous = logMetricsRepository
                .findTopByProjectIdOrderByAggregatedAtDesc(project.getId())
                .orElse(null);

        LocalDateTime from = previous != null
                ? previous.getAggregatedAt()
                : project.getCreatedAt();

        LocalDateTime to = LocalDateTime.now();

        // 1분 이내 업데이트된 경우 스킵
        if (!from.isBefore(to.minusMinutes(1))) {
            log.debug("{} 최근 집계됨, 스킵: projectId={}, lastAggregatedAt={}",
                    LOG_PREFIX, project.getId(), from);
            return false;
        }

        log.debug("{} 프로젝트 증분 집계 시작: projectId={}, from={}, to={}",
                LOG_PREFIX, project.getId(), from, to);

        logMetricsTransactionalService.aggregateProjectMetricsIncremental(project, from, to, previous);

        log.debug("{} 프로젝트 증분 집계 완료: projectId={}", LOG_PREFIX, project.getId());
        return true;
    }

    /**
     * API 엔드포인트 메트릭 집계
     * OpenSearch 조회는 트랜잭션 밖에서, DB 저장만 트랜잭션 처리
     */
    private void aggregateApiEndpointMetrics(Project project) {
        LocalDateTime endTime = LocalDateTime.now().minusMinutes(10);
        LocalDateTime startTime = endTime.minusMinutes(5);

        apiEndpointTransactionalService.aggregateApiEndpointMetrics(project, startTime, endTime);
    }
}
