package S13P31A306.loglens.domain.dashboard.dto.opensearch;

import java.time.LocalDateTime;

/**
 * OpenSearch 에러 집계 결과
 */
public record ErrorAggregation(
        String exceptionType,
        String message,
        Integer count,
        LocalDateTime firstOccurrence,
        LocalDateTime lastOccurrence,
        String stackTrace,
        String logger
) {
}