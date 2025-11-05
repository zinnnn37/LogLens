package S13P31A306.loglens.domain.project.mapper;

import S13P31A306.loglens.domain.project.dto.response.ProjectCreateResponse;
import S13P31A306.loglens.domain.project.dto.response.ProjectDetailResponse;
import S13P31A306.loglens.domain.project.dto.response.ProjectListResponse;
import S13P31A306.loglens.domain.project.entity.Project;
import S13P31A306.loglens.domain.project.entity.ProjectMember;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ProjectMapper {
    /**
     * 프로젝트 생성
     *
     * @param project 생성된 프로젝트
     * @return ProjectCreateResponse 생성된 프로젝트 DTO
     */
    @Mapping(target = "createdAt", source = "createdAt", dateFormat = "yyyy-MM-dd'T'HH:mm:ss")
    ProjectCreateResponse toCreateResponse(Project project);

    /**
     * 프로젝트 상세 조회
     *
     * @param project 조회하고자 하는 프로젝트
     * @return ProjectDetailResponse 프로젝트 상세 정보
     */
    @Mapping(target = "members", expression = "java(toMemberDtoList(project.getMembers()))")
    @Mapping(target = "createdAt", source = "createdAt", dateFormat = "yyyy-MM-dd'T'HH:mm:ss")
    @Mapping(target = "updatedAt", source = "updatedAt", dateFormat = "yyyy-MM-dd'T'HH:mm:ss")
    ProjectDetailResponse toDetailResponse(Project project);

    /**
     * 프로젝트 단일 정보
     *
     * @param project
     * @param jiraConnectionMap Jira 연결 정보 Map (projectId -> 연결 여부)
     * @return
     */
    @Mapping(source = "members", target = "memberCount", qualifiedByName = "getMemberCount")
    @Mapping(source = "project", target = "jiraConnectionExist", qualifiedByName = "getJiraConnectionExist")
    @Mapping(target = "createdAt", source = "createdAt", dateFormat = "yyyy-MM-dd'T'HH:mm:ss")
    @Mapping(target = "updatedAt", source = "updatedAt", dateFormat = "yyyy-MM-dd'T'HH:mm:ss")
    ProjectListResponse.ProjectInfo toProjectInfo(Project project, @Context Map<Integer, Boolean> jiraConnectionMap);

    /**
     * 프로젝트 리스트 조회
     *
     * @param projects 프로젝트 리스트
     * @param jiraConnectionMap Jira 연결 정보 Map (projectId -> 연결 여부)
     * @return List<ProjectListResponse.ProjectInfo>
     */
    List<ProjectListResponse.ProjectInfo> toProjectInfoList(List<Project> projects, @Context Map<Integer, Boolean> jiraConnectionMap);

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
        return !Objects.isNull(members) ? members.size() : 0;
    }

    @Named("getJiraConnectionExist")
    default boolean getJiraConnectionExist(Project project, @Context Map<Integer, Boolean> jiraConnectionMap) {
        return jiraConnectionMap.getOrDefault(project.getId(), false);
    }
}
