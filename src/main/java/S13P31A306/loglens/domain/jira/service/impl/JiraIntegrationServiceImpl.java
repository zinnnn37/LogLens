package S13P31A306.loglens.domain.jira.service.impl;

import S13P31A306.loglens.domain.auth.util.AuthenticationHelper;
import S13P31A306.loglens.domain.jira.client.JiraApiClient;
import S13P31A306.loglens.domain.jira.constants.JiraErrorCode;
import S13P31A306.loglens.domain.jira.dto.request.JiraConnectRequest;
import S13P31A306.loglens.domain.jira.dto.response.JiraConnectResponse;
import S13P31A306.loglens.domain.jira.dto.response.JiraConnectionStatusResponse;
import S13P31A306.loglens.domain.jira.entity.JiraConnection;
import S13P31A306.loglens.domain.jira.mapper.JiraMapper;
import S13P31A306.loglens.domain.jira.repository.JiraConnectionRepository;
import S13P31A306.loglens.domain.jira.service.JiraIntegrationService;
import S13P31A306.loglens.domain.jira.validator.JiraValidator;
import S13P31A306.loglens.domain.project.entity.Project;
import S13P31A306.loglens.domain.project.repository.ProjectRepository;
import S13P31A306.loglens.global.exception.BusinessException;
import S13P31A306.loglens.global.utils.EncryptionUtils;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Jira 연동 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JiraIntegrationServiceImpl implements JiraIntegrationService {

    private static final String LOG_PREFIX = "[JiraIntegration]";

    private final AuthenticationHelper authenticationHelper;
    private final JiraConnectionRepository jiraConnectionRepository;
    private final ProjectRepository projectRepository;
    private final JiraApiClient jiraApiClient;
    private final JiraValidator jiraValidator;
    private final JiraMapper jiraMapper;
    private final EncryptionUtils encryptionUtils;

    //@formatter:off
    /**
     * Jira 연동 설정
     * 외부 API 호출을 트랜잭션 밖에서 수행하여 DB 커넥션 점유 시간 최소화
     *
     * @param request 연동 요청 DTO
     * @return JiraConnectResponse 연동 응답 DTO
     */
    //@formatter:on
    @Override
    public JiraConnectResponse connect(JiraConnectRequest request) {
        // 현재 인증된 사용자 ID 조회
        Integer userId = authenticationHelper.getCurrentUserId();
        log.info("{} 🔗 Jira 연동 설정 시작: projectUuid={}, userId={}", LOG_PREFIX, request.projectUuid(), userId);

        // 1. projectUuid로 Project 조회
        Project project = projectRepository.findByProjectUuid(request.projectUuid())
                .orElseThrow(() -> new BusinessException(JiraErrorCode.PROJECT_NOT_FOUND));
        log.debug("{} ✅ 프로젝트 조회 완료: projectId={}", LOG_PREFIX, project.getId());

        // 2. 프로젝트 존재 여부 및 권한 확인
        jiraValidator.validateProjectAccess(project.getId(), userId);
        log.debug("{} ✅ 프로젝트 접근 권한 확인 완료", LOG_PREFIX);

        // 3. 중복 연동 체크
        jiraValidator.validateDuplicateConnection(project.getId());
        log.debug("{} ✅ 중복 연동 체크 완료", LOG_PREFIX);

        // 4. Jira API 연결 테스트 (트랜잭션 외부에서 실행)
        boolean connected = jiraApiClient.testConnection(
                request.jiraUrl(),
                request.jiraEmail(),
                request.jiraApiToken(),
                request.jiraProjectKey()
        );

        if (!connected) {
            log.warn("{} ⚠️ Jira 연결 테스트 실패: projectUuid={}", LOG_PREFIX, request.projectUuid());
            throw new BusinessException(JiraErrorCode.JIRA_API_CONNECTION_FAILED);
        }
        log.info("{} ✅ Jira 연결 테스트 성공", LOG_PREFIX);

        // 5. API 토큰 암호화 (트랜잭션 외부에서 실행)
        String encryptedToken = encryptionUtils.encrypt(request.jiraApiToken());
        log.debug("{} 💾 API 토큰 암호화 완료", LOG_PREFIX);

        // 6. 연동 정보 저장 (트랜잭션 내에서 DB 작업만 수행)
        JiraConnectResponse response = saveConnectionInTransaction(request, project, encryptedToken);

        log.info("{} 🎉 Jira 연동 설정 완료: projectUuid={}", LOG_PREFIX, request.projectUuid());
        return response;
    }

    /**
     * Jira 연동 정보 저장 (트랜잭션) DB 저장 작업만 트랜잭션으로 처리
     */
    @Transactional
    protected JiraConnectResponse saveConnectionInTransaction(
            JiraConnectRequest request,
            Project project,
            String encryptedToken) {
        JiraConnection connection = jiraMapper.toEntity(request, project.getId(), encryptedToken);
        JiraConnection saved = jiraConnectionRepository.save(connection);
        log.info("{} ✅ Jira 연동 저장 완료: connectionId={}, projectId={}",
                LOG_PREFIX, saved.getId(), saved.getProjectId());

        return jiraMapper.toConnectResponse(saved, request.projectUuid());
    }

    /**
     * Jira 연동 상태 조회 특정 프로젝트의 Jira 연동 상태를 조회합니다.
     *
     * @param projectUuid 프로젝트 UUID
     * @return JiraConnectionStatusResponse 연동 상태 응답 DTO
     */
    @Override
    @Transactional(readOnly = true)
    public JiraConnectionStatusResponse getConnectionStatus(String projectUuid) {
        // 현재 인증된 사용자 ID 조회
        Integer userId = authenticationHelper.getCurrentUserId();
        log.info("{} 🔍 Jira 연동 상태 조회 시작: projectUuid={}, userId={}", LOG_PREFIX, projectUuid, userId);

        // 1. projectUuid로 Project 조회
        Project project = projectRepository.findByProjectUuid(projectUuid)
                .orElseThrow(() -> new BusinessException(JiraErrorCode.PROJECT_NOT_FOUND));
        log.debug("{} ✅ 프로젝트 조회 완료: projectId={}", LOG_PREFIX, project.getId());

        // 2. 프로젝트 존재 여부 및 권한 확인
        jiraValidator.validateProjectAccess(project.getId(), userId);
        log.debug("{} ✅ 프로젝트 접근 권한 확인 완료", LOG_PREFIX);

        // 3. 연동 정보 조회
        Optional<JiraConnection> connection = jiraConnectionRepository.findByProjectId(project.getId());

        if (connection.isPresent()) {
            JiraConnection conn = connection.get();
            log.info("{} ✅ Jira 연동 존재: projectUuid={}, connectionId={}, jiraProjectKey={}",
                    LOG_PREFIX, projectUuid, conn.getId(), conn.getJiraProjectKey());

            return new JiraConnectionStatusResponse(
                    true,
                    projectUuid,
                    conn.getId(),
                    conn.getJiraProjectKey()
            );
        } else {
            log.info("{} ℹ️ Jira 연동 없음: projectUuid={}", LOG_PREFIX, projectUuid);

            return new JiraConnectionStatusResponse(
                    false,
                    projectUuid,
                    null,
                    null
            );
        }
    }
}
