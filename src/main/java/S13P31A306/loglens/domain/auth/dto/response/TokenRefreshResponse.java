package S13P31A306.loglens.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenRefreshResponse(
        @Schema(description = "새로 발급된 액세스 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String accessToken,

        @Schema(description = "토큰 타입", example = "Bearer")
        String tokenType,

        @Schema(description = "토큰 만료 시간(초)", example = "3600")
        Integer expiresIn
) {
}
