package S13P31A306.loglens.domain.dashboard.dto.response;

import S13P31A306.loglens.domain.component.entity.Component;
import S13P31A306.loglens.domain.component.entity.ComponentMetrics;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record ComponentInfo(
        Integer id,
        String name,
        String type,
        String classType,
        String layer,
        String packageName,
        String technology,
        Metrics metrics
) {
    /**
     * 단일 컴포넌트 + 메트릭 → ComponentInfo
     */
    public static ComponentInfo from(Component component, ComponentMetrics metrics) {
        Metrics metricsDto = null;
        if (metrics != null) {
            metricsDto = Metrics.of(
                    metrics.getCallCount(),
                    metrics.getErrorCount(),
                    metrics.getWarnCount(),
                    metrics.getMeasuredAt()
            );
        }

        return new ComponentInfo(
                component.getId(),
                component.getName(),
                component.getComponentType().name(),
                component.getClassType(),
                component.getLayer() != null ? component.getLayer().name() : null,
                component.getPackageName(),
                component.getTechnology(),
                metricsDto
        );
    }

    /**
     * 컴포넌트 ID 집합 + 맵들 → ComponentInfo 리스트
     */
    public static List<ComponentInfo> fromMaps(
            Set<Integer> componentIds,
            Map<Integer, Component> componentMap,
            Map<Integer, ComponentMetrics> metricsMap
    ) {
        return componentIds.stream()
                .map(id -> from(componentMap.get(id), metricsMap.get(id)))
                .toList();
    }
}
