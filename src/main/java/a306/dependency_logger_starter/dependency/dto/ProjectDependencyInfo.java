package a306.dependency_logger_starter.dependency.dto;

import java.util.List;

/**
 * 프로젝트 전체 의존성 정보
 * 모든 컴포넌트와 의존성 관계를 한 번에 전송
 */
public record ProjectDependencyInfo(
        String projectName,
        List<Component> components,
        List<DependencyRelation> dependencies
) {
}
