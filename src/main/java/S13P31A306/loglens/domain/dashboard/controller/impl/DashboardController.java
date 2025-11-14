package S13P31A306.loglens.domain.dashboard.controller.impl;

import S13P31A306.loglens.domain.dashboard.constants.DashboardSuccessCode;
import S13P31A306.loglens.domain.dashboard.controller.DashboardApi;
import S13P31A306.loglens.domain.dashboard.dto.response.*;
import S13P31A306.loglens.domain.dashboard.service.ApiEndpointService;
import S13P31A306.loglens.domain.dashboard.service.DashboardService;
import S13P31A306.loglens.domain.dashboard.service.HeatmapService;
import S13P31A306.loglens.domain.dashboard.service.TopFrequentErrorsService;
import S13P31A306.loglens.domain.project.entity.LogMetrics;
import S13P31A306.loglens.domain.project.entity.Project;
import S13P31A306.loglens.domain.project.repository.LogMetricsRepository;
import S13P31A306.loglens.domain.project.repository.ProjectRepository;
import S13P31A306.loglens.domain.project.service.LogMetricsTransactionalService;
import S13P31A306.loglens.global.annotation.ValidUuid;
import S13P31A306.loglens.global.dto.response.ApiResponseFactory;
import S13P31A306.loglens.global.dto.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardController implements DashboardApi {

    private static final String LOG_PREFIX = "[DashboardController]";

    private final DashboardService dashboardService;
    private final TopFrequentErrorsService topFrequentErrorsService;
    private final ApiEndpointService apiEndpointService;
    private final HeatmapService heatmapService;

    // TODO: 추후 삭제
    private final ProjectRepository projectRepository;
    private final LogMetricsRepository logMetricsRepository;
    private final LogMetricsTransactionalService logMetricsTransactionalService;

    /**
     * 통계 개요 조회
     */
    @GetMapping("/statistics/overview")
    public ResponseEntity<? extends BaseResponse> getStatisticsOverview(
            @ValidUuid @RequestParam String projectUuid,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    ) {
        log.info("{} 대시보드 통계 api 호출", LOG_PREFIX);

        DashboardOverviewResponse response = dashboardService.getStatisticsOverview(projectUuid, startTime, endTime);
        return ApiResponseFactory.success(
                DashboardSuccessCode.OVERVIEW_RETRIEVED,
                response
        );
    }

    /**
     * 자주 발생하는 에러 Top 10 조회
     */
    @Override
    @GetMapping("/errors/top")
    public ResponseEntity<? extends BaseResponse> getTopFrequentErrors(
            @ValidUuid @RequestParam String projectUuid,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    ) {
        log.info("{} 자주 발생하는 에러 TOP {} api 호출", LOG_PREFIX, limit);

        TopFrequentErrorsResponse response = topFrequentErrorsService.getTopFrequentErrors(projectUuid, startTime, endTime, limit);
        return ApiResponseFactory.success(
                DashboardSuccessCode.FREQUENT_ERROR_RETRIEVED,
                response
        );
    }

    /**
     * API 호출 통계 조회
     */
    @Override
    @GetMapping("/statistics/api-calls")
    public ResponseEntity<? extends BaseResponse> getApiCallStatistics(
            @ValidUuid @RequestParam String projectUuid,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) Integer limit
    ) {
        log.info("{} API 통계 {}개 호출", LOG_PREFIX, limit);

        ApiEndpointResponse response = apiEndpointService.getApiEndpointStatistics(projectUuid, startTime, endTime, limit);
        return ApiResponseFactory.success(
                DashboardSuccessCode.API_STATISTICS_RETRIEVED,
                response
        );
    }

    @Override
    @GetMapping("/statistics/logs/heatmap")
    public ResponseEntity<? extends BaseResponse> getHeatmap(
            @ValidUuid @RequestParam String projectUuid,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String logLevel
    ) {
        log.info("{} 히트맵 통계 호출: projectUuid={}", LOG_PREFIX, projectUuid);

        HeatmapResponse response = heatmapService.getLogHeatmap(projectUuid, startTime, endTime, logLevel);

        return ApiResponseFactory.success(
                DashboardSuccessCode.HEATMAP_RETRIEVED,
                response
        );
    }

    /**
     * 의존성 컴포넌트 목록 조회
     */
    @GetMapping("/dashboards/dependencies/architecture")
    public ResponseEntity<? extends BaseResponse> getDatabaseComponents(
            @ValidUuid @RequestParam String projectUuid,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        DatabaseComponentResponse response = dashboardService.getDatabaseComponents(projectUuid, userDetails);

        return ApiResponseFactory.success(DashboardSuccessCode.DATABASES_RETRIEVED, response);
    }

    /**
     * 컴포넌트의 의존성 관계 조회
     */
    @GetMapping("/dashboards/components")
    public ResponseEntity<? extends BaseResponse> getComponentDependencies(
            @ValidUuid @RequestParam String projectUuid,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ProjectComponentsResponse response = dashboardService.getProjectComponents(
                projectUuid,
                userDetails
        );
        return ApiResponseFactory.success(DashboardSuccessCode.COMPONENTS_RETRIEVED, response);
    }

    /**
     * 특정 컴포넌트 상세 정보 조회
     */
    @GetMapping("/dashboards/components/{componentId}/dependencies")
    public ResponseEntity<? extends BaseResponse> getComponentDependencies(
            @PathVariable Integer componentId,
            @ValidUuid @RequestParam String projectUuid,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        ComponentDependencyResponse response = dashboardService.getComponentDependencies(
                projectUuid,
                componentId,
                userDetails
        );
        return ApiResponseFactory.success(
                DashboardSuccessCode.COMPONENT_DEPENDENCY_RETRIEVED,
                response
        );
    }

    /**
     * 알림 피드 조회
     */
    @GetMapping("/alerts")
    public ResponseEntity<? extends BaseResponse> getAlertFeed(
            @RequestParam String projectUuid,
            @RequestParam(required = false) String severity, // "critical", "warning", "info"
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        // PageResponse<AlertResponse> response = dashboardService.getAlertFeed(projectId, severity, isRead, page, size);
        return null;
    }

    /**
     * [임시] 수동 메트릭 집계 - 배포 후 삭제 예정
     */
    @PostMapping("/admin/aggregate/{projectUuid}")
    public ResponseEntity<String> manualAggregate(
            @PathVariable String projectUuid
    ) {
        log.info("{} [임시] 수동 집계 시작: projectUuid={}", LOG_PREFIX, projectUuid);

        try {
            Project project = projectRepository.findByProjectUuid(projectUuid)
                    .orElseThrow(() -> new RuntimeException("프로젝트 없음"));

            LocalDateTime to = LocalDateTime.now();
            LocalDateTime from = to.minusDays(90);

            LogMetrics previous = logMetricsRepository
                    .findTopByProjectIdOrderByAggregatedAtDesc(project.getId())
                    .orElse(null);

            logMetricsTransactionalService.aggregateProjectMetricsIncremental(
                    project, from, to, previous
            );

            return ResponseEntity.ok("집계 완료: " + projectUuid);

        } catch (Exception e) {
            log.error("{} 집계 실패", LOG_PREFIX, e);
            return ResponseEntity.status(500).body("실패: " + e.getMessage());
        }
    }

}
