package S13P31A306.loglens.global.client;

import S13P31A306.loglens.domain.analysis.constants.DocumentFormat;
import S13P31A306.loglens.domain.analysis.constants.DocumentType;
import S13P31A306.loglens.domain.analysis.dto.ai.AiDocumentMetadata;
import S13P31A306.loglens.domain.analysis.dto.ai.AiHtmlDocumentRequest;
import S13P31A306.loglens.domain.analysis.dto.ai.AiHtmlDocumentResponse;
import S13P31A306.loglens.domain.analysis.dto.ai.AiValidationStatus;
import S13P31A306.loglens.domain.log.dto.ai.AiAnalysisDto;
import S13P31A306.loglens.domain.log.dto.ai.AiAnalysisResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AiServiceClient 테스트
 * AI 서비스의 GET /api/v2/logs/{log_id}/analysis 엔드포인트를 테스트합니다.
 */
class AiServiceClientTest {

    private MockWebServer mockWebServer;
    private AiServiceClient aiServiceClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("/").toString();
        WebClient.Builder webClientBuilder = WebClient.builder();

        aiServiceClient = new AiServiceClient(webClientBuilder, baseUrl, 30000);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("AI_로그_분석_성공_시_AiAnalysisResponse를_반환한다")
    void AI_로그_분석_성공_시_AiAnalysisResponse를_반환한다() {
        // given
        Long logId = 1234567890L;
        String projectUuid = "550e8400-e29b-41d4-a716-446655440000";

        String responseBody = """
                {
                  "log_id": 1234567890,
                  "analysis": {
                    "summary": "데이터베이스 연결 오류가 발생했습니다.",
                    "error_cause": "커넥션 풀이 고갈되었습니다.",
                    "solution": "커넥션 풀 크기를 증가시키세요.",
                    "tags": ["DATABASE", "CONNECTION"],
                    "analysis_type": "TRACE_BASED",
                    "target_type": "LOG",
                    "analyzed_at": "2025-11-07T15:00:00.000Z"
                  },
                  "from_cache": false,
                  "similar_log_id": null,
                  "similarity_score": null
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json")
                .setResponseCode(200));

        // when
        AiAnalysisResponse response = aiServiceClient.analyzeLog(logId, projectUuid);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getLogId()).isEqualTo(logId);
        assertThat(response.getFromCache()).isFalse();
        assertThat(response.getAnalysis()).isNotNull();
        assertThat(response.getAnalysis().getSummary()).isEqualTo("데이터베이스 연결 오류가 발생했습니다.");
        assertThat(response.getAnalysis().getErrorCause()).isEqualTo("커넥션 풀이 고갈되었습니다.");
        assertThat(response.getAnalysis().getTags()).containsExactly("DATABASE", "CONNECTION");
    }

    @Test
    @DisplayName("AI_분석_캐시_사용_시_fromCache가_true이다")
    void AI_분석_캐시_사용_시_fromCache가_true이다() {
        // given
        Long logId = 1234567890L;
        String projectUuid = "550e8400-e29b-41d4-a716-446655440000";

        String responseBody = """
                {
                  "log_id": 1234567890,
                  "analysis": {
                    "summary": "캐시된 분석 결과",
                    "error_cause": "NULL 참조",
                    "solution": "NULL 체크 추가",
                    "tags": ["NULL_POINTER"],
                    "analysis_type": "SINGLE",
                    "target_type": "LOG",
                    "analyzed_at": "2025-11-07T14:00:00.000Z"
                  },
                  "from_cache": true,
                  "similar_log_id": 1234567800,
                  "similarity_score": 0.92
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json")
                .setResponseCode(200));

        // when
        AiAnalysisResponse response = aiServiceClient.analyzeLog(logId, projectUuid);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getFromCache()).isTrue();
        assertThat(response.getSimilarLogId()).isEqualTo(1234567800L);
        assertThat(response.getSimilarityScore()).isEqualTo(0.92);
    }

    @Test
    @DisplayName("AI_서비스_404_응답_시_null을_반환한다")
    void AI_서비스_404_응답_시_null을_반환한다() {
        // given
        Long logId = 9999999999L;
        String projectUuid = "550e8400-e29b-41d4-a716-446655440000";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody("{\"error\": \"Log not found\"}"));

        // when
        AiAnalysisResponse response = aiServiceClient.analyzeLog(logId, projectUuid);

        // then
        assertThat(response).isNull();
    }

    @Test
    @DisplayName("AI_서비스_500_응답_시_null을_반환한다")
    void AI_서비스_500_응답_시_null을_반환한다() {
        // given
        Long logId = 1234567890L;
        String projectUuid = "550e8400-e29b-41d4-a716-446655440000";

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("{\"error\": \"Internal server error\"}"));

        // when
        AiAnalysisResponse response = aiServiceClient.analyzeLog(logId, projectUuid);

        // then
        assertThat(response).isNull();
    }

    @Test
    @DisplayName("AI_서비스_네트워크_오류_시_null을_반환한다")
    void AI_서비스_네트워크_오류_시_null을_반환한다() throws IOException {
        // given
        Long logId = 1234567890L;
        String projectUuid = "550e8400-e29b-41d4-a716-446655440000";

        // 서버 종료로 네트워크 오류 시뮬레이션
        mockWebServer.shutdown();

        // when
        AiAnalysisResponse response = aiServiceClient.analyzeLog(logId, projectUuid);

        // then
        assertThat(response).isNull();
    }

    @Nested
    @DisplayName("프로젝트 분석 HTML 생성 테스트")
    class GenerateProjectAnalysisHtmlTest {

        @Test
        @DisplayName("프로젝트_분석_HTML_생성_성공_시_AiHtmlDocumentResponse를_반환한다")
        void 프로젝트_분석_HTML_생성_성공_시_AiHtmlDocumentResponse를_반환한다() {
            // given
            String projectUuid = "test-project-uuid";
            Map<String, Object> data = new HashMap<>();
            data.put("projectInfo", Map.of("name", "TestProject"));

            AiHtmlDocumentRequest request = AiHtmlDocumentRequest.builder()
                    .projectUuid(projectUuid)
                    .documentType(DocumentType.PROJECT_ANALYSIS)
                    .format(DocumentFormat.HTML)
                    .data(data)
                    .build();

            String responseBody = """
                    {
                      "html_content": "<!DOCTYPE html><html><head><title>Test</title></head><body><h1>Report</h1></body></html>",
                      "metadata": {
                        "word_count": 1500,
                        "estimated_reading_time": "5분",
                        "sections_generated": ["executive_summary", "metrics_overview"],
                        "charts_included": ["error_timeline"],
                        "css_libraries_used": ["tailwindcss"],
                        "js_libraries_used": ["chartjs"],
                        "generation_time": 8.5,
                        "health_score": 85,
                        "critical_issues": 2,
                        "total_issues": 10,
                        "recommendations": 5
                      },
                      "validation_status": {
                        "is_valid_html": true,
                        "has_required_sections": true,
                        "warnings": []
                      }
                    }
                    """;

            mockWebServer.enqueue(new MockResponse()
                    .setBody(responseBody)
                    .addHeader("Content-Type", "application/json")
                    .setResponseCode(200));

            // when
            AiHtmlDocumentResponse response = aiServiceClient.generateProjectAnalysisHtml(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getHtmlContent()).contains("<!DOCTYPE html>");
            assertThat(response.getMetadata()).isNotNull();
            assertThat(response.getMetadata().getHealthScore()).isEqualTo(85);
            assertThat(response.getMetadata().getSectionsGenerated()).contains("executive_summary");
            assertThat(response.getValidationStatus().getIsValidHtml()).isTrue();
        }

        @Test
        @DisplayName("프로젝트_분석_HTML_생성_404_응답_시_null을_반환한다")
        void 프로젝트_분석_HTML_생성_404_응답_시_null을_반환한다() {
            // given
            AiHtmlDocumentRequest request = AiHtmlDocumentRequest.builder()
                    .projectUuid("invalid-uuid")
                    .documentType(DocumentType.PROJECT_ANALYSIS)
                    .format(DocumentFormat.HTML)
                    .build();

            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(404)
                    .setBody("{\"error\": \"Project not found\"}"));

            // when
            AiHtmlDocumentResponse response = aiServiceClient.generateProjectAnalysisHtml(request);

            // then
            assertThat(response).isNull();
        }

        @Test
        @DisplayName("프로젝트_분석_HTML_생성_500_응답_시_null을_반환한다")
        void 프로젝트_분석_HTML_생성_500_응답_시_null을_반환한다() {
            // given
            AiHtmlDocumentRequest request = AiHtmlDocumentRequest.builder()
                    .projectUuid("test-uuid")
                    .documentType(DocumentType.PROJECT_ANALYSIS)
                    .format(DocumentFormat.HTML)
                    .build();

            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(500)
                    .setBody("{\"error\": \"AI service error\"}"));

            // when
            AiHtmlDocumentResponse response = aiServiceClient.generateProjectAnalysisHtml(request);

            // then
            assertThat(response).isNull();
        }
    }

    @Nested
    @DisplayName("에러 분석 HTML 생성 테스트")
    class GenerateErrorAnalysisHtmlTest {

        @Test
        @DisplayName("에러_분석_HTML_생성_성공_시_AiHtmlDocumentResponse를_반환한다")
        void 에러_분석_HTML_생성_성공_시_AiHtmlDocumentResponse를_반환한다() {
            // given
            Long logId = 12345L;
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("errorLog", Map.of("message", "NullPointerException"));

            AiHtmlDocumentRequest request = AiHtmlDocumentRequest.builder()
                    .logId(logId)
                    .documentType(DocumentType.ERROR_ANALYSIS)
                    .format(DocumentFormat.HTML)
                    .data(errorData)
                    .build();

            String responseBody = """
                    {
                      "html_content": "<!DOCTYPE html><html><head><title>Error Analysis</title></head><body><h1>Error Report</h1></body></html>",
                      "metadata": {
                        "word_count": 800,
                        "estimated_reading_time": "3분",
                        "sections_generated": ["root_cause", "solutions"],
                        "charts_included": [],
                        "css_libraries_used": ["tailwindcss"],
                        "js_libraries_used": [],
                        "generation_time": 5.2,
                        "severity": "HIGH",
                        "root_cause": "Null pointer dereference",
                        "affected_users": 150
                      },
                      "validation_status": {
                        "is_valid_html": true,
                        "has_required_sections": true,
                        "warnings": []
                      }
                    }
                    """;

            mockWebServer.enqueue(new MockResponse()
                    .setBody(responseBody)
                    .addHeader("Content-Type", "application/json")
                    .setResponseCode(200));

            // when
            AiHtmlDocumentResponse response = aiServiceClient.generateErrorAnalysisHtml(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getHtmlContent()).contains("Error Report");
            assertThat(response.getMetadata().getSeverity()).isEqualTo("HIGH");
            assertThat(response.getMetadata().getRootCause()).isEqualTo("Null pointer dereference");
            assertThat(response.getMetadata().getAffectedUsers()).isEqualTo(150);
        }

        @Test
        @DisplayName("에러_분석_HTML_생성_404_응답_시_null을_반환한다")
        void 에러_분석_HTML_생성_404_응답_시_null을_반환한다() {
            // given
            AiHtmlDocumentRequest request = AiHtmlDocumentRequest.builder()
                    .logId(99999L)
                    .documentType(DocumentType.ERROR_ANALYSIS)
                    .format(DocumentFormat.HTML)
                    .build();

            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(404)
                    .setBody("{\"error\": \"Log not found\"}"));

            // when
            AiHtmlDocumentResponse response = aiServiceClient.generateErrorAnalysisHtml(request);

            // then
            assertThat(response).isNull();
        }
    }

    @Nested
    @DisplayName("HTML 재생성 테스트")
    class RegenerateWithFeedbackTest {

        @Test
        @DisplayName("프로젝트_분석_재생성_시_피드백이_포함된다")
        void 프로젝트_분석_재생성_시_피드백이_포함된다() {
            // given
            AiHtmlDocumentRequest request = AiHtmlDocumentRequest.builder()
                    .projectUuid("test-uuid")
                    .documentType(DocumentType.PROJECT_ANALYSIS)
                    .format(DocumentFormat.HTML)
                    .data(new HashMap<>())
                    .build();

            List<String> validationErrors = List.of("Missing <title> tag", "Missing <body> tag");

            String responseBody = """
                    {
                      "html_content": "<!DOCTYPE html><html><head><title>Fixed</title></head><body>Content</body></html>",
                      "metadata": {
                        "word_count": 500,
                        "generation_time": 3.0
                      },
                      "validation_status": {
                        "is_valid_html": true,
                        "has_required_sections": true,
                        "warnings": []
                      }
                    }
                    """;

            mockWebServer.enqueue(new MockResponse()
                    .setBody(responseBody)
                    .addHeader("Content-Type", "application/json")
                    .setResponseCode(200));

            // when
            AiHtmlDocumentResponse response = aiServiceClient.regenerateWithFeedback(request, validationErrors);

            // then
            assertThat(response).isNotNull();
            assertThat(request.getRegenerationFeedback()).isEqualTo(validationErrors);
            assertThat(response.getHtmlContent()).contains("<title>");
        }

        @Test
        @DisplayName("에러_분석_재생성_시_generateErrorAnalysisHtml이_호출된다")
        void 에러_분석_재생성_시_generateErrorAnalysisHtml이_호출된다() {
            // given
            AiHtmlDocumentRequest request = AiHtmlDocumentRequest.builder()
                    .logId(123L)
                    .documentType(DocumentType.ERROR_ANALYSIS)
                    .format(DocumentFormat.HTML)
                    .data(new HashMap<>())
                    .build();

            List<String> validationErrors = List.of("Missing sections");

            String responseBody = """
                    {
                      "html_content": "<!DOCTYPE html><html><body>Fixed Error Report</body></html>",
                      "metadata": {
                        "severity": "MEDIUM"
                      },
                      "validation_status": {
                        "is_valid_html": true,
                        "has_required_sections": true,
                        "warnings": []
                      }
                    }
                    """;

            mockWebServer.enqueue(new MockResponse()
                    .setBody(responseBody)
                    .addHeader("Content-Type", "application/json")
                    .setResponseCode(200));

            // when
            AiHtmlDocumentResponse response = aiServiceClient.regenerateWithFeedback(request, validationErrors);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getHtmlContent()).contains("Fixed Error Report");
        }
    }

    @Nested
    @DisplayName("AI vs DB 통계 비교 테스트")
    class CompareAiVsDbStatisticsTest {

        @Test
        @DisplayName("통계_비교_성공_시_AIComparisonResponse를_반환한다")
        void 통계_비교_성공_시_AIComparisonResponse를_반환한다() {
            // given
            String projectUuid = "test-project-uuid";
            int timeHours = 24;
            int sampleSize = 100;

            String responseBody = """
                    {
                      "project_uuid": "test-project-uuid",
                      "analysis_period_hours": 24,
                      "sample_size": 100,
                      "analyzed_at": "2025-11-14T15:30:00",
                      "db_statistics": {
                        "total_logs": 15420,
                        "error_count": 342,
                        "warn_count": 1205,
                        "info_count": 13873,
                        "error_rate": 2.22,
                        "peak_hour": "2025-11-14T12",
                        "peak_count": 892
                      },
                      "ai_statistics": {
                        "estimated_total_logs": 15380,
                        "estimated_error_count": 338,
                        "estimated_warn_count": 1198,
                        "estimated_info_count": 13844,
                        "estimated_error_rate": 2.20,
                        "confidence_score": 85,
                        "reasoning": "샘플 100개 중 ERROR 2.2% 비율을 전체에 적용"
                      },
                      "accuracy_metrics": {
                        "total_logs_accuracy": 99.74,
                        "error_count_accuracy": 98.83,
                        "warn_count_accuracy": 99.42,
                        "info_count_accuracy": 99.79,
                        "error_rate_accuracy": 99.80,
                        "overall_accuracy": 99.28,
                        "ai_confidence": 85
                      },
                      "verdict": {
                        "grade": "매우 우수",
                        "can_replace_db": true,
                        "explanation": "오차율 5% 미만으로 프로덕션 환경에서 신뢰성 있게 사용 가능합니다.",
                        "recommendations": [
                          "프로덕션 환경 적용 권장",
                          "실시간 대시보드 AI 기반 분석 도입 가능"
                        ]
                      },
                      "technical_highlights": [
                        "Temperature 0.1로 일관된 추론",
                        "종합 정확도 99.28% 달성"
                      ]
                    }
                    """;

            mockWebServer.enqueue(new MockResponse()
                    .setBody(responseBody)
                    .addHeader("Content-Type", "application/json")
                    .setResponseCode(200));

            // when
            var response = aiServiceClient.compareAiVsDbStatistics(projectUuid, timeHours, sampleSize);

            // then
            assertThat(response).isNotNull();
            assertThat(response.projectUuid()).isEqualTo("test-project-uuid");
            assertThat(response.analysisPeriodHours()).isEqualTo(24);
            assertThat(response.sampleSize()).isEqualTo(100);

            // DB Statistics
            assertThat(response.dbStatistics()).isNotNull();
            assertThat(response.dbStatistics().totalLogs()).isEqualTo(15420);
            assertThat(response.dbStatistics().errorCount()).isEqualTo(342);
            assertThat(response.dbStatistics().errorRate()).isEqualTo(2.22);

            // AI Statistics
            assertThat(response.aiStatistics()).isNotNull();
            assertThat(response.aiStatistics().estimatedTotalLogs()).isEqualTo(15380);
            assertThat(response.aiStatistics().confidenceScore()).isEqualTo(85);

            // Accuracy Metrics
            assertThat(response.accuracyMetrics()).isNotNull();
            assertThat(response.accuracyMetrics().overallAccuracy()).isEqualTo(99.28);
            assertThat(response.accuracyMetrics().errorCountAccuracy()).isEqualTo(98.83);

            // Verdict
            assertThat(response.verdict()).isNotNull();
            assertThat(response.verdict().grade()).isEqualTo("매우 우수");
            assertThat(response.verdict().canReplaceDb()).isTrue();
            assertThat(response.verdict().recommendations()).hasSize(2);
        }

        @Test
        @DisplayName("통계_비교_404_응답_시_null을_반환한다")
        void 통계_비교_404_응답_시_null을_반환한다() {
            // given
            String projectUuid = "non-existent-project";

            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(404)
                    .setBody("{\"detail\": \"최근 24시간 동안 로그 데이터가 없습니다.\"}"));

            // when
            var response = aiServiceClient.compareAiVsDbStatistics(projectUuid, 24, 100);

            // then
            assertThat(response).isNull();
        }

        @Test
        @DisplayName("통계_비교_500_응답_시_null을_반환한다")
        void 통계_비교_500_응답_시_null을_반환한다() {
            // given
            String projectUuid = "test-project-uuid";

            mockWebServer.enqueue(new MockResponse()
                    .setResponseCode(500)
                    .setBody("{\"detail\": \"AI vs DB 통계 비교 중 오류 발생\"}"));

            // when
            var response = aiServiceClient.compareAiVsDbStatistics(projectUuid, 24, 100);

            // then
            assertThat(response).isNull();
        }

        @Test
        @DisplayName("통계_비교_네트워크_오류_시_null을_반환한다")
        void 통계_비교_네트워크_오류_시_null을_반환한다() throws IOException {
            // given
            String projectUuid = "test-project-uuid";

            // 서버 종료로 네트워크 오류 시뮬레이션
            mockWebServer.shutdown();

            // when
            var response = aiServiceClient.compareAiVsDbStatistics(projectUuid, 24, 100);

            // then
            assertThat(response).isNull();
        }

        @Test
        @DisplayName("우수_등급_응답_시_canReplaceDb가_true이다")
        void 우수_등급_응답_시_canReplaceDb가_true이다() {
            // given
            String responseBody = """
                    {
                      "project_uuid": "test-uuid",
                      "analysis_period_hours": 24,
                      "sample_size": 100,
                      "analyzed_at": "2025-11-14T15:30:00",
                      "db_statistics": {
                        "total_logs": 1000,
                        "error_count": 100,
                        "warn_count": 100,
                        "info_count": 800,
                        "error_rate": 10.0,
                        "peak_hour": "2025-11-14T12",
                        "peak_count": 100
                      },
                      "ai_statistics": {
                        "estimated_total_logs": 920,
                        "estimated_error_count": 93,
                        "estimated_warn_count": 95,
                        "estimated_info_count": 732,
                        "estimated_error_rate": 10.1,
                        "confidence_score": 80,
                        "reasoning": "추론 기반"
                      },
                      "accuracy_metrics": {
                        "total_logs_accuracy": 92.0,
                        "error_count_accuracy": 93.0,
                        "warn_count_accuracy": 95.0,
                        "info_count_accuracy": 91.5,
                        "error_rate_accuracy": 99.0,
                        "overall_accuracy": 92.35,
                        "ai_confidence": 80
                      },
                      "verdict": {
                        "grade": "우수",
                        "can_replace_db": true,
                        "explanation": "오차율 10% 미만으로 대부분의 분석 업무에 활용 가능합니다.",
                        "recommendations": ["보조 분석 도구로 즉시 활용 가능"]
                      },
                      "technical_highlights": ["종합 정확도 92.35% 달성"]
                    }
                    """;

            mockWebServer.enqueue(new MockResponse()
                    .setBody(responseBody)
                    .addHeader("Content-Type", "application/json")
                    .setResponseCode(200));

            // when
            var response = aiServiceClient.compareAiVsDbStatistics("test-uuid", 24, 100);

            // then
            assertThat(response.verdict().grade()).isEqualTo("우수");
            assertThat(response.verdict().canReplaceDb()).isTrue();
            assertThat(response.accuracyMetrics().overallAccuracy()).isGreaterThanOrEqualTo(90.0);
        }

        @Test
        @DisplayName("미흡_등급_응답_시_canReplaceDb가_false이다")
        void 미흡_등급_응답_시_canReplaceDb가_false이다() {
            // given
            String responseBody = """
                    {
                      "project_uuid": "test-uuid",
                      "analysis_period_hours": 24,
                      "sample_size": 100,
                      "analyzed_at": "2025-11-14T15:30:00",
                      "db_statistics": {
                        "total_logs": 1000,
                        "error_count": 100,
                        "warn_count": 100,
                        "info_count": 800,
                        "error_rate": 10.0,
                        "peak_hour": "2025-11-14T12",
                        "peak_count": 100
                      },
                      "ai_statistics": {
                        "estimated_total_logs": 600,
                        "estimated_error_count": 60,
                        "estimated_warn_count": 60,
                        "estimated_info_count": 480,
                        "estimated_error_rate": 10.0,
                        "confidence_score": 30,
                        "reasoning": "추론 실패"
                      },
                      "accuracy_metrics": {
                        "total_logs_accuracy": 60.0,
                        "error_count_accuracy": 60.0,
                        "warn_count_accuracy": 60.0,
                        "info_count_accuracy": 60.0,
                        "error_rate_accuracy": 100.0,
                        "overall_accuracy": 62.0,
                        "ai_confidence": 30
                      },
                      "verdict": {
                        "grade": "미흡",
                        "can_replace_db": false,
                        "explanation": "정확도가 낮아 근본적인 개선이 필요합니다.",
                        "recommendations": ["LLM 모델 업그레이드 고려"]
                      },
                      "technical_highlights": []
                    }
                    """;

            mockWebServer.enqueue(new MockResponse()
                    .setBody(responseBody)
                    .addHeader("Content-Type", "application/json")
                    .setResponseCode(200));

            // when
            var response = aiServiceClient.compareAiVsDbStatistics("test-uuid", 24, 100);

            // then
            assertThat(response.verdict().grade()).isEqualTo("미흡");
            assertThat(response.verdict().canReplaceDb()).isFalse();
            assertThat(response.accuracyMetrics().overallAccuracy()).isLessThan(70.0);
        }

        @Test
        @DisplayName("불완전한_응답_수신_시_request_파라미터로_보완한다")
        void 불완전한_응답_수신_시_request_파라미터로_보완한다() {
            // given - verdict만 있고 나머지 필드는 null인 불완전한 응답
            String responseBody = """
                    {
                      "project_uuid": null,
                      "analysis_period_hours": null,
                      "sample_size": null,
                      "analyzed_at": null,
                      "db_statistics": null,
                      "ai_statistics": null,
                      "accuracy_metrics": null,
                      "verdict": {
                        "grade": "미흡",
                        "can_replace_db": null,
                        "explanation": "정확도가 낮아 근본적인 개선이 필요합니다.",
                        "recommendations": ["LLM 모델 업그레이드 고려"]
                      },
                      "technical_highlights": null
                    }
                    """;

            mockWebServer.enqueue(new MockResponse()
                    .setBody(responseBody)
                    .addHeader("Content-Type", "application/json")
                    .setResponseCode(200));

            // when
            String requestProjectUuid = "request-project-uuid-1234";
            int requestTimeHours = 48;
            int requestSampleSize = 200;
            var response = aiServiceClient.compareAiVsDbStatistics(requestProjectUuid, requestTimeHours, requestSampleSize);

            // then - request 파라미터로 보완됨
            assertThat(response).isNotNull();
            assertThat(response.projectUuid()).isEqualTo(requestProjectUuid);
            assertThat(response.analysisPeriodHours()).isEqualTo(requestTimeHours);
            assertThat(response.sampleSize()).isEqualTo(requestSampleSize);
            assertThat(response.analyzedAt()).isNotNull(); // LocalDateTime.now()로 설정됨

            // verdict는 원래 값 유지
            assertThat(response.verdict()).isNotNull();
            assertThat(response.verdict().grade()).isEqualTo("미흡");

            // 나머지 필드는 여전히 null
            assertThat(response.dbStatistics()).isNull();
            assertThat(response.aiStatistics()).isNull();
            assertThat(response.accuracyMetrics()).isNull();
        }

        @Test
        @DisplayName("projectUuid만_null인_경우에도_보완한다")
        void projectUuid만_null인_경우에도_보완한다() {
            // given - projectUuid만 null
            String responseBody = """
                    {
                      "project_uuid": null,
                      "analysis_period_hours": 24,
                      "sample_size": 100,
                      "analyzed_at": "2025-11-14T15:30:00",
                      "db_statistics": {
                        "total_logs": 1000,
                        "error_count": 100,
                        "warn_count": 100,
                        "info_count": 800,
                        "error_rate": 10.0,
                        "peak_hour": "2025-11-14T12",
                        "peak_count": 100
                      },
                      "ai_statistics": {
                        "estimated_total_logs": 980,
                        "estimated_error_count": 98,
                        "estimated_warn_count": 99,
                        "estimated_info_count": 783,
                        "estimated_error_rate": 10.0,
                        "confidence_score": 85,
                        "reasoning": "추론 완료"
                      },
                      "accuracy_metrics": {
                        "total_logs_accuracy": 98.0,
                        "error_count_accuracy": 98.0,
                        "warn_count_accuracy": 99.0,
                        "info_count_accuracy": 97.875,
                        "error_rate_accuracy": 100.0,
                        "overall_accuracy": 98.4,
                        "ai_confidence": 85
                      },
                      "verdict": {
                        "grade": "매우 우수",
                        "can_replace_db": true,
                        "explanation": "오차율 5% 미만",
                        "recommendations": []
                      },
                      "technical_highlights": []
                    }
                    """;

            mockWebServer.enqueue(new MockResponse()
                    .setBody(responseBody)
                    .addHeader("Content-Type", "application/json")
                    .setResponseCode(200));

            // when
            String requestProjectUuid = "enriched-project-uuid";
            var response = aiServiceClient.compareAiVsDbStatistics(requestProjectUuid, 24, 100);

            // then - projectUuid만 보완됨
            assertThat(response.projectUuid()).isEqualTo(requestProjectUuid);
            // 원래 값은 유지
            assertThat(response.analysisPeriodHours()).isEqualTo(24);
            assertThat(response.sampleSize()).isEqualTo(100);
            assertThat(response.dbStatistics()).isNotNull();
            assertThat(response.aiStatistics()).isNotNull();
            assertThat(response.accuracyMetrics()).isNotNull();
        }

        @Test
        @DisplayName("완전한_응답_수신_시_보완하지_않는다")
        void 완전한_응답_수신_시_보완하지_않는다() {
            // given - 모든 필드가 채워진 완전한 응답
            String responseBody = """
                    {
                      "project_uuid": "original-uuid",
                      "analysis_period_hours": 12,
                      "sample_size": 50,
                      "analyzed_at": "2025-11-14T10:00:00",
                      "db_statistics": {
                        "total_logs": 500,
                        "error_count": 50,
                        "warn_count": 50,
                        "info_count": 400,
                        "error_rate": 10.0,
                        "peak_hour": "2025-11-14T09",
                        "peak_count": 60
                      },
                      "ai_statistics": {
                        "estimated_total_logs": 490,
                        "estimated_error_count": 49,
                        "estimated_warn_count": 49,
                        "estimated_info_count": 392,
                        "estimated_error_rate": 10.0,
                        "confidence_score": 90,
                        "reasoning": "정확한 추론"
                      },
                      "accuracy_metrics": {
                        "total_logs_accuracy": 98.0,
                        "error_count_accuracy": 98.0,
                        "warn_count_accuracy": 98.0,
                        "info_count_accuracy": 98.0,
                        "error_rate_accuracy": 100.0,
                        "overall_accuracy": 98.4,
                        "ai_confidence": 90
                      },
                      "verdict": {
                        "grade": "매우 우수",
                        "can_replace_db": true,
                        "explanation": "높은 정확도",
                        "recommendations": []
                      },
                      "technical_highlights": ["정확도 98.4%"]
                    }
                    """;

            mockWebServer.enqueue(new MockResponse()
                    .setBody(responseBody)
                    .addHeader("Content-Type", "application/json")
                    .setResponseCode(200));

            // when - 다른 파라미터로 호출
            var response = aiServiceClient.compareAiVsDbStatistics("different-uuid", 24, 100);

            // then - 원래 응답 값이 유지됨 (보완되지 않음)
            assertThat(response.projectUuid()).isEqualTo("original-uuid");
            assertThat(response.analysisPeriodHours()).isEqualTo(12);
            assertThat(response.sampleSize()).isEqualTo(50);
        }
    }
}

