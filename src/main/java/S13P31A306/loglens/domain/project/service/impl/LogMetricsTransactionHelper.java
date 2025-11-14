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

    private final LogMetricsRepository logMetricsRepository;
    private final HeatmapMetricsRepository heatmapMetricsRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveMetrics(LogMetrics logMetrics, List<HeatmapMetrics> heatmapMetrics) {

        Optional<LogMetrics> existingLogMetrics = logMetricsRepository
                .findTopByProjectIdOrderByAggregatedAtDesc(logMetrics.getProject().getId());

        if (existingLogMetrics.isPresent()) {
            LogMetrics merged = LogMetrics.builder()
                    .id(existingLogMetrics.get().getId())
                    .project(logMetrics.getProject())
                    .totalLogs(logMetrics.getTotalLogs())
                    .errorLogs(logMetrics.getErrorLogs())
                    .warnLogs(logMetrics.getWarnLogs())
                    .infoLogs(logMetrics.getInfoLogs())
                    .sumResponseTime(logMetrics.getSumResponseTime())
                    .avgResponseTime(logMetrics.getAvgResponseTime())
                    .aggregatedAt(logMetrics.getAggregatedAt())
                    .build();
            logMetricsRepository.save(merged);
        } else {
            logMetricsRepository.save(logMetrics);
        }

        for (HeatmapMetrics heatmap : heatmapMetrics) {
            Optional<HeatmapMetrics> existingHeatmap = heatmapMetricsRepository
                    .findByProjectIdAndDateAndHour(
                            heatmap.getProject().getId(),
                            heatmap.getDate(),
                            heatmap.getHour()
                    );

            if (existingHeatmap.isPresent()) {
                HeatmapMetrics merged = mergeHeatmapMetrics(existingHeatmap.get(), heatmap);
                heatmapMetricsRepository.save(merged);
            } else {
                heatmapMetricsRepository.save(heatmap);
            }
        }
    }

    private HeatmapMetrics mergeHeatmapMetrics(HeatmapMetrics existing, HeatmapMetrics increment) {
        return HeatmapMetrics.builder()
                .id(existing.getId())
                .project(existing.getProject())
                .date(existing.getDate())
                .hour(existing.getHour())
                .totalCount(existing.getTotalCount() + increment.getTotalCount())
                .errorCount(existing.getErrorCount() + increment.getErrorCount())
                .warnCount(existing.getWarnCount() + increment.getWarnCount())
                .infoCount(existing.getInfoCount() + increment.getInfoCount())
                .aggregatedAt(increment.getAggregatedAt())
                .build();
    }
}
