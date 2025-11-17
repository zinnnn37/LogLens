package S13P31A306.loglens.domain.statistics.service.impl;

import S13P31A306.loglens.domain.log.repository.LogRepository;
import S13P31A306.loglens.domain.project.entity.Project;
import S13P31A306.loglens.domain.statistics.dto.internal.LogTrendAggregation;
import S13P31A306.loglens.domain.statistics.dto.response.LogTrendResponse;
import S13P31A306.loglens.domain.statistics.mapper.LogTrendMapper;
import S13P31A306.loglens.domain.statistics.service.LogTrendService;
import S13P31A306.loglens.domain.statistics.validator.StatisticsValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static S13P31A306.loglens.domain.statistics.constants.StatisticsConstants.*;

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

        // 2. 시간 범위 계산 (현재 시간 정각, 24시간 전)
        LocalDateTime endTime = LocalDateTime.now(ZoneId.of("Asia/Seoul")).truncatedTo(ChronoUnit.HOURS);
        LocalDateTime startTime = endTime.minusHours(TREND_HOURS);

        log.debug("{} 조회 기간: {} ~ {}", LOG_PREFIX, startTime, endTime);

        // 3. OpenSearch 집계 조회
        List<LogTrendAggregation> aggregations = logRepository.aggregateLogTrendByTimeRange(
                projectUuid,
                startTime,
                endTime,
                INTERVAL_HOURS + "h"
        );

        log.debug("{} 집계 결과: {}개 데이터 포인트", LOG_PREFIX, aggregations.size());

        // 4. DTO 변환 (Mapper 사용)
        LogTrendResponse response = logTrendMapper.toLogTrendResponse(
                projectUuid,
                startTime,
                endTime,
                aggregations
        );

        log.info("{} 로그 추이 조회 완료: projectUuid={}, dataPoints={}",
                LOG_PREFIX, projectUuid, response.dataPoints().size());

        return response;
    }
}
