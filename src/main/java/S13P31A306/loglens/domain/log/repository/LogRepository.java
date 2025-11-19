package S13P31A306.loglens.domain.log.repository;

import S13P31A306.loglens.domain.log.dto.internal.LogSearchResult;
import S13P31A306.loglens.domain.log.dto.internal.TraceLogSearchResult;
import S13P31A306.loglens.domain.log.dto.request.LogSearchRequest;
import S13P31A306.loglens.domain.log.entity.Log;
import S13P31A306.loglens.domain.statistics.dto.internal.LogTrendAggregation;
import S13P31A306.loglens.domain.statistics.dto.internal.TrafficAggregation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LogRepository {

    LogSearchResult findWithCursor(String projectUuid, LogSearchRequest request);

    TraceLogSearchResult findByTraceId(String projectUuid, LogSearchRequest request);

    /**
     * 로그 ID로 단일 로그 조회
     *
     * @param logId       로그 ID
     * @param projectUuid 프로젝트 UUID
     * @return 로그 엔티티 (존재하지 않으면 Optional.empty())
     */
    Optional<Log> findByLogId(Long logId, String projectUuid);

    /**
     * 프로젝트 UUID로 로그가 존재하는지 확인
     *
     * @param projectUuid 프로젝트 UUID
     * @return true: 로그 존재, false: 로그 없음
     */
    boolean existsByProjectUuid(String projectUuid);

    /**
     * 프로젝트 UUID와 시간 범위로 ERROR 로그 개수 조회
     * Alert 도메인에서 실시간 ERROR 로그 개수를 확인하기 위해 사용
     *
     * @param projectUuid 프로젝트 UUID
     * @param startTime   조회 시작 시간
     * @param endTime     조회 종료 시간
     * @return ERROR 로그 개수
     */
    long countErrorLogsByProjectUuidAndTimeRange(
            String projectUuid,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    /**
     * Alert 발생 시간 범위 내의 ERROR 로그 목록 조회
     * Alert 도메인에서 알림 발생 원인 로그를 조회하기 위해 사용
     *
     * @param projectUuid 프로젝트 UUID
     * @param startTime   조회 시작 시간
     * @param endTime     조회 종료 시간
     * @param limit       최대 조회 개수
     * @return ERROR 로그 목록
     */
    List<Log> findErrorLogsByProjectUuidAndTimeRange(
            String projectUuid,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int limit
    );

    /**
     * 시간 범위별 로그 추이 집계
     * Statistics 도메인에서 로그 발생 추이 그래프를 위해 사용
     *
     * @param projectUuid 프로젝트 UUID
     * @param startTime   조회 시작 시간
     * @param endTime     조회 종료 시간
     * @param interval    시간 간격 (예: "3h")
     * @return 시계열 집계 결과 리스트
     */
    List<LogTrendAggregation> aggregateLogTrendByTimeRange(
            String projectUuid,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String interval
    );

    /**
     * 시간 범위별 Traffic(FE/BE) 집계
     * Statistics 도메인에서 Traffic 그래프를 위해 사용
     *
     * @param projectUuid 프로젝트 UUID
     * @param startTime   조회 시작 시간
     * @param endTime     조회 종료 시간
     * @param interval    시간 간격 (예: "3h")
     * @return 시계열 집계 결과 리스트
     */
    List<TrafficAggregation> aggregateTrafficByTimeRange(
            String projectUuid,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String interval
    );
}
