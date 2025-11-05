package S13P31A306.loglens.domain.dashboard.service.impl;

import S13P31A306.loglens.domain.component.entity.Component;
import S13P31A306.loglens.domain.component.entity.ComponentMetrics;
import S13P31A306.loglens.domain.component.service.ComponentMetricsService;
import S13P31A306.loglens.domain.component.service.ComponentService;
import S13P31A306.loglens.domain.dashboard.dto.response.*;
import S13P31A306.loglens.domain.dashboard.mapper.DashboardMapper;
import S13P31A306.loglens.domain.dashboard.dto.response.DashboardOverviewResponse;
import S13P31A306.loglens.domain.dashboard.service.DashboardService;
import S13P31A306.loglens.domain.dashboard.validator.DashboardValidator;
import S13P31A306.loglens.domain.dependency.dto.response.DependencyGraphResponse;
import S13P31A306.loglens.domain.dependency.entity.DependencyGraph;
import S13P31A306.loglens.domain.dependency.service.DependencyGraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.opensearch.client.opensearch.OpenSearchClient;
import S13P31A306.loglens.domain.project.repository.ProjectMemberRepository;
import S13P31A306.loglens.domain.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private static final String LOG_PREFIX = "[DashboardService]";

    private final ComponentService componentService;
    private final ComponentMetricsService componentMetricsService;
    private final DependencyGraphService dependencyGraphService;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final DashboardValidator validator;
    private final DashboardMapper mapper;
//    private final OpenSearchClient openSearchClient;

    @Override
    public DashboardOverviewResponse getStatisticsOverview(String projectUuid, String startTime, String endTime) {
        log.info("{} 대시보드 통계 개요 조회 시도", LOG_PREFIX);

        // 권한 검증
        validator.validateProjectAccess(projectUuid);



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

        Integer projectId = validator.validateProjectAccess(projectUuid, userDetails);
        validator.validateComponentAccess(componentId, projectId);

        List<DependencyGraph> allDependencies = dependencyGraphService.findAllDependenciesByComponentId(componentId);

        // 3. 관련된 모든 컴포넌트 ID 수집
        Set<Integer> allComponentIds = new HashSet<>();
        allComponentIds.add(componentId);
        allDependencies.forEach(edge -> {
            allComponentIds.add(edge.getFrom());
            allComponentIds.add(edge.getTo());
        });

        // 4. 컴포넌트 정보 조회
        Map<Integer, Component> componentMap = componentService
                .getComponentMapByIds(allComponentIds);

        // 5. 메트릭 정보 조회
        Map<Integer, ComponentMetrics> metricsMap = componentMetricsService
                .getMetricsByComponentIds(new ArrayList<>(allComponentIds));

        // 6. ComponentInfo 리스트 생성
        List<ComponentInfo> componentInfos = ComponentInfo.fromMaps(
                allComponentIds,
                componentMap,
                metricsMap
        );

        // 7. 그래프 생성
        DependencyGraphResponse graph = DependencyGraphResponse.from(allDependencies);

        log.debug("{} 컴포넌트 의존성 조회 완료: componentId={}, totalComponents={}, edges={}",
                LOG_PREFIX, componentId, componentInfos.size(), graph.edges().size());

        return new ComponentDependencyResponse(componentInfos, graph);
    }
}
