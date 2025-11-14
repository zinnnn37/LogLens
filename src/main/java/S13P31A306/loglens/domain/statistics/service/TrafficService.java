package S13P31A306.loglens.domain.statistics.service;

import S13P31A306.loglens.domain.statistics.dto.response.TrafficResponse;

/**
 * Traffic 그래프 서비스
 */
public interface TrafficService {

    /**
     * Traffic 그래프 데이터 조회
     * 24시간 전부터 3시간 간격으로 FE/BE 로그 발생 추이를 조회
     *
     * @param projectUuid 프로젝트 UUID
     * @return Traffic 그래프 응답
     */
    TrafficResponse getTraffic(String projectUuid);
}
