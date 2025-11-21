package S13P31A306.loglens.domain.statistics.dto.internal;

import java.time.LocalDateTime;

/**
 * Traffic 그래프를 위한 시간대별 FE/BE 로그 집계 결과
 * OpenSearch의 date_histogram + source_type 집계 결과를 담는 내부 DTO
 */
public record TrafficAggregation(
        LocalDateTime timestamp,
        Integer totalCount,
        Integer feCount,
        Integer beCount
) {
}
