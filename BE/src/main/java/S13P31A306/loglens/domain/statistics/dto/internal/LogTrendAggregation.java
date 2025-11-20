package S13P31A306.loglens.domain.statistics.dto.internal;

import java.time.LocalDateTime;

/**
 * OpenSearch 집계 결과를 담는 내부 DTO
 */
public record LogTrendAggregation(
        LocalDateTime timestamp,    // 시간대
        Integer totalCount,         // 전체 로그 수
        Integer infoCount,          // INFO 로그 수
        Integer warnCount,          // WARN 로그 수
        Integer errorCount          // ERROR 로그 수
) {}
