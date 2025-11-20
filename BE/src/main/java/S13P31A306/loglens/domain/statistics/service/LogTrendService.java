package S13P31A306.loglens.domain.statistics.service;

import S13P31A306.loglens.domain.statistics.dto.response.LogTrendResponse;

/**
 * 로그 추이 서비스
 */
public interface LogTrendService {

    /**
     * 로그 발생 추이 조회 (24시간, 3시간 간격)
     *
     * @param projectUuid 프로젝트 UUID
     * @return 로그 추이 응답
     */
    LogTrendResponse getLogTrend(String projectUuid);
}
