package S13P31A306.loglens.domain.project.repository;

import S13P31A306.loglens.domain.project.entity.HeatmapMetrics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * HeatmapMetrics 엔티티에 대한 데이터 접근 레포지토리
 * 프로젝트별 날짜/시간대별 로그 집계 데이터를 관리합니다.
 */
public interface HeatmapMetricsRepository extends JpaRepository<HeatmapMetrics, Integer> {

    /**
     * 프로젝트, 날짜, 시간대로 히트맵 메트릭 조회
     * UPSERT 로직에서 기존 데이터 존재 여부 확인 시 사용
     *
     * @param projectId 프로젝트 ID
     * @param date 조회할 날짜
     * @param hour 조회할 시간대 (0~23)
     * @return 조회된 히트맵 메트릭
     */
    Optional<HeatmapMetrics> findByProjectIdAndDateAndHour(
            Integer projectId,
            LocalDate date,
            Integer hour
    );

    /**
     * 프로젝트의 특정 기간 동안의 히트맵 메트릭 조회
     * 히트맵 API 응답 생성 시 사용
     *
     * @param projectId 프로젝트 ID
     * @param startDate 시작 날짜 (포함)
     * @param endDate 종료 날짜 (포함)
     * @return 기간 내 모든 히트맵 메트릭 리스트
     */
    List<HeatmapMetrics> findByProjectIdAndDateBetween(
            Integer projectId,
            LocalDate startDate,
            LocalDate endDate
    );

}
