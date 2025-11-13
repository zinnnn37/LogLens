package S13P31A306.loglens.global.client;

import S13P31A306.loglens.domain.log.dto.ai.AiAnalysisResponse;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

//@formatter:off
/**
 * AI 서비스 REST API 클라이언트
 * WebClient를 사용하여 AI 로그 분석 서비스와 통신합니다.
 * WebClient 인스턴스를 재사용하여 리소스 효율성을 높입니다.
 */
//@formatter:on
@Slf4j
@Component
public class AiServiceClient {

    private static final String LOG_PREFIX = "[AiServiceClient]";
    private static final String AI_API_V2_LANGGRAPH_LOGS_PATH = "/api/v2-langgraph/logs";

    private final WebClient webClient;
    private final int timeout;

    public AiServiceClient(
            WebClient.Builder webClientBuilder,
            @Value("${ai.service.base-url}") String aiServiceBaseUrl,
            @Value("${ai.service.timeout}") int timeout) {
        this.webClient = webClientBuilder
                .baseUrl(aiServiceBaseUrl)
                .build();
        this.timeout = timeout;
    }

    /**
     * 로그 AI 분석 요청 AI 서비스의 GET /api/v2-langgraph/logs/{log_id}/analysis 엔드포인트를 호출합니다.
     * LangGraph 기반 V2 API를 사용하여 3-tier 캐싱, Map-Reduce 처리, 구조화된 워크플로우를 활용합니다.
     *
     * @param logId       분석할 로그 ID
     * @param projectUuid 프로젝트 UUID (멀티테넌시, Query Parameter로 전달)
     * @return AI 분석 결과 응답 DTO, 실패 시 null
     */
    public AiAnalysisResponse analyzeLog(Long logId, String projectUuid) {
        log.debug("{} 🤖 AI 로그 분석 요청: logId={}, projectUuid={}", LOG_PREFIX, logId, projectUuid);

        try {
            AiAnalysisResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(AI_API_V2_LANGGRAPH_LOGS_PATH + "/{log_id}/analysis")
                            .queryParam("project_uuid", projectUuid)
                            .build(logId))
                    .retrieve()
                    .bodyToMono(AiAnalysisResponse.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            if (response != null) {
                log.info("{} ✅ AI 분석 완료: logId={}, fromCache={}, analysisType={}",
                        LOG_PREFIX, logId, response.getFromCache(),
                        response.getAnalysis() != null ? response.getAnalysis().getAnalysisType() : "null");
            }
            return response;

        } catch (WebClientResponseException e) {
            log.error("{} 🔴 AI 분석 API 호출 실패: logId={}, status={}, body={}",
                    LOG_PREFIX, logId, e.getStatusCode(), e.getResponseBodyAsString());

            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("{} ⚠️ 로그를 찾을 수 없음: logId={}, projectUuid={}", LOG_PREFIX, logId, projectUuid);
            } else if (e.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
                log.error("{} ⚠️ AI 서비스 내부 오류: logId={}", LOG_PREFIX, logId);
            }
            return null;

        } catch (Exception e) {
            log.error("{} 🔴 AI 분석 중 예외 발생: logId={}, error={}", LOG_PREFIX, logId, e.getMessage(), e);
            return null;
        }
    }
}
