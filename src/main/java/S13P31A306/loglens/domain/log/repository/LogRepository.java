package S13P31A306.loglens.domain.log.repository;

import S13P31A306.loglens.domain.log.dto.internal.LogSearchResult;
import S13P31A306.loglens.domain.log.dto.internal.TraceLogSearchResult;
import S13P31A306.loglens.domain.log.dto.request.LogSearchRequest;
import S13P31A306.loglens.domain.log.entity.Log;

import java.time.LocalDateTime;
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
}
