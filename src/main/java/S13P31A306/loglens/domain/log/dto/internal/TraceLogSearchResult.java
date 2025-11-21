package S13P31A306.loglens.domain.log.dto.internal;

import S13P31A306.loglens.domain.log.dto.response.LogSummaryResponse;
import S13P31A306.loglens.domain.log.entity.Log;
import java.util.List;

public record TraceLogSearchResult(List<Log> logs, LogSummaryResponse summary) {
}
