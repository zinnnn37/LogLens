package S13P31A306.loglens.domain.log.repository;

import S13P31A306.loglens.domain.log.dto.internal.LogSearchResult;
import S13P31A306.loglens.domain.log.dto.internal.TraceLogSearchResult;
import S13P31A306.loglens.domain.log.dto.request.LogSearchRequest;

public interface LogRepository {

    LogSearchResult findWithCursor(String projectUuid, LogSearchRequest request);

    TraceLogSearchResult findByTraceId(String projectUuid, LogSearchRequest request);
}
