package S13P31A306.loglens.domain.dashboard.dto.response;

import S13P31A306.loglens.domain.dependency.dto.response.DependencyGraphResponse;

import java.util.List;

public record ComponentDependencyResponse(
        List<ComponentInfo> components,
        DependencyGraphResponse graph,
        GraphMetricsSummary summary  // ✅ 전체 메트릭 추가
) {
    /**
     * 모든 컴포넌트의 메트릭을 합산한 요약 정보
     */
    public record GraphMetricsSummary(
            Integer totalComponents,      // 전체 컴포넌트 수
            Integer totalCalls,            // 전체 호출 수
            Integer totalErrors,           // 전체 에러 수
            Integer totalWarns,            // 전체 경고 수
            Double averageErrorRate,       // 평균 에러율
            ComponentMetricRank highestErrorComponent,  // 에러가 가장 많은 컴포넌트
            ComponentMetricRank highestCallComponent    // 호출이 가장 많은 컴포넌트
    ) {}

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
