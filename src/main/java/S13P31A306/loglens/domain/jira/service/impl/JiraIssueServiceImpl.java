package S13P31A306.loglens.domain.jira.service.impl;

import S13P31A306.loglens.domain.auth.entity.User;
import S13P31A306.loglens.domain.auth.util.AuthenticationHelper;
import S13P31A306.loglens.domain.jira.client.JiraApiClient;
import S13P31A306.loglens.domain.jira.client.dto.JiraIssueRequest;
import S13P31A306.loglens.domain.jira.client.dto.JiraIssueResponse;
import S13P31A306.loglens.domain.jira.constants.JiraErrorCode;
import S13P31A306.loglens.domain.jira.dto.request.JiraIssueCreateRequest;
import S13P31A306.loglens.domain.jira.dto.response.CreatedByResponse;
import S13P31A306.loglens.domain.jira.dto.response.JiraIssueCreateResponse;
import S13P31A306.loglens.domain.jira.entity.JiraConnection;
import S13P31A306.loglens.domain.jira.mapper.JiraMapper;
import S13P31A306.loglens.domain.jira.repository.JiraConnectionRepository;
import S13P31A306.loglens.domain.jira.service.JiraIssueService;
import S13P31A306.loglens.domain.jira.validator.JiraValidator;
import S13P31A306.loglens.domain.project.entity.Project;
import S13P31A306.loglens.domain.project.repository.ProjectRepository;
import S13P31A306.loglens.global.constants.GlobalErrorCode;
import S13P31A306.loglens.global.exception.BusinessException;
import S13P31A306.loglens.global.utils.EncryptionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Jira 이슈 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JiraIssueServiceImpl implements JiraIssueService {

    private static final String LOG_PREFIX = "[JiraIssue]";

    private final AuthenticationHelper authenticationHelper;
    private final JiraConnectionRepository jiraConnectionRepository;
    private final ProjectRepository projectRepository;
    private final JiraApiClient jiraApiClient;
    private final JiraValidator jiraValidator;
    private final JiraMapper jiraMapper;
    private final EncryptionUtils encryptionUtils;

    /**
     * Jira 이슈 생성
     * 현재 인증된 사용자의 정보를 사용합니다.
     *
     * @param request 이슈 생성 요청 DTO
     * @return JiraIssueCreateResponse 이슈 생성 응답 DTO
     */
    @Override
    public JiraIssueCreateResponse createIssue(JiraIssueCreateRequest request) {
        // 현재 인증된 사용자 ID 조회
        Integer userId = authenticationHelper.getCurrentUserId();
        log.info("{} 🎫 Jira 이슈 생성 시작: projectUuid={}, logId={}, userId={}",
                LOG_PREFIX, request.projectUuid(), request.logId(), userId);

        // 1. projectUuid로 Project 조회
        Project project = projectRepository.findByProjectUuid(request.projectUuid())
                .orElseThrow(() -> new BusinessException(JiraErrorCode.PROJECT_NOT_FOUND));
        log.debug("{} ✅ 프로젝트 조회 완료: projectId={}", LOG_PREFIX, project.getId());

        // 2. 프로젝트 접근 권한 확인
        jiraValidator.validateProjectAccess(project.getId(), userId);
        log.debug("{} ✅ 프로젝트 접근 권한 확인 완료", LOG_PREFIX);

        // 3. 로그 존재 여부 확인
        jiraValidator.validateLogExists(request.logId());
        log.debug("{} ✅ 로그 존재 확인 완료", LOG_PREFIX);

        // 4. Jira 연동 정보 조회
        JiraConnection connection = jiraConnectionRepository.findByProjectId(project.getId())
                .orElseThrow(() -> {
                    log.warn("{} ⚠️ Jira 연동 정보 없음: projectUuid={}", LOG_PREFIX, request.projectUuid());
                    return new BusinessException(GlobalErrorCode.NOT_FOUND);
                });
        log.debug("{} ✅ Jira 연동 정보 조회 완료", LOG_PREFIX);

        // 4. API 토큰 복호화
        String decryptedToken = encryptionUtils.decrypt(connection.getJiraApiToken());
        log.debug("{} 🔓 API 토큰 복호화 완료", LOG_PREFIX);

        // 5. 로그 정보 조회 및 설명 생성 (TODO: 실제 로그 조회 로직 추가)
        String logDescription = createLogDescription(request.logId());
        log.debug("{} 📝 로그 설명 생성 완료", LOG_PREFIX);

        // 6. Jira API 요청 DTO 생성
        JiraIssueRequest jiraRequest = jiraMapper.toJiraApiRequest(
                request,
                connection.getJiraProjectKey(),
                logDescription
        );

        // 7. Jira API를 통한 이슈 생성
        JiraIssueResponse jiraResponse = jiraApiClient.createIssue(
                connection.getJiraUrl(),
                connection.getJiraEmail(),
                decryptedToken,
                jiraRequest
        );
        log.info("{} ✅ Jira 이슈 생성 완료: issueKey={}", LOG_PREFIX, jiraResponse.key());

        // 8. 사용자 정보 조회
        User user = authenticationHelper.getCurrentUser();
        CreatedByResponse createdBy = jiraMapper.toCreatedByResponse(user);

        // 9. 응답 생성
        String jiraUrl = connection.getJiraUrl() + "/browse/" + jiraResponse.key();
        JiraIssueCreateResponse response = new JiraIssueCreateResponse(
                jiraResponse.key(),
                jiraUrl,
                createdBy
        );

        log.info("{} 🎉 Jira 이슈 생성 프로세스 완료: issueKey={}, projectUuid={}",
                LOG_PREFIX, jiraResponse.key(), request.projectUuid());

        return response;
    }

    /**
     * 로그 정보를 기반으로 Jira 이슈 설명 생성
     *
     * @param logId 로그 ID
     * @return 이슈 설명
     */
    private String createLogDescription(Integer logId) {
        // TODO: 실제 구현 시 Log 및 LogDetail 조회하여 상세 정보 포함
        return String.format(
                "LogLens에서 자동 생성된 이슈입니다.\\n\\n" +
                        "📋 로그 정보:\\n" +
                        "- 로그 ID: %d\\n" +
                        "- LogLens URL: [로그 상세보기](링크)\\n\\n" +
                        "자세한 내용은 LogLens에서 확인해주세요.",
                logId
        );
    }
}
