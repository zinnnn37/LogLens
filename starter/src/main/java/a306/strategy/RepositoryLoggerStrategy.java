package a306.strategy;

import org.aspectj.lang.ProceedingJoinPoint;

public interface RepositoryLoggerStrategy {

    boolean supports(ProceedingJoinPoint joinPoint);

    int getOrder();

    // 이거 enum으로 처리 가능하겠다
    String getRepositoryType();

    Object logExecution(ProceedingJoinPoint joinPoint) throws Throwable;
}
