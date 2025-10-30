package S13P31A306.loglens.domain.project.repository;

import S13P31A306.loglens.domain.project.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProjectRepository {

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
     * @param userId 사용자 id
     * @param pageable 출력할 페이지 위치
     * @return Page<Project> 현재 페이지의 프로젝트
     */
    Page<Project> findProjectsByMemberId(int userId, Pageable pageable);

}
