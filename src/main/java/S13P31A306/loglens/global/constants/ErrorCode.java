package S13P31A306.loglens.global.constants;

import org.springframework.http.HttpStatus;

import java.util.Optional;

public interface ErrorCode {
    String getCode();
    String getMessage();
    int getStatus();

    // HTTP Status와 연동
    default HttpStatus getHttpStatus() {
        return HttpStatus.valueOf(getStatus());
    }

    /**
     * Enum name으로 ErrorCode를 안전하게 조회한다.
     * @param name ErrorCode enum의 이름
     * @return 존재하면 해당 ErrorCode, 없으면 Optional.empty()
     */
    static Optional<ErrorCode> safeValueOf(String name) {
        try {
            return Optional.of(GlobalErrorCode.valueOf(name));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

