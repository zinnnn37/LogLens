package S13P31A306.loglens.domain.dashboard.dto.response;

import S13P31A306.loglens.domain.dependency.dto.response.DependencyGraphResponse;

import java.util.List;

public record ComponentDependencyResponse(
        List<ComponentInfo> components,
        DependencyGraphResponse graph
) {}
