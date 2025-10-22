package S13P31A306.loglens.global.dto.response;


import S13P31A306.loglens.global.constants.ErrorCode;
import S13P31A306.loglens.global.utils.TimestampUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Builder(access = AccessLevel.PRIVATE)
public record ErrorResponse<T>(
        @NotNull String code,
        @NotNull String message,
        @NotNull int status,
        @JsonInclude(NON_NULL) T details,
        @NotNull String timestamp
) implements BaseResponse {

    public static <T> ErrorResponse<T> of(final ErrorCode errorCode, final T details) {
        return ErrorResponse.<T>builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .status(errorCode.getStatus())
                .details(details)
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
