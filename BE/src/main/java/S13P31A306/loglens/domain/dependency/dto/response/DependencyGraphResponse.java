package S13P31A306.loglens.domain.dependency.dto.response;

import S13P31A306.loglens.domain.dependency.entity.DependencyGraph;

import java.util.List;

/**
 * 의존성 관계 응답 DTO
 * from → to 관계를 나타냄
 */
public record DependencyGraphResponse(
        List<Edge> edges
) {
    public static DependencyGraphResponse from(List<DependencyGraph> dependencies) {
        List<Edge> edges = dependencies.stream()
                .map(entity -> new Edge(entity.getFrom(), entity.getTo()))
                .toList();
        return new DependencyGraphResponse(edges);
    }
}
