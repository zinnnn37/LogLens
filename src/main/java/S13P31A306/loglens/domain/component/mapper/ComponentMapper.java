package S13P31A306.loglens.domain.component.mapper;

import S13P31A306.loglens.domain.component.dto.request.ComponentRequest;
import S13P31A306.loglens.domain.component.entity.Component;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ComponentMapper {

    List<Component> toEntityList(List<ComponentRequest> requests);
}
