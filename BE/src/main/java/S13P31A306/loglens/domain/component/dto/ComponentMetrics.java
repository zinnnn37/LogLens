package S13P31A306.loglens.domain.component.dto;

import java.time.LocalDateTime;

public record ComponentMetrics(
        String componentName,
        Integer callCount,
        Integer errorCount,
        LocalDateTime measuredAt
) {
    /**
     * 에러율 계산
     */
    public double getErrorRate() {
        return callCount > 0
                ? Math.round((errorCount * 100.0 / callCount) * 100.0) / 100.0
                : 0.0;
    }
}
