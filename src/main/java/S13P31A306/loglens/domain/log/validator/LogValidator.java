package S13P31A306.loglens.domain.log.validator;

import S13P31A306.loglens.domain.log.constants.LogErrorCode;
import S13P31A306.loglens.domain.log.constants.SortDirection;
import S13P31A306.loglens.domain.log.constants.SortField;
import S13P31A306.loglens.domain.log.dto.request.LogSearchRequest;
import S13P31A306.loglens.domain.log.entity.LogLevel;
import S13P31A306.loglens.domain.log.entity.SourceType;
import S13P31A306.loglens.domain.project.service.ProjectService;
import S13P31A306.loglens.domain.project.util.ProjectMembershipHelper;
import S13P31A306.loglens.global.exception.BusinessException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogValidator {

    private static final String LOG_PREFIX = "[LogValidator]";
    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 100;

    private final ProjectMembershipHelper projectMembershipHelper;
    private final ProjectService projectService;

    /**
     * 로그 검색 요청 전체 유효성 검증
     *
     * @param request 로그 검색 요청 객체
     */
    public void validate(LogSearchRequest request) {
        log.debug("{} 로그 검색 요청 검증 시작", LOG_PREFIX);
        validateProjectUuid(request.getProjectUuid());
        validateSize(request.getSize());
        validateTimeRange(request);
        validateLogLevel(request.getLogLevel());
        validateSourceType(request.getSourceType());
        validateSort(request.getSort());
        log.debug("{} 로그 검색 요청 검증 완료", LOG_PREFIX);
    }

    /**
     * 프로젝트 UUID 검증 및 멤버십 확인
     *
     * @param projectUuid 프로젝트 UUID
     */
    private void validateProjectUuid(String projectUuid) {
        log.debug("{} 프로젝트 UUID 검증: {}", LOG_PREFIX, projectUuid);
        if (Objects.isNull(projectUuid) || projectUuid.isBlank()) {
            log.warn("{} 프로젝트 UUID 누락", LOG_PREFIX);
            throw new BusinessException(LogErrorCode.PROJECT_UUID_REQUIRED);
        }

        // UUID로 projectId를 조회하고 멤버십 검증
        Integer projectId = projectService.getProjectIdByUuid(projectUuid);
        projectMembershipHelper.validateProjectMembership(projectId);

        log.debug("{} 프로젝트 UUID 검증 완료", LOG_PREFIX);
    }

    /**
     * 페이지 크기 검증 (1~100)
     *
     * @param size 페이지 크기
     */
    private void validateSize(Integer size) {
        log.debug("{} 페이지 크기 검증: {}", LOG_PREFIX, size);

        // null check: size가 null이면 기본값(100)이 사용되므로 검증 통과
        if (Objects.isNull(size)) {
            log.debug("{} 페이지 크기가 null, 기본값 사용 예정", LOG_PREFIX);
            return;
        }

        if (size < MIN_SIZE || size > MAX_SIZE) {
            log.warn("{} 페이지 크기 유효성 실패: {}", LOG_PREFIX, size);
            throw new BusinessException(LogErrorCode.INVALID_SIZE);
        }
        log.debug("{} 페이지 크기 검증 완료", LOG_PREFIX);
    }

    /**
     * 시간 범위 검증 (시작 시간 < 종료 시간)
     *
     * @param request 로그 검색 요청 객체
     */
    private void validateTimeRange(LogSearchRequest request) {
        log.debug("{} 시간 범위 검증: startTime={}, endTime={}", LOG_PREFIX,
                request.getStartTime(), request.getEndTime());
        if (Objects.nonNull(request.getStartTime()) && Objects.nonNull(request.getEndTime())) {
            if (request.getStartTime().isAfter(request.getEndTime())) {
                log.warn("{} 시간 범위 유효성 실패: 시작 시간이 종료 시간보다 늦음", LOG_PREFIX);
                throw new BusinessException(LogErrorCode.INVALID_TIME_RANGE);
            }
        }
        log.debug("{} 시간 범위 검증 완료", LOG_PREFIX);
    }

    /**
     * 로그 레벨 검증
     *
     * @param logLevels 로그 레벨 목록
     */
    private void validateLogLevel(List<String> logLevels) {
        if (Objects.isNull(logLevels) || logLevels.isEmpty()) {
            log.debug("{} 로그 레벨 필터 없음, 검증 건너뜀", LOG_PREFIX);
            return;
        }

        log.debug("{} 로그 레벨 검증: {}", LOG_PREFIX, logLevels);
//        for (String level : logLevels) {
//            boolean isValid = Arrays.stream(LogLevel.values())
//                    .anyMatch(e -> e.name().equalsIgnoreCase(level));
//            if (!isValid) {
//                log.warn("{} 유효하지 않은 로그 레벨: {}", LOG_PREFIX, level);
//                throw new BusinessException(LogErrorCode.INVALID_LOG_LEVEL);
//            }
//        }
        log.debug("{} 로그 레벨 검증 완료", LOG_PREFIX);
    }

    /**
     * 소스 타입 검증
     *
     * @param sourceTypes 소스 타입 목록
     */
    private void validateSourceType(List<String> sourceTypes) {
        if (Objects.isNull(sourceTypes) || sourceTypes.isEmpty()) {
            log.debug("{} 소스 타입 필터 없음, 검증 건너뜀", LOG_PREFIX);
            return;
        }

        log.debug("{} 소스 타입 검증: {}", LOG_PREFIX, sourceTypes);
        for (String type : sourceTypes) {
            boolean isValid = Arrays.stream(SourceType.values())
                    .anyMatch(e -> e.name().equalsIgnoreCase(type));
            if (!isValid) {
                log.warn("{} 유효하지 않은 소스 타입: {}", LOG_PREFIX, type);
                throw new BusinessException(LogErrorCode.INVALID_SOURCE_TYPE);
            }
        }
        log.debug("{} 소스 타입 검증 완료", LOG_PREFIX);
    }

    /**
     * 정렬 옵션 검증 (TIMESTAMP,ASC 또는 TIMESTAMP,DESC만 허용)
     *
     * @param sort 정렬 옵션 (형식: "필드,방향")
     */
    private void validateSort(String sort) {
        log.debug("{} 정렬 옵션 검증: {}", LOG_PREFIX, sort);
        String[] sortParams = sort.split(",");
        if (sortParams.length != 2) {
            log.warn("{} 유효하지 않은 정렬 옵션: {}", LOG_PREFIX, sort);
            throw new BusinessException(LogErrorCode.INVALID_SORT);
        }

        SortField sortField = SortField.fromString(sortParams[0]);
        SortDirection sortDirection = SortDirection.fromString(sortParams[1]);

        if (Objects.isNull(sortField) || Objects.isNull(sortDirection)) {
            log.warn("{} 유효하지 않은 정렬 옵션: {}", LOG_PREFIX, sort);
            throw new BusinessException(LogErrorCode.INVALID_SORT);
        }

        log.debug("{} 정렬 옵션 검증 완료", LOG_PREFIX);
    }
}
