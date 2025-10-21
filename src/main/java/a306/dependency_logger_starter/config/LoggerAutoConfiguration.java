package a306.dependency_logger_starter.config;

import a306.dependency_logger_starter.dependency.DependencyCollector;
import a306.dependency_logger_starter.dependency.client.DependencyLogSender;
import a306.dependency_logger_starter.logging.aspect.MethodLoggingAspect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

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

    /**
     * Collector로 의존성 정보를 전송하는 클라이언트
     */
    @Bean
    public DependencyLogSender dependencyLogSender(
            @Value("${dependency.logger.collector.url:http://localhost:8081}") String collectorUrl,
            @Value("${dependency.logger.sender.enabled:true}") boolean enabled) {
        return new DependencyLogSender(collectorUrl, enabled);
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
}
