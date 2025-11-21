package S13P31A306.loglens.domain.dashboard.service;

import S13P31A306.loglens.domain.dashboard.dto.response.TopFrequentErrorsResponse;

/**
 * 자주 발생하는 에러 조회 서비스
 */
public interface TopFrequentErrorsService {

    /**
     * 자주 발생하는 에러 Top N 조회
     *
     * @param projectUuid 프로젝트 UUID
     * @param startTime 조회 시작 시간 (ISO 8601, Optional)
     * @param endTime 조회 종료 시간 (ISO 8601, Optional)
     * @param limit 조회할 에러 개수 (1~50, 기본값 10)
     * @return TopFrequentErrorsResponse
     */
    TopFrequentErrorsResponse getTopFrequentErrors(
            String projectUuid,
            String startTime,
            String endTime,
            Integer limit
    );
}
