package S13P31A306.loglens.domain.project.repository;

import S13P31A306.loglens.domain.dashboard.entity.LogMetrics;
import S13P31A306.loglens.domain.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LogMetricsRepository extends JpaRepository<Project, Integer> {

    /**
     * 특정 프로젝트의 가장 최신 로그 메트릭 조회
     *
     * @param projectId 프로젝트 ID
     * @return Optional<LogMetrics> 최신 로그 메트릭
     */
    Optional<LogMetrics> findFirstByProjectIdOrderByAggregatedAtDesc(String projectId);

    /**
     * 특정 프로젝트의 특정 시간대별 로그 조회
     */
    List<LogMetrics> findByProjectIdAndAggregatedAtBetween(
            int projectId,
            LocalDateTime start,
            LocalDateTime end
    );

    /**
     * 중복 집계 방지를 위한 존재 여부 확인
     *
     * @param projectId 프로젝트 ID
     * @param aggregatedAt 집계 시간
     * @return boolean 해당 시간에 집계된 데이터 존재 여부
     */
    boolean existsByProjectIdAndAggregatedAt(int projectId, LocalDateTime aggregatedAt);

}
