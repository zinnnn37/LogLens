package S13P31A306.loglens.global.config.opensearch;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.security.cert.X509Certificate;
import java.util.Optional;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
public class OpenSearchConfig {

    private static final String LOG_PREFIX = "[OpenSearchConfig]";

    @Value("${opensearch.scheme:http}")
    private String scheme;
    @Value("${opensearch.host:localhost}")
    private String host;
    @Value("${opensearch.port:9200}")
    private int port;
    @Value("${opensearch.username:}")
    private String username;
    @Value("${opensearch.password:}")
    private String password;

    @Bean
    public OpenSearchClient openSearchClient() {
        HttpHost httpHost = new HttpHost(scheme, host, port);

        RestClientBuilder.HttpClientConfigCallback configCallback = createHttpClientConfigCallback();

        RestClient restClient = RestClient.builder(httpHost)
                .setHttpClientConfigCallback(configCallback)
                .build();

        // Jackson ObjectMapper 커스터마이징
        ObjectMapper objectMapper = createObjectMapper();

        log.info("{} OpenSearch 클라이언트 생성 완료 ({}://{}:{})", LOG_PREFIX, scheme, host, port);

        return new OpenSearchClient(new RestClientTransport(restClient, new JacksonJsonpMapper(objectMapper)));
    }

    /**
     * Jackson ObjectMapper 설정 분리
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // JavaTimeModule 등록 (LocalDateTime, ZonedDateTime 등 처리)
        objectMapper.registerModule(new JavaTimeModule());

        // 역직렬화 설정
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
        objectMapper.configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false);

        // 직렬화 설정 (날짜를 타임스탬프가 아닌 ISO 8601 문자열로)
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return objectMapper;
    }

    private RestClientBuilder.HttpClientConfigCallback createHttpClientConfigCallback() {
        return httpClientBuilder -> {
            TlsStrategy tlsStrategy = createSslStrategy();
            PoolingAsyncClientConnectionManager connectionManager = PoolingAsyncClientConnectionManagerBuilder.create()
                    .setTlsStrategy(tlsStrategy)
                    .build();
            httpClientBuilder.setConnectionManager(connectionManager);

            createCredentialsProvider().ifPresent(httpClientBuilder::setDefaultCredentialsProvider);

            return httpClientBuilder;
        };
    }

    private TlsStrategy createSslStrategy() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            log.info("{} OpenSearch 클라이언트의 SSL 인증서 검증을 비활성화", LOG_PREFIX);
            return ClientTlsStrategyBuilder.create()
                    .setSslContext(sslContext)
                    .setHostnameVerifier((hostname, session) -> true)
                    .build();
        } catch (Exception e) {
            log.error("{} OpenSearch 클라이언트의 SSL 설정", LOG_PREFIX, e);
            throw new RuntimeException("OpenSearch SSL 설정 실패", e);
        }
    }

    private Optional<BasicCredentialsProvider> createCredentialsProvider() {
        if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    new AuthScope(new HttpHost(scheme, host, port)),
                    new UsernamePasswordCredentials(username, password.toCharArray())
            );
            log.info("{} OpenSearch 클라이언트에 사용자 자격증명을 설정", LOG_PREFIX);
            return Optional.of(credentialsProvider);
        }
        return Optional.empty();
    }
}
