package S13P31A306.loglens.domain.project.service.impl;

import S13P31A306.loglens.domain.dashboard.dto.response.ApiEndpointResponse;
import S13P31A306.loglens.domain.dashboard.validator.DashboardValidator;
import S13P31A306.loglens.domain.project.entity.ApiEndpoint;
import S13P31A306.loglens.domain.project.repository.ApiEndpointRepository;
import S13P31A306.loglens.domain.project.service.ApiEndpointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * API 엔드포인트 서비스 구현체
 * DB에 저장된 API 엔드포인트 통계를 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApiEndpointServiceImpl implements ApiEndpointService {

    private static final String LOG_PREFIX = "[ApiEndpointService]";

    private final ApiEndpointRepository apiEndpointsRepository;
    private final DashboardValidator dashboardValidator;

    @Override
    public ApiEndpointResponse getApiEndpointStatistics(String projectUuid, Integer limit) {
        log.info("{} API 통계 조회 시작: projectUuid={}, limit={}", LOG_PREFIX, projectUuid, limit);

        Integer projectId = dashboardValidator.validateProjectAccess(projectUuid);

        limit = dashboardValidator.validateApiEndpointLimit(limit);

        // DB에서 조회
        List<ApiEndpoint> endpoints = apiEndpointsRepository
                .findTopByProjectIdOrderByTotalRequests(projectId, limit);

        List<ApiEndpointResponse.EndpointStats> endpointStatsList = endpoints.stream()
                .map(this::toEndpointStats)
                .toList();

        ApiEndpointResponse.Summary summary = calculateSummary(projectId);

        log.info("{} API 통계 조회 성공: projectUuid={}, limit={}", LOG_PREFIX, projectUuid, limit);

        return new ApiEndpointResponse(
                projectId,
                endpointStatsList,
                summary
        );
    }

    private ApiEndpointResponse.EndpointStats toEndpointStats(ApiEndpoint endpoint) {
        BigDecimal errorRate = calculateErrorRate(endpoint.getErrorCount(), endpoint.getTotalRequests());

        return new ApiEndpointResponse.EndpointStats(
                endpoint.getId(),
                endpoint.getEndpointPath(),
                endpoint.getHttpMethod(),
                endpoint.getTotalRequests(),
                endpoint.getErrorCount(),
                errorRate,
                endpoint.getAvgResponseTime(),
                endpoint.getAnomalyCount(),
                endpoint.getLastAccessed()
        );
    }

    private ApiEndpointResponse.Summary calculateSummary(Integer projectId) {
        long totalEndpoints = apiEndpointsRepository.countByProjectId(projectId);
        Long totalRequests = apiEndpointsRepository.sumTotalRequestsByProjectId(projectId);
        Long totalErrors = apiEndpointsRepository.sumErrorCountByProjectId(projectId);
        Double avgResponseTime = apiEndpointsRepository.avgResponseTimeByProjectId(projectId);
        long criticalEndpoints = apiEndpointsRepository.countCriticalEndpointsByProjectId(projectId);

        BigDecimal overallErrorRate = calculateErrorRate(totalErrors.intValue(), totalRequests.intValue());

        return new ApiEndpointResponse.Summary(
                (int) totalEndpoints,
                totalRequests,
                totalErrors,
                overallErrorRate,
                BigDecimal.valueOf(avgResponseTime).setScale(2, RoundingMode.HALF_UP),
                (int) criticalEndpoints
        );
    }

    private BigDecimal calculateErrorRate(int errorCount, int totalRequests) {
        if (totalRequests == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(errorCount)
                .divide(BigDecimal.valueOf(totalRequests), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);
    }

}
