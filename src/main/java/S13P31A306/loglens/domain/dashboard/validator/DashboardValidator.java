package S13P31A306.loglens.domain.dashboard.validator;

import S13P31A306.loglens.domain.auth.util.AuthenticationHelper;
import S13P31A306.loglens.domain.auth.validator.AuthValidator;
import S13P31A306.loglens.domain.component.repository.ComponentRepository;
import S13P31A306.loglens.domain.project.entity.Project;
import S13P31A306.loglens.domain.project.validator.ProjectValidator;
import S13P31A306.loglens.global.constants.GlobalErrorCode;
import S13P31A306.loglens.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import static S13P31A306.loglens.domain.dashboard.constants.DashboardErrorCode.*;


@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardValidator {

    private static final String LOG_PREFIX = "[DashboardValidator]";

    private final ProjectValidator projectValidator;
    private final AuthValidator authValidator;
    private final AuthenticationHelper authHelper;
    private final ComponentRepository componentRepository;

    /**
     * 프로젝트 접근 권한 검증
     *
     * @param projectUuid 프로젝트 UUID
     * @return 프로젝트 ID
     * @throws BusinessException 프로젝트가 존재하지 않거나 접근 권한 없음
     */
    public Integer validateProjectAccess(String projectUuid) {
        log.debug("{} 프로젝트 접근 권한 확인: projectUuid={}", LOG_PREFIX, projectUuid);

        // 프로젝트 존재 검증
        Project project = projectValidator.validateProjectExists(projectUuid);

        // 프로젝트 멤버 여부 검증
        Integer userId = authHelper.getCurrentUserId();
        projectValidator.validateMemberExists(project.getId(), userId);

        return project.getId();
    }

    /**
     * 프로젝트 접근 권한 검증
     *
     * @param projectId 프로젝트 UUID
     * @return 프로젝트 ID
     * @throws BusinessException 프로젝트가 존재하지 않거나 접근 권한 없음
     */
    public Integer validateProjectAccess(Integer projectId) {
        log.debug("{} 프로젝트 접근 권한 확인: projectId={}", LOG_PREFIX, projectId);

        // 프로젝트 존재 검증
        Project project = projectValidator.validateProjectExists(projectId);

        // 프로젝트 멤버 여부 검증
        Integer userId = authHelper.getCurrentUserId();
        projectValidator.validateMemberExists(project.getId(), userId);

        return project.getId();
    }

    /**
     * 프로젝트 존재 여부 및 접근 권한 검증
     *
     * @param projectUuid 프로젝트 UUID
     * @param userDetails 인증된 사용자 정보
     * @return 검증된 프로젝트 ID
     * @throws BusinessException 프로젝트가 존재하지 않거나 접근 권한이 없는 경우
     */
    public Integer validateProjectAccess(final String projectUuid, final UserDetails userDetails) {
        log.debug("{} 대시보드 프로젝트 접근 검증 시작: projectUuid={}", LOG_PREFIX, projectUuid);

        String email = authValidator.validateAndGetEmail(userDetails);
        Project project = projectValidator.validateProjectExists(projectUuid);
        projectValidator.validateProjectAccess(project, email);

        log.debug("{} 대시보드 프로젝트 접근 검증 완료: projectId={}, projectName={}, user={}",
                LOG_PREFIX, project.getId(), project.getProjectName(), email);

        return project.getId();
    }

    public void validateComponentAccess(Integer componentId, Integer projectId) {
        S13P31A306.loglens.domain.component.entity.Component component = componentRepository.findById(componentId)
                .orElseThrow(() -> new BusinessException(GlobalErrorCode.NOT_FOUND));

        if (!component.getProjectId().equals(projectId)) {
            log.warn("{} 컴포넌트가 해당 프로젝트에 속하지 않음: componentId={}, projectId={}",
                    LOG_PREFIX, componentId, projectId);
            throw new BusinessException(GlobalErrorCode.FORBIDDEN);
        }

    }

    /**
     * limit 값 검증 (1~50 범위)
     *
     * @param limit 조회할 에러 개수
     * @throws BusinessException limit이 1~50 범위를 벗어나는 경우 (INVALID_LIMIT)
     */
    public void validateLimit(Integer limit) {
        if (limit < 1 || limit > 50) {
            log.warn("{} 잘못된 limit 값: {}", LOG_PREFIX, limit);
            throw new BusinessException(INVALID_LIMIT);
        }
    }

    /**
     * 시간 문자열을 파싱하거나 기본값을 반환
     *
     * @param time 파싱할 시간 문자열 (ISO 8601 형식, null 또는 빈 문자열 가능)
     * @return 파싱된 LocalDateTime 또는 기본값
     * @throws BusinessException timeStr이 유효하지 않은 ISO 8601 형식인 경우 (INVALID_TIME_FORMAT)
     */
    public LocalDateTime validateAndParseTime(String time) {
        log.info("{} 시간 문자열 파싱 시작: time={}", LOG_PREFIX, time);

        if (Objects.isNull(time)) {
            return null;
        }

        try {
            log.info("{} 시간 문자열 파싱 성공: time={}", LOG_PREFIX, time);
            return LocalDateTime.parse(time, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            log.warn("{} 시간 파싱 실패: {}", LOG_PREFIX, time);
            throw new BusinessException(INVALID_TIME_FORMAT);
        }
    }

    /**
     * 시간 범위 유효성 검증
     *
     * @param start 조회 시작 시간
     * @param end 조회 종료 시간
     * @throws BusinessException 시작 시간이 종료 시간보다 늦은 경우 (INVALID_TIME_RANGE)
     * @throws BusinessException 조회 기간이 90일을 초과하는 경우 (PERIOD_EXCEEDS_LIMIT)
     */
    public void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        log.info("{} 시간 범위 유효성 검증: start={}, end={}", LOG_PREFIX, start, end);
        if (start.isAfter(end)) {
            log.warn("{} 시작 시간은 종료 시간보다 앞서야 합니다: start={}, end={}", LOG_PREFIX, start, end);
            throw new BusinessException(INVALID_TIME_RANGE);
        }
        if (ChronoUnit.DAYS.between(start, end) > 90) {
            log.warn("{} 최대 조회 기간을 초과합니다(최대 90일): {}일",
                    LOG_PREFIX, ChronoUnit.DAYS.between(start, end));
            throw new BusinessException(INVALID_TIME_RANGE);
        }
    }

    /**
     * API 통계 조회 요청 파라미터 전체 검증 및 기본값 설정
     * 다음 항목을 순서대로 검증:
     * 1. 프로젝트 존재 여부 및 접근 권한</li>
     * 2. limit 범위 (1~50)</li>
     * 3. 시간 형식 (ISO 8601)</li>
     * 4. 시간 범위 (startTime < endTime)</li>
     *
     * 시간 파라미터 기본값:
     * 1. startTime만 있는 경우: endTime = startTime + 1일</li>
     * 2. endTime만 있는 경우: startTime = endTime - 1일</li>
     * 3. 둘 다 없는 경우: startTime = 현재 - 1일, endTime = 현재</li>
     *
     * @param projectId 프로젝트 ID
     * @param limit 조회할 API 개수 (nullable, 기본값 10)
     * @param startTimeStr 조회 시작 시간 (nullable)
     * @param endTimeStr 조회 종료 시간 (nullable)
     * @return 검증 및 기본값이 설정된 [startTime, endTime] 배열
     * @throws BusinessException 검증 실패시 적절한 에러 코드와 함께 예외 발생
     */
    public LocalDateTime[] validateApiEndpointRequest(Integer projectId, Integer limit, String startTimeStr, String endTimeStr) {
        log.info("{} api 통계 검증 시도: projectId={}, limit={}, start={}, end={}", LOG_PREFIX, projectId, limit, startTimeStr, endTimeStr);

        if (!Objects.isNull(limit)) {
            validateLimit(limit);
        }

        LocalDateTime startTime = validateAndParseTime(startTimeStr);
        LocalDateTime endTime = validateAndParseTime(endTimeStr);
        LocalDateTime now = LocalDateTime.now();

        // 시간 기본값 설정
        if (Objects.isNull(startTime) && Objects.isNull(endTime)) {
            startTime = now.minusDays(1);
            endTime = now;
        } else if (!Objects.isNull(startTime) && Objects.isNull(endTime)) {
            endTime = startTime.plusDays(1);
        } else if (Objects.isNull(startTime) && !Objects.isNull(endTime)) {
            startTime = endTime.minusDays(1);
        }
        validateTimeRange(startTime, endTime);

        log.debug("{} API 통계 조회 요청 검증 완료: start={}, end={}",
                LOG_PREFIX, startTime, endTime);

        return new LocalDateTime[]{startTime, endTime};
    }

}
