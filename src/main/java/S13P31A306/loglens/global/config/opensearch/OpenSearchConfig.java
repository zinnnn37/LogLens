package S13P31A306.loglens.global.config.opensearch;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenSearch 클라이언트 설정
 */
@Slf4j
@Configuration
public class OpenSearchConfig {

    @Value("${opensearch.host:localhost}")
    private String host;

    @Value("${opensearch.port:9200}")
    private int port;

    @Value("${opensearch.username:}")
    private String username;

    @Value("${opensearch.password:}")
    private String password;

    @Value("${opensearch.scheme:http}")
    private String scheme;

    @Bean
    public RestHighLevelClient openSearchClient() {
        log.info("🔧 OpenSearch 클라이언트 설정 시작: {}://{}:{}", scheme, host, port);

        RestClient.RestClientBuilder builder = RestClient.builder(
                new HttpHost(host, port, scheme)
        );

        // 인증이 필요한 경우
        if (username != null && !username.isEmpty()) {
            log.info("🔐 OpenSearch 인증 설정: username={}", username);
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password)
            );

            builder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
            );
        }

        // 타임아웃 설정
        builder.setRequestConfigCallback(requestConfigBuilder ->
                requestConfigBuilder
                        .setConnectTimeout(5000)      // 연결 타임아웃: 5초
                        .setSocketTimeout(60000)       // 소켓 타임아웃: 60초
        );

        log.info("✅ OpenSearch 클라이언트 설정 완료");
        return new RestHighLevelClient(builder);
    }
}
