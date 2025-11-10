package S13P31A306.loglens.domain.project.service;

/**
 * 로그 메트릭 배치 집계 서비스
 */
public interface LogMetricsBatchService {

    /**
     * 모든 프로젝트의 로그 메트릭 집계
     * 스케줄러에서 주기적으로 호출
     */
    void aggregateAllProjects();
}
