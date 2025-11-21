package S13P31A306.loglens.domain.alert.repository;

import S13P31A306.loglens.domain.alert.entity.AlertHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 알림 이력 Repository
 */
@Repository
public interface AlertHistoryRepository extends JpaRepository<AlertHistory, Integer> {

    /**
     * 프로젝트 ID로 알림 이력 조회 (최신순)
     */
    List<AlertHistory> findByProjectIdOrderByAlertTimeDesc(Integer projectId);

    /**
     * 프로젝트 ID와 읽음 여부로 알림 이력 조회 (최신순)
     */
    List<AlertHistory> findByProjectIdAndResolvedYNOrderByAlertTimeDesc(
            Integer projectId, String resolvedYN);

    /**
     * 프로젝트 ID와 읽음 여부로 알림 개수 조회
     */
    long countByProjectIdAndResolvedYN(Integer projectId, String resolvedYN);

    /**
     * 프로젝트 ID와 특정 시간 이후의 알림 이력 조회 (최신순)
     * SSE 스트리밍에서 사용
     */
    List<AlertHistory> findByProjectIdAndAlertTimeAfterOrderByAlertTimeDesc(
            Integer projectId, LocalDateTime afterTime);
}
