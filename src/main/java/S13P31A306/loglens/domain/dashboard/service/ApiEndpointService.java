package S13P31A306.loglens.domain.dashboard.service;

import S13P31A306.loglens.domain.dashboard.dto.response.ApiEndpointResponse;

/**
 * Api 통계 조회 서비스
 */
public interface ApiEndpointService {

    /**
     * 프로젝트의 API 엔드포인트 통계 조회
     *
     * @param projectUuid 프로젝트 ID
     * @param startTime 조회 시작 시간 (ISO 8601 형식, nullable)
     * @param endTime 조회 종료 시간 (ISO 8601 형식, nullable)
     * @param limit 조회할 API 개수 (nullable, 기본값 10)
     * @return API 통계 응답
     */
    ApiEndpointResponse getApiEndpointStatistics(String projectUuid, String startTime, String endTime, Integer limit);

}
