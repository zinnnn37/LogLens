package S13P31A306.loglens.domain.dashboard.controller.impl;

import S13P31A306.loglens.domain.dashboard.controller.DashboardApi;
import S13P31A306.loglens.domain.dashboard.dto.response.DashboardOverviewResponse;
import S13P31A306.loglens.domain.dashboard.service.DashboardService;
import S13P31A306.loglens.global.dto.response.ApiResponseFactory;
import S13P31A306.loglens.global.dto.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardController implements DashboardApi {

    private final DashboardService dashboardService;

    /**
     * 통계 개요 조회
     */
    @GetMapping("/statistics/overview")
    public ResponseEntity<? extends BaseResponse> getStatisticsOverview(
            @RequestParam Integer projectId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    ) {
        DashboardOverviewResponse response = dashboardService.getStatisticsOverview(projectId, startTime, endTime);
        return ApiResponseFactory.success(/* SuccessCode */, response);
    }

    /**
     * 자주 발생하는 에러 Top 10 조회
     */
    @GetMapping("/statistics/top")
    public ResponseEntity<? extends BaseResponse> getTopFrequentErrors(
            @RequestParam Integer projectId,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    ) {
        // TopErrorResponse response = dashboardService.getTopFrequentErrors(projectId, limit, startTime, endTime);
        return ApiResponseFactory.success(/* SuccessCode */, response);
    }

    /**
     * API 호출 통계 조회
     */
    @GetMapping("/statistics/api-calls")
    public ResponseEntity<? extends BaseResponse> getApiCallStatistics(
            @RequestParam Integer projectId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    ) {
        // ApiCallStatisticsResponse response = dashboardService.getApiCallStatistics(projectId, startTime, endTime);
        return ApiResponseFactory.success(/* SuccessCode */, response);
    }

    /**
     * 로그 히트맵 조회
     */
    @GetMapping("/statistics/logs/heatmap")
    public ResponseEntity<? extends BaseResponse> getLogHeatmap(
            @RequestParam Integer projectId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    ) {
        // LogHeatmapResponse response = dashboardService.getLogHeatmap(projectId, startTime, endTime);
        return ApiResponseFactory.success(/* SuccessCode */, response);
    }

    /**
     * 의존성 아키텍처 전체 구조 조회
     */
    @GetMapping("/dashboards/dependencies/architecture")
    public ResponseEntity<? extends BaseResponse> getDependencyArchitecture(
            @RequestParam Integer projectId
    ) {
        // ArchitectureResponse response = dashboardService.getDependencyArchitecture(projectId);
        return ApiResponseFactory.success(/* SuccessCode */, response);
    }

    /**
     * 의존성 컴포넌트 목록 조회
     */
    @GetMapping("/dashboards/dependencies/components")
    public ResponseEntity<? extends BaseResponse> getDependencyComponents(
            @RequestParam Integer projectId,
            @RequestParam(required = false) String layer,
            @RequestParam(required = false) String componentType
    ) {
        // List<ComponentResponse> response = dashboardService.getDependencyComponents(projectId, layer, componentType);
        return ApiResponseFactory.success(/* SuccessCode */, response);
    }

    /**
     * 특정 컴포넌트 상세 정보 조회
     */
    @GetMapping("/dashboards/dependencies/components/{componentId}/details")
    public ResponseEntity<? extends BaseResponse> getComponentDetails(
            @PathVariable Integer componentId
    ) {
        // ComponentDetailResponse response = dashboardService.getComponentDetails(componentId);
        return ApiResponseFactory.success(/* SuccessCode */, response);
    }

    /**
     * 컴포넌트의 의존성 관계 조회
     */
    @GetMapping("/components/{componentId}/dependencies")
    public ResponseEntity<? extends BaseResponse> getComponentDependencies(
            @PathVariable Integer componentId,
            @RequestParam(required = false, defaultValue = "both") String direction // "from", "to", "both"
    ) {
        // ComponentDependenciesResponse response = dashboardService.getComponentDependencies(componentId, direction);
        return ApiResponseFactory.success(/* SuccessCode */, response);
    }

    /**
     * 트레이스 플로우 조회 (로그 호출 흐름)
     */
    @GetMapping("/traces/{traceId}/flow")
    public ResponseEntity<? extends BaseResponse> getTraceFlow(
            @PathVariable String traceId
    ) {
        // TraceFlowResponse response = dashboardService.getTraceFlow(traceId);
        return ApiResponseFactory.success(/* SuccessCode */, response);
    }

    /**
     * 알림 피드 조회
     */
    @GetMapping("/alerts")
    public ResponseEntity<? extends BaseResponse> getAlertFeed(
            @RequestParam Integer projectId,
            @RequestParam(required = false) String severity, // "critical", "warning", "info"
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        // PageResponse<AlertResponse> response = dashboardService.getAlertFeed(projectId, severity, isRead, page, size);
        return ApiResponseFactory.success(/* SuccessCode */, response);
    }
}
