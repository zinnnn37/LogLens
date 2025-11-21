package S13P31A306.loglens.global.config.security;

import S13P31A306.loglens.domain.auth.jwt.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @ConfigurationProperties 클래스들을 Spring 컨테이너에 등록하기 위한 설정 클래스
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class PropertiesConfig {
}
