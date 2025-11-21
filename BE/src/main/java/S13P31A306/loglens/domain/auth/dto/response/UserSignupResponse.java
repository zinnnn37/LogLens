package S13P31A306.loglens.domain.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record UserSignupResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "사용자 이름", example = "홍길동")
        @JsonProperty("name")
        String name,

        @Schema(description = "사용자 이메일", example = "developer@example.com")
        String email,

        @Schema(description = "가입 일시", example = "2025-10-17T12:30:00")
        LocalDateTime createdAt
) {
}
