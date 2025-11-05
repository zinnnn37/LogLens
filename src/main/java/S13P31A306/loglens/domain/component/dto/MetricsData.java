package S13P31A306.loglens.domain.component.dto;

public record MetricsData(
        Integer totalCalls,
        Integer errorCount,
        Integer warnCount,
        Double errorRate
) {
}
