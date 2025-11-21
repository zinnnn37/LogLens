package com.example.demo.domain.user.dto;

import a306.dependency_logger_starter.logging.annotation.ExcludeValue;
import a306.dependency_logger_starter.logging.annotation.Sensitive;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "이름은 필수입니다")
        @Size(min = 2, max = 50, message = "이름은 2-50자 사이여야 합니다")
        String name,

        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        String email,

        @Sensitive
        @NotBlank(message = "비밀번호는 필수입니다")
        @Size(min = 8, max = 20, message = "비밀번호는 8-20자 사이여야 합니다")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String password,

        @ExcludeValue
        @Size(max = 200, message = "비밀 정보는 200자 이하여야 합니다")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String secret
) {
}
