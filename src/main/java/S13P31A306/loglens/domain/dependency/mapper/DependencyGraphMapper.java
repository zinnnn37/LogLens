package S13P31A306.loglens.domain.dependency.mapper;

import S13P31A306.loglens.domain.dependency.dto.response.DependencyGraphResponse;
import S13P31A306.loglens.domain.dependency.entity.DependencyGraph;
import org.mapstruct.Mapper;

import java.util.List;

import static org.mapstruct.ReportingPolicy.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = IGNORE)
public interface DependencyGraphMapper {

    /**
     * Entity → Response DTO 변환
     */
    DependencyGraphResponse toResponse(DependencyGraph entity);

    /**
     * Entity List → Response DTO List 변환
     */
    List<DependencyGraphResponse> toResponseList(List<DependencyGraph> entities);
}
