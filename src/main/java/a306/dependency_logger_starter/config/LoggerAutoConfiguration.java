package a306.dependency_logger_starter.config;

import a306.dependency_logger_starter.dependency.DependencyCollector;
import a306.dependency_logger_starter.dependency.client.DependencyLogSender;
import a306.dependency_logger_starter.logging.aspect.ExceptionHandlerLoggingAspect;
import a306.dependency_logger_starter.logging.aspect.MethodLoggingAspect;
import a306.dependency_logger_starter.logging.async.AsyncExecutor;
import a306.dependency_logger_starter.logging.async.MDCTaskDecorator;
import a306.dependency_logger_starter.logging.filter.FrontendLogFilter;
import a306.dependency_logger_starter.logging.filter.TraceIdFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 자동 설정
 */
@AutoConfiguration
public class LoggerAutoConfiguration {

    /**
     * ObjectMapper Bean (JSON 변환용)
     */
    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Bean
    @ConditionalOnMissingBean
    public AsyncExecutor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setTaskDecorator(new MDCTaskDecorator());
        executor.setThreadNamePrefix("async-");
        executor.initialize();

        return new AsyncExecutor(executor);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "dependency.logger.trace",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public FilterRegistrationBean<TraceIdFilter> traceIdFilter() {
        FilterRegistrationBean<TraceIdFilter> registration =
                new FilterRegistrationBean<>();

        registration.setFilter(new TraceIdFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setName("traceIdFilter");

        return registration;
    }

    /**
     * Collector로 의존성 정보를 전송하는 클라이언트
     */
    @Bean
    public DependencyLogSender dependencyLogSender(
            @Value("${dependency.logger.collector.url:http://localhost:8081}") String collectorUrl,
            @Value("${dependency.logger.api-key}") String apiKey,
            @Value("${dependency.logger.sender.enabled:true}") boolean enabled) {
        return new DependencyLogSender(collectorUrl, apiKey, enabled);
    }

    /**
     * 의존성 수집기 (ApplicationReadyEvent 사용)
     */
    @Bean
    public DependencyCollector dependencyCollector(
            ApplicationContext applicationContext,
            ObjectMapper objectMapper,
            DependencyLogSender sender) {
        return new DependencyCollector(applicationContext, objectMapper, sender);
    }

    /**
     * 메서드 로깅 Aspect
     *
     * 스택트레이스 설정:
     * - dependency.logger.stacktrace.max-lines: -1 (기본값: 전체 출력)
     *   * -1: 전체 스택트레이스 (개발 환경 권장)
     *   * 0: 스택트레이스 출력 안함
     *   * N: 상위 N줄만 출력 (운영 환경)
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "dependency.logger.method-execution",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public MethodLoggingAspect methodLoggingAspect(ObjectMapper objectMapper) {
        return new MethodLoggingAspect(objectMapper);
    }

    /**
     * Exception Handler 로깅 Aspect
     * 사용자의 @ExceptionHandler 메서드 실행 시 예외를 자동으로 로깅
     *
     * 스택트레이스 설정:
     * - dependency.logger.stacktrace.max-lines: -1 (기본값: 전체 출력)
     *   * -1: 전체 스택트레이스 (개발 환경 권장)
     *   * 0: 스택트레이스 출력 안함
     *   * N: 상위 N줄만 출력 (운영 환경)
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "dependency.logger.exception-handler",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public ExceptionHandlerLoggingAspect exceptionHandlerLoggingAspect(ObjectMapper objectMapper) {
        return new ExceptionHandlerLoggingAspect(objectMapper);
    }

    /**
     * 프론트엔드 로그 수집 필터
     * POST /api/logs/frontend 요청을 처리하여 yml 설정 경로에 로그 저장
     *
     * 로그 경로: ${dependency.logger.frontend.log-path}
     * 기본값: ./logs/fe/app.log
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "dependency.logger.frontend",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = false
    )
    public FilterRegistrationBean<FrontendLogFilter> frontendLogFilter(
            @Value("${dependency.logger.frontend.log-path:./logs/fe/app.log}") String frontendLogPath) {

        FilterRegistrationBean<FrontendLogFilter> registration =
                new FilterRegistrationBean<>();

        registration.setFilter(new FrontendLogFilter(frontendLogPath));
        registration.addUrlPatterns("/api/logs/frontend");
        registration.setOrder(Ordered.LOWEST_PRECEDENCE);
        registration.setName("frontendLogFilter");

        return registration;
    }
}
