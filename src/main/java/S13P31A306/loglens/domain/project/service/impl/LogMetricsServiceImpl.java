package S13P31A306.loglens.domain.project.service.impl;

import S13P31A306.loglens.domain.project.entity.LogMetrics;
import S13P31A306.loglens.domain.project.entity.Project;
import S13P31A306.loglens.domain.project.repository.LogMetricsRepository;
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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogMetricsServiceImpl implements LogMetricsService {

    private static final String LOG_PREFIX = "[LogMetricsService]";

    private final LogMetricsRepository logMetricsRepository;
    private final LogMetricsTransactionalService transactionalService;
    private final ProjectValidator projectValidator;

    private final ConcurrentHashMap<Integer, ReentrantLock> projectLocks = new ConcurrentHashMap<>();

    @Override
    public void aggregateProjectMetricsOnDemand(Integer projectId) {
        ReentrantLock lock = projectLocks.computeIfAbsent(projectId, k -> new ReentrantLock());

        if (!lock.tryLock()) {
            log.info("{} 프로젝트 이미 집계 중, 스킵: projectId={}", LOG_PREFIX, projectId);
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

            // 1분 이상 경과 시에만 증분 집계
            if (lastAggregatedAt.isBefore(now.minusMinutes(1))) {
                log.info("{} 온디맨드 증분 집계 시작: projectId={}, from={}, to={}",
                        LOG_PREFIX, projectId, lastAggregatedAt, now);

                transactionalService.aggregateProjectMetricsIncremental(
                        project, lastAggregatedAt, now, previous
                );

                log.info("{} 온디맨드 증분 집계 완료: projectId={}", LOG_PREFIX, projectId);
            } else {
                log.debug("{} 최근 집계됨, 스킵: projectId={}, lastAggregatedAt={}",
                        LOG_PREFIX, projectId, lastAggregatedAt);
            }

        } catch (Exception e) {
            log.error("{} 온디맨드 집계 실패: projectId={}", LOG_PREFIX, projectId, e);
        } finally {
            lock.unlock();
        }
    }

}
