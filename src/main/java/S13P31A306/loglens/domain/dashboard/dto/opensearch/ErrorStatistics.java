package S13P31A306.loglens.domain.dashboard.dto.opensearch;

/**
 * 에러 통계 정보
 */
public record ErrorStatistics(
        Integer totalErrors,
        Integer uniqueErrorTypes
) {
}