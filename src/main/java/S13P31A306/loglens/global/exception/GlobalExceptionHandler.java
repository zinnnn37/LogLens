package S13P31A306.loglens.global.exception;

import S13P31A306.loglens.global.constants.ErrorCode;
import S13P31A306.loglens.global.constants.GlobalErrorCode;
import S13P31A306.loglens.global.dto.response.BaseResponse;
import S13P31A306.loglens.global.dto.response.ErrorResponse;
import S13P31A306.loglens.global.dto.response.ValidationErrorDetail;
import S13P31A306.loglens.global.dto.response.ValidationErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.util.StringUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 핸들러 - 서비스 전체에서 발생하는 예외를 일관된 형식으로 처리한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String LOG_PREFIX = "[Prometheus(GlobalExceptionHandler)]";

    /**
     * 전역 비즈니스 예외 처리
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse> handleBusinessException(final BusinessException e) {
        return buildErrorResponse(e.getErrorCode(), e.getDetails());
    }

    /**
     * @Valid, @Validated 유효성 검증 실패 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse> handleMethodArgumentNotValidException(
            final MethodArgumentNotValidException e, final HttpServletRequest request) {

        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        List<ValidationErrorDetail> validationErrorDetails = getValidationErrorDetails(fieldErrors);

        return buildErrorResponse(GlobalErrorCode.VALIDATION_ERROR,
                new ValidationErrorResponse(request.getRequestURI(), validationErrorDetails));
    }

    /**
     * URI 파라미터 등의 제약조건 검증 실패 처리
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<BaseResponse> handleConstraintViolationException(
            final ConstraintViolationException e) {
        var details = extractConstraintViolations(e);
        return buildErrorResponse(GlobalErrorCode.VALIDATION_ERROR, details);
    }

    /**
     * 잘못된 JSON 형식 등 본문 파싱 실패
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse> handleInvalidFormat(final HttpMessageNotReadableException e) {
        return buildErrorResponse(GlobalErrorCode.INVALID_FORMAT);
    }

    /**
     * 허용되지 않은 HTTP 메서드 요청 처리
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<BaseResponse> handleMethodNotAllowed(
            final HttpRequestMethodNotSupportedException e) {
        return buildErrorResponse(GlobalErrorCode.METHOD_NOT_ALLOWED);
    }

    //formatter:off

    /**
     * Spring Security Authorization 예외 처리 SSE 연결 종료 시 정상적으로 발생할 수 있는 예외
     */
    //formatter:on
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<BaseResponse> handleAuthorizationDeniedException(
            final AuthorizationDeniedException e, final HttpServletRequest request) {
        // SSE 엔드포인트인 경우 간단한 로그만 출력 (정상적인 연결 종료)
        if (request.getRequestURI().contains("/stream")) {
            log.debug("{} SSE 연결 종료로 인한 Authorization 예외 (정상): uri={}", LOG_PREFIX, request.getRequestURI());
        } else {
            log.warn("{} 권한 거부: uri={}, message={}", LOG_PREFIX, request.getRequestURI(), e.getMessage());
        }
        return buildErrorResponse(GlobalErrorCode.FORBIDDEN);
    }

    /**
     * 정의되지 않은 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse> handleUncaughtException(final Exception e, final HttpServletRequest request) {
        // Actuator 엔드포인트인 경우 깔끔한 로그 처리
        if (isActuatorEndpoint(request.getRequestURI())) {
            log.info("{} Actuator 엔드포인트 오류 처리: uri={}, error={}", LOG_PREFIX, request.getRequestURI(), e.getMessage());
            return buildErrorResponse(GlobalErrorCode.INTERNAL_SERVER_ERROR);
        }

        // 일반 예외는 기존 로직 사용
        log.error("{} 예상하지 못한 오류 발생: uri={}, error={}", LOG_PREFIX, request.getRequestURI(), e.getMessage());
        return buildErrorResponse(GlobalErrorCode.INTERNAL_SERVER_ERROR);
    }

    /**
     * Actuator 엔드포인트 여부 확인
     */
    private boolean isActuatorEndpoint(String requestUri) {
        return StringUtils.hasText(requestUri) && requestUri.startsWith("/actuator/");
    }

    /**
     * 필드 오류를 ValidationErrorDetail 목록으로 변환한다.
     */
    private List<ValidationErrorDetail> getValidationErrorDetails(final List<FieldError> fieldErrors) {
        return fieldErrors.stream()
                .map(error -> {
                    String errorCodeString = error.getDefaultMessage();
                    ErrorCode errorCode = GlobalErrorCode.safeValueOf(errorCodeString)
                            .orElse(GlobalErrorCode.VALIDATION_ERROR);

                    return new ValidationErrorDetail(
                            error.getField(),
                            String.valueOf(error.getRejectedValue()),
                            errorCode.getCode(),
                            errorCode.getMessage()
                    );
                })
                .toList();
    }

    /**
     * 에러 응답 공통 생성 로직 - details 포함
     */
    private ResponseEntity<BaseResponse> buildErrorResponse(final ErrorCode code, final Object details) {
        var body = ErrorResponse.of(code, details);
        return ResponseEntity.status(code.getStatus()).body(body);
    }

    /**
     * 에러 응답 공통 생성 로직 - details 없음
     */
    private ResponseEntity<BaseResponse> buildErrorResponse(final ErrorCode code) {
        var body = ErrorResponse.of(code);
        return ResponseEntity.status(code.getStatus()).body(body);
    }

    /**
     * 제약조건 위반 오류 메시지 추출
     */
    private Map<String, String> extractConstraintViolations(final ConstraintViolationException e) {
        return e.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> extractLeafProperty(violation.getPropertyPath().toString()),
                        ConstraintViolation::getMessage,
                        (first, second) -> first
                ));
    }

    /**
     * 경로 문자열에서 마지막 필드명만 추출
     */
    private String extractLeafProperty(final String path) {
        final var lastDot = path.lastIndexOf('.');
        return (lastDot != -1) ? path.substring(lastDot + 1) : path;
    }
}
