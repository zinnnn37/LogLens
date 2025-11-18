package S13P31A306.loglens.domain.statistics.service.impl;

import static S13P31A306.loglens.domain.statistics.constants.StatisticsConstants.INTERVAL_HOURS;
import static S13P31A306.loglens.domain.statistics.constants.StatisticsConstants.TREND_HOURS;

import S13P31A306.loglens.domain.log.repository.LogRepository;
import S13P31A306.loglens.domain.project.entity.Project;
import S13P31A306.loglens.domain.statistics.dto.internal.LogTrendAggregation;
import S13P31A306.loglens.domain.statistics.dto.response.LogTrendResponse;
import S13P31A306.loglens.domain.statistics.mapper.LogTrendMapper;
import S13P31A306.loglens.domain.statistics.service.LogTrendService;
import S13P31A306.loglens.domain.statistics.validator.StatisticsValidator;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 로그 추이 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogTrendServiceImpl implements LogTrendService {

    private static final String LOG_PREFIX = "[LogTrendService]";

    private final LogRepository logRepository;
    private final LogTrendMapper logTrendMapper;
    private final StatisticsValidator statisticsValidator;

    @Override
    public LogTrendResponse getLogTrend(String projectUuid) {
        log.info("{} 로그 추이 조회 시작: projectUuid={}", LOG_PREFIX, projectUuid);

        // 1. 요청 검증
        Project project = statisticsValidator.validateLogTrendRequest(projectUuid);

        // 2. UTC 시간으로 범위 계산
        LocalDateTime endTimeUtc = LocalDateTime.now(ZoneOffset.UTC)
                .truncatedTo(ChronoUnit.HOURS);
        LocalDateTime startTimeUtc = endTimeUtc.minusHours(TREND_HOURS);

        log.debug("{} 조회 기간 (UTC): {} ~ {}", LOG_PREFIX, startTimeUtc, endTimeUtc);

        // 3. OpenSearch 집계 조회
        List<LogTrendAggregation> aggregations = logRepository.aggregateLogTrendByTimeRange(
                projectUuid,
                startTimeUtc,
                endTimeUtc,
                INTERVAL_HOURS + "h"
        );

        log.debug("{} 집계 결과: {}개 데이터 포인트", LOG_PREFIX, aggregations.size());

        // 4. DTO 변환
        LogTrendResponse response = logTrendMapper.toLogTrendResponse(
                projectUuid,
                startTimeUtc,
                endTimeUtc,
                aggregations
        );

        return response;
    }
}
