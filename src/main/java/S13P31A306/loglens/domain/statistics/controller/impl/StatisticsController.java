package S13P31A306.loglens.domain.statistics.controller.impl;

import S13P31A306.loglens.domain.statistics.constants.StatisticsSuccessCode;
import S13P31A306.loglens.domain.statistics.controller.StatisticsApi;
import S13P31A306.loglens.domain.statistics.dto.response.AIComparisonResponse;
import S13P31A306.loglens.domain.statistics.dto.response.LogTrendResponse;
import S13P31A306.loglens.domain.statistics.dto.response.TrafficResponse;
import S13P31A306.loglens.domain.statistics.service.LogTrendService;
import S13P31A306.loglens.domain.statistics.service.TrafficService;
import S13P31A306.loglens.global.annotation.ValidUuid;
import S13P31A306.loglens.global.client.AiServiceClient;
import S13P31A306.loglens.global.dto.response.ApiResponseFactory;
import S13P31A306.loglens.global.dto.response.BaseResponse;
import S13P31A306.loglens.global.exception.BusinessException;
import S13P31A306.loglens.global.constants.GlobalErrorCode;
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
    private final AiServiceClient aiServiceClient;

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

    @Override
    @GetMapping("/ai-comparison")
    public ResponseEntity<? extends BaseResponse> getAiComparison(
            @ValidUuid @RequestParam String projectUuid,
            @RequestParam(defaultValue = "24") Integer timeHours,
            @RequestParam(defaultValue = "100") Integer sampleSize
    ) {
        log.info("{} AI vs DB 통계 비교 API 호출: projectUuid={}, timeHours={}, sampleSize={}",
                LOG_PREFIX, projectUuid, timeHours, sampleSize);

        AIComparisonResponse response = aiServiceClient.compareAiVsDbStatistics(
                projectUuid, timeHours, sampleSize);

        if (response == null) {
            log.error("{} AI 서비스 호출 실패: projectUuid={}", LOG_PREFIX, projectUuid);
            throw new BusinessException(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        }

        // 불완전한 응답 검증 및 경고
        if (response.dbStatistics() == null || response.aiStatistics() == null || response.accuracyMetrics() == null) {
            log.warn("{} ⚠️ AI 서비스로부터 불완전한 응답 수신. " +
                            "dbStatistics={}, aiStatistics={}, accuracyMetrics={}, verdict={}",
                    LOG_PREFIX,
                    response.dbStatistics() != null ? "OK" : "NULL",
                    response.aiStatistics() != null ? "OK" : "NULL",
                    response.accuracyMetrics() != null ? "OK" : "NULL",
                    response.verdict() != null ? response.verdict().grade() : "NULL");
        }

        log.info("{} AI vs DB 비교 완료: overallAccuracy={}%, canReplaceDb={}",
                LOG_PREFIX,
                response.accuracyMetrics() != null ? response.accuracyMetrics().overallAccuracy() : null,
                response.verdict() != null ? response.verdict().canReplaceDb() : null);

        return ApiResponseFactory.success(
                StatisticsSuccessCode.AI_COMPARISON_RETRIEVED,
                response
        );
    }
}

