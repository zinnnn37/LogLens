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

        // LogMetrics 처리
        Optional<LogMetrics> existingLogMetrics = logMetricsRepository
                .findTopByProjectIdOrderByAggregatedAtDesc(logMetrics.getProject().getId());

        if (existingLogMetrics.isPresent()) {
            LogMetrics existing = existingLogMetrics.get();
            // 기존 엔티티를 직접 수정 (더티 체킹 활용)
            existing.updateMetrics(
                    logMetrics.getTotalLogs(),
                    logMetrics.getErrorLogs(),
                    logMetrics.getWarnLogs(),
                    logMetrics.getInfoLogs(),
                    logMetrics.getSumResponseTime(),
                    logMetrics.getAvgResponseTime(),
                    logMetrics.getAggregatedAt()
            );
            // save 호출 불필요 (더티 체킹으로 자동 업데이트)
        } else {
            logMetricsRepository.save(logMetrics);
        }

        // HeatmapMetrics 처리
        for (HeatmapMetrics heatmap : heatmapMetrics) {
            Optional<HeatmapMetrics> existingHeatmap = heatmapMetricsRepository
                    .findByProjectIdAndDateAndHour(
                            heatmap.getProject().getId(),
                            heatmap.getDate(),
                            heatmap.getHour()
                    );

            if (existingHeatmap.isPresent()) {
                HeatmapMetrics existing = existingHeatmap.get();
                existing.updateMetrics(
                        existing.getTotalCount() + heatmap.getTotalCount(),
                        existing.getErrorCount() + heatmap.getErrorCount(),
                        existing.getWarnCount() + heatmap.getWarnCount(),
                        existing.getInfoCount() + heatmap.getInfoCount(),
                        heatmap.getAggregatedAt()
                );
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
