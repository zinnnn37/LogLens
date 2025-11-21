package S13P31A306.loglens.domain.dashboard.dto.response;

import java.time.LocalDateTime;

public record Metrics(
        Integer callCount,
        Integer errorCount,
        Integer warnCount,
        Double errorRate,
        LocalDateTime lastMeasuredAt
) {
    public static Metrics of(Integer callCount, Integer errorCount, Integer warnCount, LocalDateTime measuredAt) {
        double errorRate = (callCount != null && callCount > 0)
                ? Math.round((errorCount * 100.0 / callCount) * 100.0) / 100.0
                : 0.0;
        return new Metrics(
                callCount != null ? callCount : 0,
                errorCount != null ? errorCount : 0,
                warnCount != null ? warnCount : 0,
                errorRate,
                measuredAt
        );
    }
}
