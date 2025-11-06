package S13P31A306.loglens.domain.dashboard.service.impl;

import S13P31A306.loglens.domain.component.entity.Component;
import S13P31A306.loglens.domain.component.entity.ComponentMetrics;
import S13P31A306.loglens.domain.component.service.ComponentMetricsService;
import S13P31A306.loglens.domain.component.service.ComponentService;
import S13P31A306.loglens.domain.component.service.FrontendMetricsService;
import S13P31A306.loglens.domain.dashboard.dto.FrontendMetricsSummary;
import S13P31A306.loglens.domain.dashboard.dto.GraphMetricsSummary;
import S13P31A306.loglens.domain.dashboard.dto.response.*;
import S13P31A306.loglens.domain.dashboard.mapper.DashboardMapper;
import S13P31A306.loglens.domain.dashboard.service.DashboardService;
import S13P31A306.loglens.domain.dashboard.validator.DashboardValidator;
import S13P31A306.loglens.domain.dependency.dto.response.DependencyGraphResponse;
import S13P31A306.loglens.domain.dependency.entity.DependencyGraph;
import S13P31A306.loglens.domain.dependency.service.DependencyGraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private static final String LOG_PREFIX = "[DashboardService]";

    private final ComponentService componentService;
    private final ComponentMetricsService componentMetricsService;
    private final FrontendMetricsService frontendMetricsService;
    private final DependencyGraphService dependencyGraphService;
    private final DashboardValidator validator;
    private final DashboardMapper mapper;

    @Override
    public DashboardOverviewResponse getStatisticsOverview(int projectId, String startTime, String endTime) {
        return null;
    }

    @Override
    public ProjectComponentsResponse getProjectComponents(
            final String projectUuid,
            final UserDetails userDetails
    ) {
        Integer projectId = validator.validateProjectAccess(projectUuid, userDetails);
        List<Component> components = componentService.getProjectComponents(projectId);

        return mapper.toProjectComponentsResponse(projectId, components);
    }

    @Override
    @Cacheable(value = "componentDependencies", key = "#projectUuid + '::' + #componentId")
    public ComponentDependencyResponse getComponentDependencies(
            final String projectUuid,
            final Integer componentId,
            final UserDetails userDetails) {

        log.debug("{} 컴포넌트 의존성 조회 시작: projectUuid={}, componentId={}",
                LOG_PREFIX, projectUuid, componentId);

        // 1. 권한 검증
        Integer projectId = validator.validateProjectAccess(projectUuid, userDetails);
        validator.validateComponentAccess(componentId, projectId);

        // 2. 전체 컴포넌트 및 의존성 그래프 조회
        List<Component> allProjectComponents = componentService.getProjectComponents(projectId);

        Set<Integer> allComponentIds = allProjectComponents.stream()
                .map(Component::getId)
                .collect(Collectors.toSet());

        List<DependencyGraph> allDependencies = allProjectComponents.stream()
                .flatMap(component -> dependencyGraphService
                        .findAllDependenciesByComponentId(component.getId())
                        .stream())
                .distinct()
                .toList();

        log.debug("{} 전체 그래프 조회: components={}, edges={}",
                LOG_PREFIX, allComponentIds.size(), allDependencies.size());

        // 3. 컴포넌트 정보 Map 생성
        Map<Integer, Component> componentMap = allProjectComponents.stream()
                .collect(Collectors.toMap(Component::getId, component -> component));

        // 4. Backend 메트릭 정보 조회 (DB에서)
        Map<Integer, ComponentMetrics> metricsMap = componentMetricsService
                .getMetricsByComponentIds(new ArrayList<>(allComponentIds));

        // 5. ComponentInfo 리스트 생성
        List<ComponentInfo> componentInfos = ComponentInfo.fromMaps(
                allComponentIds,
                componentMap,
                metricsMap
        );

        // 6. 그래프 생성
        DependencyGraphResponse graph = DependencyGraphResponse.from(allDependencies);

        // 7. Backend 메트릭 요약 생성
        GraphMetricsSummary graphSummary =
                calculateGraphMetricsSummary(componentInfos);

        // 8. Frontend 메트릭 조회 (DB에서)
        FrontendMetricsSummary frontendMetrics =
                frontendMetricsService.getFrontendMetricsByProjectId(projectId);

        log.debug("{} 컴포넌트 의존성 조회 완료: componentId={}, totalComponents={}, edges={}, frontendTraces={}",
                LOG_PREFIX, componentId, componentInfos.size(), graph.edges().size(), frontendMetrics.totalTraces());

        // 9. 응답 생성 (Frontend 메트릭 포함)
        return new ComponentDependencyResponse(
                componentInfos,
                graph,
                graphSummary,
                frontendMetrics  // ✅ Frontend 메트릭 추가
        );
    }

    /**
     * 그래프 전체 메트릭 요약 계산 (Backend만)
     */
    private GraphMetricsSummary calculateGraphMetricsSummary(
            List<ComponentInfo> componentInfos) {

        // 메트릭이 있는 컴포넌트만 필터링
        List<ComponentInfo> componentsWithMetrics = componentInfos.stream()
                .filter(c -> c.metrics() != null && c.metrics().callCount() != null && c.metrics().callCount() > 0)
                .toList();

        if (componentsWithMetrics.isEmpty()) {
            log.debug("{} 메트릭이 있는 컴포넌트가 없음", LOG_PREFIX);
            return new GraphMetricsSummary(
                    componentInfos.size(),
                    0, 0, 0, 0.0,
                    null, null
            );
        }

        // 전체 합산
        int totalCalls = componentsWithMetrics.stream()
                .mapToInt(c -> c.metrics().callCount())
                .sum();

        int totalErrors = componentsWithMetrics.stream()
                .mapToInt(c -> c.metrics().errorCount())
                .sum();

        int totalWarns = componentsWithMetrics.stream()
                .mapToInt(c -> c.metrics().warnCount())
                .sum();

        // 평균 에러율
        double averageErrorRate = componentsWithMetrics.stream()
                .mapToDouble(c -> c.metrics().errorRate())
                .average()
                .orElse(0.0);
        averageErrorRate = Math.round(averageErrorRate * 100.0) / 100.0;

        // 에러가 가장 많은 컴포넌트
        ComponentDependencyResponse.ComponentMetricRank highestErrorComponent =
                componentsWithMetrics.stream()
                        .max(Comparator.comparingInt(c -> c.metrics().errorCount()))
                        .map(c -> new ComponentDependencyResponse.ComponentMetricRank(
                                c.id(),
                                c.name(),
                                c.metrics().errorCount(),
                                c.metrics().errorRate()
                        ))
                        .orElse(null);

        // 호출이 가장 많은 컴포넌트
        ComponentDependencyResponse.ComponentMetricRank highestCallComponent =
                componentsWithMetrics.stream()
                        .max(Comparator.comparingInt(c -> c.metrics().callCount()))
                        .map(c -> new ComponentDependencyResponse.ComponentMetricRank(
                                c.id(),
                                c.name(),
                                c.metrics().callCount(),
                                c.metrics().errorRate()
                        ))
                        .orElse(null);

        log.debug("{} 메트릭 요약 계산 완료: totalCalls={}, totalErrors={}, averageErrorRate={}",
                LOG_PREFIX, totalCalls, totalErrors, averageErrorRate);

        return new GraphMetricsSummary(
                componentInfos.size(),
                totalCalls,
                totalErrors,
                totalWarns,
                averageErrorRate,
                highestErrorComponent,
                highestCallComponent
        );
    }
}
