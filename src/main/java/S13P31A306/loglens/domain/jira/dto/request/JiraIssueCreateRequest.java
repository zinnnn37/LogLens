package S13P31A306.loglens.domain.jira.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * Jira 이슈 생성 요청 DTO
 */
public record JiraIssueCreateRequest(
        @NotBlank(message = "PROJECT_UUID_REQUIRED")
        @Schema(description = "LogLens 프로젝트 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        String projectUuid,

        @NotNull(message = "LOG_ID_REQUIRED")
        @Schema(description = "로그 ID", example = "12345")
        Integer logId,

        @NotBlank(message = "SUMMARY_REQUIRED")
        @Size(min = 1, max = 255, message = "SUMMARY_LENGTH_INVALID")
        @Schema(description = "이슈 제목", example = "[ERROR] Database connection timeout")
        String summary,

        @Size(max = 32767, message = "DESCRIPTION_TOO_LONG")
        @Schema(description = "이슈 상세 설명", example = "Database connection pool exhausted during peak hours.")
        String description,

        @Pattern(regexp = "^(Bug|Task|Story|Epic)$", message = "ISSUE_TYPE_INVALID")
        @Schema(description = "이슈 타입", example = "Bug", defaultValue = "Bug")
        String issueType,

        @Pattern(regexp = "^(Highest|High|Medium|Low|Lowest)$", message = "PRIORITY_INVALID")
        @Schema(description = "우선순위", example = "High", defaultValue = "Medium")
        String priority
) {
}
