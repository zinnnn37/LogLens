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
import java.time.ZonedDateTime;
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

        // 1. 요청 검증 (프로젝트 존재, 접근 권한 포함)
        Project project = statisticsValidator.validateLogTrendRequest(projectUuid);

        // 2. 시간 범위 계산 (한국 시간)
        ZonedDateTime endTimeKst = ZonedDateTime.now(ZoneId.of("Asia/Seoul"))
                .truncatedTo(ChronoUnit.HOURS);
        ZonedDateTime startTimeKst = endTimeKst.minusHours(TREND_HOURS);

        // 3. UTC로 변환
        LocalDateTime endTimeUtc = endTimeKst.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        LocalDateTime startTimeUtc = startTimeKst.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();

        log.debug("{} 조회 기간 (KST): {} ~ {}", LOG_PREFIX, startTimeKst, endTimeKst);
        log.debug("{} 조회 기간 (UTC): {} ~ {}", LOG_PREFIX, startTimeUtc, endTimeUtc);

        // 4. OpenSearch 집계 조회 (UTC 시간 전달)
        List<LogTrendAggregation> aggregations = logRepository.aggregateLogTrendByTimeRange(
                projectUuid,
                startTimeUtc,
                endTimeUtc,
                INTERVAL_HOURS + "h"
        );

        log.debug("{} 집계 결과: {}개 데이터 포인트", LOG_PREFIX, aggregations.size());

        // 5. DTO 변환 (원본 한국 시간으로 응답)
        LogTrendResponse response = logTrendMapper.toLogTrendResponse(
                projectUuid,
                startTimeKst.toLocalDateTime(),  // 응답은 한국 시간으로
                endTimeKst.toLocalDateTime(),
                aggregations
        );

        return response;
    }
}
