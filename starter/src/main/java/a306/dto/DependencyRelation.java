package a306.dto;

/**
 * 의존성 관계 (from → to)
 */
public record DependencyRelation(
        String from,  // 호출하는 컴포넌트 이름
        String to     // 호출받는 컴포넌트 이름
) {
}
