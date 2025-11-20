package com.example.demo.global.dto.response;

import com.example.demo.global.constants.ErrorCode;
import com.example.demo.global.constants.SuccessCode;
import org.springframework.http.ResponseEntity;

public final class ApiResponseFactory {

    private ApiResponseFactory() {
        throw new IllegalStateException();
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
