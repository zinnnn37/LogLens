package S13P31A306.loglens.domain.dashboard.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalTime;

@Getter
@Entity
@Table(name = "alert_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlertHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "alert_message", nullable = false, length = 256)
    private String alertMessage;

    @Column(name = "alert_time", nullable = false)
    private LocalTime alertTime;

    @Column(name = "resolved_YN", nullable = false)
    private Character resolvedYN;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "log_reference", nullable = false, columnDefinition = "JSON")
    private String logReference;

    @Column(name = "project_id", nullable = false, columnDefinition = "TINYINT")
    private Integer projectId;

}
