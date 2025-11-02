package S13P31A306.loglens.domain.jira.service;

import S13P31A306.loglens.domain.jira.dto.request.JiraIssueCreateRequest;
import S13P31A306.loglens.domain.jira.dto.response.JiraIssueCreateResponse;

/**
 * Jira 이슈 서비스
 * Jira 이슈 생성 관련 비즈니스 로직을 처리합니다.
 */
public interface JiraIssueService {

    /**
     * Jira 이슈 생성
     * 로그 정보를 기반으로 Jira 이슈를 생성합니다.
     *
     * @param request 이슈 생성 요청 DTO
     * @param userId  요청 사용자 ID
     * @return JiraIssueCreateResponse 이슈 생성 응답 DTO
     */
    JiraIssueCreateResponse createIssue(JiraIssueCreateRequest request, Integer userId);
}
