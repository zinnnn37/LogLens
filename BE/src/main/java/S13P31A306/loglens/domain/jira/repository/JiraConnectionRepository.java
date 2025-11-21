package S13P31A306.loglens.domain.jira.repository;

import S13P31A306.loglens.domain.jira.entity.JiraConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Jira 연동 정보 Repository
 */
public interface JiraConnectionRepository extends JpaRepository<JiraConnection, Integer> {

    /**
     * 프로젝트 ID로 Jira 연동 정보 조회
     *
     * @param projectId 프로젝트 ID
     * @return Optional<JiraConnection>
     */
    Optional<JiraConnection> findByProjectId(Integer projectId);

    /**
     * 프로젝트 ID로 Jira 연동 존재 여부 확인
     *
     * @param projectId 프로젝트 ID
     * @return 존재 여부
     */
    boolean existsByProjectId(Integer projectId);
}
