package S13P31A306.loglens.domain.alert.scheduler;

import S13P31A306.loglens.domain.alert.service.AlertMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 알림 모니터링 스케줄러
 * 주기적으로 프로젝트의 메트릭을 확인하고 임계치 초과 시 알림을 생성합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertMonitoringScheduler {

    private static final String LOG_PREFIX = "[AlertMonitoringScheduler]";

    private final AlertMonitoringService alertMonitoringService;

    /**
     * 알림 모니터링 스케줄러
     * 매 15초마다 실행 (예: 10:00:00, 10:00:15, 10:00:30, 10:00:45)
     */
    @Scheduled(cron = "*/15 * * * * *")
    public void checkAlerts() {
        log.info("{} 알림 모니터링 스케줄러 시작", LOG_PREFIX);

        try {
            alertMonitoringService.checkAndCreateAlerts();
            log.info("{} 알림 모니터링 스케줄러 완료", LOG_PREFIX);
        } catch (Exception e) {
            log.error("{} 알림 모니터링 스케줄러 실행 중 오류 발생", LOG_PREFIX, e);
        }
    }
}
