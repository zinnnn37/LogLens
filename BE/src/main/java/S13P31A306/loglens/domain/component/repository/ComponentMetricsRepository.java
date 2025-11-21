package S13P31A306.loglens.domain.component.repository;

import S13P31A306.loglens.domain.component.entity.ComponentMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ComponentMetricsRepository extends JpaRepository<ComponentMetrics, Integer> {

    /**
     * 컴포넌트의 최신 메트릭 조회
     */
    @Query("SELECT cm FROM ComponentMetrics cm " +
            "WHERE cm.componentId = :componentId " +
            "ORDER BY cm.measuredAt DESC " +
            "LIMIT 1")
    Optional<ComponentMetrics> findLatestByComponentId(@Param("componentId") Integer componentId);

    boolean existsByComponentId(Integer componentId);
}
