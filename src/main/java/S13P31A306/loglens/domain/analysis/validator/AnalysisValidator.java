package S13P31A306.loglens.domain.analysis.validator;

import S13P31A306.loglens.domain.analysis.constants.AnalysisErrorCode;
import S13P31A306.loglens.domain.analysis.dto.request.ErrorAnalysisRequest;
import S13P31A306.loglens.domain.analysis.dto.request.ProjectAnalysisRequest;
import S13P31A306.loglens.domain.log.dto.response.LogDetailResponse;
import S13P31A306.loglens.domain.log.service.LogService;
import S13P31A306.loglens.domain.project.entity.Project;
import S13P31A306.loglens.domain.project.validator.ProjectValidator;
import S13P31A306.loglens.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 분석 요청 유효성 검증
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisValidator {

    private final ProjectValidator projectValidator;
    private final LogService logService;

    private static final long MAX_TIME_RANGE_DAYS = 365; // 최대 1년
    private static final int MAX_RELATED_LOGS = 100; // 관련 로그 최대 개수

    /**
     * 프로젝트 분석 요청 검증
     */
    public void validateProjectAnalysisRequest(
            String projectUuid,
            ProjectAnalysisRequest request,
            UserDetails userDetails
    ) {
        log.debug("Validating project analysis request: projectUuid={}", projectUuid);

        // 1. 프로젝트 접근 권한 검증
        Project project = projectValidator.validateProjectExists(projectUuid);
        projectValidator.validateProjectAccess(project.getId());

        // 2. 시간 범위 검증
        if (request.getStartTime() != null && request.getEndTime() != null) {
            validateTimeRange(request.getStartTime(), request.getEndTime());
        }

        // 3. 종료 시간이 미래가 아닌지 확인
        if (request.getEndTime() != null && request.getEndTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(AnalysisErrorCode.INVALID_TIME_RANGE, "종료 시간은 현재 시간 이후일 수 없습니다");
        }

        // 4. 문서 형식 검증
        if (request.getFormat() == null) {
            throw new BusinessException(AnalysisErrorCode.INVALID_FORMAT, "문서 형식을 지정해주세요");
        }

        log.info("Project analysis request validation passed: projectUuid={}", projectUuid);
    }

    /**
     * 에러 분석 요청 검증
     */
    public void validateErrorAnalysisRequest(
            Long logId,
            ErrorAnalysisRequest request,
            UserDetails userDetails
    ) {
        log.debug("Validating error analysis request: logId={}", logId);

        // 1. 프로젝트 접근 권한 검증
        Project project = projectValidator.validateProjectExists(request.getProjectUuid());
        projectValidator.validateProjectAccess(project.getId());

        // 2. 로그 존재 확인
        try {
            LogDetailResponse log = logService.getLogDetail(logId, request.getProjectUuid());
            if (log == null) {
                throw new BusinessException(AnalysisErrorCode.LOG_NOT_FOUND, "로그를 찾을 수 없습니다: " + logId);
            }
        } catch (Exception e) {
            log.error("Failed to retrieve log: logId={}, error={}", logId, e.getMessage());
            throw new BusinessException(AnalysisErrorCode.LOG_NOT_FOUND, "로그 조회 실패: " + logId);
        }

        // 3. 문서 형식 검증
        if (request.getFormat() == null) {
            throw new BusinessException(AnalysisErrorCode.INVALID_FORMAT, "문서 형식을 지정해주세요");
        }

        // 4. 옵션 검증
        if (request.getOptions() != null) {
            int maxRelatedLogs = request.getOptions().getMaxRelatedLogs();
            if (maxRelatedLogs < 1 || maxRelatedLogs > MAX_RELATED_LOGS) {
                throw new BusinessException(
                        AnalysisErrorCode.INVALID_OPTIONS,
                        String.format("관련 로그 개수는 1~%d 범위 내여야 합니다", MAX_RELATED_LOGS)
                );
            }
        }

        log.info("Error analysis request validation passed: logId={}", logId);
    }

    /**
     * 시간 범위 유효성 검증
     */
    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        // 1. 시작 시간이 종료 시간보다 늦지 않은지 확인
        if (startTime.isAfter(endTime)) {
            throw new BusinessException(
                    AnalysisErrorCode.INVALID_TIME_RANGE,
                    "시작 시간이 종료 시간보다 늦을 수 없습니다"
            );
        }

        // 2. 시간 범위가 너무 길지 않은지 확인 (최대 1년)
        long daysDiff = ChronoUnit.DAYS.between(startTime, endTime);
        if (daysDiff > MAX_TIME_RANGE_DAYS) {
            throw new BusinessException(
                    AnalysisErrorCode.INVALID_TIME_RANGE,
                    String.format("시간 범위는 최대 %d일까지 가능합니다", MAX_TIME_RANGE_DAYS)
            );
        }

        // 3. 시작 시간이 미래가 아닌지 확인
        if (startTime.isAfter(LocalDateTime.now())) {
            throw new BusinessException(
                    AnalysisErrorCode.INVALID_TIME_RANGE,
                    "시작 시간은 현재 시간 이후일 수 없습니다"
            );
        }

        log.debug("Time range validation passed: {} ~ {}", startTime, endTime);
    }
}
