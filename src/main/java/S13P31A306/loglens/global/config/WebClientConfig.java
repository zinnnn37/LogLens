package S13P31A306.loglens.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient 설정
 * 외부 API 호출 (Jira 등)을 위한 WebClient Bean 등록
 */
@Configuration
public class WebClientConfig {

    /**
     * WebClient.Builder Bean 등록
     * 각 서비스에서 필요에 따라 Builder를 주입받아 WebClient를 생성할 수 있습니다.
     *
     * @return WebClient.Builder
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    }
}
