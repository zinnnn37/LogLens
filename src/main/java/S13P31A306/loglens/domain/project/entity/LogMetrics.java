package S13P31A306.loglens.domain.project.entity;

import S13P31A306.loglens.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "log_metrics")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LogMetrics extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "total_logs", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer totalLogs;

    @Column(name = "error_logs", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer errorLogs;

    @Column(name = "warn_logs", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer warnLogs;

    @Column(name = "info_logs", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer infoLogs;

    @Column(name = "avg_response_time", nullable = false, precision = 10, scale = 3, columnDefinition = "DECIMAL(10,3) DEFAULT 0")
    private BigDecimal avgResponseTime;

    @Column(name = "aggregated_at", nullable = false)
    private LocalDateTime aggregatedAt;

    @Builder
    public LogMetrics(Project project, Integer totalLogs, Integer errorLogs,
                      Integer warnLogs, Integer infoLogs, BigDecimal avgResponseTime, LocalDateTime aggregatedAt) {
        this.project = project;
        this.totalLogs = totalLogs;
        this.errorLogs = errorLogs;
        this.warnLogs = warnLogs;
        this.infoLogs = infoLogs;
        this.avgResponseTime = avgResponseTime;
        this.aggregatedAt = aggregatedAt;
    }


}
