package S13P31A306.loglens.domain.project.service;

import S13P31A306.loglens.domain.project.dto.request.ProjectCreateRequest;
import S13P31A306.loglens.domain.project.dto.request.ProjectMemberInviteRequest;
import S13P31A306.loglens.domain.project.dto.response.ProjectCreateResponse;
import S13P31A306.loglens.domain.project.dto.response.ProjectDetailResponse;
import S13P31A306.loglens.domain.project.dto.response.ProjectListResponse;
import S13P31A306.loglens.domain.project.dto.response.ProjectMemberInviteResponse;

/**
 * 프로젝트 관련 비즈니스 로직을 처리하는 서비스
 */
public interface ProjectService {

    /**
     * 프로젝트 생성
     *
     * @param request 생성할 프로젝트 정보
     * @return ProjectCreateResponse 생성한 프로젝트 정보
     */
    ProjectCreateResponse createProject(ProjectCreateRequest request);

    /**
     * 멤버 초대
     *
     * @param projectUuid 사용자를 초대할 프로젝트 UUID
     * @param request     초대할 사용자 ID
     * @return ProjectMemberInviteResponse 초대한 사용자 정보
     */
    ProjectMemberInviteResponse inviteMember(String projectUuid, ProjectMemberInviteRequest request);

    /**
     * 프로젝트 목록 조회
     *
     * @param page  출력할 페이지 번호 - default 0
     * @param size  한 페이지에 나타낼 프로젝트의 수 - default 10
     * @param sort  정렬 조건 - default createdAt
     * @param order 정렬 방향 - default desc
     * @return ProjectListResponse 프로젝트 리스트
     */
    ProjectListResponse getProjects(int page, int size, String sort, String order);

    /**
     * 프로젝트 상세 조회
     *
     * @param projectUuid 프로젝트 UUID
     * @return ProjectDetailResponse
     */
    ProjectDetailResponse getProjectDetail(String projectUuid);

    Integer getProjectIdByUuid(String uuid);

    /**
     * 프로젝트 삭제
     *
     * @param projectUuid 프로젝트 UUID
     */
    void deleteProject(String projectUuid);

    /**
     * 멤버 삭제
     *
     * @param projectUuid 프로젝트 UUID
     * @param memberId    멤버 ID
     */
    void deleteMember(String projectUuid, int memberId);
}
