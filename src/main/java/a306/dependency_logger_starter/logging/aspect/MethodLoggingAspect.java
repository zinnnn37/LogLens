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
import org.springframework.util.ClassUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 메서드 실행 로깅 Aspect
 * @Sensitive, @ExcludeValue 어노테이션 기반 마스킹
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class MethodLoggingAspect {

    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    /**
     * Controller 메서드
     */
    @Around("within(@org.springframework.web.bind.annotation.RestController *) && execution(public * *(..))")
    public Object logControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint);
    }

    /**
     * Service 메서드
     */
    @Around("within(@org.springframework.stereotype.Service *) && execution(public * *(..))")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint);
    }

    /**
     * Repository 메서드
     */
    @Around("within(@org.springframework.stereotype.Repository *) && execution(public * *(..))")
    public Object logRepositoryMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint);
    }

    /**
     * JPA Repository 메서드
     */
    @Around("target(org.springframework.data.repository.Repository)")
    public Object logJpaRepositoryMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint);
    }

    /**
     * @LogMethodExecution 어노테이션
     */
    @Around("@annotation(a306.dependency_logger_starter.logging.annotation.LogMethodExecution)")
    public Object logAnnotatedMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint);
    }

    /**
     * 메서드 실행 로깅
     */
    private Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> targetClass = ClassUtils.getUserClass(joinPoint.getTarget().getClass());

        String methodName = signature.getMethod().getName();
        String logger = targetClass.getName();
        String componentName = extractComponentName(targetClass);

        HttpInfo httpInfo = extractHttpInfo();
        Map<String, Object> parameters = collectParameters(signature, joinPoint.getArgs());

        logRequest(logger, methodName, componentName, parameters, httpInfo);

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

            logResponse(logger, methodName, componentName, responseData, executionTime, exception, httpInfo);
        }

        return result;
    }

    /**
     * 컴포넌트 이름 추출 (프록시 처리)
     */
    private String extractComponentName(Class<?> targetClass) {
        String name = targetClass.getSimpleName();

        // CGLIB 프록시 suffix 제거
        if (name.contains("$$")) {
            name = name.substring(0, name.indexOf("$$"));
        }

        return name;
    }

    /**
     * HTTP 정보 추출
     */
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

    /**
     * REQUEST 로그 출력
     */
    private void logRequest(String logger, String methodName, String componentName,
                            Map<String, Object> parameters, HttpInfo httpInfo) {
        try {
            Map<String, Object> logEntry = new LinkedHashMap<>();
            logEntry.put("@timestamp", LocalDateTime.now().atZone(ZoneOffset.UTC).format(ISO_FORMATTER));
            logEntry.put("trace_id", null);
            logEntry.put("level", "INFO");
            logEntry.put("logger", logger);
            logEntry.put("message", "Request received: " + methodName);
            logEntry.put("component_name", componentName);

            Map<String, Object> request = new LinkedHashMap<>();

            if (httpInfo != null) {
                Map<String, Object> http = new LinkedHashMap<>();
                http.put("method", httpInfo.method);
                http.put("endpoint", httpInfo.uri);
                if (httpInfo.queryString != null) {
                    http.put("queryString", httpInfo.queryString);
                }
                request.put("http", http);
            }

            request.put("method", methodName);
            request.put("parameters", parameters);
            logEntry.put("request", request);
            logEntry.put("response", null);
            logEntry.put("execution_time_ms", null);

            log.info("{}", objectMapper.writeValueAsString(logEntry));

        } catch (Exception e) {
            log.error("REQUEST 로그 출력 실패", e);
        }
    }

    /**
     * RESPONSE 로그 출력
     */
    private void logResponse(String logger, String methodName, String componentName,
                             Object responseData, Long executionTime, Throwable exception,
                             HttpInfo httpInfo) {
        try {
            Map<String, Object> logEntry = new LinkedHashMap<>();
            logEntry.put("@timestamp", LocalDateTime.now().atZone(ZoneOffset.UTC).format(ISO_FORMATTER));
            logEntry.put("trace_id", null);
            logEntry.put("logger", logger);
            logEntry.put("component_name", componentName);
            logEntry.put("request", null);
            logEntry.put("execution_time_ms", executionTime);

            if (exception != null) {
                logEntry.put("level", "ERROR");
                logEntry.put("message", "Failed to " + methodName + ": " + exception.getMessage());

                Map<String, Object> exceptionInfo = new LinkedHashMap<>();
                exceptionInfo.put("type", exception.getClass().getName());
                exceptionInfo.put("message", exception.getMessage());
                exceptionInfo.put("stacktrace", getStackTrace(exception));
                logEntry.put("exception", exceptionInfo);
                logEntry.put("response", null);

                log.error("{}", objectMapper.writeValueAsString(logEntry));

            } else {
                logEntry.put("level", "INFO");
                logEntry.put("message", "Response completed: " + methodName);

                Map<String, Object> response = new LinkedHashMap<>();

                if (httpInfo != null) {
                    Map<String, Object> http = new LinkedHashMap<>();
                    http.put("method", httpInfo.method);
                    http.put("endpoint", httpInfo.uri);
                    if (httpInfo.statusCode != null) {
                        http.put("statusCode", httpInfo.statusCode);
                    }
                    response.put("http", http);
                }

                response.put("method", methodName);
                response.put("result", responseData);
                logEntry.put("response", response);

                log.info("{}", objectMapper.writeValueAsString(logEntry));
            }

        } catch (Exception e) {
            log.error("RESPONSE 로그 출력 실패", e);
        }
    }

    /**
     * 파라미터 수집 (어노테이션 기반)
     */
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

            // Framework 객체 제외
            if (TypeChecker.isFrameworkClass(parameterTypes[i])) {
                continue;
            }

            // 어노테이션 체크
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

    /**
     * 응답 수집
     */
    private Object collectResponse(Object result) {
        if (result == null) {
            return null;
        }

        if (result instanceof Void) {
            return "void";
        }

        // ResponseEntity 처리
        if (result.getClass().getName().contains("ResponseEntity")) {
            try {
                Method getBodyMethod = result.getClass().getMethod("getBody");
                Object body = getBodyMethod.invoke(result);
                return collectResponse(body);
            } catch (Exception e) {
                log.debug("ResponseEntity body 추출 실패", e);
            }
        }

        // Collection 크기 체크 (3개 이상이면 요약)
        if (result instanceof Collection) {
            Collection<?> collection = (Collection<?>) result;
            if (collection.size() >= 3) {
                return ValueProcessor.processValue(result);
            }
            return result;
        }

        // Map 크기 체크 (3개 이상이면 요약)
        if (result instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) result;
            if (map.size() >= 3) {
                return ValueProcessor.processValue(result);
            }
            return result;
        }

        // 큰 객체 처리
        String resultStr = result.toString();
        if (resultStr.length() > 1000) {
            return result.getClass().getSimpleName() + " [truncated]";
        }

        return ValueProcessor.processValue(result);
    }

    /**
     * 어노테이션 존재 여부 체크
     */
    private boolean hasAnnotation(Annotation[] annotations, Class<? extends Annotation> annotationClass) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType() == annotationClass) {
                return true;
            }
        }
        return false;
    }

    /**
     * StackTrace 문자열 변환
     */
    private String getStackTrace(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /**
     * HTTP 정보를 담는 내부 클래스
     */
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
