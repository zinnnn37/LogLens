package S13P31A306.loglens.domain.component.dto;

import lombok.Builder;

@Builder
public record MetricsData(
        Integer totalCalls,
        Integer errorCount,
        Integer warnCount,
        Double errorRate
) {
    public static MetricsData empty() {
        return MetricsData.builder()
                .totalCalls(0)
                .errorCount(0)
                .warnCount(0)
                .errorRate(0.0)
                .build();
    }

    public static MetricsData of(Integer totalCalls, Integer errorCount, Integer warnCount) {
        double errorRate = calculateErrorRate(totalCalls, errorCount);

        return MetricsData.builder()
                .totalCalls(totalCalls != null ? totalCalls : 0)
                .errorCount(errorCount != null ? errorCount : 0)
                .warnCount(warnCount != null ? warnCount : 0)
                .errorRate(errorRate)
                .build();
    }

    private static double calculateErrorRate(Integer totalCalls, Integer errorCount) {
        if (totalCalls == null || totalCalls == 0) {
            return 0.0;
        }
        if (errorCount == null) {
            return 0.0;
        }
        return Math.round((errorCount * 100.0 / totalCalls) * 100.0) / 100.0;
    }
}
