package S13P31A306.loglens.domain.dependency.mapper;

import S13P31A306.loglens.domain.dependency.dto.response.DependencyGraphResponse;
import S13P31A306.loglens.domain.dependency.dto.response.Edge;
import S13P31A306.loglens.domain.dependency.entity.DependencyGraph;
import org.mapstruct.Mapper;

import java.util.List;

import static org.mapstruct.ReportingPolicy.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = IGNORE)
public interface DependencyGraphMapper {

    /**
     * Entity List → DependencyGraphResponse 변환
     */
    default DependencyGraphResponse toGraphResponse(List<DependencyGraph> entities) {
        return DependencyGraphResponse.from(entities);
    }

    /**
     * 단일 Entity → Edge 변환
     */
    default Edge toEdge(DependencyGraph entity) {
        return new Edge(entity.getFrom(), entity.getTo());
    }
}
