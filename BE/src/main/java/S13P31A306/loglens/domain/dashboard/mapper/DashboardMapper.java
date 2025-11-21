package S13P31A306.loglens.domain.dashboard.mapper;

import S13P31A306.loglens.domain.component.entity.Component;
import S13P31A306.loglens.domain.dashboard.dto.response.ComponentInfo;
import S13P31A306.loglens.domain.dashboard.dto.response.ProjectComponentsResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DashboardMapper {

    /**
     * 전체 응답 생성
     */
    default ProjectComponentsResponse toProjectComponentsResponse(
            Integer projectId,
            List<Component> components
    ) {
        List<ComponentInfo> componentInfos = components.stream()
                .map(c -> ComponentInfo.from(c, null))
                .toList();

        return new ProjectComponentsResponse(projectId, componentInfos);
    }
}
