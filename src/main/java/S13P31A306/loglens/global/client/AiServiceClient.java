package S13P31A306.loglens.global.client;

import S13P31A306.loglens.domain.analysis.dto.ai.AiHtmlDocumentRequest;
import S13P31A306.loglens.domain.analysis.dto.ai.AiHtmlDocumentResponse;
import S13P31A306.loglens.domain.log.dto.ai.AiAnalysisResponse;
import S13P31A306.loglens.domain.statistics.dto.response.AIComparisonResponse;
import java.time.Duration;
import java.util.List;
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
    private static final String AI_API_V2_LANGGRAPH_ANALYSIS_PATH = "/api/v2-langgraph/analysis";
    private static final String AI_API_V2_LANGGRAPH_STATISTICS_PATH = "/api/v2-langgraph/statistics";
    private static final int DOCUMENT_GENERATION_TIMEOUT = 60000; // 60초
    private static final int STATISTICS_COMPARISON_TIMEOUT = 30000; // 30초

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

    /**
     * 프로젝트 분석 HTML 문서 생성 요청
     *
     * @param request AI 문서 생성 요청 DTO
     * @return AI 생성 HTML 문서 응답, 실패 시 null
     */
    public AiHtmlDocumentResponse generateProjectAnalysisHtml(AiHtmlDocumentRequest request) {
        log.debug("{} 🤖 프로젝트 분석 HTML 문서 생성 요청: projectUuid={}, format={}",
                LOG_PREFIX, request.getProjectUuid(), request.getFormat());

        try {
            AiHtmlDocumentResponse response = webClient.post()
                    .uri(AI_API_V2_LANGGRAPH_ANALYSIS_PATH + "/projects/html-document")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AiHtmlDocumentResponse.class)
                    .timeout(Duration.ofMillis(DOCUMENT_GENERATION_TIMEOUT))
                    .block();

            if (response != null) {
                log.info("{} ✅ 프로젝트 분석 HTML 생성 완료: projectUuid={}, sections={}, generationTime={}s",
                        LOG_PREFIX, request.getProjectUuid(),
                        response.getMetadata() != null ? response.getMetadata().getSectionsGenerated() : null,
                        response.getMetadata() != null ? response.getMetadata().getGenerationTime() : null);
            }
            return response;

        } catch (WebClientResponseException e) {
            log.error("{} 🔴 프로젝트 분석 HTML 생성 실패: projectUuid={}, status={}, body={}",
                    LOG_PREFIX, request.getProjectUuid(), e.getStatusCode(), e.getResponseBodyAsString());
            return null;

        } catch (Exception e) {
            log.error("{} 🔴 프로젝트 분석 HTML 생성 중 예외 발생: projectUuid={}, error={}",
                    LOG_PREFIX, request.getProjectUuid(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * 에러 분석 HTML 문서 생성 요청
     *
     * @param request AI 문서 생성 요청 DTO
     * @return AI 생성 HTML 문서 응답, 실패 시 null
     */
    public AiHtmlDocumentResponse generateErrorAnalysisHtml(AiHtmlDocumentRequest request) {
        log.debug("{} 🤖 에러 분석 HTML 문서 생성 요청: logId={}, format={}",
                LOG_PREFIX, request.getLogId(), request.getFormat());

        try {
            AiHtmlDocumentResponse response = webClient.post()
                    .uri(AI_API_V2_LANGGRAPH_ANALYSIS_PATH + "/errors/html-document")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AiHtmlDocumentResponse.class)
                    .timeout(Duration.ofMillis(DOCUMENT_GENERATION_TIMEOUT))
                    .block();

            if (response != null) {
                log.info("{} ✅ 에러 분석 HTML 생성 완료: logId={}, severity={}, generationTime={}s",
                        LOG_PREFIX, request.getLogId(),
                        response.getMetadata() != null ? response.getMetadata().getSeverity() : null,
                        response.getMetadata() != null ? response.getMetadata().getGenerationTime() : null);
            }
            return response;

        } catch (WebClientResponseException e) {
            log.error("{} 🔴 에러 분석 HTML 생성 실패: logId={}, status={}, body={}",
                    LOG_PREFIX, request.getLogId(), e.getStatusCode(), e.getResponseBodyAsString());
            return null;

        } catch (Exception e) {
            log.error("{} 🔴 에러 분석 HTML 생성 중 예외 발생: logId={}, error={}",
                    LOG_PREFIX, request.getLogId(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * HTML 검증 실패 피드백과 함께 문서 재생성 요청
     *
     * @param request            원본 요청
     * @param validationErrors   검증 에러 목록
     * @return 재생성된 HTML 문서 응답
     */
    public AiHtmlDocumentResponse regenerateWithFeedback(
            AiHtmlDocumentRequest request,
            List<String> validationErrors
    ) {
        log.info("{} 🔄 HTML 검증 실패로 재생성 요청: errors={}", LOG_PREFIX, validationErrors);

        // 재생성 피드백 추가
        request.setRegenerationFeedback(validationErrors);

        // 문서 타입에 따라 적절한 메서드 호출
        return switch (request.getDocumentType()) {
            case PROJECT_ANALYSIS -> generateProjectAnalysisHtml(request);
            case ERROR_ANALYSIS -> generateErrorAnalysisHtml(request);
        };
    }

    /**
     * AI vs DB 통계 비교 검증 요청
     * AI 서비스의 GET /api/v2-langgraph/statistics/compare 엔드포인트를 호출합니다.
     * LLM이 DB 쿼리를 대체할 수 있는 역량을 검증합니다.
     *
     * @param projectUuid 프로젝트 UUID
     * @param timeHours   분석 기간 (시간)
     * @param sampleSize  AI 분석용 샘플 크기
     * @return AI vs DB 비교 검증 결과, 실패 시 null
     */
    public AIComparisonResponse compareAiVsDbStatistics(String projectUuid, int timeHours, int sampleSize) {
        log.debug("{} 🤖 AI vs DB 통계 비교 요청: projectUuid={}, timeHours={}, sampleSize={}",
                LOG_PREFIX, projectUuid, timeHours, sampleSize);

        try {
            AIComparisonResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(AI_API_V2_LANGGRAPH_STATISTICS_PATH + "/compare")
                            .queryParam("project_uuid", projectUuid)
                            .queryParam("time_hours", timeHours)
                            .queryParam("sample_size", sampleSize)
                            .build())
                    .retrieve()
                    .bodyToMono(AIComparisonResponse.class)
                    .timeout(Duration.ofMillis(STATISTICS_COMPARISON_TIMEOUT))
                    .block();

            if (response != null) {
                // 응답 검증: 필수 필드가 null인지 확인
                if (isIncompleteResponse(response)) {
                    log.warn("{} ⚠️ AI 서비스로부터 불완전한 응답 수신: projectUuid={}, " +
                                    "dbStatistics={}, aiStatistics={}, accuracyMetrics={}",
                            LOG_PREFIX, projectUuid,
                            response.dbStatistics() != null ? "OK" : "NULL",
                            response.aiStatistics() != null ? "OK" : "NULL",
                            response.accuracyMetrics() != null ? "OK" : "NULL");

                    // 기본 메타데이터로 응답 보완
                    response = enrichIncompleteResponse(response, projectUuid, timeHours, sampleSize);
                }

                log.info("{} ✅ AI vs DB 통계 비교 완료: projectUuid={}, overallAccuracy={}%, canReplaceDb={}",
                        LOG_PREFIX, projectUuid,
                        response.accuracyMetrics() != null ? response.accuracyMetrics().overallAccuracy() : null,
                        response.verdict() != null ? response.verdict().canReplaceDb() : null);
            }
            return response;

        } catch (WebClientResponseException e) {
            log.error("{} 🔴 AI vs DB 통계 비교 실패: projectUuid={}, status={}, body={}",
                    LOG_PREFIX, projectUuid, e.getStatusCode(), e.getResponseBodyAsString());

            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("{} ⚠️ 로그 데이터 없음: projectUuid={}", LOG_PREFIX, projectUuid);
            }
            return null;

        } catch (Exception e) {
            log.error("{} 🔴 AI vs DB 통계 비교 중 예외 발생: projectUuid={}, error={}",
                    LOG_PREFIX, projectUuid, e.getMessage(), e);
            return null;
        }
    }

    /**
     * AI 비교 응답이 불완전한지 확인
     */
    private boolean isIncompleteResponse(AIComparisonResponse response) {
        return response.projectUuid() == null
                || response.dbStatistics() == null
                || response.aiStatistics() == null
                || response.accuracyMetrics() == null;
    }

    /**
     * 불완전한 응답을 요청 파라미터로 보완
     */
    private AIComparisonResponse enrichIncompleteResponse(
            AIComparisonResponse response,
            String projectUuid,
            int timeHours,
            int sampleSize) {

        return new AIComparisonResponse(
                response.projectUuid() != null ? response.projectUuid() : projectUuid,
                response.analysisPeriodHours() != null ? response.analysisPeriodHours() : timeHours,
                response.sampleSize() != null ? response.sampleSize() : sampleSize,
                response.analyzedAt() != null ? response.analyzedAt() : java.time.LocalDateTime.now(),
                response.dbStatistics(),
                response.aiStatistics(),
                response.accuracyMetrics(),
                response.verdict(),
                response.technicalHighlights()
        );
    }
}

