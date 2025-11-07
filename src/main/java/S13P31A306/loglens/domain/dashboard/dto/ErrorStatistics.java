package S13P31A306.loglens.domain.dashboard.dto;

/**
 * 에러 통계 정보
 */
public record ErrorStatistics(
        Long totalErrors,
        Integer uniqueErrorTypes
) {
}