package a306.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 메서드 실행 로그
 * ERD log + log_details 테이블 매핑
 */
@JsonInclude(JsonInclude.Include.NON_NULL)  // null 필드 제외
public record MethodLog(
        // === log 테이블 필드 ===
        String traceId,              // trace_id
        String logLevel,             // log_level (INFO, WARN, ERROR)
        String sourceType,           // source_type (BE 고정)
        LocalDateTime timestamp,     // timestamp
        String comment,              // comment (선택)

        // === log_details 테이블 필드 ===
        String methodName,           // method_name
        String className,            // class_name
        String stackTrace,           // stack_trace (에러 시)
        Object requestData,          // request_data (파라미터 JSON)
        Object responseData,         // response_data (응답 JSON)
        BigDecimal executionTime,    // execution_time (밀리초)
        Object additionalInfo,       // additional_info (기타 JSON)

        // === 추가 정보 ===
        String projectName           // 프로젝트 식별용
) {
}
