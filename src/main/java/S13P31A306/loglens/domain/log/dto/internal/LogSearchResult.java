package S13P31A306.loglens.domain.log.dto.internal;

import S13P31A306.loglens.domain.log.entity.Log;
import java.util.List;

public record LogSearchResult(List<Log> logs, boolean hasNext, Object[] sortValues) {
}
