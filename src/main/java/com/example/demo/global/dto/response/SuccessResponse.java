package com.example.demo.global.dto.response;

import com.example.demo.global.constants.SuccessCode;
import com.example.demo.global.utils.TimestampUtils;
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
        @JsonInclude(NON_NULL) T data,
        @NotNull String timestamp

) implements BaseResponse{

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
