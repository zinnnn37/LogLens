package S13P31A306.loglens.domain.project.scheduler;

import S13P31A306.loglens.domain.project.service.LogMetricsBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 로그 메트릭 배치 스케줄러
 * 5분마다 실행되어 모든 프로젝트의 로그 메트릭을 집계
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogMetricsScheduler {

    private static final String LOG_PREFIX = "[LogMetricsScheduler]";

    private final LogMetricsBatchService logMetricsBatchService;

    /**
     * 로그 메트릭 집계 스케줄러
     * 5분마다 실행(0'30", 10'30", 20'30", ...)
     */
    @Scheduled(cron = "30 */10 * * * *")
    public void aggregateLogMetrics() {
        log.info("{} 로그 메트릭 집계 시작", LOG_PREFIX);

        try {
            logMetricsBatchService.aggregateAllProjects();
            log.info("{} 로그 메트릭 집계 완료", LOG_PREFIX);
        } catch (Exception e) {
            log.error("{} 로그 메트릭 집계 실패", LOG_PREFIX, e);
        }
    }

}
