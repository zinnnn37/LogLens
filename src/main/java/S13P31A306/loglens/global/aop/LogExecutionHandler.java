package S13P31A306.loglens.global.aop;

import S13P31A306.loglens.global.utils.MethodSignatureUtils;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.boot.logging.LogLevel;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LogExecutionHandler {
    private final LogTrace logTrace;

    /**
     * 주어진 로그 레벨로 메서드 실행을 처리하고 로깅합니다.
     */
    public Object executeWithLevel(final ProceedingJoinPoint joinPoint, final LogLevel level) throws Throwable {
        TraceStatus status = null;
        try {
            String methodSignature = MethodSignatureUtils.formatMethodSignature(joinPoint);
            status = logTrace.begin(methodSignature, level);
            Object result = joinPoint.proceed();
            logTrace.end(status, level);
            return result;
        } catch (Exception e) {
            logTrace.exception(status, e, level);
            throw e;
        }
    }
}
