package S13P31A306.loglens.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EmailValidateRequest(
        @NotBlank(message = "EMAIL_REQUIRED")
        @Email(message = "EMAIL_INVALID_FORMAT")
        @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9._-]*@[a-zA-Z][a-zA-Z0-9.-]*\\.[a-zA-Z]{2,}$\n",
                message = "EMAIL_INVALID_FORMAT")
        @Schema(description = "사용자 이메일", example = "developer@example.com")
        String email
) {
}
