package S13P31A306.loglens.domain.jira.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Jira 이슈 생성 응답 DTO
 */
public record JiraIssueCreateResponse(
        @Schema(description = "생성된 Jira 이슈 키", example = "LOGLENS-1234")
        String issueKey,

        @Schema(description = "Jira 이슈 URL", example = "https://your-domain.atlassian.net/browse/LOGLENS-1234")
        String jiraUrl,

        @Schema(description = "생성자 정보")
        CreatedByResponse createdBy
) {
}
