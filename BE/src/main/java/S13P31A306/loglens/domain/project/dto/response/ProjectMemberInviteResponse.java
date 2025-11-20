package S13P31A306.loglens.domain.project.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProjectMemberInviteResponse(
        @Schema(description = "초대한 멤버 정보")
        MemberInfo member
) {

    public record MemberInfo(
            @Schema(description = "사용자 ID", example = "42")
            int userId,

            @Schema(description = "사용자 이름", example = "김철수")
            String userName,

            @Schema(description = "사용자 이메일", example = "email@email.com")
            String email,

            @Schema(description = "초대한 일시", example = "2025-10-29T10:30:00")
            String joinedAt
    ) {
    }
}
