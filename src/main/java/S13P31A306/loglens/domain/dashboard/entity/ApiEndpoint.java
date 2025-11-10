package S13P31A306.loglens.domain.dashboard.entity;

import S13P31A306.loglens.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Entity
@Table(name = "api_endpoints")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiEndpoint extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "endpoint_path", nullable = false, length = 100)
    private String endpointPath;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "total_requests", nullable = false)
    private Integer totalRequests;

    @Column(name = "error_count", nullable = false)
    private Integer errorCount;

    @Column(name = "avg_response_time", nullable = false, precision = 10, scale = 2)
    private BigDecimal avgResponseTime;

    @Column(name = "anomaly_count", nullable = false)
    private Integer anomalyCount;

    @Column(name = "last_accessed")
    private LocalTime lastAccessed;

    @Column(name = "project_id", nullable = false, columnDefinition = "TINYINT")
    private Integer projectId;

    @Column(name = "component_id", nullable = false)
    private Integer componentId;

}
