//package S13P31A306.loglens.global.client;
//
//import S13P31A306.loglens.domain.log.dto.ai.AiAnalysisDto;
//import S13P31A306.loglens.domain.log.dto.ai.AiAnalysisResponse;
//import okhttp3.mockwebserver .MockResponse;
//import okhttp3.mockwebserver.MockWebServer;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.web.reactive.function.client.WebClient;
//
//import java.io.IOException;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
///**
// * AiServiceClient 테스트
// * AI 서비스의 GET /api/v2/logs/{log_id}/analysis 엔드포인트를 테스트합니다.
// */
//class AiServiceClientTest {
//
//    private MockWebServer mockWebServer;
//    private AiServiceClient aiServiceClient;
//
//    @BeforeEach
//    void setUp() throws IOException {
//        mockWebServer = new MockWebServer();
//        mockWebServer.start();
//
//        String baseUrl = mockWebServer.url("/").toString();
//        WebClient.Builder webClientBuilder = WebClient.builder();
//
//        aiServiceClient = new AiServiceClient(webClientBuilder, baseUrl, 30000);
//    }
//
//    @AfterEach
//    void tearDown() throws IOException {
//        mockWebServer.shutdown();
//    }
//
//    @Test
//    @DisplayName("AI_로그_분석_성공_시_AiAnalysisResponse를_반환한다")
//    void AI_로그_분석_성공_시_AiAnalysisResponse를_반환한다() {
//        // given
//        Long logId = 1234567890L;
//        String projectUuid = "550e8400-e29b-41d4-a716-446655440000";
//
//        String responseBody = """
//                {
//                  "log_id": 1234567890,
//                  "analysis": {
//                    "summary": "데이터베이스 연결 오류가 발생했습니다.",
//                    "error_cause": "커넥션 풀이 고갈되었습니다.",
//                    "solution": "커넥션 풀 크기를 증가시키세요.",
//                    "tags": ["DATABASE", "CONNECTION"],
//                    "analysis_type": "TRACE_BASED",
//                    "target_type": "LOG",
//                    "analyzed_at": "2025-11-07T15:00:00.000Z"
//                  },
//                  "from_cache": false,
//                  "similar_log_id": null,
//                  "similarity_score": null
//                }
//                """;
//
//        mockWebServer.enqueue(new MockResponse()
//                .setBody(responseBody)
//                .addHeader("Content-Type", "application/json")
//                .setResponseCode(200));
//
//        // when
//        AiAnalysisResponse response = aiServiceClient.analyzeLog(logId, projectUuid);
//
//        // then
//        assertThat(response).isNotNull();
//        assertThat(response.getLogId()).isEqualTo(logId);
//        assertThat(response.getFromCache()).isFalse();
//        assertThat(response.getAnalysis()).isNotNull();
//        assertThat(response.getAnalysis().getSummary()).isEqualTo("데이터베이스 연결 오류가 발생했습니다.");
//        assertThat(response.getAnalysis().getErrorCause()).isEqualTo("커넥션 풀이 고갈되었습니다.");
//        assertThat(response.getAnalysis().getTags()).containsExactly("DATABASE", "CONNECTION");
//    }
//
//    @Test
//    @DisplayName("AI_분석_캐시_사용_시_fromCache가_true이다")
//    void AI_분석_캐시_사용_시_fromCache가_true이다() {
//        // given
//        Long logId = 1234567890L;
//        String projectUuid = "550e8400-e29b-41d4-a716-446655440000";
//
//        String responseBody = """
//                {
//                  "log_id": 1234567890,
//                  "analysis": {
//                    "summary": "캐시된 분석 결과",
//                    "error_cause": "NULL 참조",
//                    "solution": "NULL 체크 추가",
//                    "tags": ["NULL_POINTER"],
//                    "analysis_type": "SINGLE",
//                    "target_type": "LOG",
//                    "analyzed_at": "2025-11-07T14:00:00.000Z"
//                  },
//                  "from_cache": true,
//                  "similar_log_id": 1234567800,
//                  "similarity_score": 0.92
//                }
//                """;
//
//        mockWebServer.enqueue(new MockResponse()
//                .setBody(responseBody)
//                .addHeader("Content-Type", "application/json")
//                .setResponseCode(200));
//
//        // when
//        AiAnalysisResponse response = aiServiceClient.analyzeLog(logId, projectUuid);
//
//        // then
//        assertThat(response).isNotNull();
//        assertThat(response.getFromCache()).isTrue();
//        assertThat(response.getSimilarLogId()).isEqualTo(1234567800L);
//        assertThat(response.getSimilarityScore()).isEqualTo(0.92);
//    }
//
//    @Test
//    @DisplayName("AI_서비스_404_응답_시_null을_반환한다")
//    void AI_서비스_404_응답_시_null을_반환한다() {
//        // given
//        Long logId = 9999999999L;
//        String projectUuid = "550e8400-e29b-41d4-a716-446655440000";
//
//        mockWebServer.enqueue(new MockResponse()
//                .setResponseCode(404)
//                .setBody("{\"error\": \"Log not found\"}"));
//
//        // when
//        AiAnalysisResponse response = aiServiceClient.analyzeLog(logId, projectUuid);
//
//        // then
//        assertThat(response).isNull();
//    }
//
//    @Test
//    @DisplayName("AI_서비스_500_응답_시_null을_반환한다")
//    void AI_서비스_500_응답_시_null을_반환한다() {
//        // given
//        Long logId = 1234567890L;
//        String projectUuid = "550e8400-e29b-41d4-a716-446655440000";
//
//        mockWebServer.enqueue(new MockResponse()
//                .setResponseCode(500)
//                .setBody("{\"error\": \"Internal server error\"}"));
//
//        // when
//        AiAnalysisResponse response = aiServiceClient.analyzeLog(logId, projectUuid);
//
//        // then
//        assertThat(response).isNull();
//    }
//
//    @Test
//    @DisplayName("AI_서비스_네트워크_오류_시_null을_반환한다")
//    void AI_서비스_네트워크_오류_시_null을_반환한다() throws IOException {
//        // given
//        Long logId = 1234567890L;
//        String projectUuid = "550e8400-e29b-41d4-a716-446655440000";
//
//        // 서버 종료로 네트워크 오류 시뮬레이션
//        mockWebServer.shutdown();
//
//        // when
//        AiAnalysisResponse response = aiServiceClient.analyzeLog(logId, projectUuid);
//
//        // then
//        assertThat(response).isNull();
//    }
//}
