package S13P31A306.loglens.domain.jira.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Jira 연동 설정 응답 DTO
 */
public record JiraConnectResponse(
        @Schema(description = "연동 ID", example = "1")
        Integer id,

        @Schema(description = "프로젝트 ID", example = "1")
        Integer projectId,

        @Schema(description = "Jira URL", example = "https://your-domain.atlassian.net")
        String jiraUrl,

        @Schema(description = "Jira 이메일", example = "admin@example.com")
        String jiraEmail,

        @Schema(description = "Jira 프로젝트 키", example = "LOGLENS")
        String jiraProjectKey,

        @Schema(description = "연결 테스트 결과")
        JiraConnectionTestResponse connectionTest
) {
}
