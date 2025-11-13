package S13P31A306.loglens.domain.project.repository;

import S13P31A306.loglens.domain.project.entity.HeatmapMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 히트맵 메트릭 데이터 접근을 위한 Repository
 * 요일별/시간대별 로그 통계 데이터를 관리
 */
public interface HeatmapMetricsRepository extends JpaRepository<HeatmapMetrics, Integer> {

    /**
     * 프로젝트의 특정 시간 범위 내 히트맵 메트릭 조회
     *
     * @param projectId 조회할 프로젝트 ID
     * @param start 조회 시작 시간 (포함)
     * @param end 조회 종료 시간 (포함)
     * @return 조회된 히트맵 메트릭 리스트
     */
    List<HeatmapMetrics> findByProjectIdAndAggregatedAtBetween(
            Integer projectId,
            LocalDateTime start,
            LocalDateTime end
    );

    /**
     * 프로젝트의 특정 요일과 시간대에 해당하는 히트맵 메트릭 조회
     *
     * @param projectId 조회할 프로젝트 ID
     * @param dayOfWeek 요일 (1=월요일 ~ 7=일요일)
     * @param hour 시간대 (0 ~ 23)
     * @return 조회된 히트맵 메트릭
     */
    Optional<HeatmapMetrics> findByProjectIdAndDayOfWeekAndHour(
            Integer projectId,
            Integer dayOfWeek,
            Integer hour
    );

}
