package S13P31A306.loglens.domain.dashboard.service;

import S13P31A306.loglens.domain.dashboard.dto.opensearch.ErrorAggregation;
import S13P31A306.loglens.domain.dashboard.dto.opensearch.ErrorStatistics;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 자주 발생하는 에러 조회를 위한 OpenSearch 쿼리 서비스
 */
public interface TopFrequentErrorsQueryService {

    /**
     * Top N 에러 집계 조회
     *
     * @param projectUuid 프로젝트 UUID
     * @param start 조회 시작 시간
     * @param end 조회 종료 시간
     * @param limit 조회할 에러 개수
     * @return 에러 집계 결과 리스트
     */
    List<ErrorAggregation> queryTopErrors(
            String projectUuid,
            LocalDateTime start,
            LocalDateTime end,
            Integer limit
    );

    /**
     * 전체 에러 통계 조회
     *
     * @param projectUuid 프로젝트 UUID
     * @param start 조회 시작 시간
     * @param end 조회 종료 시간
     * @return 에러 통계 (총 개수, 고유 타입 수)
     */
    ErrorStatistics queryErrorStatistics(
            String projectUuid,
            LocalDateTime start,
            LocalDateTime end
    );
}
