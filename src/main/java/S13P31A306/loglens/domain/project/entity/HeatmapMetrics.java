package S13P31A306.loglens.domain.project.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "heatmap_metrics",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"project_id", "day_of_week", "hour"}
        )
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class HeatmapMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;  // 1(월) ~ 7(일)

    @Column(name = "hour", nullable = false)
    private Integer hour;  // 0 ~ 23

    @Column(name = "total_count", nullable = false)
    private Integer totalCount;

    @Column(name = "error_count", nullable = false)
    private Integer errorCount;

    @Column(name = "warn_count", nullable = false)
    private Integer warnCount;

    @Column(name = "info_count", nullable = false)
    private Integer infoCount;

    @Column(name = "aggregated_at", nullable = false)
    private LocalDateTime aggregatedAt;
}
