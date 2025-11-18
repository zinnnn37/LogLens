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
import java.time.ZoneId;
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

        // 2. 한국 시간 기준으로 조회 시간 계산
        ZoneId KST = ZoneId.of("Asia/Seoul");

        LocalDateTime endKst = LocalDateTime.now(KST).truncatedTo(ChronoUnit.HOURS);
        LocalDateTime startKst = endKst.minusHours(TREND_HOURS);

        // 3. 검색용 UTC 변환
        LocalDateTime startUtc = startKst.atZone(KST).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        LocalDateTime endUtc = endKst.atZone(KST).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();

        log.debug("{} 조회 기간 KST: {} ~ {}", LOG_PREFIX, startKst, endKst);
        log.debug("{} 조회 기간 UTC: {} ~ {}", LOG_PREFIX, startUtc, endUtc);

        // 4. OpenSearch 집계 조회 (UTC)
        List<LogTrendAggregation> aggregations = logRepository.aggregateLogTrendByTimeRange(
                projectUuid,
                startUtc,
                endUtc,
                INTERVAL_HOURS + "h"
        );

        // 5. 응답은 KST로 출력
        return logTrendMapper.toLogTrendResponse(
                projectUuid,
                startKst,
                endKst,
                aggregations
        );
    }
}
