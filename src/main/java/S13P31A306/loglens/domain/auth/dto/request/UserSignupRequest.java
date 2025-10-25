package S13P31A306.loglens.domain.auth.dto.request;

import S13P31A306.loglens.global.annotation.Sensitive;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserSignupRequest(
        @NotBlank(message = "NAME_REQUIRED")
        @Pattern(regexp = "^[가-힣a-zA-Z]{2,30}$", message = "NAME_INVALID_FORMAT")
        @Schema(description = "사용자 이름", example = "홍길동")
        @JsonProperty("userName")
        String userName,

        @NotBlank(message = "EMAIL_REQUIRED")
        @Email(message = "EMAIL_INVALID_FORMAT")
        @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9._-]*@[a-zA-Z][a-zA-Z0-9.-]*\\.[a-zA-Z]{2,}$",
                message = "EMAIL_INVALID_FORMAT")
        @Schema(description = "사용자 이메일", example = "developer@example.com")
        String email,

        @Sensitive
        @NotBlank(message = "PASSWORD_REQUIRED")
        @Size(min = 8, max = 16, message = "PASSWORD_INVALID_FORMAT")
        @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,16}$",
                message = "PASSWORD_INVALID_FORMAT"
        )
        @Schema(description = "로그인용 비밀번호 (8~16자, 영문 대/소문자, 숫자, 특수문자 조합)", example = "Secure123!")
        String password,

        @Sensitive
        @NotBlank(message = "PASSWORD_CONFIRMATION_REQUIRED")
        @Schema(description = "비밀번호 확인", example = "Secure123!")
        String passwordConfirm
) {
}
