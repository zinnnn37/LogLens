package S13P31A306.loglens.domain.dashboard.dto;

public record HeatmapAggregation(
        Integer dayOfWeek,
        Integer hour,
        Integer totalCount,
        Integer errorCount,
        Integer warnCount,
        Integer infoCount
) {}
