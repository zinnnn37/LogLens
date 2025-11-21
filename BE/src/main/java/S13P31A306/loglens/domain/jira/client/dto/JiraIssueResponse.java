package S13P31A306.loglens.domain.jira.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Jira API 이슈 생성 응답 DTO
 * Jira REST API v3 응답 형식
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraIssueResponse(
        String id,
        String key,
        String self
) {
}
