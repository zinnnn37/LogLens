package S13P31A306.loglens.domain.jira.dto.request;

import S13P31A306.loglens.global.annotation.Sensitive;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * Jira 연동 설정 요청 DTO
 */
public record JiraConnectRequest(
        @NotBlank(message = "PROJECT_UUID_REQUIRED")
        @Schema(description = "LogLens 프로젝트 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        String projectUuid,

        @NotBlank(message = "JIRA_URL_REQUIRED")
        @Size(max = 255, message = "JIRA_URL_TOO_LONG")
        @Pattern(regexp = "^https?://[\\w.-]+\\.[a-zA-Z]{2,}/?.*$", message = "JIRA_URL_INVALID_FORMAT")
        @Schema(description = "Jira 인스턴스 URL", example = "https://your-domain.atlassian.net")
        @Sensitive
        String jiraUrl,

        @NotBlank(message = "JIRA_EMAIL_REQUIRED")
        @Email(message = "JIRA_EMAIL_INVALID_FORMAT")
        @Size(max = 255, message = "JIRA_EMAIL_TOO_LONG")
        @Schema(description = "Jira 계정 이메일", example = "admin@example.com")
        String jiraEmail,

        @NotBlank(message = "JIRA_API_TOKEN_REQUIRED")
        @Size(max = 255, message = "JIRA_API_TOKEN_TOO_LONG")
        @Schema(description = "Jira API 토큰", example = "ATATT3xFfGF0...")
        @Sensitive
        String jiraApiToken,

        @NotBlank(message = "JIRA_PROJECT_KEY_REQUIRED")
        @Size(max = 255, message = "JIRA_PROJECT_KEY_TOO_LONG")
        @Pattern(regexp = "^[A-Z][A-Z0-9]{1,9}$", message = "JIRA_PROJECT_KEY_INVALID_FORMAT")
        @Schema(description = "Jira 프로젝트 키", example = "LOGLENS")
        String jiraProjectKey
) {
}
