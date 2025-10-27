package S13P31A306.loglens.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record EmailValidateResponse(
        @Schema(description = "확인한 이메일 주소", example = "developer@example.com")
        String email,

        @Schema(description = "사용 가능 여부 (true: 사용 가능, false: 이미 사용 중)", example = "true")
        boolean available
) {
}
