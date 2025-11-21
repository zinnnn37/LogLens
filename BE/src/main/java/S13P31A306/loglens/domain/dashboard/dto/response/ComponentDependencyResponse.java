package S13P31A306.loglens.domain.dashboard.dto.response;

import S13P31A306.loglens.domain.dashboard.dto.FrontendMetricsSummary;
import S13P31A306.loglens.domain.dashboard.dto.GraphMetricsSummary;
import S13P31A306.loglens.domain.dependency.dto.response.DependencyGraphResponse;

import java.util.List;

public record ComponentDependencyResponse(
        List<ComponentInfo> components,
        DependencyGraphResponse graph,
        GraphMetricsSummary graphSummary,  // ✅ 전체 메트릭 추가
        FrontendMetricsSummary frontendSummary
) {

    /**
     * 순위 정보 (Top N에 사용)
     */
    public record ComponentMetricRank(
            Integer componentId,
            String componentName,
            Integer value,
            Double errorRate
    ) {}
}
