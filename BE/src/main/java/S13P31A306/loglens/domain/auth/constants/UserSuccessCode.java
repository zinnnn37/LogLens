package S13P31A306.loglens.domain.auth.constants;

import S13P31A306.loglens.global.constants.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserSuccessCode implements SuccessCode {

    EMAIL_AVAILABLE("U200-1", "사용 가능한 이메일입니다.", HttpStatus.OK.value()),
    EMAIL_DUPLICATE("U200-2", "이미 사용 중인 이메일입니다.", HttpStatus.OK.value()),
    USER_SEARCH_SUCCESS("U200-3", "사용자 검색이 완료되었습니다.", HttpStatus.OK.value()),

    SIGNUP_SUCCESS("U201-1", "회원가입이 완료되었습니다.", HttpStatus.CREATED.value());

    private final String code;
    private final String message;
    private final int status;
}
