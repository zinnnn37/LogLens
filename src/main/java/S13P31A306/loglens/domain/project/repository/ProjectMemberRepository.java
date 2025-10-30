package S13P31A306.loglens.domain.project.repository;

import S13P31A306.loglens.domain.project.entity.ProjectMember;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository {

    /**
     * 멤버 초대 시 중복 여부 확인
     * 
     * @param projectId 프로젝트 ID
     * @param userId 사용자 ID
     * @return boolean 프로젝트 초대 가능 여부(이미 초대된 경우 false)
     */
    boolean existsByProjectIdAndUserId(int projectId, int userId);

    /**
     * 특정 프로젝트의 멤버 조회
     *
     * @param projectId 프로젝트 ID
     * @return List<ProjectMember> 프로젝트에 참여 중인 멤버 리스트
     */
    List<ProjectMember> findByProjectId(int projectId);

    /**
     * 멤버 삭제
     * 
     * @param projectId 프로젝트 ID
     * @param userId 멤버 ID
     */
    void deleteByProjectIdAndMemberId(int projectId, int userId);

    /**
     * 프로젝트 내 특정 멤버 찾기
     *
     * @param projectId 프로젝트 ID
     * @param userId 멤버 ID
     * @return Optional<ProjectMember> 검색된 멤버 정보
     */
    Optional<ProjectMember> findByProjectIdAndUserId(int projectId, int userId);

}
