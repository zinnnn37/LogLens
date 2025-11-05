package S13P31A306.loglens.domain.component.scheduler;

import S13P31A306.loglens.domain.component.dto.MetricsData;
import S13P31A306.loglens.domain.component.entity.Component;
import S13P31A306.loglens.domain.component.entity.ComponentMetrics;
import S13P31A306.loglens.domain.component.repository.ComponentMetricsRepository;
import S13P31A306.loglens.domain.component.repository.ComponentRepository;
import S13P31A306.loglens.domain.component.service.OpenSearchMetricsService;
import S13P31A306.loglens.domain.project.entity.Project;
import S13P31A306.loglens.domain.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComponentMetricsScheduler {

    private static final String LOG_PREFIX = "[ComponentMetricsScheduler]";

    private final ComponentRepository componentRepository;
    private final ComponentMetricsRepository componentMetricsRepository;
    private final ProjectRepository projectRepository;
    private final OpenSearchMetricsService openSearchMetricsService;

    /**
     * 매 5분마다 모든 컴포넌트의 메트릭 갱신
     * cron: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void updateAllComponentsMetrics() {
        log.info("{} ========== 메트릭 갱신 시작 ==========", LOG_PREFIX);
        long startTime = System.currentTimeMillis();

        try {
            // Step 1: DB에서 모든 Component 조회
            List<Component> allComponents = componentRepository.findAll();
            if (allComponents.isEmpty()) {
                log.warn("{} DB에 등록된 컴포넌트가 없습니다.", LOG_PREFIX);
                return;
            }
            log.info("{} 총 {} 개 컴포넌트 발견", LOG_PREFIX, allComponents.size());

            // Step 2: ProjectId → ProjectUuid 매핑 생성
            Set<Integer> projectIds = allComponents.stream()
                    .map(Component::getProjectId)
                    .collect(Collectors.toSet());

            Map<Integer, String> projectIdToUuidMap = projectRepository.findAllById(projectIds).stream()
                    .collect(Collectors.toMap(Project::getId, Project::getProjectUuid));

            log.debug("{} {} 개 프로젝트 매핑 완료", LOG_PREFIX, projectIdToUuidMap.size());

            Map<String, List<Component>> componentsByProject = allComponents.stream()
                    .filter(component -> projectIdToUuidMap.containsKey(component.getProjectId()))
                    .collect(Collectors.groupingBy(component ->
                            projectIdToUuidMap.get(component.getProjectId())
                    ));

            log.info("{} {} 개 프로젝트로 그룹화 완료", LOG_PREFIX, componentsByProject.size());

            List<ComponentMetrics> allMetricsToSave = new ArrayList<>();
            int totalUpdated = 0;
            int totalNoData = 0;

            for (Map.Entry<String, List<Component>> entry : componentsByProject.entrySet()) {
                String projectUuid = entry.getKey();
                List<Component> projectComponents = entry.getValue();

                log.debug("{} 프로젝트 메트릭 조회 시작: projectUuid={}, components={}",
                        LOG_PREFIX, projectUuid, projectComponents.size());

                Map<String, MetricsData> metricsMap =
                        openSearchMetricsService.getProjectMetrics(projectUuid);
                
                LocalDateTime now = LocalDateTime.now();
                for (Component component : projectComponents) {
                    MetricsData metricsData = metricsMap.get(component.getName());

                    if (metricsData != null && metricsData.totalCalls() > 0) {
                        ComponentMetrics metrics = ComponentMetrics.builder()
                                .componentId(component.getId())
                                .callCount(metricsData.totalCalls())
                                .errorCount(metricsData.errorCount())
                                .warnCount(metricsData.warnCount())
                                .measuredAt(now)
                                .build();

                        allMetricsToSave.add(metrics);
                        totalUpdated++;

                        log.trace("{} 메트릭 준비: {} - calls={}, errors={}, warns={}",
                                LOG_PREFIX, component.getName(),
                                metricsData.totalCalls(), metricsData.errorCount(), metricsData.warnCount());
                    } else {
                        totalNoData++;
                        log.trace("{} 메트릭 데이터 없음: {}", LOG_PREFIX, component.getName());
                    }
                }

                log.debug("{} 프로젝트 메트릭 매핑 완료: projectUuid={}, 데이터 있음={}, 없음={}",
                        LOG_PREFIX, projectUuid, metricsMap.size(), projectComponents.size() - metricsMap.size());
            }

            // Step 5: 배치 저장
            if (!allMetricsToSave.isEmpty()) {
                componentMetricsRepository.saveAll(allMetricsToSave);
                log.info("{} ✅ 메트릭 배치 저장 완료: {} 개 업데이트, {} 개 데이터 없음",
                        LOG_PREFIX, totalUpdated, totalNoData);
            } else {
                log.warn("{} 저장할 메트릭 데이터가 없습니다.", LOG_PREFIX);
            }

            long elapsedTime = System.currentTimeMillis() - startTime;
            log.info("{} ========== 메트릭 갱신 완료 ({}ms) ==========", LOG_PREFIX, elapsedTime);

        } catch (Exception e) {
            log.error("{} 메트릭 갱신 중 오류 발생", LOG_PREFIX, e);
        }
    }

    /**
     * 특정 컴포넌트의 메트릭만 갱신 (필요시 수동 호출)
     *
     * @param componentId 컴포넌트 ID
     */
    @Transactional
    public void updateComponentMetrics(Integer componentId) {
        log.info("{} 단일 컴포넌트 메트릭 갱신: componentId={}", LOG_PREFIX, componentId);

        try {
            Component component = componentRepository.findById(componentId)
                    .orElseThrow(() -> new IllegalArgumentException("컴포넌트를 찾을 수 없습니다: " + componentId));

            Project project = projectRepository.findById(component.getProjectId())
                    .orElseThrow(() -> new IllegalArgumentException("프로젝트를 찾을 수 없습니다: " + component.getProjectId()));

            MetricsData metricsData = openSearchMetricsService.getComponentMetrics(
                    project.getProjectUuid(),
                    component.getName()
            );

            ComponentMetrics metrics = ComponentMetrics.builder()
                    .componentId(component.getId())
                    .callCount(metricsData.totalCalls())
                    .errorCount(metricsData.errorCount())
                    .warnCount(metricsData.warnCount())
                    .measuredAt(LocalDateTime.now())
                    .build();

            componentMetricsRepository.save(metrics);

            log.info("{} ✅ 단일 컴포넌트 메트릭 갱신 완료: {} - calls={}, errors={}, warns={}",
                    LOG_PREFIX, component.getName(),
                    metricsData.totalCalls(), metricsData.errorCount(), metricsData.warnCount());

        } catch (Exception e) {
            log.error("{} 단일 컴포넌트 메트릭 갱신 실패: componentId={}", LOG_PREFIX, componentId, e);
        }
    }
}
