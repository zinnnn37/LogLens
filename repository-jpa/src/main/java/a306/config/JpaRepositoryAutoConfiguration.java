package a306.config;

import a306.strategy.JpaRepositoryLoggerStrategy;
import a306.strategy.RepositoryLoggerStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@Slf4j
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.data.jpa.repository.JpaRepository")
public class JpaRepositoryAutoConfiguration {

    @Bean
    @ConditionalOnProperty(
            prefix = "loglens.repository.jpa",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public RepositoryLoggerStrategy jpaRepositoryLoggerStrategy(ObjectMapper objectMapper) {
        log.info("✅ JPA Repository 로깅 전략 활성화");
        return new JpaRepositoryLoggerStrategy(objectMapper);
    }
}
