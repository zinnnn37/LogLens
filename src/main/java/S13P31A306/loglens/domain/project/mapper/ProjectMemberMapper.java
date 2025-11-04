package S13P31A306.loglens.domain.project.mapper;

import S13P31A306.loglens.domain.project.dto.response.ProjectMemberInviteResponse;
import S13P31A306.loglens.domain.project.entity.ProjectMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProjectMemberMapper {

    /**
     * ProjectMember를 초대 응답 DTO로 변환
     *
     * @param projectMember 초대된 프로젝트 멤버
     * @return ProjectMemberInviteResponse 멤버 초대 응답 DTO
     */
    @Mapping(source = "project.id", target = "projectId")
    @Mapping(source = ".", target = "member")
    ProjectMemberInviteResponse toInviteResponse(ProjectMember projectMember);

    /**
     * ProjectMember를 MemberInfo DTO로 변환
     *
     * @param projectMember Member 정보로 변환할 ProjectMember 객체
     * @return ProjectMemberInviteResponse.MemberInfo 멤버 정보 DTO
     */
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.name", target = "userName")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "joinedAt", target = "joinedAt", dateFormat = "yyyy-MM-dd'T'HH:mm:ss")
    ProjectMemberInviteResponse.MemberInfo toMemberInfo(ProjectMember projectMember);

}
