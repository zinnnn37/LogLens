package S13P31A306.loglens.domain.dashboard.controller.impl;

import S13P31A306.loglens.domain.dashboard.constants.DashboardSuccessCode;
import S13P31A306.loglens.domain.dashboard.controller.DashboardApi;
import S13P31A306.loglens.domain.dashboard.dto.response.ComponentDependencyResponse;
import S13P31A306.loglens.domain.dashboard.dto.response.DashboardOverviewResponse;
import S13P31A306.loglens.domain.dashboard.dto.response.ProjectComponentsResponse;
import S13P31A306.loglens.domain.dashboard.dto.response.TopFrequentErrorsResponse;
import S13P31A306.loglens.domain.dashboard.service.DashboardService;
import S13P31A306.loglens.domain.dashboard.service.TopFrequentErrorsService;
import S13P31A306.loglens.global.annotation.ValidUuid;
import S13P31A306.loglens.global.dto.response.ApiResponseFactory;
import S13P31A306.loglens.global.dto.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardController implements DashboardApi {

    private final DashboardService dashboardService;
    private final TopFrequentErrorsService topFrequentErrorsService;

    /**
     * 통계 개요 조회
     */
    @GetMapping("/statistics/overview")
    public ResponseEntity<? extends BaseResponse> getStatisticsOverview(
            @RequestParam String projectUuid,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    ) {
        DashboardOverviewResponse response = dashboardService.getStatisticsOverview(projectUuid, startTime, endTime);
        return ApiResponseFactory.success(
                DashboardSuccessCode.OVERVIEW_RETRIEVED,
                response
        );
    }

    /**
     * 자주 발생하는 에러 Top 10 조회
     */
    @GetMapping("/errors/top")
    public ResponseEntity<? extends BaseResponse> getTopFrequentErrors(
            @RequestParam String projectUuid,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    ) {
        TopFrequentErrorsResponse response = topFrequentErrorsService.getTopFrequentErrors(projectUuid, startTime, endTime, limit);
        return ApiResponseFactory.success(
                DashboardSuccessCode.FREQUENT_ERROR_RETRIEVED,
                response
        );
    }

    /**
     * API 호출 통계 조회
     */
    @GetMapping("/statistics/api-calls")
    public ResponseEntity<? extends BaseResponse> getApiCallStatistics(
            @RequestParam String projectUuid,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    ) {
        return null;
    }

    /**
     * 로그 히트맵 조회
     */
    @GetMapping("/statistics/logs/heatmap")
    public ResponseEntity<? extends BaseResponse> getLogHeatmap(
            @RequestParam String projectUuid,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    ) {
        return null;
    }

    /**
     * 의존성 아키텍처 전체 구조 조회
     */
    @GetMapping("/dashboards/dependencies/architecture")
    public ResponseEntity<? extends BaseResponse> getDependencyArchitecture(
            @RequestParam String projectUuid
    ) {
        return null;
    }

    /**
     * 의존성 컴포넌트 목록 조회
     */
    @GetMapping("/dashboards/dependencies/components")
    public ResponseEntity<? extends BaseResponse> getDependencyComponents(
            @RequestParam String projectUuid,
            @RequestParam(required = false) String layer,
            @RequestParam(required = false) String componentType
    ) {
        return null;
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
     * 트레이스 플로우 조회 (로그 호출 흐름)
     */
    @GetMapping("/traces/{traceId}/flow")
    public ResponseEntity<? extends BaseResponse> getTraceFlow(
            @PathVariable String traceId
    ) {
        return null;
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
}
