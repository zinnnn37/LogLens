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

        int userId = authHelper.getCurrentUserId();


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
}
