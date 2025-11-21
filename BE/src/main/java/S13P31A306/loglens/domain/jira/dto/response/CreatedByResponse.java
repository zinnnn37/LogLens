package S13P31A306.loglens.domain.jira.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 생성자 정보 응답 DTO
 */
public record CreatedByResponse(
        @Schema(description = "사용자 ID", example = "1")
        Integer userId,

        @Schema(description = "사용자 이메일", example = "user@example.com")
        String email,

        @Schema(description = "사용자 이름", example = "John Doe")
        String name
) {
}
