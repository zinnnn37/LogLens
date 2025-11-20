package S13P31A306.loglens.domain.component.service;

import S13P31A306.loglens.domain.component.dto.MetricsData;
import S13P31A306.loglens.domain.dashboard.dto.FrontendMetricsSummary;

import java.util.Map;

public interface OpenSearchMetricsService {
    /**
     * 특정 프로젝트의 모든 컴포넌트 메트릭을 일괄 조회
     *
     * @param projectUuid 프로젝트 UUID
     * @return componentName -> MetricsData 맵
     */
    Map<String, MetricsData> getProjectMetrics(String projectUuid);

    /**
     * 특정 컴포넌트의 메트릭 조회 (필요시 사용)
     *
     * @param projectUuid 프로젝트 UUID
     * @param componentName 컴포넌트 이름
     * @return 메트릭 데이터
     */
    MetricsData getComponentMetrics(String projectUuid, String componentName);
    FrontendMetricsSummary getFrontendMetrics(String projectUuid);
}
