package S13P31A306.loglens.domain.project.service.impl;

import S13P31A306.loglens.domain.project.entity.HeatmapMetrics;
import S13P31A306.loglens.domain.project.entity.LogMetrics;
import S13P31A306.loglens.domain.project.repository.HeatmapMetricsRepository;
import S13P31A306.loglens.domain.project.repository.LogMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogMetricsTransactionHelper {

    private static final String LOG_PREFIX = "[LogMetricsTransactionHelper]";

    private final LogMetricsRepository logMetricsRepository;
    private final HeatmapMetricsRepository heatmapMetricsRepository;

    /**
     * LogMetrics 저장 (독립 트랜잭션)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLogMetrics(LogMetrics logMetrics) {
        Optional<LogMetrics> existing = logMetricsRepository
                .findTopByProjectIdOrderByAggregatedAtDesc(logMetrics.getProject().getId());

        if (existing.isPresent()) {
            LogMetrics entity = existing.get();
            entity.updateMetrics(
                    logMetrics.getTotalLogs(),
                    logMetrics.getErrorLogs(),
                    logMetrics.getWarnLogs(),
                    logMetrics.getInfoLogs(),
                    logMetrics.getSumResponseTime(),
                    logMetrics.getAvgResponseTime(),
                    logMetrics.getAggregatedAt()
            );
            log.debug("{} LogMetrics 업데이트: id={}, totalLogs={}",
                    LOG_PREFIX, entity.getId(), entity.getTotalLogs());
        } else {
            logMetricsRepository.save(logMetrics);
            log.debug("{} LogMetrics 신규 저장: totalLogs={}",
                    LOG_PREFIX, logMetrics.getTotalLogs());
        }
    }

    /**
     * HeatmapMetrics 저장 (독립 트랜잭션)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveHeatmapMetrics(List<HeatmapMetrics> heatmapMetrics) {
        int savedCount = 0;
        int updatedCount = 0;

        for (HeatmapMetrics heatmap : heatmapMetrics) {
            Optional<HeatmapMetrics> existing = heatmapMetricsRepository
                    .findByProjectIdAndDateAndHour(
                            heatmap.getProject().getId(),
                            heatmap.getDate(),
                            heatmap.getHour()
                    );

            if (existing.isPresent()) {
                HeatmapMetrics entity = existing.get();
                entity.updateMetrics(
                        entity.getTotalCount() + heatmap.getTotalCount(),
                        entity.getErrorCount() + heatmap.getErrorCount(),
                        entity.getWarnCount() + heatmap.getWarnCount(),
                        entity.getInfoCount() + heatmap.getInfoCount(),
                        heatmap.getAggregatedAt()
                );
                updatedCount++;
            } else {
                heatmapMetricsRepository.save(heatmap);
                savedCount++;
            }
        }

        log.info("{} HeatmapMetrics 저장 완료: 신규={}, 업데이트={}",
                LOG_PREFIX, savedCount, updatedCount);
    }

}
