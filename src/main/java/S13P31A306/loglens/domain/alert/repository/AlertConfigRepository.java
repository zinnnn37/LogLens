package S13P31A306.loglens.domain.alert.repository;

import S13P31A306.loglens.domain.alert.entity.AlertConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 알림 설정 Repository
 */
@Repository
public interface AlertConfigRepository extends JpaRepository<AlertConfig, Integer> {

    /**
     * 프로젝트 ID로 알림 설정 조회
     */
    Optional<AlertConfig> findByProjectId(Integer projectId);

    /**
     * 프로젝트 ID로 알림 설정 존재 여부 확인
     */
    boolean existsByProjectId(Integer projectId);
}
