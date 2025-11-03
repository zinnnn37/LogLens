package S13P31A306.loglens.domain.jira.service.impl;

import S13P31A306.loglens.domain.auth.util.AuthenticationHelper;
import S13P31A306.loglens.domain.jira.client.JiraApiClient;
import S13P31A306.loglens.domain.jira.constants.JiraErrorCode;
import S13P31A306.loglens.domain.jira.dto.request.JiraConnectRequest;
import S13P31A306.loglens.domain.jira.dto.response.JiraConnectResponse;
import S13P31A306.loglens.domain.jira.entity.JiraConnection;
import S13P31A306.loglens.domain.jira.mapper.JiraMapper;
import S13P31A306.loglens.domain.jira.repository.JiraConnectionRepository;
import S13P31A306.loglens.domain.jira.service.JiraIntegrationService;
import S13P31A306.loglens.domain.jira.validator.JiraValidator;
import S13P31A306.loglens.global.exception.BusinessException;
import S13P31A306.loglens.global.utils.EncryptionUtils;
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
    private final JiraApiClient jiraApiClient;
    private final JiraValidator jiraValidator;
    private final JiraMapper jiraMapper;
    private final EncryptionUtils encryptionUtils;

    /**
     * Jira 연동 설정
     * 현재 인증된 사용자의 정보를 사용합니다.
     *
     * @param request 연동 요청 DTO
     * @return JiraConnectResponse 연동 응답 DTO
     */
    @Override
    @Transactional
    public JiraConnectResponse connect(JiraConnectRequest request) {
        // 현재 인증된 사용자 ID 조회
        Integer userId = authenticationHelper.getCurrentUserId();
        log.info("{} 🔗 Jira 연동 설정 시작: projectId={}, userId={}", LOG_PREFIX, request.projectId(), userId);

        // 1. 프로젝트 존재 여부 및 권한 확인
        jiraValidator.validateProjectAccess(request.projectId(), userId);
        log.debug("{} ✅ 프로젝트 접근 권한 확인 완료", LOG_PREFIX);

        // 2. 중복 연동 체크
        jiraValidator.validateDuplicateConnection(request.projectId());
        log.debug("{} ✅ 중복 연동 체크 완료", LOG_PREFIX);

        // 3. Jira API 연결 테스트
        boolean connected = jiraApiClient.testConnection(
                request.jiraUrl(),
                request.jiraEmail(),
                request.jiraApiToken(),
                request.jiraProjectKey()
        );

        if (!connected) {
            log.warn("{} ⚠️ Jira 연결 테스트 실패: projectId={}", LOG_PREFIX, request.projectId());
            throw new BusinessException(JiraErrorCode.JIRA_API_CONNECTION_FAILED);
        }
        log.info("{} ✅ Jira 연결 테스트 성공", LOG_PREFIX);

        // 4. API 토큰 암호화
        String encryptedToken = encryptionUtils.encrypt(request.jiraApiToken());
        log.debug("{} 💾 API 토큰 암호화 완료", LOG_PREFIX);

        // 5. 연동 정보 저장
        JiraConnection connection = jiraMapper.toEntity(request, encryptedToken);
        JiraConnection saved = jiraConnectionRepository.save(connection);
        log.info("{} ✅ Jira 연동 저장 완료: connectionId={}, projectId={}",
                LOG_PREFIX, saved.getId(), saved.getProjectId());

        // 6. 응답 생성
        JiraConnectResponse response = jiraMapper.toConnectResponse(saved);
        log.info("{} 🎉 Jira 연동 설정 완료: projectId={}", LOG_PREFIX, request.projectId());

        return response;
    }
}
