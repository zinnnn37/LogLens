package S13P31A306.loglens.domain.project.mapper;

import S13P31A306.loglens.domain.project.dto.response.ProjectMemberInviteResponse;
import S13P31A306.loglens.domain.project.entity.ProjectMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProjectMemberMapper {

    /**
     * 사용자 초대
     */
    @Mapping(source = "projectMember", target = "member")
    ProjectMemberInviteResponse toInviteResponse(ProjectMember projectMember);

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.name", target = "userName")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "joinedAt", target = "joinedAt", dateFormat = "yyyy-MM-dd'T'HH:mm:ss")
    ProjectMemberInviteResponse.MemberInfo toMemberInfo(ProjectMember projectMember);
}
