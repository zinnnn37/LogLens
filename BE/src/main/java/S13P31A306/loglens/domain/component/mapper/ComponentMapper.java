package S13P31A306.loglens.domain.component.mapper;

import S13P31A306.loglens.domain.component.dto.request.ComponentRequest;
import S13P31A306.loglens.domain.component.entity.Component;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ComponentMapper {

    // ✅ 개별 매핑 메서드
    @Mapping(target = "projectId", source = "projectId")
    Component toEntity(ComponentRequest request, Integer projectId);

    // ✅ 리스트 매핑은 default 메서드로 직접 구현
    default List<Component> toEntityList(List<ComponentRequest> requests, Integer projectId) {
        return requests.stream()
                .map(request -> toEntity(request, projectId))
                .collect(Collectors.toList());
    }
}
