package com.example.demo.domain.user.constants;

import com.example.demo.global.constants.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    // 404 Not Found
    USER_NOT_FOUND("U404", "사용자를 찾을 수 없습니다", 404),

    // 409 Conflict
    EMAIL_ALREADY_EXISTS("U409-1", "이미 사용 중인 이메일입니다", 409),

    // 400 Bad Request
    INVALID_PASSWORD("U400-1", "기존 비밀번호가 일치하지 않습니다", 400),
    PASSWORD_SAME_AS_OLD("U400-2", "새 비밀번호는 기존 비밀번호와 달라야 합니다", 400);

    private final String code;
    private final String message;
    private final int status;
}
