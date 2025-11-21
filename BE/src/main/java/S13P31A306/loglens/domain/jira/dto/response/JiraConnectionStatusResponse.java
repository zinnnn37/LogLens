package S13P31A306.loglens.domain.jira.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Jira 연동 상태 조회 응답")
public record JiraConnectionStatusResponse(

        @Schema(description = "Jira 연동 존재 여부", example = "true")
        boolean exists,

        @Schema(description = "프로젝트 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        String projectUuid,

        @Schema(description = "연동 ID (연동이 존재하는 경우에만)", example = "1", nullable = true)
        Integer connectionId,

        @Schema(description = "Jira 프로젝트 키 (연동이 존재하는 경우에만)", example = "LOGLENS", nullable = true)
        String jiraProjectKey
) {
}
