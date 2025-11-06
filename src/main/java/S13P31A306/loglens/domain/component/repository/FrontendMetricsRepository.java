package S13P31A306.loglens.domain.component.repository;

import S13P31A306.loglens.domain.component.entity.FrontendMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FrontendMetricsRepository extends JpaRepository<FrontendMetrics, Integer> {

    /**
     * 프로젝트의 최신 Frontend 메트릭 조회
     */
    @Query("SELECT fm FROM FrontendMetrics fm " +
            "WHERE fm.projectId = :projectId " +
            "ORDER BY fm.measuredAt DESC " +
            "LIMIT 1")
    Optional<FrontendMetrics> findLatestByProjectId(@Param("projectId") Integer projectId);

    /**
     * 프로젝트의 Frontend 메트릭 존재 여부 확인
     */
    boolean existsByProjectId(Integer projectId);
}
