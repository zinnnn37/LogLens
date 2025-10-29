package S13P31A306.loglens.domain.project.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ProjectMemberInviteRequest(
        @NotNull(message = "USER_ID_REQUIRED")
        @Schema(description = "초대할 사용자 ID", example = "42")
        int userId
) {
}
