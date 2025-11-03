package S13P31A306.loglens.domain.jira.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Jira 연결 테스트 응답 DTO
 */
public record JiraConnectionTestResponse(
        @Schema(description = "테스트 상태", example = "SUCCESS")
        String status,

        @Schema(description = "테스트 메시지", example = "Jira 연결이 성공적으로 테스트되었습니다.")
        String message,

        @Schema(description = "테스트 시각", example = "2025-11-02T10:30:00Z")
        String testedAt
) {
}
