package S13P31A306.loglens.domain.project.service;

/**
 * 로그 메트릭 배치 집계 서비스
 */
public interface LogMetricsService {

    /**
     * 특정 프로젝트의 로그 메트릭을 증분 집계합니다.
     * 마지막 집계 시점 이후의 데이터만 조회하여 기존 누적 값에 합산합니다.
     * 프로젝트 대시보드 진입 시 호출되어 최신 데이터를 반영합니다.
     *
     * @param projectId 집계할 프로젝트 ID
     */
    void aggregateProjectMetricsOnDemand(Integer projectId);

}
