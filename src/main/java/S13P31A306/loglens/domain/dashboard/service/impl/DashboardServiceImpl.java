package S13P31A306.loglens.domain.dashboard.service.impl;

import S13P31A306.loglens.domain.auth.util.AuthenticationHelper;
import S13P31A306.loglens.domain.dashboard.dto.response.DashboardOverviewResponse;
import S13P31A306.loglens.domain.dashboard.service.DashboardService;
import S13P31A306.loglens.domain.project.entity.Project;
import S13P31A306.loglens.domain.project.repository.ProjectMemberRepository;
import S13P31A306.loglens.domain.project.repository.ProjectRepository;
import S13P31A306.loglens.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static S13P31A306.loglens.domain.dashboard.constants.DashboardErrorCode.ACCESS_DENIED;
import static S13P31A306.loglens.domain.dashboard.constants.DashboardErrorCode.PROJECT_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private static final String LOG_PREFIX = "[DashboardService]";

    private final OpenSearchClient openSearchClient;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final AuthenticationHelper authHelper;

    @Override
    public DashboardOverviewResponse getStatisticsOverview(int projectId, String startTime, String endTime) {
        log.info("{} 대시보드 통계 개요 조회 시도", LOG_PREFIX);

        // 유저 권한 검증
        Integer userId = authHelper.getCurrentUserId();
        validateProjectAccess(userId, projectId);



        return null;
    }

    private void validateProjectAccess(int projectId, int userId) {
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            log.warn("{} 프로젝트 접근 권한 없음: projectId={}", LOG_PREFIX, projectId);
            throw new BusinessException(ACCESS_DENIED);
        }
    }

    private Project findProjectById(int projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> {
                    log.warn("{} 프로젝트를 찾을 수 없음", LOG_PREFIX);
                    return new BusinessException(PROJECT_NOT_FOUND);
                });
    }

}
