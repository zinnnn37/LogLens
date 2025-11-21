package S13P31A306.loglens.domain.alert.service;

/**
 * 알림 모니터링 서비스 인터페이스
 * 주기적으로 프로젝트의 메트릭을 확인하고 임계치 초과 시 알림을 생성합니다.
 */
public interface AlertMonitoringService {

    /**
     * 모든 프로젝트의 알림 조건을 확인하고 필요 시 알림을 생성합니다.
     * 스케줄러에 의해 주기적으로 호출됩니다.
     */
    void checkAndCreateAlerts();
}
