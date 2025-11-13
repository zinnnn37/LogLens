package S13P31A306.loglens.domain.project.service.impl;

import S13P31A306.loglens.domain.auth.util.AuthenticationHelper;
import S13P31A306.loglens.domain.project.entity.LogMetrics;
import S13P31A306.loglens.domain.project.entity.Project;
import S13P31A306.loglens.domain.project.repository.LogMetricsRepository;
import S13P31A306.loglens.domain.project.repository.ProjectRepository;
import S13P31A306.loglens.domain.project.service.LogMetricsService;
import S13P31A306.loglens.domain.project.service.LogMetricsTransactionalService;
import S13P31A306.loglens.domain.project.validator.ProjectValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 로그 메트릭 집계를 관리하는 서비스
 * 동시성 제어 및 집계 조건 검증을 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogMetricsServiceImpl implements LogMetricsService {

    private static final String LOG_PREFIX = "[LogMetricsService]";

    private final ProjectRepository projectRepository;
    private final LogMetricsRepository logMetricsRepository;
    private final LogMetricsTransactionalService transactionalService;
    private final ProjectValidator projectValidator;

    private final ConcurrentHashMap<Integer, ReentrantLock> projectLocks = new ConcurrentHashMap<>();

    @Override
    public void aggregateProjectMetricsOnDemand(Integer projectId) {
        ReentrantLock lock = projectLocks.computeIfAbsent(projectId, k -> new ReentrantLock());

        if (!lock.tryLock()) {
            log.info("{} Project {} is already being aggregated, skipping", LOG_PREFIX, projectId);
            return;
        }

        try {
            Project project = projectValidator.validateProjectExists(projectId);

            LogMetrics previous = logMetricsRepository
                    .findTopByProjectIdOrderByAggregatedAtDesc(projectId)
                    .orElse(null);

            LocalDateTime lastAggregatedAt = previous != null
                    ? previous.getAggregatedAt()
                    : project.getCreatedAt();

            LocalDateTime now = LocalDateTime.now();

            // 1분 이상 차이나면 업데이트
            if (lastAggregatedAt.isBefore(now.minusMinutes(1))) {
                transactionalService.aggregateProjectMetricsIncremental(project, lastAggregatedAt, now, previous);
                log.info("{} Incremental aggregation completed for project {} (from: {}, to: {})",
                        LOG_PREFIX, projectId, lastAggregatedAt, now);
            } else {
                log.debug("{} Project {} was recently aggregated, skipping", LOG_PREFIX, projectId);
            }

        } catch (Exception e) {
            log.error("{} Failed to aggregate metrics for project {}", LOG_PREFIX, projectId, e);
        } finally {
            lock.unlock();
        }
    }
}
