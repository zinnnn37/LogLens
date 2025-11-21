package S13P31A306.loglens.domain.dashboard.service.impl;

import S13P31A306.loglens.domain.dashboard.dto.opensearch.ErrorAggregation;
import S13P31A306.loglens.domain.dashboard.dto.opensearch.ErrorStatistics;
import S13P31A306.loglens.domain.dashboard.repository.DashboardRepository;
import S13P31A306.loglens.domain.dashboard.service.TopFrequentErrorsQueryService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TopFrequentErrorsQueryServiceImpl implements TopFrequentErrorsQueryService {

    private final DashboardRepository dashboardRepository;

    @Override
    public List<ErrorAggregation> queryTopErrors(
            String projectUuid,
            LocalDateTime start,
            LocalDateTime end,
            Integer limit) {
        return dashboardRepository.findTopErrors(projectUuid, start, end, limit);
    }

    @Override
    public ErrorStatistics queryErrorStatistics(
            String projectUuid,
            LocalDateTime start,
            LocalDateTime end) {
        return dashboardRepository.findErrorStatistics(projectUuid, start, end);
    }
}