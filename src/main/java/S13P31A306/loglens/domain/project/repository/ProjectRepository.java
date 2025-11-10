package S13P31A306.loglens.domain.project.repository;

import S13P31A306.loglens.domain.project.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Integer> {

    /**
     * 프로젝트 이름을 기반으로 프로젝트를 조회합니다.
     *
     * @param projectName 프로젝트 이름
     * @return boolean 존재 여부
     */
    boolean existsByProjectName(String projectName);

    /**
     * 로그인 한 사용자의 프로젝트 목록 조회
     *
     * @param userId   사용자 id
     * @param pageable 출력할 페이지 위치
     * @return Page<Project> 현재 페이지의 프로젝트
     */
    Page<Project> findByMembersUserId(int userId, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p " +
            "LEFT JOIN FETCH p.members " +
            "WHERE p.id IN (SELECT pm.project.id FROM ProjectMember pm WHERE pm.user.id = :userId)")
    List<Project> findProjectsWithMembersByUserId(@Param("userId") int userId);

    /**
     * 프로젝트 UUID로 프로젝트 조회
     *
     * @param projectUuid 프로젝트 UUID
     * @return Optional<Project> 조회된 프로젝트
     */
    Optional<Project> findByProjectUuid(String projectUuid);
}
