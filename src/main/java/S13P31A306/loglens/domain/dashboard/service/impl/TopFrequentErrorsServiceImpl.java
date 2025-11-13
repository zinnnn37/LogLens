package S13P31A306.loglens.domain.dashboard.service.impl;

import static S13P31A306.loglens.domain.dashboard.constants.DashboardConstants.ERROR_DEFAULT_TIME_RANGE;
import static S13P31A306.loglens.domain.dashboard.constants.DashboardConstants.ERROR_MAX_DEFAULT_RETRIEVAL_TIME;

import S13P31A306.loglens.domain.component.entity.Component;
import S13P31A306.loglens.domain.component.repository.ComponentRepository;
import S13P31A306.loglens.domain.dashboard.dto.opensearch.ErrorAggregation;
import S13P31A306.loglens.domain.dashboard.dto.opensearch.ErrorStatistics;
import S13P31A306.loglens.domain.dashboard.dto.response.TopFrequentErrorsResponse;
import S13P31A306.loglens.domain.dashboard.service.TopFrequentErrorsQueryService;
import S13P31A306.loglens.domain.dashboard.service.TopFrequentErrorsService;
import S13P31A306.loglens.domain.dashboard.validator.DashboardValidator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 자주 발생하는 에러 조회 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TopFrequentErrorsServiceImpl implements TopFrequentErrorsService {

    private static final String LOG_PREFIX = "[TopFrequentErrorsService]";

    private final TopFrequentErrorsQueryService topFrequentErrorsQueryService;
    private final DashboardValidator dashboardValidator;
    private final ComponentRepository componentRepository;

    /**
     * 자주 발생하는 에러 Top N 조회
     *
     * @param projectUuid 프로젝트 UUID
     * @param startTime   조회 시작 시간 (ISO 8601, Optional)
     * @param endTime     조회 종료 시간 (ISO 8601, Optional)
     * @param limit       조회할 에러 개수 (1~50, 기본값 10)
     * @return TopFrequentErrorsResponse
     */
    @Override
    public TopFrequentErrorsResponse getTopFrequentErrors(
            String projectUuid,
            String startTime,
            String endTime,
            Integer limit) {

        log.info("{} 자주 발생하는 에러 Top {} 조회 시작: projectUuid={}",
                LOG_PREFIX, limit, projectUuid);

        // 1. 권한 검증
        Integer projectId = dashboardValidator.validateProjectAccess(projectUuid);

        // 2. limit 검증
        dashboardValidator.validateErrorLimit(limit);

        // 3. 시간 파싱 및 기본값 설정
        LocalDateTime parsedEnd = dashboardValidator.validateAndParseTime(endTime);
        LocalDateTime parsedStart = dashboardValidator.validateAndParseTime(startTime);

        LocalDateTime end;
        if (parsedEnd != null) {
            end = parsedEnd;
        } else if (parsedStart != null) {
            end = parsedStart.plusDays(ERROR_DEFAULT_TIME_RANGE);
        } else {
            end = LocalDateTime.now();
        }
        LocalDateTime start = parsedStart != null ? parsedStart : end.minusDays(ERROR_DEFAULT_TIME_RANGE);

        // 4. 시간 범위 검증
        dashboardValidator.validateTimeRange(start, end, ERROR_MAX_DEFAULT_RETRIEVAL_TIME);

        // 5. OpenSearch: Top N 에러 집계
        List<ErrorAggregation> errorAggs =
                topFrequentErrorsQueryService.queryTopErrors(projectUuid, start, end, limit);

        // 6. OpenSearch: 전체 에러 통계
        ErrorStatistics statistics =
                topFrequentErrorsQueryService.queryErrorStatistics(projectUuid, start, end);

        // 7. 컴포넌트 매칭
        Map<String, List<Component>> loggerToComponents = matchComponents(projectId, errorAggs);

        // 8. ErrorInfo 생성
        List<TopFrequentErrorsResponse.ErrorInfo> errorInfos =
                buildErrorInfos(errorAggs, statistics.totalErrors(), loggerToComponents);

        // 9. ErrorSummary 생성
        TopFrequentErrorsResponse.ErrorSummary summary =
                buildErrorSummary(errorInfos, statistics);

        log.info("{} 자주 발생하는 에러 Top {} 조회 완료: totalErrors={}, uniqueTypes={}",
                LOG_PREFIX, limit, statistics.totalErrors(), statistics.uniqueErrorTypes());

        return new TopFrequentErrorsResponse(
                projectUuid,
                new TopFrequentErrorsResponse.Period(start, end),
                errorInfos,
                summary
        );
    }

    //@formatter:off

    /**
     * 컴포넌트 매칭: logger 필드를 기반으로 MySQL components 테이블과 매칭
     * logger가 component의 package_name으로 시작하면 해당 컴포넌트로 매칭
     *
     * @param projectId 프로젝트 ID
     * @param errorAggs 에러 집계 결과 리스트
     * @return logger → 컴포넌트 리스트 매핑
     */
    //@formatter:on
    private Map<String, List<Component>> matchComponents(
            Integer projectId,
            List<ErrorAggregation> errorAggs) {

        // logger 목록 추출
        Set<String> loggers = errorAggs.stream()
                .map(ErrorAggregation::logger)
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());

        if (loggers.isEmpty()) {
            return Map.of();
        }

        // MySQL: 프로젝트의 모든 컴포넌트 조회
        List<Component> allComponents = componentRepository.findAllByProjectId(projectId);

        // logger → Components 매핑 (logger가 package_name으로 시작하는지 확인)
        Map<String, List<Component>> loggerToComponents = new HashMap<>();
        for (String logger : loggers) {
            List<Component> matched = allComponents.stream()
                    .filter(c -> c.getPackageName() != null && logger.startsWith(c.getPackageName()))
                    .toList();
            loggerToComponents.put(logger, matched);
        }

        return loggerToComponents;
    }

    //@formatter:off

    /**
     * ErrorInfo 리스트 생성
     * rank, percentage를 계산하고 컴포넌트 정보를 매칭하여 ErrorInfo 객체 생성
     *
     * @param errorAggs          에러 집계 결과
     * @param totalErrorCount    전체 에러 수
     * @param loggerToComponents logger → 컴포넌트 매핑
     * @return ErrorInfo 리스트
     */
    //@formatter:on
    private List<TopFrequentErrorsResponse.ErrorInfo> buildErrorInfos(
            List<ErrorAggregation> errorAggs,
            Integer totalErrorCount,
            Map<String, List<Component>> loggerToComponents) {

        int rank = 1;
        List<TopFrequentErrorsResponse.ErrorInfo> result = new ArrayList<>();

        for (ErrorAggregation agg : errorAggs) {
            // percentage 계산 (소수점 1자리)
            float percentage = (float) (totalErrorCount > 0
                    ? Math.round((agg.count() * 100.0 / totalErrorCount) * 10.0) / 10.0
                    : 0.0);

            // 컴포넌트 매칭
            List<TopFrequentErrorsResponse.ErrorInfo.ComponentInfo> componentInfos =
                    loggerToComponents.getOrDefault(agg.logger(), List.of()).stream()
                            .map(c -> new TopFrequentErrorsResponse.ErrorInfo.ComponentInfo(
                                    c.getId(),
                                    c.getName()))
                            .toList();

            result.add(new TopFrequentErrorsResponse.ErrorInfo(
                    rank++,
                    agg.exceptionType(),
                    agg.message(),
                    agg.count(),
                    percentage,
                    agg.firstOccurrence(),
                    agg.lastOccurrence(),
                    agg.stackTrace(),
                    componentInfos
            ));
        }

        return result;
    }

    //@formatter:off

    /**
     * ErrorSummary 생성
     * 전체 에러 통계와 Top N 비율을 계산하여 Summary 객체 생성
     *
     * @param errorInfos ErrorInfo 리스트
     * @param statistics 에러 통계
     * @return ErrorSummary
     */
    //@formatter:on
    private TopFrequentErrorsResponse.ErrorSummary buildErrorSummary(
            List<TopFrequentErrorsResponse.ErrorInfo> errorInfos,
            ErrorStatistics statistics) {

        // Top N이 차지하는 비율 (소수점 1자리)
        long topNCount = errorInfos.stream()
                .mapToLong(TopFrequentErrorsResponse.ErrorInfo::count)
                .sum();

        float topNPercentage = (float) (statistics.totalErrors() > 0
                ? Math.round((topNCount * 100.0 / statistics.totalErrors()) * 10.0) / 10.0
                : 0.0);

        return new TopFrequentErrorsResponse.ErrorSummary(
                statistics.totalErrors(),
                statistics.uniqueErrorTypes(),
                topNPercentage
        );
    }
}
