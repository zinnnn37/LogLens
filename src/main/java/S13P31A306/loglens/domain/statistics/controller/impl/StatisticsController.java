package S13P31A306.loglens.domain.statistics.controller.impl;

import S13P31A306.loglens.domain.statistics.constants.StatisticsSuccessCode;
import S13P31A306.loglens.domain.statistics.controller.StatisticsApi;
import S13P31A306.loglens.domain.statistics.dto.response.LogTrendResponse;
import S13P31A306.loglens.domain.statistics.dto.response.TrafficResponse;
import S13P31A306.loglens.domain.statistics.service.LogTrendService;
import S13P31A306.loglens.domain.statistics.service.TrafficService;
import S13P31A306.loglens.global.annotation.ValidUuid;
import S13P31A306.loglens.global.dto.response.ApiResponseFactory;
import S13P31A306.loglens.global.dto.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 통계 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@Validated
public class StatisticsController implements StatisticsApi {

    private static final String LOG_PREFIX = "[StatisticsController]";

    private final LogTrendService logTrendService;
    private final TrafficService trafficService;

    @Override
    @GetMapping("/log-trend")
    public ResponseEntity<? extends BaseResponse> getLogTrend(
            @ValidUuid @RequestParam String projectUuid
    ) {
        log.info("{} 로그 추이 API 호출: projectUuid={}", LOG_PREFIX, projectUuid);

        LogTrendResponse response = logTrendService.getLogTrend(projectUuid);

        return ApiResponseFactory.success(
                StatisticsSuccessCode.LOG_TREND_RETRIEVED,
                response
        );
    }

    @Override
    @GetMapping("/traffic")
    public ResponseEntity<? extends BaseResponse> getTraffic(
            @ValidUuid @RequestParam String projectUuid
    ) {
        log.info("{} Traffic API 호출: projectUuid={}", LOG_PREFIX, projectUuid);

        TrafficResponse response = trafficService.getTraffic(projectUuid);

        return ApiResponseFactory.success(
                StatisticsSuccessCode.TRAFFIC_RETRIEVED,
                response
        );
    }
}
