package S13P31A306.loglens.domain.auth.constants;

import S13P31A306.loglens.global.constants.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    // 400 Bad Request
    NAME_REQUIRED("U400-1", "이름은 필수 입력값입니다.", 400),
    NAME_LENGTH_INVALID("U400-2", "이름은 1~50자 이내여야 합니다.", 400),
    NAME_FORMAT_INVALID("U400-3", "이름에는 한글만 사용할 수 있습니다.", 400),
    PAGE_INVALID("U400-4", "페이지 번호는 0 이상이어야 합니다.", 400),
    SIZE_INVALID("U400-5", "페이지 크기는 1~100 범위여야 합니다.", 400),
    SORT_INVALID("U400-6", "유효하지 않은 정렬 필드입니다.", 400),
    ORDER_INVALID("U400-7", "유효하지 않은 정렬 방향입니다.", 400);

    private final String code;
    private final String message;
    private final int status;
}
