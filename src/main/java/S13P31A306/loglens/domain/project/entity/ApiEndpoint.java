package S13P31A306.loglens.domain.project.entity;

import S13P31A306.loglens.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;

@Entity
@Table(name = "api_endpoints")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
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

    /**
     * 누적 통계 업데이트
     */
    public void updateMetrics(
            Integer additionalRequests,
            Integer additionalErrors,
            Double newAvgResponseTime,
            LocalTime lastAccessed) {

        this.totalRequests += additionalRequests;
        this.errorCount += additionalErrors;

        // 평균 응답시간 재계산 (가중 평균)
        if (newAvgResponseTime != null && additionalRequests > 0) {
            double currentTotal = this.avgResponseTime.doubleValue() * (this.totalRequests - additionalRequests);
            double newTotal = newAvgResponseTime * additionalRequests;
            this.avgResponseTime = BigDecimal.valueOf((currentTotal + newTotal) / this.totalRequests)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        this.lastAccessed = lastAccessed;
    }

}
