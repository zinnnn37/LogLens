package a306.dependency_logger_starter.logging.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
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
 *
 * @ExceptionHandler 메서드 실행 시 자동으로 예외 로깅
 * - Validation 예외 상세 정보 수집
 * - HTTP 요청 정보 포함
 * - 스택트레이스 설정 가능 (전체/부분/없음)
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class ExceptionHandlerLoggingAspect {

    private final ObjectMapper objectMapper;

    @Value("${dependency.logger.stacktrace.max-lines:-1}")
    private int maxStackTraceLines;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    @Around("@annotation(org.springframework.web.bind.annotation.ExceptionHandler)")
    public Object logExceptionHandler(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Exception exception = extractException(joinPoint.getArgs());

        if (exception != null) {
            logException(exception, startTime);
        }

        return joinPoint.proceed();
    }

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

    private void logException(Exception ex, long startTime) {
        try {
            Map<String, Object> logEntry = createBaseLogEntry(ex, startTime);
            Map<String, Object> exceptionInfo = createExceptionInfo(ex);

            logEntry.put("exception", exceptionInfo);
            log.error("{}", objectMapper.writeValueAsString(logEntry));

        } catch (Exception e) {
            log.error("예외 로깅 실패", e);
        }
    }

    private Map<String, Object> createBaseLogEntry(Exception ex, long startTime) {
        Map<String, Object> logEntry = new LinkedHashMap<>();
        logEntry.put("@timestamp", LocalDateTime.now().atZone(ZoneOffset.UTC).format(ISO_FORMATTER));
        logEntry.put("trace_id", MDC.get("traceId"));
        logEntry.put("level", "ERROR");
        logEntry.put("package", ex.getClass().getName());
        logEntry.put("layer", "CONTROLLER");
        logEntry.put("message", buildMessage(ex));
        logEntry.put("execution_time_ms", System.currentTimeMillis() - startTime);
        logEntry.put("request", null);
        logEntry.put("response", null);
        return logEntry;
    }

    private Map<String, Object> createExceptionInfo(Exception ex) {
        Map<String, Object> exceptionInfo = new LinkedHashMap<>();
        exceptionInfo.put("type", ex.getClass().getName());
        exceptionInfo.put("message", buildMessage(ex));

        if (ex instanceof MethodArgumentNotValidException) {
            addValidationErrors(exceptionInfo, (MethodArgumentNotValidException) ex);
        }

        addHttpInfo(exceptionInfo);
        exceptionInfo.put("stacktrace", getStackTrace(ex));

        return exceptionInfo;
    }

    private String buildMessage(Exception ex) {
        if (ex instanceof MethodArgumentNotValidException validEx) {
            return "Validation failed: " + validEx.getBindingResult().getErrorCount() + " error(s)";
        }
        return ex.getMessage();
    }

    private void addValidationErrors(Map<String, Object> exceptionInfo, MethodArgumentNotValidException ex) {
        Map<String, String> validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> String.format("%s (rejected: %s)",
                                fieldError.getDefaultMessage() != null
                                        ? fieldError.getDefaultMessage()
                                        : "Validation failed",
                                fieldError.getRejectedValue()),
                        (existing, replacement) -> existing
                ));
        exceptionInfo.put("validationErrors", validationErrors);
    }

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
            log.debug("HTTP 정보 추출 실패: {}", e.getMessage());
        }
    }

    private String getStackTrace(Throwable e) {
        if (e == null) return null;

        StackTraceElement[] stackTrace = e.getStackTrace();
        if (stackTrace == null || stackTrace.length == 0) {
            return e.toString();
        }

        StringBuilder sb = new StringBuilder(e.toString()).append("\n");

        if (maxStackTraceLines == 0) {
            return sb.toString().trim();
        }

        int limit = (maxStackTraceLines == -1)
                ? stackTrace.length
                : Math.min(maxStackTraceLines, stackTrace.length);

        for (int i = 0; i < limit; i++) {
            sb.append("\tat ").append(stackTrace[i]).append("\n");
        }

        if (maxStackTraceLines != -1 && stackTrace.length > maxStackTraceLines) {
            sb.append("\t... ").append(stackTrace.length - maxStackTraceLines).append(" more");
        }

        return sb.toString();
    }
}
