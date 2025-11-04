package S13P31A306.loglens.domain.project.validator;

import S13P31A306.loglens.domain.project.constants.ProjectErrorCode;
import S13P31A306.loglens.domain.project.entity.Project;
import S13P31A306.loglens.domain.project.repository.ProjectRepository;
import S13P31A306.loglens.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectValidator {
    private static final String LOG_PREFIX = "[ProjectValidator]";
    private final ProjectRepository projectRepository;

    public Project validateProjectExists(final String projectUuid) {
        log.debug("{} 프로젝트 존재 여부 검증: projectUuid={}", LOG_PREFIX, projectUuid);
        return projectRepository.findByProjectUuid(projectUuid)
                .orElseThrow(() -> new BusinessException(
                        ProjectErrorCode.PROJECT_NOT_FOUND
                ));
    }

    public void validateProjectAccess(final Project project, final String username) {
        log.debug("{} 프로젝트 접근 권한 검증: projectId={}, projectName={}, user={}",
                LOG_PREFIX, project.getId(), project.getProjectName(), username);

        if (!hasProjectAccess(project, username)) {
            log.warn("{} 프로젝트 접근 권한 없음: projectId={}, projectName={}, user={}",
                    LOG_PREFIX, project.getId(), project.getProjectName(), username);
            throw new BusinessException(
                    ProjectErrorCode.ACCESS_FORBIDDEN
            );
        }
    }

    private boolean hasProjectAccess(final Project project, final String username) {
        return project.getMembers().stream()
                .anyMatch(member ->
                        member.getUser().getName().equals(username)
                );
    }
}
