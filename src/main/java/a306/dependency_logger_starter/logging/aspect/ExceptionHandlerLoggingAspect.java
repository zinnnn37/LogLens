package a306.dependency_logger_starter.logging.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Exception Handler 로깅 Aspect
 * 사용자의 @ExceptionHandler 메서드 실행 시 자동으로 예외 로깅
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class ExceptionHandlerLoggingAspect {

    private final ObjectMapper objectMapper;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    /**
     * @ExceptionHandler 어노테이션이 붙은 모든 메서드 로깅
     */
    @Around("@annotation(org.springframework.web.bind.annotation.ExceptionHandler)")
    public Object logExceptionHandler(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        Exception exception = extractException(joinPoint.getArgs());

        if (exception != null) {
            logException(exception, startTime);
        }

        return joinPoint.proceed();
    }

    /**
     * 파라미터에서 Exception 추출
     */
    private Exception extractException(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }

        for (Object arg : args) {
            if (arg instanceof Exception) {
                return (Exception) arg;
            }
        }

        return null;
    }

    /**
     * 예외 로깅
     */
    private void logException(Exception ex, long startTime) {
        try {
            long executionTime = System.currentTimeMillis() - startTime;

            Map<String, Object> logEntry = new LinkedHashMap<>();
            logEntry.put("@timestamp", LocalDateTime.now().atZone(ZoneOffset.UTC).format(ISO_FORMATTER));
            logEntry.put("trace_id", null);
            logEntry.put("level", "ERROR");
            logEntry.put("package", ex.getClass().getName());
            logEntry.put("layer", "CONTROLLER");
            logEntry.put("message", buildMessage(ex));
            logEntry.put("execution_time_ms", executionTime);
            logEntry.put("request", null);
            logEntry.put("response", null);

            // Exception 정보
            Map<String, Object> exceptionInfo = new LinkedHashMap<>();
            exceptionInfo.put("type", ex.getClass().getName());
            exceptionInfo.put("message", buildMessage(ex));

            // Validation 예외인 경우 상세 정보 추가
            if (ex instanceof MethodArgumentNotValidException) {
                addValidationErrors(exceptionInfo, (MethodArgumentNotValidException) ex);
            }

            // HTTP 정보
            addHttpInfo(exceptionInfo);

            exceptionInfo.put("stacktrace", getStackTrace(ex));

            logEntry.put("exception", exceptionInfo);

            log.error("{}", objectMapper.writeValueAsString(logEntry));

        } catch (Exception e) {
            log.error("예외 로깅 실패", e);
        }
    }

    /**
     * 예외 메시지 생성 (간결하게)
     */
    private String buildMessage(Exception ex) {
        if (ex instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException validEx = (MethodArgumentNotValidException) ex;
            int errorCount = validEx.getBindingResult().getErrorCount();
            return "Validation failed: " + errorCount + " error(s)";
        }
        return ex.getMessage();
    }

    /**
     * Validation 에러 상세 정보 추가
     */
    private void addValidationErrors(Map<String, Object> exceptionInfo,
                                     MethodArgumentNotValidException ex) {
        Map<String, String> validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> {
                            String message = fieldError.getDefaultMessage();
                            Object rejectedValue = fieldError.getRejectedValue();
                            return String.format("%s (rejected: %s)",
                                    message != null ? message : "Validation failed",
                                    rejectedValue);
                        },
                        (existing, replacement) -> existing
                ));
        exceptionInfo.put("validationErrors", validationErrors);
    }

    /**
     * HTTP 정보 추가
     */
    private void addHttpInfo(Map<String, Object> exceptionInfo) {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                Map<String, Object> httpInfo = new LinkedHashMap<>();
                httpInfo.put("method", request.getMethod());
                httpInfo.put("endpoint", request.getRequestURI());

                if (request.getQueryString() != null) {
                    httpInfo.put("queryString", request.getQueryString());
                }

                exceptionInfo.put("http", httpInfo);
            }
        } catch (Exception e) {
        }
    }

    /**
     * StackTrace 문자열 변환 (상위 3줄만)
     */
    private String getStackTrace(Throwable e) {
        if (e == null) {
            return null;
        }

        StackTraceElement[] stackTrace = e.getStackTrace();

        if (stackTrace == null || stackTrace.length == 0) {
            return e.toString();
        }

        StringBuilder sb = new StringBuilder();
        sb.append(e.toString()).append("\n");

        int limit = Math.min(3, stackTrace.length);
        for (int i = 0; i < limit; i++) {
            sb.append("\tat ").append(stackTrace[i].toString()).append("\n");
        }

        if (stackTrace.length > 3) {
            sb.append("\t... ").append(stackTrace.length - 3).append(" more");
        }

        return sb.toString();
    }
}
