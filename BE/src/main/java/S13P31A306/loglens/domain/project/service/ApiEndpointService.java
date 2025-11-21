package S13P31A306.loglens.domain.project.service;

import S13P31A306.loglens.domain.dashboard.dto.response.ApiEndpointResponse;

/**
 * API 엔드포인트 서비스
 */
public interface ApiEndpointService {

    /**
     * 프로젝트의 API 엔드포인트 통계 조회 (Dashboard용)
     * DB에 저장된 데이터를 조회하여 응답
     */
    ApiEndpointResponse getApiEndpointStatistics(
            String projectUuid,
            Integer limit
    );

}
