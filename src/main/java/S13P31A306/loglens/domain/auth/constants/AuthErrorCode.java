package S13P31A306.loglens.domain.auth.constants;

import S13P31A306.loglens.global.constants.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    // 400 Bad Request
    PASSWORD_CONFIRMATION_MISMATCH("A400-1", "비밀번호가 일치하지 않습니다.", 400),
    EMAIL_REQUIRED("A400-2", "이메일은 필수 입력입니다.", 400),
    EMAIL_INVALID_FORMAT("A400-3", "이메일 형식이 올바르지 않습니다.", 400),
    PASSWORD_REQUIRED("A400-4", "비밀번호는 필수 입력입니다.", 400),
    REFRESH_TOKEN_MISSING("A400-5", "Refresh Token이 누락되었습니다.", 400),
    ACCESS_TOKEN_MISSING("A400-6", "Access Token이 누락되었습니다.", 400),
    INVALID_TOKEN_FORMAT("A400-7", "토큰 형식이 올바르지 않습니다.", 400),

    // 401 Unauthorized
    INVALID_CREDENTIALS("A401-1", "이메일 또는 비밀번호가 일치하지 않습니다.", 401),
    REFRESH_TOKEN_INVALID("A401-2", "유효하지 않거나 취소된 Refresh Token입니다.", 401),
    REFRESH_TOKEN_EXPIRED("A401-3", "Refresh Token이 만료되었습니다. 다시 로그인해주세요.", 401),
    ACCESS_TOKEN_INVALID("A401-4", "유효하지 않은 Access Token입니다.", 401);

    private final String code;
    private final String message;
    private final int status;
}
