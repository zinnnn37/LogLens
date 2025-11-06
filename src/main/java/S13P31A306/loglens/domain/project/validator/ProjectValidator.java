package S13P31A306.loglens.domain.project.validator;

import static S13P31A306.loglens.domain.project.constants.ProjectErrorCode.ACCESS_FORBIDDEN;
import static S13P31A306.loglens.domain.project.constants.ProjectErrorCode.CANNOT_DELETE_SELF;
import static S13P31A306.loglens.domain.project.constants.ProjectErrorCode.MEMBER_EXISTS;
import static S13P31A306.loglens.domain.project.constants.ProjectErrorCode.MEMBER_NOT_FOUND;
import static S13P31A306.loglens.domain.project.constants.ProjectErrorCode.PROJECT_NOT_FOUND;
import static S13P31A306.loglens.domain.project.constants.ProjectErrorCode.USER_NOT_FOUND;

import S13P31A306.loglens.domain.auth.entity.User;
import S13P31A306.loglens.domain.auth.respository.UserRepository;
import S13P31A306.loglens.domain.auth.util.AuthenticationHelper;
import S13P31A306.loglens.domain.project.entity.Project;
import S13P31A306.loglens.domain.project.repository.ProjectMemberRepository;
import S13P31A306.loglens.domain.project.repository.ProjectRepository;
import S13P31A306.loglens.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

//@formatter:off
/**
 * 프로젝트 관련 검증 로직을 담당하는 Validator 클래스
 * 프로젝트, 사용자, 멤버십 등의 존재 여부 및 권한을 검증
 */
//@formatter:on
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectValidator {

    private static final String LOG_PREFIX = "[ProjectValidator]";

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final AuthenticationHelper authHelper;

    /**
     * 프로젝트 UUID로 프로젝트 존재 여부 검증
     *
     * @param projectUuid 프로젝트 UUID
     * @return Project 조회된 프로젝트 엔티티
     * @throws BusinessException 프로젝트가 존재하지 않는 경우
     */
    public Project validateProjectExists(String projectUuid) {
        log.debug("{} 프로젝트 존재 검증: uuid={}", LOG_PREFIX, projectUuid);
        return projectRepository.findByProjectUuid(projectUuid)
                .orElseThrow(() -> {
                    log.warn("{} 프로젝트 없음: uuid={}", LOG_PREFIX, projectUuid);
                    return new BusinessException(PROJECT_NOT_FOUND);
                });
    }

    /**
     * 현재 사용자의 프로젝트 접근 권한 검증
     *
     * @param projectId 프로젝트 ID
     * @throws BusinessException 프로젝트 멤버가 아닌 경우
     */
    public void validateProjectAccess(Integer projectId) {
        Integer userId = authHelper.getCurrentUserId();
        log.debug("{} 접근 권한 검증: projectId={}, userId={}", LOG_PREFIX, projectId, userId);
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            log.warn("{} 접근 권한 없음: projectId={}, userId={}", LOG_PREFIX, projectId, userId);
            throw new BusinessException(ACCESS_FORBIDDEN);
        }
    }

    /**
     * 사용자 ID로 사용자 존재 여부 검증
     *
     * @param userId 사용자 ID
     * @return User 조회된 사용자 엔티티
     * @throws BusinessException 사용자가 존재하지 않는 경우
     */
    public User validateUserExists(Integer userId) {
        log.debug("{} 사용자 존재 검증: userId={}", LOG_PREFIX, userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("{} 사용자 없음: userId={}", LOG_PREFIX, userId);
                    return new BusinessException(USER_NOT_FOUND);
                });
    }

    /**
     * 프로젝트에 멤버가 존재하지 않음을 검증 (중복 초대 방지)
     *
     * @param projectId 프로젝트 ID
     * @param userId    사용자 ID
     * @throws BusinessException 이미 프로젝트 멤버인 경우
     */
    public void validateMemberNotExists(Integer projectId, Integer userId) {
        log.debug("{} 멤버 중복 검증: projectId={}, userId={}", LOG_PREFIX, projectId, userId);
        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            log.warn("{} 이미 존재하는 멤버: projectId={}, userId={}", LOG_PREFIX, projectId, userId);
            throw new BusinessException(MEMBER_EXISTS);
        }
    }

    /**
     * 프로젝트에 멤버가 존재함을 검증
     *
     * @param projectId 프로젝트 ID
     * @param userId    사용자 ID
     * @throws BusinessException 프로젝트에 멤버가 존재하지 않는 경우
     */
    public void validateMemberExists(Integer projectId, Integer userId) {
        log.debug("{} 멤버 존재 검증: projectId={}, userId={}", LOG_PREFIX, projectId, userId);
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            log.warn("{} 멤버 없음: projectId={}, userId={}", LOG_PREFIX, projectId, userId);
            throw new BusinessException(MEMBER_NOT_FOUND);
        }
    }

    /**
     * 현재 인증된 사용자가 자기 자신을 삭제하려는 시도인지 검증
     *
     * @param targetId 삭제 대상 사용자 ID
     * @throws BusinessException 자기 자신을 삭제하려는 경우
     */
    public void validateCurrentUserNotSelfDeletion(Integer targetId) {
        Integer userId = authHelper.getCurrentUserId();

        log.debug("{} 현재 사용자 자기 삭제 방지 검증: currentUserId={}, targetId={}", LOG_PREFIX, userId, targetId);

        if (userId.equals(targetId)) {
            log.warn("{} 자기 자신 삭제 시도: userId={}", LOG_PREFIX, userId);
            throw new BusinessException(CANNOT_DELETE_SELF);
        }
    }

    /**
     * 특정 사용자가 프로젝트 멤버인지 확인
     *
     * @param projectId 프로젝트 ID
     * @param userId    사용자 ID
     * @return true: 멤버, false: 비멤버
     */
    public boolean isProjectMember(Integer projectId, Integer userId) {
        return projectMemberRepository.existsByProjectIdAndUserId(projectId, userId);
    }
}
