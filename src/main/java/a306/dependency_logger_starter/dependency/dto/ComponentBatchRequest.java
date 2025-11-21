package a306.dependency_logger_starter.dependency.dto;

import java.util.List;

public record ComponentBatchRequest(
        List<ComponentRequest> components
) {
}
