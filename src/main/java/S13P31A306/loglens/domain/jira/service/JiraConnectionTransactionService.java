package S13P31A306.loglens.domain.jira.service;

import S13P31A306.loglens.domain.jira.dto.request.JiraConnectRequest;
import S13P31A306.loglens.domain.jira.dto.response.JiraConnectResponse;
import S13P31A306.loglens.domain.jira.entity.JiraConnection;
import S13P31A306.loglens.domain.jira.mapper.JiraMapper;
import S13P31A306.loglens.domain.jira.repository.JiraConnectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Jira 연동 트랜잭션 서비스
 * DB 저장 작업만 별도 트랜잭션으로 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JiraConnectionTransactionService {

    private static final String LOG_PREFIX = "[JiraConnectionTransaction]";

    private final JiraConnectionRepository jiraConnectionRepository;
    private final JiraMapper jiraMapper;

    /**
     * Jira 연동 정보 저장
     * 별도의 쓰기 가능한 트랜잭션으로 실행
     *
     * @param request 연동 요청 DTO
     * @param projectId 프로젝트 ID
     * @param projectUuid 프로젝트 UUID
     * @param encryptedToken 암호화된 API 토큰
     * @return JiraConnectResponse 연동 응답 DTO
     */
    @Transactional(readOnly = false)
    public JiraConnectResponse saveConnection(
            JiraConnectRequest request,
            Integer projectId,
            String projectUuid,
            String encryptedToken) {

        log.debug("{} 💾 Jira 연동 정보 저장 시작: projectId={}", LOG_PREFIX, projectId);

        JiraConnection connection = jiraMapper.toEntity(request, projectId, encryptedToken);
        JiraConnection saved = jiraConnectionRepository.save(connection);

        log.info("{} ✅ Jira 연동 저장 완료: connectionId={}, projectId={}",
                LOG_PREFIX, saved.getId(), saved.getProjectId());

        return jiraMapper.toConnectResponse(saved, projectUuid);
    }
}
