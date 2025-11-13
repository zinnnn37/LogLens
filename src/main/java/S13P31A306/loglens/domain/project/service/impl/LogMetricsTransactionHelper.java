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

/**
 * 트랜잭션 분리를 위한 헬퍼 클래스
 * Self-invocation 문제를 해결하기 위해 별도로 분리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogMetricsTransactionHelper {

    private final LogMetricsRepository logMetricsRepository;
    private final HeatmapMetricsRepository heatmapMetricsRepository;

    /**
     * LogMetrics와 HeatmapMetrics를 트랜잭션으로 저장 (커넥션 점유 시간 최소화)
     * 프로젝트당 최신 데이터만 유지
     *
     * @param logMetrics 저장할 로그 메트릭
     * @param heatmapMetrics 저장할 히트맵 메트릭 리스트
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveMetrics(LogMetrics logMetrics, List<HeatmapMetrics> heatmapMetrics) {

        // 1. LogMetrics UPSERT (프로젝트당 1개만 유지)
        Optional<LogMetrics> existingLogMetrics = logMetricsRepository
                .findTopByProjectIdOrderByAggregatedAtDesc(logMetrics.getProject().getId());

        if (existingLogMetrics.isPresent()) {
            // 기존 행 UPDATE
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
            // 새 행 INSERT
            logMetricsRepository.save(logMetrics);
        }

        // 2. HeatmapMetrics UPSERT (요일/시간대별 누적)
        for (HeatmapMetrics heatmap : heatmapMetrics) {
            Optional<HeatmapMetrics> existingHeatmap = heatmapMetricsRepository
                    .findByProjectIdAndDayOfWeekAndHour(
                            heatmap.getProject().getId(),
                            heatmap.getDayOfWeek(),
                            heatmap.getHour()
                    );

            if (existingHeatmap.isPresent()) {
                // 기존 데이터와 병합 (누적)
                HeatmapMetrics merged = mergeHeatmapMetrics(existingHeatmap.get(), heatmap);
                heatmapMetricsRepository.save(merged);
            } else {
                // 새로 생성
                heatmapMetricsRepository.save(heatmap);
            }
        }
    }

    /**
     * 기존 히트맵 메트릭과 새 메트릭을 병합
     *
     * @param existing 기존 메트릭
     * @param increment 증분 메트릭
     * @return 병합된 메트릭
     */
    private HeatmapMetrics mergeHeatmapMetrics(HeatmapMetrics existing, HeatmapMetrics increment) {
        return HeatmapMetrics.builder()
                .id(existing.getId())
                .project(existing.getProject())
                .dayOfWeek(existing.getDayOfWeek())
                .hour(existing.getHour())
                .totalCount(existing.getTotalCount() + increment.getTotalCount())
                .errorCount(existing.getErrorCount() + increment.getErrorCount())
                .warnCount(existing.getWarnCount() + increment.getWarnCount())
                .infoCount(existing.getInfoCount() + increment.getInfoCount())
                .aggregatedAt(increment.getAggregatedAt())  // 최신 집계 시간
                .build();
    }
}
