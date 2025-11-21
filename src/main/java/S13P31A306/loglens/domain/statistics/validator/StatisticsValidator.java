package S13P31A306.loglens.domain.statistics.validator;

import S13P31A306.loglens.domain.auth.util.AuthenticationHelper;
import S13P31A306.loglens.domain.project.entity.Project;
import S13P31A306.loglens.domain.project.validator.ProjectValidator;
import S13P31A306.loglens.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import static S13P31A306.loglens.domain.statistics.constants.StatisticsConstants.*;
import static S13P31A306.loglens.domain.statistics.constants.StatisticsErrorCode.*;

/**
 * 통계 도메인 검증 로직을 담당하는 Validator 클래스
 * 프로젝트 접근 권한, 시간 범위, 통계 파라미터 등을 검증
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatisticsValidator {

    private static final String LOG_PREFIX = "[StatisticsValidator]";

    private final ProjectValidator projectValidator;
    private final AuthenticationHelper authHelper;

    /**
     * 프로젝트 UUID 검증 및 접근 권한 확인
     *
     * @param projectUuid 프로젝트 UUID
     * @return 검증된 프로젝트 엔티티
     * @throws BusinessException 프로젝트가 존재하지 않거나 접근 권한이 없는 경우
     */
    public Project validateProjectAccess(String projectUuid) {
        log.debug("{} 프로젝트 접근 검증 시작: projectUuid={}", LOG_PREFIX, projectUuid);

        // 1. 현재 사용자 조회
        Integer userId = authHelper.getCurrentUserId();
        log.debug("{} 현재 사용자 ID: {}", LOG_PREFIX, userId);

        // 2. 프로젝트 존재 여부 검증
        Project project = projectValidator.validateProjectExists(projectUuid);
        log.debug("{} 프로젝트 존재 확인: projectId={}, projectName={}",
                LOG_PREFIX, project.getId(), project.getProjectName());

        // 3. 프로젝트 멤버 여부 검증
        projectValidator.validateMemberExists(project.getId(), userId);
        log.debug("{} 프로젝트 멤버 확인 완료", LOG_PREFIX);

        return project;
    }

    /**
     * 시간 범위 유효성 검증
     *
     * @param startTime 시작 시간
     * @param endTime   종료 시간
     * @throws BusinessException 시작 시간이 종료 시간보다 늦거나, 범위가 유효하지 않은 경우
     */
    public void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        log.debug("{} 시간 범위 검증: start={}, end={}", LOG_PREFIX, startTime, endTime);

        if (Objects.isNull(startTime) || Objects.isNull(endTime)) {
            log.warn("{} 시간 범위가 null입니다", LOG_PREFIX);
            throw new BusinessException(INVALID_TIME_RANGE);
        }

        if (startTime.isAfter(endTime)) {
            log.warn("{} 시작 시간이 종료 시간보다 늦습니다: start={}, end={}",
                    LOG_PREFIX, startTime, endTime);
            throw new BusinessException(INVALID_TIME_RANGE);
        }

        if (startTime.isEqual(endTime)) {
            log.warn("{} 시작 시간과 종료 시간이 동일합니다: time={}", LOG_PREFIX, startTime);
            throw new BusinessException(INVALID_TIME_RANGE);
        }

        log.debug("{} 시간 범위 검증 완료", LOG_PREFIX);
    }

    /**
     * 집계 간격(interval) 유효성 검증
     *
     * @param intervalHours 집계 간격 (시간 단위)
     * @throws BusinessException 간격이 유효하지 않은 경우
     */
    public void validateInterval(int intervalHours) {
        log.debug("{} 집계 간격 검증: intervalHours={}", LOG_PREFIX, intervalHours);

        if (intervalHours <= 0) {
            log.warn("{} 집계 간격이 0 이하입니다: {}", LOG_PREFIX, intervalHours);
            throw new BusinessException(INVALID_INTERVAL);
        }

        if (intervalHours > MAX_INTERVAL_HOURS) {
            log.warn("{} 집계 간격이 최대값을 초과합니다: {} > {}",
                    LOG_PREFIX, intervalHours, MAX_INTERVAL_HOURS);
            throw new BusinessException(INVALID_INTERVAL);
        }

        log.debug("{} 집계 간격 검증 완료", LOG_PREFIX);
    }

    /**
     * 조회 기간 제한 검증
     *
     * @param startTime 시작 시간
     * @param endTime   종료 시간
     * @param maxDays   최대 조회 가능 일수
     * @throws BusinessException 조회 기간이 최대값을 초과하는 경우
     */
    public void validatePeriodLimit(LocalDateTime startTime, LocalDateTime endTime, int maxDays) {
        log.debug("{} 조회 기간 제한 검증: start={}, end={}, maxDays={}",
                LOG_PREFIX, startTime, endTime, maxDays);

        long daysBetween = ChronoUnit.DAYS.between(startTime, endTime);

        if (daysBetween > maxDays) {
            log.warn("{} 조회 기간이 최대값을 초과합니다: {}일 > {}일",
                    LOG_PREFIX, daysBetween, maxDays);
            throw new BusinessException(PERIOD_EXCEEDS_LIMIT);
        }

        log.debug("{} 조회 기간 제한 검증 완료: {}일", LOG_PREFIX, daysBetween);
    }

    /**
     * 로그 추이 조회 요청 전체 검증
     *
     * @param projectUuid 프로젝트 UUID
     * @return 검증된 프로젝트 엔티티
     * @throws BusinessException 검증 실패 시
     */
    public Project validateLogTrendRequest(String projectUuid) {
        log.debug("{} 로그 추이 요청 검증 시작: projectUuid={}", LOG_PREFIX, projectUuid);

        // 프로젝트 접근 검증
        Project project = validateProjectAccess(projectUuid);

        log.debug("{} 로그 추이 요청 검증 완료", LOG_PREFIX);
        return project;
    }

    /**
     * Traffic 조회 요청 전체 검증
     *
     * @param projectUuid 프로젝트 UUID
     * @return 검증된 프로젝트 엔티티
     * @throws BusinessException 검증 실패 시
     */
    public Project validateTrafficRequest(String projectUuid) {
        log.debug("{} Traffic 요청 검증 시작: projectUuid={}", LOG_PREFIX, projectUuid);

        // 프로젝트 접근 검증
        Project project = validateProjectAccess(projectUuid);

        log.debug("{} Traffic 요청 검증 완료", LOG_PREFIX);
        return project;
    }

    /**
     * 커스텀 시간 범위 통계 요청 검증 (향후 확장용)
     *
     * @param projectUuid   프로젝트 UUID
     * @param startTime     시작 시간
     * @param endTime       종료 시간
     * @param intervalHours 집계 간격
     * @return 검증된 프로젝트 엔티티
     * @throws BusinessException 검증 실패 시
     */
    public Project validateCustomTimeRangeRequest(
            String projectUuid,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int intervalHours) {

        log.debug("{} 커스텀 시간 범위 요청 검증 시작: projectUuid={}, start={}, end={}, interval={}",
                LOG_PREFIX, projectUuid, startTime, endTime, intervalHours);

        // 1. 프로젝트 접근 검증
        Project project = validateProjectAccess(projectUuid);

        // 2. 시간 범위 검증
        validateTimeRange(startTime, endTime);

        // 3. 집계 간격 검증
        validateInterval(intervalHours);

        // 4. 조회 기간 제한 검증
        validatePeriodLimit(startTime, endTime, MAX_QUERY_DAYS);

        log.debug("{} 커스텀 시간 범위 요청 검증 완료", LOG_PREFIX);
        return project;
    }
}
