package S13P31A306.loglens.domain.project.mapper;

import S13P31A306.loglens.domain.project.dto.response.ProjectCreateResponse;
import S13P31A306.loglens.domain.project.dto.response.ProjectDetailResponse;
import S13P31A306.loglens.domain.project.dto.response.ProjectListResponse;
import S13P31A306.loglens.domain.project.entity.Project;
import S13P31A306.loglens.domain.project.entity.ProjectMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProjectMapper  {

    /**
     * 프로젝트 생성
     *
     * @param project 생성된 프로젝트
     * @return ProjectCreateResponse 생성된 프로젝트 DTO
     */
    ProjectCreateResponse toCreateResponse(Project project);

    /**
     * 프로젝트 상세 조회
     *
     * @param project 조회하고자 하는 프로젝트
     * @return ProjectDetailResponse 프로젝트 상세 정보
     */
    ProjectDetailResponse toDetailResponse(Project project);

    /**
     * 프로젝트 단일 정보
     *
     * @param project
     * @return
     */
    @Mapping(source = "id", target = "projectId")
    @Mapping(source = "members", target = "memberCount", qualifiedByName = "getMemberCount")
    ProjectListResponse.ProjectInfo toProjectInfo(Project project);

    /**
     * 프로젝트 리스트 조회
     *
     * @param projects 프로젝트 리스트
     * @return List<ProjectListResponse.ProjectInfo>
     */
    List<ProjectListResponse.ProjectInfo> toProjectInfoList(List<Project> projects);

    /**
     * ProjectMember를 Member DTO로 변환
     *
     * @param projectMember Member 객체로 변환할 ProjectMember 객체
     */
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.name", target = "name")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "joinedAt", target = "joinedAt", dateFormat = "yyyy-MM-dd'T'HH:mm:ss")
    ProjectDetailResponse.Member toMemberDto(ProjectMember projectMember);

    /**
     * ProjectMember 리스트를 Member DTO 리스트로 변환
     *
     * @param projectMembers Member DTO 리스트로 변환할 ProjectMember 리스트
     */
    List<ProjectDetailResponse.Member> toMemberDtoList(List<ProjectMember> projectMembers);

    @Named("getMemberCount")
    default int getMemberCount(List<ProjectMember> members) {
        return members != null ? members.size() : 0;
    }
}
