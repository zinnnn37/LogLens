package S13P31A306.loglens.global.dto.response;

import S13P31A306.loglens.global.constants.SuccessCode;
import S13P31A306.loglens.global.utils.TimestampUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Builder(access = AccessLevel.PRIVATE)
public record SuccessResponse<T>(
        @NotNull int code,
        @NotNull String message,
        @JsonInclude(NON_NULL) T data,
        @NotNull String timestamp

) implements BaseResponse{

    public static <T> SuccessResponse<T> of(final SuccessCode successCode, final T data) {
        return SuccessResponse.<T>builder()
                .code(successCode.getStatus())
                .message(successCode.getMessage())
                .data(data)
                .timestamp(TimestampUtils.now())
                .build();
    }

    public static <T> SuccessResponse<T> of(final SuccessCode successCode) {
        return SuccessResponse.<T>builder()
                .code(successCode.getStatus())
                .message(successCode.getMessage())
                .timestamp(TimestampUtils.now())
                .build();
    }
}
