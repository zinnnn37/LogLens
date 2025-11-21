package S13P31A306.loglens.domain.jira.client;

import S13P31A306.loglens.domain.jira.client.dto.JiraIssueRequest;
import S13P31A306.loglens.domain.jira.client.dto.JiraIssueResponse;
import S13P31A306.loglens.domain.jira.constants.JiraErrorCode;
import S13P31A306.loglens.global.annotation.Sensitive;
import S13P31A306.loglens.global.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Jira REST API 클라이언트 WebClient를 사용하여 Jira API v3와 통신합니다. WebClient 인스턴스를 캐싱하여 리소스 효율성을 높입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JiraApiClient {

    private static final String LOG_PREFIX = "[JiraApiClient]";
    private static final String JIRA_API_V3_BASE_PATH = "/rest/api/3";
    private static final int MAX_CACHE_SIZE = 50;

    private final WebClient.Builder webClientBuilder;

    // Jira URL + Email 조합별로 WebClient 캐싱 (LRU 방식)
    private final Map<String, WebClient> webClientCache = new ConcurrentHashMap<>();

    /**
     * Jira 연결 테스트 프로젝트 조회 API를 호출하여 인증 정보 및 프로젝트 키 유효성을 검증합니다.
     *
     * @param jiraUrl    Jira 인스턴스 URL
     * @param email      Jira 계정 이메일
     * @param apiToken   Jira API 토큰
     * @param projectKey Jira 프로젝트 키
     * @return 연결 성공 여부
     */
    public boolean testConnection(String jiraUrl, String email, @Sensitive String apiToken, String projectKey) {
        log.debug("{} 🔍 Jira 연결 테스트 시작: projectKey={}", LOG_PREFIX, projectKey);

        try {
            WebClient webClient = getOrCreateWebClient(jiraUrl, email, apiToken);

            webClient.get()
                    .uri(JIRA_API_V3_BASE_PATH + "/project/{key}", projectKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("{} ✅ Jira 연결 테스트 성공: projectKey={}", LOG_PREFIX, projectKey);
            return true;

        } catch (WebClientResponseException e) {
            log.error("{} 🔴 Jira 연결 테스트 실패: status={}, body={}",
                    LOG_PREFIX, e.getStatusCode(), e.getResponseBodyAsString());

            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.warn("{} ⚠️ Jira 인증 실패: 이메일 또는 API 토큰을 확인해주세요.", LOG_PREFIX);
            } else if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("{} ⚠️ Jira 프로젝트를 찾을 수 없습니다: projectKey={}", LOG_PREFIX, projectKey);
            }
            return false;

        } catch (Exception e) {
            log.error("{} 🔴 Jira 연결 테스트 중 예외 발생: {}", LOG_PREFIX, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Jira 이슈 생성
     *
     * @param jiraUrl  Jira 인스턴스 URL
     * @param email    Jira 계정 이메일
     * @param apiToken Jira API 토큰
     * @param request  이슈 생성 요청 DTO
     * @return 생성된 이슈 응답 DTO
     * @throws BusinessException Jira API 호출 실패 시
     */
    public JiraIssueResponse createIssue(
            String jiraUrl,
            String email,
            @Sensitive String apiToken,
            JiraIssueRequest request
    ) {
        log.debug("{} 🎫 Jira 이슈 생성 요청: summary={}", LOG_PREFIX, request.fields().summary());

        try {
            WebClient webClient = getOrCreateWebClient(jiraUrl, email, apiToken);

            JiraIssueResponse response = webClient.post()
                    .uri(JIRA_API_V3_BASE_PATH + "/issue")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(JiraIssueResponse.class)
                    .block();

            if (response != null) {
                log.info("{} ✅ Jira 이슈 생성 성공: key={}, id={}", LOG_PREFIX, response.key(), response.id());
            }
            return response;

        } catch (WebClientResponseException e) {
            log.error("{} 🔴 Jira 이슈 생성 실패: status={}, body={}",
                    LOG_PREFIX, e.getStatusCode(), e.getResponseBodyAsString());

            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new BusinessException(JiraErrorCode.JIRA_API_AUTHENTICATION_FAILED);
            } else if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new BusinessException(JiraErrorCode.JIRA_PROJECT_NOT_FOUND);
            } else {
                throw new BusinessException(JiraErrorCode.JIRA_API_ERROR);
            }

        } catch (Exception e) {
            log.error("{} 🔴 Jira 이슈 생성 중 예외 발생: {}", LOG_PREFIX, e.getMessage(), e);
            throw new BusinessException(JiraErrorCode.JIRA_API_ERROR);
        }
    }

    /**
     * WebClient 캐시에서 가져오거나 새로 생성 Jira URL + Email 조합으로 캐싱하여 재사용
     *
     * @param baseUrl  Jira 인스턴스 URL
     * @param email    Jira 계정 이메일
     * @param apiToken Jira API 토큰
     * @return 인증 헤더가 설정된 WebClient
     */
    private WebClient getOrCreateWebClient(String baseUrl, String email, @Sensitive String apiToken) {
        String cacheKey = baseUrl + ":" + email;

        return webClientCache.computeIfAbsent(cacheKey, key -> {
            // 캐시 크기 제한 (LRU 정책)
            if (webClientCache.size() >= MAX_CACHE_SIZE) {
                // 가장 오래된 항목 1개 제거
                String oldestKey = webClientCache.keySet().iterator().next();
                webClientCache.remove(oldestKey);
                log.debug("{} WebClient 캐시 정리: removed={}", LOG_PREFIX, oldestKey);
            }

            log.debug("{} WebClient 생성: cacheKey={}", LOG_PREFIX, cacheKey);
            return createWebClient(baseUrl, email, apiToken);
        });
    }

    /**
     * Jira Basic Auth용 WebClient 생성
     *
     * @param baseUrl  Jira 인스턴스 URL
     * @param email    Jira 계정 이메일
     * @param apiToken Jira API 토큰
     * @return 인증 헤더가 설정된 WebClient
     */
    private WebClient createWebClient(String baseUrl, String email, @Sensitive String apiToken) {
        String auth = email + ":" + apiToken;
        String encodedAuth = Base64.getEncoder()
                .encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        return webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth)
                .build();
    }
}
