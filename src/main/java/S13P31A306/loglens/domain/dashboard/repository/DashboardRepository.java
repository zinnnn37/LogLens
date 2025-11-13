package S13P31A306.loglens.domain.dashboard.repository;

import S13P31A306.loglens.domain.dashboard.dto.opensearch.ErrorAggregation;
import S13P31A306.loglens.domain.dashboard.dto.opensearch.ErrorStatistics;
import java.time.LocalDateTime;
import java.util.List;

public interface DashboardRepository {
    List<ErrorAggregation> findTopErrors(String projectUuid, LocalDateTime start, LocalDateTime end, Integer limit);

    ErrorStatistics findErrorStatistics(String projectUuid, LocalDateTime start, LocalDateTime end);
}
