package a306.dependency_logger_starter.dependency.dto;

public record ComponentRequest(
        String name,
        String classType,
        String componentType,  // "FE", "BE", "INFRA"
        String packageName,
        String layer,          // "CONTROLLER", "SERVICE", "REPOSITORY"
        String technology      // "Spring", "Java" 등
) {
}
