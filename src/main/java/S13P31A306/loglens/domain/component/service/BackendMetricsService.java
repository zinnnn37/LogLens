package S13P31A306.loglens.domain.component.service;

import S13P31A306.loglens.domain.component.entity.ComponentMetrics;

import java.util.List;
import java.util.Map;

public interface BackendMetricsService {

    /**
     * 여러 컴포넌트의 최신 메트릭 조회 (캐시 사용)
     *
     * @param componentIds 컴포넌트 ID 리스트
     * @return componentId -> ComponentMetrics 맵
     */
    Map<Integer, ComponentMetrics> getMetricsByComponentIds(List<Integer> componentIds);

    /**
     * 단일 컴포넌트의 최신 메트릭 조회 (캐시 사용)
     *
     * @param componentId 컴포넌트 ID
     * @return 메트릭 정보 (없으면 기본값)
     */
    ComponentMetrics getMetricsByComponentId(Integer componentId);
}
