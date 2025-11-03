package S13P31A306.loglens.global.config.opensearch;

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

    private static final String LOG_REFIX = "[OpenSearchConfig]";

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

        return new OpenSearchClient(new RestClientTransport(restClient, new JacksonJsonpMapper()));
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

            log.info("{} OpenSearch 클라이언트의 SSL 인증서 검증을 비활성화", LOG_REFIX);
            return ClientTlsStrategyBuilder.create()
                    .setSslContext(sslContext)
                    .setHostnameVerifier((hostname, session) -> true)
                    .build();
        } catch (Exception e) {
            log.error("{} OpenSearch 클라이언트의 SSL 설정", LOG_REFIX, e);
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
            log.info("{} OpenSearch 클라이언트에 사용자 자격증명을 설정", LOG_REFIX);
            return Optional.of(credentialsProvider);
        }
        return Optional.empty();
    }
}
