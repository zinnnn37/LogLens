package S13P31A306.loglens.domain.jira.service;

import S13P31A306.loglens.domain.jira.dto.request.JiraConnectRequest;
import S13P31A306.loglens.domain.jira.dto.response.JiraConnectResponse;

/**
 * Jira 연동 서비스
 * Jira 연동 설정 관련 비즈니스 로직을 처리합니다.
 */
public interface JiraIntegrationService {

    /**
     * Jira 연동 설정
     * Jira 인스턴스와 연동하고 연결 테스트를 수행합니다.
     *
     * @param request 연동 요청 DTO
     * @param userId  요청 사용자 ID
     * @return JiraConnectResponse 연동 응답 DTO
     */
    JiraConnectResponse connect(JiraConnectRequest request, Integer userId);
}
