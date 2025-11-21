package S13P31A306.loglens.domain.jira.service;

import S13P31A306.loglens.domain.jira.dto.request.JiraConnectRequest;
import S13P31A306.loglens.domain.jira.dto.response.JiraConnectResponse;
import S13P31A306.loglens.domain.jira.dto.response.JiraConnectionStatusResponse;

/**
 * Jira 연동 서비스
 * Jira 연동 설정 관련 비즈니스 로직을 처리합니다.
 */
public interface JiraIntegrationService {

    /**
     * Jira 연동 설정
     * Jira 인스턴스와 연동하고 연결 테스트를 수행합니다.
     * 현재 인증된 사용자의 정보를 사용합니다.
     *
     * @param request 연동 요청 DTO
     * @return JiraConnectResponse 연동 응답 DTO
     */
    JiraConnectResponse connect(JiraConnectRequest request);

    /**
     * Jira 연동 상태 조회
     * 특정 프로젝트의 Jira 연동 상태를 조회합니다.
     * 현재 인증된 사용자가 해당 프로젝트의 멤버인지 확인합니다.
     *
     * @param projectUuid 프로젝트 UUID
     * @return JiraConnectionStatusResponse 연동 상태 응답 DTO
     */
    JiraConnectionStatusResponse getConnectionStatus(String projectUuid);
}
