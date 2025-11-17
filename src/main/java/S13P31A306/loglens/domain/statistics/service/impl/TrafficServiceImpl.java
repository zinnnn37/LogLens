package S13P31A306.loglens.domain.statistics.service.impl;

import S13P31A306.loglens.domain.log.repository.LogRepository;
import S13P31A306.loglens.domain.project.entity.Project;
import S13P31A306.loglens.domain.statistics.dto.internal.TrafficAggregation;
import S13P31A306.loglens.domain.statistics.dto.response.TrafficResponse;
import S13P31A306.loglens.domain.statistics.mapper.TrafficMapper;
import S13P31A306.loglens.domain.statistics.service.TrafficService;
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
 * Traffic 그래프 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrafficServiceImpl implements TrafficService {

    private static final String LOG_PREFIX = "[TrafficService]";

    private final LogRepository logRepository;
    private final TrafficMapper trafficMapper;
    private final StatisticsValidator statisticsValidator;

    @Override
    public TrafficResponse getTraffic(String projectUuid) {
        log.info("{} Traffic 조회 시작: projectUuid={}", LOG_PREFIX, projectUuid);

        // 1. 요청 검증 (프로젝트 존재, 접근 권한 포함)
        Project project = statisticsValidator.validateTrafficRequest(projectUuid);

        // 2. 시간 범위 계산 (현재 시각 기준 24시간 전, 시각 단위로 절삭)
        LocalDateTime endTime = LocalDateTime.now(ZoneId.of("Asia/Seoul")).truncatedTo(ChronoUnit.HOURS);
        LocalDateTime startTime = endTime.minusHours(TREND_HOURS);

        log.debug("{} 조회 기간: {} ~ {}", LOG_PREFIX, startTime, endTime);

        // 3. OpenSearch 집계 조회
        List<TrafficAggregation> aggregations = logRepository.aggregateTrafficByTimeRange(
                projectUuid,
                startTime,
                endTime,
                INTERVAL_HOURS + "h"
        );

        log.debug("{} 집계 결과 개수: {}", LOG_PREFIX, aggregations.size());

        // 4. 응답 DTO로 변환
        TrafficResponse response = trafficMapper.toTrafficResponse(
                projectUuid,
                startTime,
                endTime,
                aggregations
        );

        log.info("{} Traffic 조회 완료: projectUuid={}, dataPoints={}",
                LOG_PREFIX, projectUuid, response.dataPoints().size());

        return response;
    }
}
