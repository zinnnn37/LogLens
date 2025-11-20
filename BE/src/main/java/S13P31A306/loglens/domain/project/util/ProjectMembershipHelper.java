package S13P31A306.loglens.domain.project.util;

import S13P31A306.loglens.domain.auth.util.AuthenticationHelper;
import S13P31A306.loglens.domain.project.constants.ProjectErrorCode;
import S13P31A306.loglens.domain.project.repository.ProjectMemberRepository;
import S13P31A306.loglens.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 프로젝트 멤버십 검증 헬퍼 클래스 사용자가 특정 프로젝트의 멤버인지 확인하는 기능 제공
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectMembershipHelper {

    private static final String LOG_PREFIX = "[ProjectMembershipHelper]";

    private final ProjectMemberRepository projectMemberRepository;
    private final AuthenticationHelper authenticationHelper;

    /**
     * 현재 인증된 사용자가 특정 프로젝트의 멤버인지 확인
     *
     * @param projectId 프로젝트 ID
     * @return true: 멤버, false: 비멤버
     */
    public boolean isProjectMember(Integer projectId) {
        Integer currentUserId = authenticationHelper.getCurrentUserId();
        log.debug("{} 프로젝트 멤버십 확인: projectId={}, userId={}",
                LOG_PREFIX, projectId, currentUserId);

        boolean isMember = projectMemberRepository.existsByProjectIdAndUserId(projectId, currentUserId);
        log.debug("{} 프로젝트 멤버십 확인 결과: {}", LOG_PREFIX, isMember);

        return isMember;
    }

    /**
     * 특정 사용자가 특정 프로젝트의 멤버인지 확인
     *
     * @param projectId 프로젝트 ID
     * @param userId    사용자 ID
     * @return true: 멤버, false: 비멤버
     */
    public boolean isProjectMember(Integer projectId, Integer userId) {
        log.debug("{} 프로젝트 멤버십 확인: projectId={}, userId={}",
                LOG_PREFIX, projectId, userId);

        boolean isMember = projectMemberRepository.existsByProjectIdAndUserId(projectId, userId);
        log.debug("{} 프로젝트 멤버십 확인 결과: {}", LOG_PREFIX, isMember);

        return isMember;
    }

    /**
     * 현재 인증된 사용자가 특정 프로젝트의 멤버인지 검증 멤버가 아니면 예외 발생
     *
     * @param projectId 프로젝트 ID
     * @throws BusinessException 멤버가 아닌 경우
     */
    public void validateProjectMembership(Integer projectId) {
        Integer currentUserId = authenticationHelper.getCurrentUserId();
        log.debug("{} 프로젝트 멤버십 검증: projectId={}, userId={}",
                LOG_PREFIX, projectId, currentUserId);

        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, currentUserId)) {
            log.warn("{} 프로젝트 접근 권한 없음: projectId={}, userId={}",
                    LOG_PREFIX, projectId, currentUserId);
            throw new BusinessException(ProjectErrorCode.ACCESS_FORBIDDEN);
        }

        log.debug("{} 프로젝트 멤버십 검증 성공", LOG_PREFIX);
    }

    /**
     * 특정 사용자가 특정 프로젝트의 멤버인지 검증 멤버가 아니면 예외 발생
     *
     * @param projectId 프로젝트 ID
     * @param userId    사용자 ID
     * @throws BusinessException 멤버가 아닌 경우
     */
    public void validateProjectMembership(Integer projectId, Integer userId) {
        log.debug("{} 프로젝트 멤버십 검증: projectId={}, userId={}",
                LOG_PREFIX, projectId, userId);

        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            log.warn("{} 프로젝트 접근 권한 없음: projectId={}, userId={}",
                    LOG_PREFIX, projectId, userId);
            throw new BusinessException(ProjectErrorCode.ACCESS_FORBIDDEN);
        }

        log.debug("{} 프로젝트 멤버십 검증 성공", LOG_PREFIX);
    }
}
