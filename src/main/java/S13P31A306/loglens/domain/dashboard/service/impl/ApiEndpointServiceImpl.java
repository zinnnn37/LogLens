package S13P31A306.loglens.domain.dashboard.service.impl;

import S13P31A306.loglens.domain.dashboard.dto.response.ApiEndpointResponse;
import S13P31A306.loglens.domain.dashboard.entity.ApiEndpoint;
import S13P31A306.loglens.domain.dashboard.repository.ApiEndpointRepository;
import S13P31A306.loglens.domain.dashboard.service.ApiEndpointService;
import S13P31A306.loglens.domain.dashboard.validator.DashboardValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import static S13P31A306.loglens.domain.dashboard.constants.DashboardConstants.API_ENDPOINT_DEFAULT_LIMIT;

/**
 * API 통계 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApiEndpointServiceImpl implements ApiEndpointService {

    private static final String LOG_PREFIX = "[ApiEndpointService]";

    private final ApiEndpointRepository apiEndpointsRepository;
    private final DashboardValidator dashboardValidator;

    /**
     * 프로젝트의 API 엔드포인트 통계 조회
     *
     * @param projectUuid 프로젝트 ID
     * @param startTime 조회 시작 시간 (ISO 8601 형식, nullable)
     * @param endTime 조회 종료 시간 (ISO 8601 형식, nullable)
     * @param limit 조회할 API 개수 (nullable, 기본값 10)
     * @return ApiEndpointResponse API 통계 응답
     */
    @Override
    public ApiEndpointResponse getApiEndpointStatistics(String projectUuid, String startTime, String endTime, Integer limit) {
        log.info("{} API 통계 조회 시작: projecUuid={}, limit={}", LOG_PREFIX, projectUuid, limit);

        // 프로젝트 권한 검증
        Integer projectId = dashboardValidator.validateProjectAccess(projectUuid);

        // 파라미터 검증 및 기본값 설정
        LocalDateTime[] timeRange = dashboardValidator.validateApiEndpointRequest(projectId, limit, startTime, endTime);
        LocalDateTime start = timeRange[0];
        LocalDateTime end = timeRange[1];
        int queryLimit = (limit != null) ? limit : API_ENDPOINT_DEFAULT_LIMIT;

        // 엔드포인트 목록 조회
        List<ApiEndpoint> endpoints = apiEndpointsRepository
                .findTopByProjectIdOrderByTotalRequests(projectId, queryLimit);

        List<ApiEndpointResponse.EndpointStats> endpointStatsList = endpoints.stream()
                .map(this::toEndpointStats)
                .toList();

        // 통계 요약
        ApiEndpointResponse.Summary summary = calculateSummary(projectId);

        log.info("{} API 통계 조회 성공: projecUuid={}, limit={}", LOG_PREFIX, projectUuid, limit);

        return new ApiEndpointResponse(
                projectId,
                new ApiEndpointResponse.Period(start, end),
                endpointStatsList,
                summary
        );
    }

    /**
     * ApiEndpoint 엔티티를 EndpointStats DTO로 변환
     * errorRate를 실시간으로 계산하여 반환
     *
     * @param endpoint API 엔드포인트 엔티티
     * @return EndpointStats 변환된 엔드포인트 통계 DTO
     */
    private ApiEndpointResponse.EndpointStats toEndpointStats(ApiEndpoint endpoint) {
        log.info("{} 개별 API 엔드포인트 Response 변환: endpoint={}", LOG_PREFIX, endpoint.getEndpointPath());

        BigDecimal errorRate = calculateErrorRate(endpoint.getErrorCount(), endpoint.getTotalRequests());

        log.info("{} 개별 API 엔드포인트 Response 변환 성공: endpoint={}", LOG_PREFIX, endpoint.getEndpointPath());

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

    /**
     * 프로젝트의 API 통계 요약 계산
     * <p>
     * Repository의 집계 쿼리를 사용하여 통계를 계산
     *
     * @param projectId 프로젝트 ID
     * @return Summary 통계 요약 DTO
     */
    private ApiEndpointResponse.Summary calculateSummary(Integer projectId) {
        log.info("{} API 호출 통계 계산 시작: projectId={}", LOG_PREFIX, projectId);
        
        // Repository에서 집계 데이터 조회
        long totalEndpoints = apiEndpointsRepository.countByProjectId(projectId);
        Long totalRequests = apiEndpointsRepository.sumTotalRequestsByProjectId(projectId);
        Long totalErrors = apiEndpointsRepository.sumErrorCountByProjectId(projectId);
        Double avgResponseTime = apiEndpointsRepository.avgResponseTimeByProjectId(projectId);
        long criticalEndpoints = apiEndpointsRepository.countCriticalEndpointsByProjectId(projectId);

        // overallErrorRate 계산
        BigDecimal overallErrorRate = calculateErrorRate(totalErrors.intValue(), totalRequests.intValue());
        
        log.info("{} API 호출 통계 계산 성공: projectId={}", LOG_PREFIX, projectId);

        return new ApiEndpointResponse.Summary(
                (int) totalEndpoints,
                totalRequests,
                totalErrors,
                overallErrorRate,
                BigDecimal.valueOf(avgResponseTime).setScale(2, RoundingMode.HALF_UP),
                (int) criticalEndpoints
        );
    }

    /**
     * 에러율 계산
     * errorRate = (errorCount / totalRequests) * 100
     *
     * @param errorCount 에러 발생 수
     * @param totalRequests 총 요청 수
     * @return BigDecimal 에러율 (소수점 첫째 자리까지, 요청이 없으면 0)
     */
    private BigDecimal calculateErrorRate(int errorCount, int totalRequests) {
        log.info("{} 에러율 계산 시작", LOG_PREFIX);

        if (totalRequests == 0) {
            return BigDecimal.ZERO;
        }

        log.info("{} 에러율 계산 성공", LOG_PREFIX);

        return BigDecimal.valueOf(errorCount)
                .divide(BigDecimal.valueOf(totalRequests), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP);
    }

}
