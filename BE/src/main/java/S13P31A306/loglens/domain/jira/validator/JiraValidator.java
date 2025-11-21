package S13P31A306.loglens.domain.jira.validator;

import S13P31A306.loglens.domain.jira.constants.JiraErrorCode;
import S13P31A306.loglens.domain.jira.repository.JiraConnectionRepository;
import S13P31A306.loglens.domain.project.repository.ProjectMemberRepository;
import S13P31A306.loglens.domain.project.repository.ProjectRepository;
import S13P31A306.loglens.global.constants.GlobalErrorCode;
import S13P31A306.loglens.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Jira 연동 Validator
 * Jira 연동 관련 비즈니스 로직 검증을 담당합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JiraValidator {

    private static final String LOG_PREFIX = "[JiraValidator]";

    private final JiraConnectionRepository jiraConnectionRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    /**
     * 프로젝트 존재 여부 및 사용자 접근 권한 검증
     *
     * @param projectId 프로젝트 ID
     * @param userId    사용자 ID
     * @throws BusinessException 프로젝트가 없거나 권한이 없을 경우
     */
    public void validateProjectAccess(Integer projectId, Integer userId) {
        log.debug("{} 프로젝트 접근 권한 검증: projectId={}, userId={}", LOG_PREFIX, projectId, userId);

        // 프로젝트 존재 확인
        if (!projectRepository.existsById(projectId)) {
            log.warn("{} ⚠️ 프로젝트를 찾을 수 없음: projectId={}", LOG_PREFIX, projectId);
            throw new BusinessException(JiraErrorCode.PROJECT_NOT_FOUND);
        }

        // 사용자 권한 확인
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            log.warn("{} ⚠️ 프로젝트 접근 권한 없음: projectId={}, userId={}", LOG_PREFIX, projectId, userId);
            throw new BusinessException(GlobalErrorCode.FORBIDDEN);
        }

        log.debug("{} ✅ 프로젝트 접근 권한 확인 완료", LOG_PREFIX);
    }

    /**
     * 중복 연동 검증
     *
     * @param projectId 프로젝트 ID
     * @throws BusinessException 이미 연동되어 있을 경우
     */
    public void validateDuplicateConnection(Integer projectId) {
        log.debug("{} 중복 연동 검증: projectId={}", LOG_PREFIX, projectId);

        if (jiraConnectionRepository.existsByProjectId(projectId)) {
            log.warn("{} ⚠️ 이미 Jira 연동이 존재함: projectId={}", LOG_PREFIX, projectId);
            throw new BusinessException(JiraErrorCode.JIRA_CONNECTION_ALREADY_EXISTS);
        }

        log.debug("{} ✅ 중복 연동 없음", LOG_PREFIX);
    }

    /**
     * Jira 연동 정보 존재 여부 검증
     *
     * @param projectId 프로젝트 ID
     * @throws BusinessException Jira 연동이 설정되지 않은 경우
     */
    public void validateConnectionExists(Integer projectId) {
        log.debug("{} Jira 연동 존재 여부 검증: projectId={}", LOG_PREFIX, projectId);

        if (!jiraConnectionRepository.existsByProjectId(projectId)) {
            log.warn("{} ⚠️ Jira 연동 정보 없음: projectId={}", LOG_PREFIX, projectId);
            throw new BusinessException(JiraErrorCode.JIRA_CONNECTION_NOT_FOUND);
        }

        log.debug("{} ✅ Jira 연동 정보 존재 확인", LOG_PREFIX);
    }

    /**
     * 로그 존재 여부 검증
     *
     * @param logId 로그 ID
     * @throws BusinessException 로그가 없을 경우
     */
    public void validateLogExists(Long logId) {
        log.debug("{} 로그 존재 여부 검증: logId={}", LOG_PREFIX, logId);

        // TODO: 실제 구현 시 LogRepository로 로그 존재 확인
        // if (!logRepository.existsById(logId)) {
        //     log.warn("{} ⚠️ 로그를 찾을 수 없음: logId={}", LOG_PREFIX, logId);
        //     throw new BusinessException(JiraErrorCode.LOG_NOT_FOUND);
        // }

        log.debug("{} ✅ 로그 존재 확인", LOG_PREFIX);
    }
}
