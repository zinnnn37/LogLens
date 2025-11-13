package S13P31A306.loglens.domain.dashboard.service;

import S13P31A306.loglens.domain.dashboard.dto.response.HeatmapResponse;

/**
 * 히트맵 서비스
 */
public interface HeatmapService {

    /**
     * 요일/시간대별 로그 히트맵 조회
     *
     * @param projectUuid 프로젝트 UUID
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @param logLevel 로그 레벨 필터 (INFO/WARN/ERROR, null인 경우 all)
     * @return 히트맵 응답 데이터
     */
    HeatmapResponse getLogHeatmap(
            String projectUuid,
            String startTime,
            String endTime,
            String logLevel
    );

}
