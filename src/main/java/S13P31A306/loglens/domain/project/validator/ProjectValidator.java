package S13P31A306.loglens.domain.project.validator;

import S13P31A306.loglens.domain.project.constants.ProjectErrorCode;
import S13P31A306.loglens.domain.project.entity.Project;
import S13P31A306.loglens.domain.project.repository.ProjectMemberRepository;
import S13P31A306.loglens.domain.project.repository.ProjectRepository;
import S13P31A306.loglens.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static S13P31A306.loglens.domain.project.constants.ProjectErrorCode.MEMBER_NOT_FOUND;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectValidator {

    private static final String LOG_PREFIX = "[ProjectValidator]";

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    /**
     * 프로젝트 UUID로 프로젝트 존재 여부 검증
     *
     * @param projectUuid 프로젝트 UUID
     * @return Project 조회된 프로젝트 엔티티
     * @throws BusinessException 프로젝트가 존재하지 않는 경우
     */
    public Project validateProjectExists(final String projectUuid) {
        log.debug("{} 프로젝트 존재 여부 검증: projectUuid={}", LOG_PREFIX, projectUuid);
        return projectRepository.findByProjectUuid(projectUuid)
                .orElseThrow(() -> new BusinessException(
                        ProjectErrorCode.PROJECT_NOT_FOUND
                ));
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

    public void validateProjectAccess(final Project project, final String email) {
        log.debug("{} 프로젝트 접근 권한 검증: projectId={}, projectName={}, user={}",
                LOG_PREFIX, project.getId(), project.getProjectName(), email);

        if (!hasProjectAccess(project, email)) {
            log.warn("{} 프로젝트 접근 권한 없음: projectId={}, projectName={}, user={}",
                    LOG_PREFIX, project.getId(), project.getProjectName(), email);
            throw new BusinessException(
                    ProjectErrorCode.ACCESS_FORBIDDEN
            );
        }
    }

    private boolean hasProjectAccess(final Project project, final String email) {
        return project.getMembers().stream()
                .anyMatch(member ->
                        member.getUser().getEmail().equals(email)
                );
    }
}
