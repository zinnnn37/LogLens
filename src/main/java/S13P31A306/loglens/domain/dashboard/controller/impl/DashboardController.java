package S13P31A306.loglens.domain.dashboard.controller.impl;

import S13P31A306.loglens.domain.dashboard.controller.DashboardApi;
import S13P31A306.loglens.domain.dashboard.dto.response.DashboardOverviewResponse;
import S13P31A306.loglens.domain.dashboard.service.DashboardService;
import S13P31A306.loglens.global.dto.response.ApiResponseFactory;
import S13P31A306.loglens.global.dto.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardControllerImpl implements DashboardApi {

    private final DashboardService dashboardService;

    @GetMapping("/statistics/overview")
    public ResponseEntity<? extends BaseResponse> getOverview(
            @RequestParam Integer projectId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    ) {
        DashboardOverviewResponse response = dashboardService.getStatisticsOverview(projectId, startTime, endTime);
        return ApiResponseFactory.success(

        );
    }

}
