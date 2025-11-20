package com.example.demo.domain.user.dto;

import a306.dependency_logger_starter.logging.annotation.Sensitive;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @Sensitive
        @NotBlank(message = "기존 비밀번호는 필수입니다")
        String oldPassword,

        @Sensitive
        @NotBlank(message = "새 비밀번호는 필수입니다")
        @Size(min = 8, max = 20, message = "비밀번호는 8-20자 사이여야 합니다")
        String newPassword
) {
}
