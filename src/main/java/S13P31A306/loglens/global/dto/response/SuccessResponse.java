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
        @NotNull String code,
        @NotNull String message,
        @NotNull int status,
        @JsonInclude(NON_NULL) T data, // 추가 에러 상세 정보 (null인 경우 JSON에서 제외)
        @NotNull String timestamp

) implements BaseResponse{

    public static <T> SuccessResponse<T> of(final String code, final String message, final int status, final T data) {
        return SuccessResponse.<T>builder()
                .code(code)
                .message(message)
                .status(status)
                .data(data)
                .timestamp(TimestampUtils.now())
                .build();
    }

    public static SuccessResponse<?> of(final String code, final String message, final int status) {
        return SuccessResponse.builder()
                .code(code)
                .message(message)
                .status(status)
                .timestamp(TimestampUtils.now())
                .build();
    }

    public static <T> SuccessResponse<T> of(final SuccessCode successCode, final T data) {
        return SuccessResponse.<T>builder()
                .code(successCode.getCode())
                .message(successCode.getMessage())
                .status(successCode.getStatus())
                .data(data)
                .timestamp(TimestampUtils.now())
                .build();
    }

    public static <T> SuccessResponse<T> of(final SuccessCode successCode) {
        return SuccessResponse.<T>builder()
                .code(successCode.getCode())
                .message(successCode.getMessage())
                .status(successCode.getStatus())
                .timestamp(TimestampUtils.now())
                .build();
    }
}

