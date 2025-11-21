package S13P31A306.loglens.domain.component.service.impl;

import S13P31A306.loglens.domain.component.entity.FrontendMetrics;
import S13P31A306.loglens.domain.component.repository.FrontendMetricsRepository;
import S13P31A306.loglens.domain.component.service.FrontendMetricsService;
import S13P31A306.loglens.domain.dashboard.dto.FrontendMetricsSummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FrontendMetricsServiceImpl implements FrontendMetricsService {

    private static final String LOG_PREFIX = "[FrontendMetricsService]";
    private final FrontendMetricsRepository frontendMetricsRepository;

    @Override
    public FrontendMetricsSummary getFrontendMetricsByProjectId(Integer projectId) {
        log.debug("{} Frontend 메트릭 조회: projectId={}", LOG_PREFIX, projectId);

        return frontendMetricsRepository
                .findLatestByProjectId(projectId)
                .map(this::toSummary)
                .orElseGet(() -> {
                    log.debug("{} Frontend 메트릭 없음, 기본값 반환: projectId={}", LOG_PREFIX, projectId);
                    return FrontendMetricsSummary.empty();
                });
    }

    /**
     * Entity → DTO 변환
     */
    private FrontendMetricsSummary toSummary(FrontendMetrics entity) {
        return new FrontendMetricsSummary(
                entity.getTotalTraces(),
                entity.getTotalInfoLogs(),
                entity.getTotalWarnLogs(),
                entity.getTotalErrorLogs(),
                entity.getErrorRate()
        );
    }
}
