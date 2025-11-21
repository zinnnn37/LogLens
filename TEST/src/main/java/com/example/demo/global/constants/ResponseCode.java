package com.example.demo.global.constants;

import org.springframework.http.HttpStatus;

/**
 * 응답 코드의 공통 인터페이스
 * <p>
 * ErrorCode와 SuccessCode의 공통 부모 인터페이스로,
 * 코드, 메시지, 상태 코드를 정의합니다.
 * </p>
 */
public interface ResponseCode {
    /**
     * 응답 코드를 반환합니다.
     * @return 응답 코드 (예: "G404", "S200")
     */
    String getCode();

    /**
     * 응답 메시지를 반환합니다.
     * @return 응답 메시지
     */
    String getMessage();

    /**
     * HTTP 상태 코드를 정수로 반환합니다.
     * @return HTTP 상태 코드 (예: 200, 404, 500)
     */
    int getStatus();

    /**
     * HTTP 상태 코드를 HttpStatus 객체로 반환합니다.
     * @return HttpStatus 객체
     */
    default HttpStatus getHttpStatus() {
        return HttpStatus.valueOf(getStatus());
    }
}
