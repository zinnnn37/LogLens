package S13P31A306.loglens.global.dto.response;

import S13P31A306.loglens.global.constants.ErrorCode;
import S13P31A306.loglens.global.constants.SystemMessages;
import S13P31A306.loglens.global.constants.SuccessCode;
import org.springframework.http.ResponseEntity;

public final class ApiResponseFactory {

    private ApiResponseFactory() {
        throw new IllegalStateException(SystemMessages.UTILITY_CLASS_ERROR.message());
    }
    public static <T> ResponseEntity<SuccessResponse<T>> success(final SuccessCode successCode, final T data) {
        return ResponseEntity.status(successCode.getStatus()).body(SuccessResponse.of(successCode, data));
    }

    public static ResponseEntity<BaseResponse> success(final SuccessCode successCode) {
        return ResponseEntity.status(successCode.getStatus()).body(SuccessResponse.of(successCode));
    }

    public static <T> ResponseEntity<ErrorResponse<T>> fail(final ErrorCode errorCode, final T data) {
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode, data));
    }

    public static ResponseEntity<BaseResponse> fail(final ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
    }
}
