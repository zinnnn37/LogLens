package S13P31A306.loglens.domain.component.service.impl;

import S13P31A306.loglens.domain.component.entity.ComponentMetrics;
import S13P31A306.loglens.domain.component.repository.ComponentMetricsRepository;
import S13P31A306.loglens.domain.component.service.ComponentMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComponentMetricsServiceImpl implements ComponentMetricsService {

    private static final String LOG_PREFIX = "[ComponentMetricsService]";
    private final ComponentMetricsRepository componentMetricsRepository;

    @Override
    public Map<Integer, ComponentMetrics> getMetricsByComponentIds(List<Integer> componentIds) {
        log.debug("{} 메트릭 일괄 조회 시작: count={}", LOG_PREFIX, componentIds.size());

        Map<Integer, ComponentMetrics> metricsMap = componentIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        this::getMetricsByComponentId
                ));
        log.debug("{} 메트릭 일괄 조회 완료: count={}", LOG_PREFIX, metricsMap.size());
        return metricsMap;
    }

    @Override
    public ComponentMetrics getMetricsByComponentId(Integer componentId) {
        log.debug("{} 메트릭 조회: componentId={}", LOG_PREFIX, componentId);

        return componentMetricsRepository
                .findLatestByComponentId(componentId)
                .orElseGet(() -> createDefaultMetrics(componentId));
    }


    private ComponentMetrics createDefaultMetrics(Integer componentId) {
        log.debug("{} DB에 메트릭 없음, 기본값 반환: componentId={}", LOG_PREFIX, componentId);
        return ComponentMetrics.builder()
                .componentId(componentId)
                .callCount(0)
                .warnCount(0)
                .errorCount(0)
                .measuredAt(LocalDateTime.now())
                .build();
    }
}
