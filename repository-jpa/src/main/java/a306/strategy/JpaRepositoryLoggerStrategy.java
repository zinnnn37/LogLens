package a306.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class JpaRepositoryLoggerStrategy implements RepositoryLoggerStrategy {

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(ProceedingJoinPoint joinPoint) {
        Class<?> targetClass = joinPoint.getTarget().getClass();

        // JpaRepository 인터페이스를 구현했는지 확인
        for (Class<?> intf : targetClass.getInterfaces()) {
            String intfName = intf.getName();
            if (intfName.contains("org.springframework.data.jpa.repository.JpaRepository") ||
                    intfName.contains("org.springframework.data.repository.Repository")) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    public String getRepositoryType() {
        return "JPA";
    }

    @Override
    public Object logExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getName();
        String className = extractClassName(joinPoint.getTarget().getClass());

        // 요청 로그
        logRequest(methodName, className, joinPoint.getArgs());

        Object result = null;
        String logLevel = "INFO";
        String errorMessage = null;

        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            logLevel = "ERROR";
            errorMessage = e.getMessage();
            throw e;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            logResponse(methodName, className, result, executionTime, logLevel, errorMessage);
        }

        return result;
    }

    private void logRequest(String methodName, String className, Object[] args) {
        Map<String, Object> log = new LinkedHashMap<>();
        log.put("@timestamp", Instant.now().toString());
        log.put("level", "INFO");
        log.put("logger", className);
        log.put("message", "Request received: " + methodName);
        log.put("layer", "REPOSITORY");
        log.put("repository_type", getRepositoryType());

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("method", methodName);
        request.put("parameters", formatParameters(args));
        log.put("request", request);

        logAsJson(log);
    }

    private void logResponse(String methodName, String className, Object result,
                             long executionTime, String logLevel, String errorMessage) {
        Map<String, Object> log = new LinkedHashMap<>();
        log.put("@timestamp", Instant.now().toString());
        log.put("logger", className);
        log.put("layer", "REPOSITORY");
        log.put("repository_type", getRepositoryType());
        log.put("execution_time_ms", executionTime);
        log.put("level", logLevel);
        log.put("message", logLevel.equals("ERROR")
                ? "Error occurred: " + methodName
                : "Response completed: " + methodName);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("method", methodName);

        if (logLevel.equals("ERROR")) {
            response.put("error", errorMessage);
        } else {
            response.put("result", formatResult(result));
        }

        log.put("response", response);
        logAsJson(log);
    }

    private Map<String, Object> formatParameters(Object[] args) {
        Map<String, Object> params = new LinkedHashMap<>();
        if (args == null || args.length == 0) return params;

        for (int i = 0; i < args.length; i++) {
            Object value = args[i];
            if (value == null) {
                params.put("arg" + i, null);
            } else {
                String className = value.getClass().getSimpleName();
                params.put("arg" + i, className + "(id=" + extractId(value) + ")");
            }
        }
        return params;
    }

    private Object formatResult(Object result) {
        if (result == null) return null;

        String simpleName = result.getClass().getSimpleName();
        if (simpleName.equals("Optional")) {
            return "Optional[" + (isOptionalEmpty(result) ? "empty" : "present") + "]";
        }

        if (result instanceof Iterable) {
            long count = 0;
            for (Object ignored : (Iterable<?>) result) count++;
            return "List[" + count + " items]";
        }

        return simpleName + "(id=" + extractId(result) + ")";
    }

    private Object extractId(Object entity) {
        try {
            var idMethod = entity.getClass().getMethod("getId");
            return idMethod.invoke(entity);
        } catch (Exception e) {
            return "unknown";
        }
    }

    private boolean isOptionalEmpty(Object optional) {
        try {
            var isEmptyMethod = optional.getClass().getMethod("isEmpty");
            return (boolean) isEmptyMethod.invoke(optional);
        } catch (Exception e) {
            return false;
        }
    }

    private String extractClassName(Class<?> clazz) {
        String name = clazz.getSimpleName();
        if (name.contains("$")) {
            name = name.substring(0, name.indexOf("$"));
        }
        return name;
    }

    private void logAsJson(Map<String, Object> logData) {
        try {
            String json = objectMapper.writeValueAsString(logData);
            log.info(json);
        } catch (Exception e) {
            log.error("로그 JSON 변환 실패", e);
        }
    }
}
