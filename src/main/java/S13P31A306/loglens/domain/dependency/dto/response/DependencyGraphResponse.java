package S13P31A306.loglens.domain.dependency.dto.response;

/**
 * 의존성 관계 요청 DTO
 * from → to 관계를 나타냄
 */
public record DependencyGraphResponse(
        Integer id,
        String fromComponent,
        String toComponent
) {
}
