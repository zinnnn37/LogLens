package S13P31A306.loglens.domain.log.repository;

import S13P31A306.loglens.domain.log.dto.internal.LogSearchResult;
import S13P31A306.loglens.domain.log.dto.internal.TraceLogSearchResult;
import S13P31A306.loglens.domain.log.dto.request.LogSearchRequest;

public interface LogRepository {

    LogSearchResult findWithCursor(String projectUuid, LogSearchRequest request);

    TraceLogSearchResult findByTraceId(String projectUuid, LogSearchRequest request);

    /**
     * 프로젝트 UUID로 로그가 존재하는지 확인
     *
     * @param projectUuid 프로젝트 UUID
     * @return true: 로그 존재, false: 로그 없음
     */
    boolean existsByProjectUuid(String projectUuid);
}
