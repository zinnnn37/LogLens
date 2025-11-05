package S13P31A306.loglens.domain.dashboard.service;

import S13P31A306.loglens.domain.dashboard.dto.response.ComponentDependencyResponse;
import S13P31A306.loglens.domain.dashboard.dto.response.DashboardOverviewResponse;
import S13P31A306.loglens.domain.dashboard.dto.response.ProjectComponentsResponse;
import org.springframework.security.core.userdetails.UserDetails;

public interface DashboardService {

    /**
     * 프로젝트 로그 통계 조회
     *
     * @param projectUuid 프로젝트 UUID
     * @param startTime 출력 데이터 필터 시작 시간
     * @param endTime 출력 데이터 필터 종료 시간
     */
    DashboardOverviewResponse getStatisticsOverview(String projectUuid, String startTime, String endTime);

    ProjectComponentsResponse getProjectComponents(String projectUuid, UserDetails userDetails);

    ComponentDependencyResponse getComponentDependencies(String projectUuid, Integer componentId, UserDetails userDetails);
}
