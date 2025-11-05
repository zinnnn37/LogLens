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
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import S13P31A306.loglens.domain.project.repository.ProjectMemberRepository;
import S13P31A306.loglens.domain.project.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final OpenSearchClient openSearchClient;

    @Override
    public DashboardOverviewResponse getStatisticsOverview(String projectUuid, String startTime, String endTime) {
        log.info("{} 대시보드 통계 개요 조회 시도", LOG_PREFIX);

        // 권한 검증
        validator.validateProjectAccess(projectUuid);

        // 2. 시간 범위 설정 (없으면 최근 7일)
        LocalDateTime end = endTime != null ?
                LocalDateTime.parse(endTime, DateTimeFormatter.ISO_DATE_TIME) :
                LocalDateTime.now();
        LocalDateTime start = startTime != null ?
                LocalDateTime.parse(startTime, DateTimeFormatter.ISO_DATE_TIME) :
                end.minusDays(7);

        // 로그 수 집계

        double avgResponseTime = getAvgResponseTimeFromOpenSearch(projectUuid, start, end);

        log.info("{} 대시보드 통계 개요 조회 완료: avgResponseTime={}",  LOG_PREFIX, avgResponseTime);

        return DashboardOverviewResponse.builder()
                .projectUuid(projectUuid)
                .period(new DashboardOverviewResponse.Period(
                                start.format(DateTimeFormatter.ISO_DATE_TIME),
                                end.format(DateTimeFormatter.ISO_DATE_TIME)
                        ))
                .summary(new DashboardOverviewResponse.Summary(

                ))
                .build();
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

    private double getAvgResponseTimeFromOpenSearch(String projectUuid, LocalDateTime start, LocalDateTime end) {
        try {
            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index("logs")
                    .size(0)
                    .query(q -> q
                            .bool(b -> b
                                    .must(m -> m.term(t -> t
                                            .field("project_uuid.keyword")
                                            .value(v -> v.stringValue(projectUuid))
                                    ))
                                    .must(m -> m.range(r -> r
                                            .field("timestamp")
                                            .gte(JsonData.of(start.toString()))
                                            .lte(JsonData.of(end.toString()))
                                    ))
                            )
                    )
                    .aggregations("avg_duration", a -> a
                            .avg(avg -> avg.field("duration"))
                    )
            );

            log.info("{} OpenSearch 쿼리 실행: projectUuid={}, start={}, end={}",
                    LOG_PREFIX, projectUuid, start, end);

            SearchResponse<Void> response = openSearchClient.search(
                    searchRequest,
                    Void.class
            );

            // null 체크
            if (Objects.isNull(response.aggregations()) ||
                    !response.aggregations().containsKey("avg_duration") ||
                    Objects.isNull(response.aggregations().get("avg_duration").avg())) {
                log.warn("{} OpenSearch 집계 결과 없음", LOG_PREFIX);
                return 0.0;
            }

            Double avgValue = response.aggregations()
                    .get("avg_duration")
                    .avg()
                    .value();

            // NaN 체크
            if (Objects.isNull(avgValue) || avgValue.isNaN() || avgValue.isInfinite()) {
                log.warn("{} 비정상 avg_duration: {}", LOG_PREFIX, avgValue);
                return 0.0;
            }

            log.info("{} 평균 응답 시간: {}ms", LOG_PREFIX, avgValue);

            return avgValue;

        } catch (IOException e) {
            log.error("{} OpenSearch 응답 시간 조회 실패: {}", LOG_PREFIX, e.getMessage());
            return 0.0;
        }
    }

}
