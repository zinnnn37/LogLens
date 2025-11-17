package S13P31A306.loglens.domain.statistics.controller.impl;

import S13P31A306.loglens.domain.statistics.dto.response.AIComparisonResponse;
import S13P31A306.loglens.global.client.AiServiceClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AI vs DB 통계 비교 컨트롤러 테스트
 * LLM이 DB 쿼리를 대체할 수 있는 역량을 검증하는 API 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("StatisticsController AI 비교 테스트")
class StatisticsControllerAIComparisonTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiServiceClient aiServiceClient;

    @Nested
    @DisplayName("AI vs DB 통계 비교 조회")
    class GetAIComparisonTest {

        @Test
        @DisplayName("AI_비교_조회_성공_시_200_OK와_정확도_메트릭을_반환한다")
        void AI_비교_조회_성공_시_200_OK와_정확도_메트릭을_반환한다() throws Exception {
            // given
            String projectUuid = "550e8400-e29b-41d4-a716-446655440000";
            AIComparisonResponse response = createSuccessfulComparisonResponse(projectUuid);

            given(aiServiceClient.compareAiVsDbStatistics(anyString(), anyInt(), anyInt()))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get("/api/statistics/ai-comparison")
                            .param("projectUuid", projectUuid)
                            .param("timeHours", "24")
                            .param("sampleSize", "100"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("STATISTICS_2003"))
                    .andExpect(jsonPath("$.message").value("AI vs DB 통계 비교 검증 성공"))
                    .andExpect(jsonPath("$.data.project_uuid").value(projectUuid))
                    .andExpect(jsonPath("$.data.analysis_period_hours").value(24))
                    .andExpect(jsonPath("$.data.sample_size").value(100))
                    .andExpect(jsonPath("$.data.db_statistics").exists())
                    .andExpect(jsonPath("$.data.ai_statistics").exists())
                    .andExpect(jsonPath("$.data.accuracy_metrics").exists())
                    .andExpect(jsonPath("$.data.verdict").exists())
                    .andExpect(jsonPath("$.data.accuracy_metrics.overall_accuracy").value(99.28))
                    .andExpect(jsonPath("$.data.verdict.grade").value("매우 우수"))
                    .andExpect(jsonPath("$.data.verdict.can_replace_db").value(true));
        }

        @Test
        @DisplayName("AI_서비스_호출_실패_시_500_에러를_반환한다")
        void AI_서비스_호출_실패_시_500_에러를_반환한다() throws Exception {
            // given
            String projectUuid = "550e8400-e29b-41d4-a716-446655440000";

            given(aiServiceClient.compareAiVsDbStatistics(anyString(), anyInt(), anyInt()))
                    .willReturn(null);

            // when & then
            mockMvc.perform(get("/api/statistics/ai-comparison")
                            .param("projectUuid", projectUuid))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("기본값_파라미터로_요청_시_24시간_100개_샘플로_조회한다")
        void 기본값_파라미터로_요청_시_24시간_100개_샘플로_조회한다() throws Exception {
            // given
            String projectUuid = "550e8400-e29b-41d4-a716-446655440000";
            AIComparisonResponse response = createSuccessfulComparisonResponse(projectUuid);

            given(aiServiceClient.compareAiVsDbStatistics(eq(projectUuid), eq(24), eq(100)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get("/api/statistics/ai-comparison")
                            .param("projectUuid", projectUuid))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.analysis_period_hours").value(24))
                    .andExpect(jsonPath("$.data.sample_size").value(100));
        }

        @Test
        @DisplayName("커스텀_파라미터로_요청_시_지정된_값으로_조회한다")
        void 커스텀_파라미터로_요청_시_지정된_값으로_조회한다() throws Exception {
            // given
            String projectUuid = "550e8400-e29b-41d4-a716-446655440000";
            int customTimeHours = 48;
            int customSampleSize = 200;

            AIComparisonResponse response = new AIComparisonResponse(
                    projectUuid,
                    customTimeHours,
                    customSampleSize,
                    LocalDateTime.now(),
                    createDBStatistics(),
                    createAIStatistics(),
                    createAccuracyMetrics(95.0),
                    createVerdict("매우 우수", true),
                    List.of("48시간 분석", "200개 샘플 사용")
            );

            given(aiServiceClient.compareAiVsDbStatistics(eq(projectUuid), eq(customTimeHours), eq(customSampleSize)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get("/api/statistics/ai-comparison")
                            .param("projectUuid", projectUuid)
                            .param("timeHours", String.valueOf(customTimeHours))
                            .param("sampleSize", String.valueOf(customSampleSize)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.analysis_period_hours").value(customTimeHours))
                    .andExpect(jsonPath("$.data.sample_size").value(customSampleSize));
        }

        @Test
        @DisplayName("정확도가_높을때_매우_우수_등급과_DB_대체_가능을_반환한다")
        void 정확도가_높을때_매우_우수_등급과_DB_대체_가능을_반환한다() throws Exception {
            // given
            String projectUuid = "550e8400-e29b-41d4-a716-446655440000";
            AIComparisonResponse response = new AIComparisonResponse(
                    projectUuid, 24, 100, LocalDateTime.now(),
                    createDBStatistics(),
                    createAIStatistics(),
                    createAccuracyMetrics(98.5),
                    createVerdict("매우 우수", true),
                    List.of("Temperature 0.1로 일관된 추론")
            );

            given(aiServiceClient.compareAiVsDbStatistics(anyString(), anyInt(), anyInt()))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get("/api/statistics/ai-comparison")
                            .param("projectUuid", projectUuid))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accuracy_metrics.overall_accuracy").value(98.5))
                    .andExpect(jsonPath("$.data.verdict.grade").value("매우 우수"))
                    .andExpect(jsonPath("$.data.verdict.can_replace_db").value(true));
        }

        @Test
        @DisplayName("정확도가_낮을때_미흡_등급과_DB_대체_불가능을_반환한다")
        void 정확도가_낮을때_미흡_등급과_DB_대체_불가능을_반환한다() throws Exception {
            // given
            String projectUuid = "550e8400-e29b-41d4-a716-446655440000";
            AIComparisonResponse response = new AIComparisonResponse(
                    projectUuid, 24, 100, LocalDateTime.now(),
                    createDBStatistics(),
                    createAIStatistics(),
                    createAccuracyMetrics(65.0),
                    createVerdict("미흡", false),
                    List.of()
            );

            given(aiServiceClient.compareAiVsDbStatistics(anyString(), anyInt(), anyInt()))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get("/api/statistics/ai-comparison")
                            .param("projectUuid", projectUuid))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accuracy_metrics.overall_accuracy").value(65.0))
                    .andExpect(jsonPath("$.data.verdict.grade").value("미흡"))
                    .andExpect(jsonPath("$.data.verdict.can_replace_db").value(false));
        }

        @Test
        @DisplayName("기술적_어필_포인트가_응답에_포함된다")
        void 기술적_어필_포인트가_응답에_포함된다() throws Exception {
            // given
            String projectUuid = "550e8400-e29b-41d4-a716-446655440000";
            List<String> highlights = List.of(
                    "Temperature 0.1로 일관된 추론",
                    "종합 정확도 99.28% 달성",
                    "MCP/멀티모달 없이 단일 LLM으로 구현"
            );

            AIComparisonResponse response = new AIComparisonResponse(
                    projectUuid, 24, 100, LocalDateTime.now(),
                    createDBStatistics(),
                    createAIStatistics(),
                    createAccuracyMetrics(99.28),
                    createVerdict("매우 우수", true),
                    highlights
            );

            given(aiServiceClient.compareAiVsDbStatistics(anyString(), anyInt(), anyInt()))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get("/api/statistics/ai-comparison")
                            .param("projectUuid", projectUuid))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.technical_highlights").isArray())
                    .andExpect(jsonPath("$.data.technical_highlights.length()").value(3))
                    .andExpect(jsonPath("$.data.technical_highlights[0]").value("Temperature 0.1로 일관된 추론"));
        }

        @Test
        @DisplayName("불완전한_응답_수신_시에도_200_OK를_반환한다")
        void 불완전한_응답_수신_시에도_200_OK를_반환한다() throws Exception {
            // given - verdict만 있고 나머지는 null인 불완전한 응답
            String projectUuid = "550e8400-e29b-41d4-a716-446655440000";
            AIComparisonResponse incompleteResponse = new AIComparisonResponse(
                    projectUuid,  // AiServiceClient에서 보완됨
                    24,           // AiServiceClient에서 보완됨
                    100,          // AiServiceClient에서 보완됨
                    LocalDateTime.now(),  // AiServiceClient에서 보완됨
                    null,  // dbStatistics null
                    null,  // aiStatistics null
                    null,  // accuracyMetrics null
                    createVerdict("미흡", false),  // verdict만 존재
                    null   // technicalHighlights null
            );

            given(aiServiceClient.compareAiVsDbStatistics(anyString(), anyInt(), anyInt()))
                    .willReturn(incompleteResponse);

            // when & then - 불완전해도 정상 반환
            mockMvc.perform(get("/api/statistics/ai-comparison")
                            .param("projectUuid", projectUuid))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("STATISTICS_2003"))
                    .andExpect(jsonPath("$.data.project_uuid").value(projectUuid))
                    .andExpect(jsonPath("$.data.analysis_period_hours").value(24))
                    .andExpect(jsonPath("$.data.sample_size").value(100))
                    .andExpect(jsonPath("$.data.db_statistics").doesNotExist())
                    .andExpect(jsonPath("$.data.ai_statistics").doesNotExist())
                    .andExpect(jsonPath("$.data.accuracy_metrics").doesNotExist())
                    .andExpect(jsonPath("$.data.verdict.grade").value("미흡"));
        }

        @Test
        @DisplayName("verdict만_있는_불완전한_응답도_처리한다")
        void verdict만_있는_불완전한_응답도_처리한다() throws Exception {
            // given
            String projectUuid = "550e8400-e29b-41d4-a716-446655440000";
            AIComparisonResponse.ComparisonVerdict verdict = new AIComparisonResponse.ComparisonVerdict(
                    "미흡",
                    null,  // canReplaceDb도 null
                    "정확도가 낮아 근본적인 개선이 필요합니다.",
                    List.of("LLM 모델 업그레이드 고려")
            );

            AIComparisonResponse response = new AIComparisonResponse(
                    projectUuid, 24, 100, LocalDateTime.now(),
                    null, null, null,
                    verdict,
                    null
            );

            given(aiServiceClient.compareAiVsDbStatistics(anyString(), anyInt(), anyInt()))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get("/api/statistics/ai-comparison")
                            .param("projectUuid", projectUuid))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.verdict.grade").value("미흡"))
                    .andExpect(jsonPath("$.data.verdict.can_replace_db").doesNotExist())
                    .andExpect(jsonPath("$.data.verdict.explanation").value("정확도가 낮아 근본적인 개선이 필요합니다."));
        }
    }

    // Helper methods for creating test data

    private AIComparisonResponse createSuccessfulComparisonResponse(String projectUuid) {
        return new AIComparisonResponse(
                projectUuid,
                24,
                100,
                LocalDateTime.now(),
                createDBStatistics(),
                createAIStatistics(),
                createAccuracyMetrics(99.28),
                createVerdict("매우 우수", true),
                List.of(
                        "Temperature 0.1로 일관된 추론",
                        "종합 정확도 99.28% 달성"
                )
        );
    }

    private AIComparisonResponse.DBStatistics createDBStatistics() {
        return new AIComparisonResponse.DBStatistics(
                15420,
                342,
                1205,
                13873,
                2.22,
                "2025-11-14T12",
                892
        );
    }

    private AIComparisonResponse.AIStatistics createAIStatistics() {
        return new AIComparisonResponse.AIStatistics(
                15380,
                338,
                1198,
                13844,
                2.20,
                85,
                "샘플 100개 중 ERROR 2.2% 비율을 전체에 적용"
        );
    }

    private AIComparisonResponse.AccuracyMetrics createAccuracyMetrics(double overallAccuracy) {
        return new AIComparisonResponse.AccuracyMetrics(
                99.74,
                98.83,
                99.42,
                99.79,
                99.80,
                overallAccuracy,
                85
        );
    }

    private AIComparisonResponse.ComparisonVerdict createVerdict(String grade, boolean canReplaceDb) {
        String explanation = canReplaceDb
                ? "오차율 5% 미만으로 프로덕션 환경에서 신뢰성 있게 사용 가능합니다."
                : "정확도가 낮아 근본적인 개선이 필요합니다.";

        List<String> recommendations = canReplaceDb
                ? List.of("프로덕션 환경 적용 권장", "실시간 대시보드 AI 기반 분석 도입 가능")
                : List.of("LLM 모델 업그레이드 고려", "프롬프트 엔지니어링 개선");

        return new AIComparisonResponse.ComparisonVerdict(
                grade,
                canReplaceDb,
                explanation,
                recommendations
        );
    }
}
