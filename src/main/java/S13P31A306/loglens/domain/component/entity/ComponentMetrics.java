package S13P31A306.loglens.domain.component.entity;

import S13P31A306.loglens.global.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Table(name = "component_metrics")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ComponentMetrics extends BaseEntity {

    @NotNull
    @Column(name = "component_id")
    private Integer componentId;

    @NotNull
    @Column(name = "call_count")
    private Integer callCount;

    @NotNull
    @Column(name = "warn_count")
    private Integer warnCount;

    @NotNull
    @Column(name = "error_count")
    private Integer errorCount;

    @NotNull
    @Column(name = "measured_at")
    private LocalDateTime measuredAt;
}
