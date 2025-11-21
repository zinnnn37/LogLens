package S13P31A306.loglens.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public record UserSearchResponse(
    @Schema(description = "사용자 ID", example = "1")
    Integer userId,

    @Schema(description = "사용자 이름", example = "홍길동")
    String name,

    @Schema(description = "사용자 이메일", example = "developer@example.com")
    String email
) {
}
