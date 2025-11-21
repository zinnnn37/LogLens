package S13P31A306.loglens.domain.auth.constants;

import S13P31A306.loglens.global.constants.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthSuccessCode implements SuccessCode {

    SIGNIN_SUCCESS("A200-1", "로그인에 성공했습니다.", HttpStatus.OK.value()),
    SIGNOUT_SUCCESS("A200-2", "로그아웃이 완료되었습니다.", HttpStatus.OK.value()),

    TOKEN_REFRESH_SUCCESS("A201-1", "토큰이 갱신되었습니다.", HttpStatus.OK.value());

    private final String code;
    private final String message;
    private final int status;
}
