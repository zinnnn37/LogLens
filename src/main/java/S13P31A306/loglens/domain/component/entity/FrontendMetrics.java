package S13P31A306.loglens.domain.component.entity;

import S13P31A306.loglens.global.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Frontend 메트릭 엔티티
 * 프로젝트별 Frontend 로그 통계 정보를 저장
 */
@Getter
@Table(name = "frontend_metrics")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FrontendMetrics extends BaseEntity {

    @NotNull
    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @NotNull
    @Column(name = "total_traces", nullable = false)
    private Integer totalTraces;

    @NotNull
    @Column(name = "total_info_logs", nullable = false)
    private Integer totalInfoLogs;

    @NotNull
    @Column(name = "total_warn_logs", nullable = false)
    private Integer totalWarnLogs;

    @NotNull
    @Column(name = "total_error_logs", nullable = false)
    private Integer totalErrorLogs;

    @NotNull
    @Column(name = "error_rate", nullable = false)
    private Double errorRate;

    @NotNull
    @Column(name = "measured_at", nullable = false)
    private LocalDateTime measuredAt;

    /**
     * 에러율 자동 계산 후 엔티티 생성
     */
    public static FrontendMetrics of(
            Integer projectId,
            Integer totalTraces,
            Integer totalInfoLogs,
            Integer totalWarnLogs,
            Integer totalErrorLogs
    ) {
        double errorRate = calculateErrorRate(totalTraces, totalErrorLogs);

        return FrontendMetrics.builder()
                .projectId(projectId)
                .totalTraces(totalTraces)
                .totalInfoLogs(totalInfoLogs)
                .totalWarnLogs(totalWarnLogs)
                .totalErrorLogs(totalErrorLogs)
                .errorRate(errorRate)
                .measuredAt(LocalDateTime.now())
                .build();
    }

    /**
     * 에러율 계산
     */
    private static double calculateErrorRate(Integer totalTraces, Integer totalErrorLogs) {
        if (totalTraces == null || totalTraces == 0) {
            return 0.0;
        }
        int errors = totalErrorLogs != null ? totalErrorLogs : 0;
        return Math.round((errors * 100.0 / totalTraces) * 100.0) / 100.0;
    }
}
