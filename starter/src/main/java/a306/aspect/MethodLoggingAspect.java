package a306.aspect;

import a306.util.LayerDetector;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 메서드 실행 로깅 Aspect (HTTP 정보 포함)
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class MethodLoggingAspect {

    private final ObjectMapper objectMapper;

    @Value("${spring.application.name:unknown-project}")
    private String projectName;

    // 상수
    private static final int MAX_COLLECTION_SIZE = 100;
    private static final int MAX_STRING_LENGTH = 500;
    private static final int MAX_FIELD_LENGTH = 200;
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

    @Around("target(org.springframework.data.repository.Repository)")
    public Object logJpaRepositoryMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return logMethodExecution(joinPoint);
    }

    /**
     * @LogMethodExecution 어노테이션
     */
    @Around("@annotation(a306.annotation.LogMethodExecution)")
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
        String logger = getFullClassName(targetClass);
        String layer = LayerDetector.detectLayer(targetClass);

        // HTTP 정보 추출
        HttpInfo httpInfo = extractHttpInfo();

        // 파라미터 수집
        Map<String, Object> parameters = collectParameters(signature, joinPoint.getArgs());

        // REQUEST 로그 출력
        logRequest(logger, methodName, layer, parameters, httpInfo);

        Object result = null;
        Throwable exception = null;

        try {
            // 메서드 실행
            result = joinPoint.proceed();

        } catch (Throwable e) {
            exception = e;
            throw e;

        } finally {
            long executionTime = System.currentTimeMillis() - startTime;

            // 응답 수집
            Object responseData = (exception == null) ? collectResponse(result) : null;

            // 응답 상태 코드 업데이트
            if (httpInfo != null) {
                httpInfo.updateStatusCode();
            }

            // RESPONSE 로그 출력
            logResponse(logger, methodName, layer, responseData, executionTime, exception, httpInfo);
        }

        return result;
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
                        request.getMethod(),           // GET, POST, PUT, DELETE
                        request.getRequestURI(),       // /users/1
                        request.getQueryString(),      // ?page=1&size=10
                        attributes                     // 나중에 상태 코드 추출용
                );
            }
        } catch (Exception e) {
            log.debug("HTTP 정보 추출 실패 (Service/Repository 계층일 수 있음): {}", e.getMessage());
        }
        return null;
    }

    /**
     * REQUEST 로그 출력
     */
    private void logRequest(String logger, String methodName, String layer,
                            Map<String, Object> parameters, HttpInfo httpInfo) {
        try {
            Map<String, Object> logEntry = new LinkedHashMap<>();
            logEntry.put("@timestamp", LocalDateTime.now().atZone(ZoneOffset.UTC).format(ISO_FORMATTER));
            logEntry.put("trace_id", null); // TODO: TraceContext 추가 시
            logEntry.put("level", "INFO");
            logEntry.put("logger", logger);
            logEntry.put("message", "Request received: " + methodName);
            logEntry.put("layer", layer);

            Map<String, Object> request = new LinkedHashMap<>();

            // HTTP 정보 추가
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

            String json = objectMapper.writeValueAsString(logEntry);
            log.info("{}", json);

        } catch (Exception e) {
            log.error("REQUEST 로그 출력 실패", e);
        }
    }

    /**
     * RESPONSE 로그 출력
     */
    private void logResponse(String logger, String methodName, String layer,
                             Object responseData, Long executionTime, Throwable exception,
                             HttpInfo httpInfo) {
        try {
            Map<String, Object> logEntry = new LinkedHashMap<>();
            logEntry.put("@timestamp", LocalDateTime.now().atZone(ZoneOffset.UTC).format(ISO_FORMATTER));
            logEntry.put("trace_id", null); // TODO: TraceContext 추가 시
            logEntry.put("logger", logger);
            logEntry.put("layer", layer);
            logEntry.put("request", null);
            logEntry.put("execution_time_ms", executionTime);

            if (exception != null) {
                // 에러 발생
                logEntry.put("level", "ERROR");
                logEntry.put("message", "Failed to " + methodName + ": " + exception.getMessage());

                Map<String, Object> exceptionInfo = new LinkedHashMap<>();
                exceptionInfo.put("type", exception.getClass().getName());
                exceptionInfo.put("message", exception.getMessage());
                exceptionInfo.put("stacktrace", getStackTrace(exception));
                logEntry.put("exception", exceptionInfo);

                logEntry.put("response", null);

                String json = objectMapper.writeValueAsString(logEntry);
                log.error("{}", json);

            } else {
                // 정상 완료
                logEntry.put("level", "INFO");
                logEntry.put("message", "Response completed: " + methodName);

                Map<String, Object> response = new LinkedHashMap<>();

                // HTTP 정보 추가
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

                String json = objectMapper.writeValueAsString(logEntry);
                log.info("{}", json);
            }

        } catch (Exception e) {
            log.error("RESPONSE 로그 출력 실패", e);
        }
    }

    /**
     * 파라미터 수집
     */
    private Map<String, Object> collectParameters(MethodSignature signature, Object[] args) {
        Map<String, Object> parameters = new LinkedHashMap<>();

        if (args == null || args.length == 0) {
            return parameters;
        }

        String[] parameterNames = signature.getParameterNames();
        Class<?>[] parameterTypes = signature.getParameterTypes();

        for (int i = 0; i < args.length; i++) {
            String paramName = (parameterNames != null && i < parameterNames.length)
                    ? parameterNames[i]
                    : "arg" + i;

            Class<?> paramType = parameterTypes[i];

            // Framework 객체 제외
            if (isFrameworkClass(paramType)) {
                continue;
            }

            Object paramValue = args[i];
            Object processedValue = processParameterValue(paramName, paramValue);
            parameters.put(paramName, processedValue);
        }

        return parameters;
    }

    /**
     * 파라미터 값 처리 (타입별)
     */
    private Object processParameterValue(String paramName, Object value) {
        if (value == null) {
            return null;
        }

        // 민감 정보 체크
        if (isSensitiveField(paramName)) {
            return "[REDACTED]";
        }

        Class<?> valueClass = value.getClass();

        // 1. Primitive/Wrapper/String
        if (isPrimitiveOrWrapper(valueClass) || value instanceof String) {
            return sanitizeStringValue(value);
        }

        // 2. Enum
        if (value instanceof Enum) {
            return ((Enum<?>) value).name();
        }

        // 3. Collection (List, Set)
        if (value instanceof Collection) {
            return processCollection((Collection<?>) value);
        }

        // 4. Map
        if (value instanceof Map) {
            return processMap((Map<?, ?>) value);
        }

        // 5. Array
        if (valueClass.isArray()) {
            return processArray(value);
        }

        // 6. Entity
        if (isEntity(valueClass)) {
            return processEntity(value);
        }

        // 7. DTO/POJO
        if (isDto(valueClass)) {
            return processDtoToPrimitive(value);
        }

        // 8. 기타 (toString)
        return truncateString(value.toString(), MAX_FIELD_LENGTH);
    }

    /**
     * 응답 수집
     */
    private Object collectResponse(Object result) {
        if (result == null) {
            return null;
        }

        // void 메서드
        if (result instanceof Void) {
            return "void";
        }

        // ResponseEntity 처리
        if (result.getClass().getName().contains("ResponseEntity")) {
            try {
                Method getBodyMethod = result.getClass().getMethod("getBody");
                Object body = getBodyMethod.invoke(result);
                return collectResponse(body); // 재귀
            } catch (Exception e) {
                log.debug("ResponseEntity body 추출 실패", e);
            }
        }

        Class<?> resultClass = result.getClass();

        // Primitive/Wrapper/String
        if (isPrimitiveOrWrapper(resultClass) || result instanceof String) {
            return sanitizeStringValue(result);
        }

        // Enum
        if (result instanceof Enum) {
            return ((Enum<?>) result).name();
        }

        // Collection
        if (result instanceof Collection) {
            Collection<?> collection = (Collection<?>) result;
            if (collection.size() > MAX_COLLECTION_SIZE) {
                return collection.getClass().getSimpleName() + "[" + collection.size() + " items]";
            }
            return result;
        }

        // Map
        if (result instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) result;
            if (map.size() > MAX_COLLECTION_SIZE) {
                return "Map[" + map.size() + " entries]";
            }
            return result;
        }

        // DTO/POJO
        if (isDto(resultClass)) {
            return processDtoToPrimitive(result);
        }

        // 큰 객체 (1000자 초과)
        String resultStr = result.toString();
        if (resultStr.length() > 1000) {
            return resultClass.getSimpleName() + " [truncated]";
        }

        return result;
    }

    /**
     * Collection 처리
     */
    private Object processCollection(Collection<?> collection) {
        if (collection.isEmpty()) {
            return List.of();
        }

        List<Object> processed = new ArrayList<>();
        int count = 0;

        for (Object item : collection) {
            if (count >= MAX_COLLECTION_SIZE) {
                processed.add("... (" + (collection.size() - MAX_COLLECTION_SIZE) + " more items)");
                break;
            }
            processed.add(processParameterValue("item", item));
            count++;
        }

        return processed;
    }

    /**
     * Map 처리
     */
    private Object processMap(Map<?, ?> map) {
        if (map.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> processed = new LinkedHashMap<>();
        int count = 0;

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (count >= MAX_COLLECTION_SIZE) {
                processed.put("...", "(" + (map.size() - MAX_COLLECTION_SIZE) + " more entries)");
                break;
            }

            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            processed.put(key, processParameterValue(key, value));
            count++;
        }

        return processed;
    }

    /**
     * Array 처리
     */
    private Object processArray(Object array) {
        int length = Array.getLength(array);

        if (length == 0) {
            return List.of();
        }

        List<Object> processed = new ArrayList<>();

        for (int i = 0; i < Math.min(length, MAX_COLLECTION_SIZE); i++) {
            Object item = Array.get(array, i);
            processed.add(processParameterValue("item", item));
        }

        if (length > MAX_COLLECTION_SIZE) {
            processed.add("... (" + (length - MAX_COLLECTION_SIZE) + " more items)");
        }

        return processed;
    }

    /**
     * Entity 처리
     */
    private Object processEntity(Object entity) {
        try {
            Method getIdMethod = entity.getClass().getMethod("getId");
            Object id = getIdMethod.invoke(entity);
            return entity.getClass().getSimpleName() + "(id=" + id + ")";
        } catch (Exception e) {
            return entity.getClass().getSimpleName() + "(?)";
        }
    }

    /**
     * DTO를 Map으로 변환
     */
    private Object processDtoToPrimitive(Object dto) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.convertValue(dto, Map.class);

            Map<String, Object> sanitized = new LinkedHashMap<>();

            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String fieldName = entry.getKey();
                Object fieldValue = entry.getValue();

                if (isSensitiveField(fieldName)) {
                    sanitized.put(fieldName, "[REDACTED]");
                } else {
                    sanitized.put(fieldName, sanitizeFieldValue(fieldValue));
                }
            }

            return sanitized;

        } catch (Exception e) {
            log.debug("DTO 변환 실패: {}", dto.getClass().getName(), e);
            return truncateString(dto.toString(), MAX_FIELD_LENGTH);
        }
    }

    /**
     * 필드 값 정리
     */
    private Object sanitizeFieldValue(Object value) {
        if (value == null) {
            return null;
        }

        // Primitive/Wrapper/String
        if (isPrimitiveOrWrapper(value.getClass()) || value instanceof String) {
            return truncateString(String.valueOf(value), MAX_STRING_LENGTH);
        }

        // Collection
        if (value instanceof Collection) {
            Collection<?> collection = (Collection<?>) value;
            if (collection.size() > 10) {
                return collection.getClass().getSimpleName() + "[" + collection.size() + " items]";
            }
            return value;
        }

        // Map
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            if (map.size() > 10) {
                return "Map[" + map.size() + " entries]";
            }
            return value;
        }

        // 중첩 DTO
        return value.getClass().getSimpleName() + "[nested]";
    }

    /**
     * Framework 클래스 체크
     */
    private boolean isFrameworkClass(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }

        String className = clazz.getName();

        return className.startsWith("javax.servlet.") ||
                className.startsWith("jakarta.servlet.") ||
                className.startsWith("org.springframework.web.") ||
                className.startsWith("org.springframework.http.") ||
                className.startsWith("org.springframework.ui.") ||
                className.startsWith("org.springframework.validation.");
    }

    /**
     * Primitive 또는 Wrapper 타입 체크
     */
    private boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return clazz.isPrimitive() ||
                clazz == Boolean.class ||
                clazz == Byte.class ||
                clazz == Short.class ||
                clazz == Integer.class ||
                clazz == Long.class ||
                clazz == Float.class ||
                clazz == Double.class ||
                clazz == Character.class;
    }

    /**
     * Entity 체크
     */
    private boolean isEntity(Class<?> clazz) {
        try {
            // Jakarta (Spring Boot 3.x)
            Class<?> entityAnnotation = Class.forName("jakarta.persistence.Entity");
            if (clazz.isAnnotationPresent((Class) entityAnnotation)) {
                return true;
            }
        } catch (ClassNotFoundException e) {
            // Jakarta 없음
        }

        try {
            // Javax (Spring Boot 2.x)
            Class<?> entityAnnotation = Class.forName("javax.persistence.Entity");
            if (clazz.isAnnotationPresent((Class) entityAnnotation)) {
                return true;
            }
        } catch (ClassNotFoundException e) {
            // Javax 없음
        }

        return false;
    }

    /**
     * DTO/POJO 체크
     */
    private boolean isDto(Class<?> clazz) {
        String packageName = clazz.getPackage() != null ? clazz.getPackage().getName() : "";

        // Java/Spring 기본 패키지 제외
        if (packageName.startsWith("java.") ||
                packageName.startsWith("javax.") ||
                packageName.startsWith("jakarta.") ||
                packageName.startsWith("org.springframework.")) {
            return false;
        }

        // Entity가 아니면 DTO로 간주
        return !isEntity(clazz);
    }

    /**
     * 민감 필드 체크
     */
    private boolean isSensitiveField(String fieldName) {
        if (fieldName == null) {
            return false;
        }

        String lowerFieldName = fieldName.toLowerCase();

        return lowerFieldName.contains("password") ||
                lowerFieldName.contains("passwd") ||
                lowerFieldName.contains("pwd") ||
                lowerFieldName.contains("token") ||
                lowerFieldName.contains("secret") ||
                lowerFieldName.contains("apikey") ||
                lowerFieldName.contains("api_key") ||
                lowerFieldName.contains("authorization");
    }

    /**
     * 문자열 값 정리
     */
    private Object sanitizeStringValue(Object value) {
        if (value == null) {
            return null;
        }

        String str = String.valueOf(value);
        return truncateString(str, MAX_STRING_LENGTH);
    }

    /**
     * 문자열 자르기
     */
    private String truncateString(String str, int maxLength) {
        if (str == null) {
            return null;
        }

        if (str.length() <= maxLength) {
            return str;
        }

        return str.substring(0, maxLength) + "... [truncated]";
    }

    /**
     * 전체 클래스 경로 반환
     */
    private String getFullClassName(Class<?> clazz) {
        return clazz.getName();
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

        /**
         * 응답 시점에 상태 코드 업데이트
         */
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
