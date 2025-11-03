package S13P31A306.loglens.domain.dashboard.service;

import S13P31A306.loglens.domain.dashboard.dto.response.DashboardOverviewResponse;

public interface DashboardService {

    /**
     * 프로젝트 로그 통계 조회
     *
     * @param projectId 프로젝트 ID
     * @param startTime 출력 데이터 필터 시작 시간
     * @param endTime 출력 데이터 필터 종료 시간
     */
    DashboardOverviewResponse getStatisticsOverview(int projectId, String startTime, String endTime);

}
