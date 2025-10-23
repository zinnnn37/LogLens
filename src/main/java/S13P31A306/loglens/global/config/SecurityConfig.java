package S13P31A306.loglens.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security Configuration
 * <p>
 * 애플리케이션의 보안 설정을 담당합니다.
 * 특정 엔드포인트는 인증 없이 접근 가능하도록 설정합니다.
 * </p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Security Filter Chain 설정
     * <p>
     * 엔드포인트별 접근 권한을 설정합니다:
     * - /health-check: 인증 없이 접근 가능 (Jenkins 및 모니터링 도구용)
     * - /actuator/health: 인증 없이 접근 가능 (Docker HEALTHCHECK용)
     * - /actuator/prometheus: 인증 없이 접근 가능 (Prometheus 모니터링용)
     * - 그 외 모든 요청: 인증 필요
     * </p>
     *
     * @param http HttpSecurity
     * @return SecurityFilterChain
     * @throws Exception 설정 중 예외 발생 시
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 보호 비활성화 (REST API이므로)
                .csrf(AbstractHttpConfigurer::disable)

                // 요청별 권한 설정
                .authorizeHttpRequests(authz -> authz
                        // Health Check 엔드포인트 - 인증 없이 접근 허용
                        .requestMatchers("/health-check").permitAll()

                        // Actuator 헬스 체크 엔드포인트 - 인증 없이 접근 허용
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()

                        // Prometheus 메트릭 엔드포인트 - 인증 없이 접근 허용
                        .requestMatchers("/actuator/prometheus").permitAll()

                        // Swagger UI 및 API 문서 - 인증 없이 접근 허용 (개발/테스트 환경)
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )

                // HTTP Basic 인증 활성화 (개발 단계)
                .httpBasic(httpBasic -> {});

        return http.build();
    }
}
