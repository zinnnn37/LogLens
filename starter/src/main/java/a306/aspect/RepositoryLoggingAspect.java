package a306.aspect;

import a306.strategy.RepositoryLoggerStrategy;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.List;

@Aspect
@Slf4j
@Component
public class RepositoryLoggingAspect {

    private final List<RepositoryLoggerStrategy> strategies;

    public RepositoryLoggingAspect(List<RepositoryLoggerStrategy> strategies) {
        this.strategies = strategies;

        // 우선순위 정렬
        this.strategies.sort((s1, s2) ->
                Integer.compare(s1.getOrder(), s2.getOrder()));

        log.info("=================================================");
        log.info("✅ Repository Logging Aspect 초기화");
        log.info("📋 등록된 전략: {} 개", strategies.size());
        for (RepositoryLoggerStrategy strategy : strategies) {
            log.info("  - {} (우선순위: {})",
                    strategy.getRepositoryType(),
                    strategy.getOrder());
        }
        log.info("=================================================");
    }

    /**
     * Repository 레이어의 모든 public 메서드
     */
    @Around("target(org.springframework.data.repository.Repository) && " +
            "execution(public * *(..))")
    public Object logRepositoryMethods(ProceedingJoinPoint joinPoint) throws Throwable {

        // 적용 가능한 전략 찾기
        for (RepositoryLoggerStrategy strategy : strategies) {
            if (strategy.supports(joinPoint)) {
                log.debug("🎯 전략 적용: {} for {}",
                        strategy.getRepositoryType(),
                        joinPoint.getSignature().toShortString());
                return strategy.logExecution(joinPoint);
            }
        }

        // 매칭되는 전략이 없으면 기본 실행
        log.debug("⚠️ 매칭되는 전략 없음: {}",
                joinPoint.getSignature().toShortString());
        return joinPoint.proceed();
    }
}
