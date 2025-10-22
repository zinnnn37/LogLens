package S13P31A306.loglens.global.constants;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
    String getCode();
    String getMessage();
    int getStatus();

    // HTTP Status와 연동
    default HttpStatus getHttpStatus() {
        return HttpStatus.valueOf(getStatus());
    }
}

