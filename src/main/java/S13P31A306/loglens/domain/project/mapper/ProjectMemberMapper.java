package S13P31A306.loglens.domain.project.mapper;

import S13P31A306.loglens.domain.project.dto.response.ProjectMemberInviteResponse;
import S13P31A306.loglens.domain.project.entity.ProjectMember;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProjectMemberMapper {

    /**
     * 사용자 초대
     */
    ProjectMemberInviteResponse toInviteResponse(ProjectMember projectMember);

}
