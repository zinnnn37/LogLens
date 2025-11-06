package S13P31A306.loglens.domain.dashboard.dto;

import S13P31A306.loglens.domain.dashboard.dto.response.ComponentDependencyResponse;

public record GraphMetricsSummary(
        Integer totalComponents,      // 전체 컴포넌트 수
        Integer totalCalls,            // 전체 호출 수
        Integer totalErrors,           // 전체 에러 수
        Integer totalWarns,            // 전체 경고 수
        Double averageErrorRate,       // 평균 에러율
        ComponentDependencyResponse.ComponentMetricRank highestErrorComponent,  // 에러가 가장 많은 컴포넌트
        ComponentDependencyResponse.ComponentMetricRank highestCallComponent    // 호출이 가장 많은 컴포넌트
) {}
