package S13P31A306.loglens.domain.auth.dto.request;

import S13P31A306.loglens.global.annotation.Sensitive;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserSigninRequest(
        @NotBlank(message = "EMAIL_REQUIRED")
        @Email(message = "EMAIL_INVALID_FORMAT")
        @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9._-]*@[a-zA-Z][a-zA-Z0-9.-]*\\.[a-zA-Z]{2,}$",
                message = "EMAIL_INVALID_FORMAT")
        @Schema(description = "로그인용 이메일", example = "som@example.com")
        String email,

        @Sensitive
        @NotBlank(message = "PASSWORD_REQUIRED")
        @Size(min = 8, max = 16, message = "PASSWORD_INVALID_FORMAT")
        @Schema(description = "로그인 비밀번호", example = "Secure123!")
        String password
) {
}
