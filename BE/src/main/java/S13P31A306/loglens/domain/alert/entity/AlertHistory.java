package S13P31A306.loglens.domain.alert.entity;

import S13P31A306.loglens.domain.project.entity.Project;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 알림 이력 엔티티
 */
@Entity
@Table(name = "alert_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AlertHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @NotNull
    @Column(name = "alert_message", length = 256, nullable = false)
    private String alertMessage;

    @NotNull
    @Column(name = "alert_time", nullable = false)
    private LocalDateTime alertTime;

    @NotNull
    @Column(name = "resolved_YN", length = 1, nullable = false)
    @Builder.Default
    private String resolvedYN = "N";

    @NotNull
    @Column(name = "log_reference", columnDefinition = "JSON", nullable = false)
    private String logReference;

    @Column(name = "alert_level", length = 10)
    private String alertLevel;

    @Column(name = "trace_id", length = 100)
    private String traceId;

    @NotNull
    @Column(name = "project_id", columnDefinition = "TINYINT", nullable = false)
    private Integer projectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    private Project project;

    /**
     * 알림을 읽음 처리
     */
    public void markAsRead() {
        this.resolvedYN = "Y";
    }
}
