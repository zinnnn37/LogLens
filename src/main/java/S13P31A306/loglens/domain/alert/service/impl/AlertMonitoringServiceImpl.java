package S13P31A306.loglens.domain.alert.service.impl;

import S13P31A306.loglens.domain.alert.entity.AlertConfig;
import S13P31A306.loglens.domain.alert.entity.AlertHistory;
import S13P31A306.loglens.domain.alert.entity.AlertType;
import S13P31A306.loglens.domain.alert.repository.AlertConfigRepository;
import S13P31A306.loglens.domain.alert.repository.AlertHistoryRepository;
import S13P31A306.loglens.domain.alert.service.AlertMonitoringService;
import S13P31A306.loglens.domain.project.entity.LogMetrics;
import S13P31A306.loglens.domain.project.entity.Project;
import S13P31A306.loglens.domain.project.repository.LogMetricsRepository;
import S13P31A306.loglens.domain.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 알림 모니터링 서비스 구현체
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AlertMonitoringServiceImpl implements AlertMonitoringService {

    private static final String LOG_PREFIX = "[AlertMonitoringService]";
    private static final int DEDUP_WINDOW_MINUTES = 5; // 중복 방지 시간 (5분)
    private static final int METRICS_FRESHNESS_MINUTES = 15; // 메트릭 유효성 시간 (15분)

    private final ProjectRepository projectRepository;
    private final AlertConfigRepository alertConfigRepository;
    private final AlertHistoryRepository alertHistoryRepository;
    private final LogMetricsRepository logMetricsRepository;

    @Override
    public void checkAndCreateAlerts() {
        log.info("{} 알림 모니터링 시작", LOG_PREFIX);

        List<Project> projects = projectRepository.findAll();
        log.debug("{} 대상 프로젝트 개수: {}", LOG_PREFIX, projects.size());

        int alertsCreated = 0;
        int projectsChecked = 0;

        for (Project project : projects) {
            try {
                boolean alertCreated = checkProjectAlertsInNewTransaction(project);
                if (alertCreated) {
                    alertsCreated++;
                }
                projectsChecked++;
            } catch (Exception e) {
                log.error("{} 프로젝트 알림 체크 실패: projectId={}, projectUuid={}",
                        LOG_PREFIX, project.getId(), project.getProjectUuid(), e);
            }
        }

        log.info("{} 알림 모니터링 완료: 체크된 프로젝트={}, 생성된 알림={}",
                LOG_PREFIX, projectsChecked, alertsCreated);
    }

    /**
     * 프로젝트별 알림 체크 (독립 트랜잭션)
     * 각 프로젝트마다 새로운 트랜잭션으로 처리하여 한 프로젝트의 실패가 다른 프로젝트에 영향을 주지 않도록 함
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean checkProjectAlertsInNewTransaction(Project project) {
        log.debug("{} 프로젝트 알림 체크 시작: projectId={}, projectUuid={}",
                LOG_PREFIX, project.getId(), project.getProjectUuid());

        // 1. AlertConfig 조회 (activeYN = 'Y'인 것만)
        Optional<AlertConfig> configOpt = alertConfigRepository.findByProjectId(project.getId());

        if (configOpt.isEmpty()) {
            log.debug("{} AlertConfig 없음: projectId={}", LOG_PREFIX, project.getId());
            return false;
        }

        AlertConfig config = configOpt.get();

        if (!"Y".equals(config.getActiveYN())) {
            log.debug("{} AlertConfig 비활성화 상태: projectId={}", LOG_PREFIX, project.getId());
            return false;
        }

        // 2. 최신 LogMetrics 조회
        Optional<LogMetrics> metricsOpt = logMetricsRepository
                .findFirstByProjectIdOrderByAggregatedAtDesc(project.getId());

        if (metricsOpt.isEmpty()) {
            log.debug("{} LogMetrics 데이터 없음: projectId={}", LOG_PREFIX, project.getId());
            return false;
        }

        LogMetrics metrics = metricsOpt.get();

        // 3. 메트릭이 너무 오래된 경우 (15분 이상) 스킵
        LocalDateTime metricsAgeThreshold = LocalDateTime.now().minusMinutes(METRICS_FRESHNESS_MINUTES);
        if (metrics.getAggregatedAt().isBefore(metricsAgeThreshold)) {
            log.debug("{} 오래된 메트릭 데이터: projectId={}, aggregatedAt={}, threshold={}",
                    LOG_PREFIX, project.getId(), metrics.getAggregatedAt(), metricsAgeThreshold);
            return false;
        }

        // 4. 알림 타입별 처리 (Phase 1: ERROR_THRESHOLD만 구현)
        if (config.getAlertType() == AlertType.ERROR_THRESHOLD) {
            return checkErrorThreshold(project, config, metrics);
        }

        // Phase 2, 3: LATENCY, ERROR_RATE는 추후 구현
        log.debug("{} 미지원 알림 타입: projectId={}, alertType={}",
                LOG_PREFIX, project.getId(), config.getAlertType());
        return false;
    }

    /**
     * ERROR_THRESHOLD 타입 알림 체크
     */
    private boolean checkErrorThreshold(Project project, AlertConfig config, LogMetrics metrics) {
        int errorCount = metrics.getErrorLogs();
        int threshold = config.getThresholdValue();

        log.debug("{} ERROR_THRESHOLD 체크: projectId={}, errorCount={}, threshold={}",
                LOG_PREFIX, project.getId(), errorCount, threshold);

        // 임계치 체크
        if (errorCount < threshold) {
            log.debug("{} 임계치 미만: projectId={}, errorCount={}, threshold={}",
                    LOG_PREFIX, project.getId(), errorCount, threshold);
            return false;
        }

        // 중복 알림 체크 (최근 5분 이내에 동일 알림이 있는지)
        LocalDateTime dedupWindow = LocalDateTime.now().minusMinutes(DEDUP_WINDOW_MINUTES);
        List<AlertHistory> recentAlerts = alertHistoryRepository
                .findByProjectIdAndAlertTimeAfterOrderByAlertTimeDesc(project.getId(), dedupWindow);

        if (!recentAlerts.isEmpty()) {
            log.debug("{} 중복 알림 방지: projectId={}, 최근 알림 시간={}",
                    LOG_PREFIX, project.getId(), recentAlerts.get(0).getAlertTime());
            return false;
        }

        // 알림 생성
        createErrorThresholdAlert(project, config, metrics);
        return true;
    }

    /**
     * ERROR_THRESHOLD 타입 알림 생성 및 저장
     */
    private void createErrorThresholdAlert(Project project, AlertConfig config, LogMetrics metrics) {
        int errorCount = metrics.getErrorLogs();
        int threshold = config.getThresholdValue();

        // 알림 메시지 생성
        String message = String.format(
                "에러 임계값 초과: 최근 10분간 %d건의 에러가 발생했습니다 (임계값: %d건)",
                errorCount, threshold
        );

        // logReference JSON 생성
        String logReference = String.format(
                "{\"alertType\":\"ERROR_THRESHOLD\",\"errorCount\":%d,\"threshold\":%d," +
                "\"totalLogs\":%d,\"period\":\"10min\",\"aggregatedAt\":\"%s\",\"projectUuid\":\"%s\"}",
                errorCount, threshold, metrics.getTotalLogs(),
                metrics.getAggregatedAt().toString(), project.getProjectUuid()
        );

        // AlertHistory 엔티티 생성
        AlertHistory alert = AlertHistory.builder()
                .alertMessage(message)
                .alertTime(LocalDateTime.now())
                .resolvedYN("N")
                .logReference(logReference)
                .projectId(project.getId())
                .build();

        // 저장
        alertHistoryRepository.save(alert);

        log.info("{} 알림 생성 완료: projectId={}, projectUuid={}, errorCount={}, threshold={}",
                LOG_PREFIX, project.getId(), project.getProjectUuid(), errorCount, threshold);
    }
}
