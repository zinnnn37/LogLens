package S13P31A306.loglens.domain.component.scheduler;

import S13P31A306.loglens.domain.component.dto.MetricsData;
import S13P31A306.loglens.domain.component.entity.ComponentMetrics;
import S13P31A306.loglens.domain.component.entity.FrontendMetrics;
import S13P31A306.loglens.domain.component.repository.ComponentMetricsRepository;
import S13P31A306.loglens.domain.component.repository.ComponentRepository;
import S13P31A306.loglens.domain.component.repository.FrontendMetricsRepository;
import S13P31A306.loglens.domain.component.service.OpenSearchMetricsService;
import S13P31A306.loglens.domain.dashboard.dto.FrontendMetricsSummary;
import S13P31A306.loglens.domain.project.entity.Project;
import S13P31A306.loglens.domain.project.repository.ProjectRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 메트릭 갱신 스케줄러 Backend 컴포넌트 메트릭 + Frontend 메트릭을 주기적으로 갱신
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetricsUpdateScheduler {

    private static final String LOG_PREFIX = "[MetricsScheduler]";

    private final ProjectRepository projectRepository;
    private final ComponentRepository componentRepository;
    private final ComponentMetricsRepository componentMetricsRepository;
    private final FrontendMetricsRepository frontendMetricsRepository;
    private final OpenSearchMetricsService openSearchMetricsService;

    /**
     * 메트릭 갱신 (매 5분마다 실행)
     * cron: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void updateMetrics() {
        log.info("{} ========== 메트릭 갱신 시작 ==========", LOG_PREFIX);
        long startTime = System.currentTimeMillis();

        try {
            List<Project> allProjects = projectRepository.findAll();
            log.info("{} 전체 프로젝트 수: {}", LOG_PREFIX, allProjects.size());

            for (Project project : allProjects) {
                try {
                    updateProjectMetricsInNewTransaction(project);
                } catch (Exception e) {
                    log.error("{} 프로젝트 메트릭 갱신 실패: projectId={}, projectName={}",
                            LOG_PREFIX, project.getId(), project.getProjectName(), e);
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("{} ========== 메트릭 갱신 완료: {}ms ==========", LOG_PREFIX, duration);

        } catch (Exception e) {
            log.error("{} 메트릭 갱신 중 오류 발생", LOG_PREFIX, e);
        }
    }

    /**
     * 프로젝트별 메트릭 갱신 (새 트랜잭션) OpenSearch 호출과 DB 저장을 하나의 독립된 트랜잭션으로 처리
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateProjectMetricsInNewTransaction(Project project) {
        Integer projectId = project.getId();
        String projectUuid = project.getProjectUuid();

        log.debug("{} 프로젝트 메트릭 갱신: projectId={}, projectName={}",
                LOG_PREFIX, projectId, project.getProjectName());

        // 1. Backend 컴포넌트 메트릭 갱신
        updateBackendMetrics(projectId, projectUuid);

        // 2. Frontend 메트릭 갱신
        updateFrontendMetrics(projectId, projectUuid);
    }

    /**
     * Backend 컴포넌트 메트릭 갱신
     */
    private void updateBackendMetrics(Integer projectId, String projectUuid) {
        // OpenSearch에서 프로젝트의 모든 컴포넌트 메트릭 조회
        Map<String, MetricsData> metricsMap =
                openSearchMetricsService.getProjectMetrics(projectUuid);

        if (metricsMap.isEmpty()) {
            log.info("{} Backend 메트릭 없음: projectId={}", LOG_PREFIX, projectId);
            return;
        }

        // DB에서 프로젝트의 모든 컴포넌트 조회
        List<S13P31A306.loglens.domain.component.entity.Component> components = componentRepository.findAllByProjectId(
                projectId);

        int updatedCount = 0;
        LocalDateTime now = LocalDateTime.now();

        for (S13P31A306.loglens.domain.component.entity.Component component : components) {
            MetricsData metricsData = metricsMap.get(component.getName());

            if (metricsData == null) {
                continue;
            }

            // ComponentMetrics 저장 또는 업데이트
            ComponentMetrics metrics = ComponentMetrics.builder()
                    .componentId(component.getId())
                    .callCount(metricsData.totalCalls())
                    .errorCount(metricsData.errorCount())
                    .warnCount(metricsData.warnCount())
                    .measuredAt(now)
                    .build();

            componentMetricsRepository.save(metrics);
            updatedCount++;
        }

        log.info("{} Backend 메트릭 갱신 완료: projectId={}, 갱신={}/{}",
                LOG_PREFIX, projectId, updatedCount, components.size());
    }

    /**
     * Frontend 메트릭 갱신
     */
    private void updateFrontendMetrics(Integer projectId, String projectUuid) {
        // OpenSearch에서 Frontend 메트릭 조회
        FrontendMetricsSummary summary =
                openSearchMetricsService.getFrontendMetrics(projectUuid);

        if (summary.totalTraces() == 0) {
            log.info("{} Frontend 메트릭 없음: projectId={}", LOG_PREFIX, projectId);
            return;
        }

        // FrontendMetrics 저장 또는 업데이트
        FrontendMetrics metrics = FrontendMetrics.of(
                projectId,
                summary.totalTraces(),
                summary.totalInfo(),
                summary.totalWarn(),
                summary.totalError()
        );

        frontendMetricsRepository.save(metrics);

        log.info("{} Frontend 메트릭 갱신 완료: projectId={}, traces={}, errors={}",
                LOG_PREFIX, projectId, summary.totalTraces(), summary.totalError());
    }

    /**
     * 애플리케이션 시작 시 즉시 실행 (선택사항)
     */
    // @PostConstruct
    // public void initializeMetrics() {
    //     log.info("{} 초기 메트릭 갱신 실행", LOG_PREFIX);
    //     updateMetrics();
    // }
}
