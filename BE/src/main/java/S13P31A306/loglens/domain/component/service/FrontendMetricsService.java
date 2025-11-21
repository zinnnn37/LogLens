package S13P31A306.loglens.domain.component.service;

import S13P31A306.loglens.domain.dashboard.dto.FrontendMetricsSummary;

/**
 * Frontend 메트릭 서비스
 */
public interface FrontendMetricsService {

    /**
     * 프로젝트의 최신 Frontend 메트릭 조회
     *
     * @param projectId 프로젝트 ID
     * @return Frontend 메트릭 (없으면 기본값)
     */
    FrontendMetricsSummary getFrontendMetricsByProjectId(Integer projectId);
}
