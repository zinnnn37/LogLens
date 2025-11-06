package a306.dependency_logger_starter.logging.aspect;

import a306.dependency_logger_starter.logging.annotation.ExcludeValue;
import a306.dependency_logger_starter.logging.annotation.Sensitive;
import a306.dependency_logger_starter.logging.util.TypeChecker;
import a306.dependency_logger_starter.logging.util.ValueProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 메서드 실행 로깅 Aspect
 *
 * Controller, Service, Repository, Component 메서드 실행을 자동으로 로깅
 * - 요청/응답 데이터 수집
 * - @Sensitive, @ExcludeValue 마스킹 지원
 * - 실행 시간 측정
 * - HTTP 정보 수집 (Controller만)
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class MethodLoggingAspect {

    private final ObjectMapper objectMapper;

    @Value("${dependency.logger.stacktrace.max-lines:-1}")
    private int maxStackTraceLines;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    @Around("within(@org.springframework.web.bind.annotation.RestController *) && execution(public * *(..))")
    public Object logControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint);
    }

    @Around("within(@org.springframework.stereotype.Service *) && execution(public * *(..))")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint);
    }

    @Around("within(@org.springframework.stereotype.Repository *) && execution(public * *(..))")
    public Object logRepositoryMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint);
    }

    @Around("target(org.springframework.data.repository.Repository)")
    public Object logJpaRepositoryMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint);
    }

    @Around("within(@org.springframework.stereotype.Component *) && execution(public * *(..))")
    public Object logComponentMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint);
    }

    @Around("@annotation(a306.dependency_logger_starter.logging.annotation.LogMethodExecution)")
    public Object logAnnotatedMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint);
    }

    private Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> targetClass = ClassUtils.getUserClass(joinPoint.getTarget().getClass());

        String methodName = signature.getMethod().getName();
        String packageName = targetClass.getName();
        String componentName = targetClass.getSimpleName();
        String layer = detectLayer(targetClass);

        HttpInfo httpInfo = extractHttpInfo();
        Map<String, Object> parameters = collectParameters(signature, joinPoint.getArgs());

        logRequest(packageName, componentName, layer, methodName, parameters, httpInfo);

        Object result = null;
        Throwable exception = null;

        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            exception = e;
            throw e;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            Object responseData = (exception == null) ? collectResponse(result) : null;

            if (httpInfo != null) {
                httpInfo.updateStatusCode();
            }

            logResponse(packageName, componentName, layer, methodName, responseData, executionTime, exception, httpInfo);
        }

        return result;
    }

    private String detectLayer(Class<?> targetClass) {
        if (targetClass.isAnnotationPresent(org.springframework.web.bind.annotation.RestController.class)) {
            return "CONTROLLER";
        }
        if (targetClass.isAnnotationPresent(org.springframework.stereotype.Service.class)) {
            return "SERVICE";
        }
        if (targetClass.isAnnotationPresent(org.springframework.stereotype.Repository.class)) {
            return "REPOSITORY";
        }

        for (Class<?> interfaceClass : targetClass.getInterfaces()) {
            if (interfaceClass.getName().contains("Repository")) {
                return "REPOSITORY";
            }
        }

        return "UNKNOWN";
    }

    private HttpInfo extractHttpInfo() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return new HttpInfo(
                        request.getMethod(),
                        request.getRequestURI(),
                        request.getQueryString(),
                        attributes
                );
            }
        } catch (Exception e) {
            log.debug("HTTP 정보 추출 실패: {}", e.getMessage());
        }
        return null;
    }

    private void logRequest(String packageName, String componentName, String layer, String methodName,
                            Map<String, Object> parameters, HttpInfo httpInfo) {
        try {
            Map<String, Object> logEntry = createBaseLogEntry(packageName, componentName, layer);
            logEntry.put("message", "Request received: " + methodName);
            logEntry.put("execution_time_ms", null);

            Map<String, Object> request = new LinkedHashMap<>();
            if (httpInfo != null) {
                request.put("http", createHttpInfoMap(httpInfo, false));
            }
            request.put("method", methodName);
            request.put("parameters", parameters);

            logEntry.put("request", request);
            logEntry.put("response", null);
            logEntry.put("exception", null);

            log.info("{}", objectMapper.writeValueAsString(logEntry));

        } catch (Exception e) {
            log.error("REQUEST 로그 출력 실패", e);
        }
    }

    private void logResponse(String packageName, String componentName, String layer, String methodName,
                             Object responseData, Long executionTime, Throwable exception,
                             HttpInfo httpInfo) {
        try {
            Map<String, Object> logEntry = createBaseLogEntry(packageName, componentName, layer);
            logEntry.put("execution_time_ms", executionTime);
            logEntry.put("request", null);

            if (exception != null) {
                logEntry.put("level", "ERROR");
                logEntry.put("message", "Failed to execute " + methodName + ": " + exception.getMessage());
                logEntry.put("response", null);
                logEntry.put("exception", createExceptionInfo(exception));

                log.error("{}", objectMapper.writeValueAsString(logEntry));

            } else {
                logEntry.put("level", "INFO");
                logEntry.put("message", "Response completed: " + methodName);

                Map<String, Object> response = new LinkedHashMap<>();
                if (httpInfo != null) {
                    response.put("http", createHttpInfoMap(httpInfo, true));
                }
                response.put("method", methodName);
                response.put("result", responseData);

                logEntry.put("response", response);
                logEntry.put("exception", null);

                log.info("{}", objectMapper.writeValueAsString(logEntry));
            }

        } catch (Exception e) {
            log.error("RESPONSE 로그 출력 실패", e);
        }
    }

    private Map<String, Object> createBaseLogEntry(String packageName, String componentName, String layer) {
        Map<String, Object> logEntry = new LinkedHashMap<>();
        logEntry.put("@timestamp", LocalDateTime.now().atZone(ZoneOffset.UTC).format(ISO_FORMATTER));
        logEntry.put("trace_id", MDC.get("traceId"));
        logEntry.put("client_ip", MDC.get("client_ip"));
        logEntry.put("level", "INFO");
        logEntry.put("package", packageName);
        logEntry.put("component_name", componentName);
        logEntry.put("layer", layer);
        return logEntry;
    }

    private Map<String, Object> createHttpInfoMap(HttpInfo httpInfo, boolean includeStatusCode) {
        Map<String, Object> http = new LinkedHashMap<>();
        http.put("method", httpInfo.method);
        http.put("endpoint", httpInfo.uri);

        if (includeStatusCode && httpInfo.statusCode != null) {
            http.put("statusCode", httpInfo.statusCode);
        }
        if (httpInfo.queryString != null) {
            http.put("queryString", httpInfo.queryString);
        }

        return http;
    }

    private Map<String, Object> createExceptionInfo(Throwable exception) {
        Map<String, Object> exceptionInfo = new LinkedHashMap<>();
        exceptionInfo.put("type", exception.getClass().getName());
        exceptionInfo.put("message", exception.getMessage());
        exceptionInfo.put("stacktrace", getStackTrace(exception));
        return exceptionInfo;
    }

    private Map<String, Object> collectParameters(MethodSignature signature, Object[] args) {
        Map<String, Object> parameters = new LinkedHashMap<>();

        if (args == null || args.length == 0) {
            return parameters;
        }

        String[] parameterNames = signature.getParameterNames();
        Class<?>[] parameterTypes = signature.getParameterTypes();
        Annotation[][] parameterAnnotations = signature.getMethod().getParameterAnnotations();

        for (int i = 0; i < args.length; i++) {
            String paramName = (parameterNames != null && i < parameterNames.length)
                    ? parameterNames[i]
                    : "arg" + i;

            if (TypeChecker.isFrameworkClass(parameterTypes[i])) {
                continue;
            }

            Annotation[] annotations = parameterAnnotations[i];

            if (hasAnnotation(annotations, ExcludeValue.class)) {
                parameters.put(paramName, ValueProcessor.getExcludedValue());
                continue;
            }

            if (hasAnnotation(annotations, Sensitive.class)) {
                parameters.put(paramName, ValueProcessor.getMaskedValue());
                continue;
            }

            parameters.put(paramName, ValueProcessor.processValue(args[i]));
        }

        return parameters;
    }

    private Object collectResponse(Object result) {
        if (result == null) return null;
        if (result instanceof Void) return "void";

        if (result.getClass().getName().contains("ResponseEntity")) {
            try {
                Method getBodyMethod = result.getClass().getMethod("getBody");
                Object body = getBodyMethod.invoke(result);
                return collectResponse(body);
            } catch (Exception e) {
                log.debug("ResponseEntity body 추출 실패", e);
            }
        }

        String resultStr = result.toString();
        if (resultStr.length() > 1000) {
            return result.getClass().getSimpleName() + " [truncated]";
        }

        return ValueProcessor.processValue(result);
    }

    private boolean hasAnnotation(Annotation[] annotations, Class<? extends Annotation> annotationClass) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType() == annotationClass) {
                return true;
            }
        }
        return false;
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

    private static class HttpInfo {
        final String method;
        final String uri;
        final String queryString;
        Integer statusCode;
        final ServletRequestAttributes attributes;

        HttpInfo(String method, String uri, String queryString, ServletRequestAttributes attributes) {
            this.method = method;
            this.uri = uri;
            this.queryString = queryString;
            this.attributes = attributes;
            this.statusCode = null;
        }

        void updateStatusCode() {
            try {
                if (attributes != null && attributes.getResponse() != null) {
                    this.statusCode = attributes.getResponse().getStatus();
                }
            } catch (Exception e) {
                // 무시
            }
        }
    }
}
