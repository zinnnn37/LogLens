package S13P31A306.loglens.domain.alert.entity;

import S13P31A306.loglens.domain.project.entity.Project;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * 알림 설정 엔티티
 */
@Entity
@Table(name = "alert_configs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AlertConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "TINYINT")
    private Integer id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private AlertType alertType;

    @NotNull
    @Column(name = "threshold_value", columnDefinition = "TINYINT", nullable = false)
    private Integer thresholdValue;

    @NotNull
    @Column(name = "active_YN", length = 1, nullable = false)
    @Builder.Default
    private String activeYN = "Y";

    @NotNull
    @Column(name = "project_id", columnDefinition = "TINYINT", nullable = false)
    private Integer projectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    private Project project;

    /**
     * 알림 설정 수정
     */
    public void update(AlertType alertType, Integer thresholdValue, String activeYN) {
        if (alertType != null) {
            this.alertType = alertType;
        }
        if (thresholdValue != null) {
            this.thresholdValue = thresholdValue;
        }
        if (activeYN != null) {
            this.activeYN = activeYN;
        }
    }
}
