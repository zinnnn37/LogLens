package S13P31A306.loglens.global.dto.response;


import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import S13P31A306.loglens.global.constants.ErrorCode;
import S13P31A306.loglens.global.utils.TimestampUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;

@Builder(access = AccessLevel.PRIVATE)
public record ErrorResponse<T>(
        @NotNull String code,
        @NotNull String message,
        @NotNull int status,
        @JsonInclude(NON_NULL) T data,
        @NotNull String timestamp
) implements BaseResponse {

    public static <T> ErrorResponse<T> of(final ErrorCode errorCode, final T data) {
        return ErrorResponse.<T>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .status(errorCode.getStatus())
                .data(data)
                .timestamp(TimestampUtils.now())
                .build();
    }

    public static <T> ErrorResponse<T> of(final ErrorCode errorCode) {
        return ErrorResponse.<T>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .status(errorCode.getStatus())
                .timestamp(TimestampUtils.now())
                .build();
    }
}
