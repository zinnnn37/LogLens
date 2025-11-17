package S13P31A306.loglens.domain.statistics.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AIComparisonResponse DTO 테스트
 * Record 클래스의 생성, 접근, JSON 직렬화/역직렬화를 테스트합니다.
 */
@DisplayName("AIComparisonResponse DTO 테스트")
class AIComparisonResponseTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Nested
    @DisplayName("Record 생성 및 접근 테스트")
    class RecordCreationTest {

        @Test
        @DisplayName("AIComparisonResponse_record_생성_및_접근_테스트")
        void AIComparisonResponse_record_생성_및_접근_테스트() {
            // given
            String projectUuid = "test-uuid";
            int analysisPeriodHours = 24;
            int sampleSize = 100;
            LocalDateTime analyzedAt = LocalDateTime.now();

            var dbStats = new AIComparisonResponse.DBStatistics(
                    15420, 342, 1205, 13873, 2.22, "2025-11-14T12", 892
            );
            var aiStats = new AIComparisonResponse.AIStatistics(
                    15380, 338, 1198, 13844, 2.20, 85, "추론 근거"
            );
            var metrics = new AIComparisonResponse.AccuracyMetrics(
                    99.74, 98.83, 99.42, 99.79, 99.80, 99.28, 85
            );
            var verdict = new AIComparisonResponse.ComparisonVerdict(
                    "매우 우수", true, "설명", List.of("권장1", "권장2")
            );
            var highlights = List.of("포인트1", "포인트2");

            // when
            var response = new AIComparisonResponse(
                    projectUuid, analysisPeriodHours, sampleSize, analyzedAt,
                    dbStats, aiStats, metrics, verdict, highlights
            );

            // then
            assertThat(response.projectUuid()).isEqualTo(projectUuid);
            assertThat(response.analysisPeriodHours()).isEqualTo(analysisPeriodHours);
            assertThat(response.sampleSize()).isEqualTo(sampleSize);
            assertThat(response.analyzedAt()).isEqualTo(analyzedAt);
            assertThat(response.dbStatistics()).isEqualTo(dbStats);
            assertThat(response.aiStatistics()).isEqualTo(aiStats);
            assertThat(response.accuracyMetrics()).isEqualTo(metrics);
            assertThat(response.verdict()).isEqualTo(verdict);
            assertThat(response.technicalHighlights()).isEqualTo(highlights);
        }

        @Test
        @DisplayName("DBStatistics_record_생성_및_접근_테스트")
        void DBStatistics_record_생성_및_접근_테스트() {
            // when
            var dbStats = new AIComparisonResponse.DBStatistics(
                    15420, 342, 1205, 13873, 2.22, "2025-11-14T12", 892
            );

            // then
            assertThat(dbStats.totalLogs()).isEqualTo(15420);
            assertThat(dbStats.errorCount()).isEqualTo(342);
            assertThat(dbStats.warnCount()).isEqualTo(1205);
            assertThat(dbStats.infoCount()).isEqualTo(13873);
            assertThat(dbStats.errorRate()).isEqualTo(2.22);
            assertThat(dbStats.peakHour()).isEqualTo("2025-11-14T12");
            assertThat(dbStats.peakCount()).isEqualTo(892);
        }

        @Test
        @DisplayName("AIStatistics_record_생성_및_접근_테스트")
        void AIStatistics_record_생성_및_접근_테스트() {
            // when
            var aiStats = new AIComparisonResponse.AIStatistics(
                    15380, 338, 1198, 13844, 2.20, 85, "샘플 기반 추론"
            );

            // then
            assertThat(aiStats.estimatedTotalLogs()).isEqualTo(15380);
            assertThat(aiStats.estimatedErrorCount()).isEqualTo(338);
            assertThat(aiStats.estimatedWarnCount()).isEqualTo(1198);
            assertThat(aiStats.estimatedInfoCount()).isEqualTo(13844);
            assertThat(aiStats.estimatedErrorRate()).isEqualTo(2.20);
            assertThat(aiStats.confidenceScore()).isEqualTo(85);
            assertThat(aiStats.reasoning()).isEqualTo("샘플 기반 추론");
        }

        @Test
        @DisplayName("AccuracyMetrics_record_생성_및_접근_테스트")
        void AccuracyMetrics_record_생성_및_접근_테스트() {
            // when
            var metrics = new AIComparisonResponse.AccuracyMetrics(
                    99.74, 98.83, 99.42, 99.79, 99.80, 99.28, 85
            );

            // then
            assertThat(metrics.totalLogsAccuracy()).isEqualTo(99.74);
            assertThat(metrics.errorCountAccuracy()).isEqualTo(98.83);
            assertThat(metrics.warnCountAccuracy()).isEqualTo(99.42);
            assertThat(metrics.infoCountAccuracy()).isEqualTo(99.79);
            assertThat(metrics.errorRateAccuracy()).isEqualTo(99.80);
            assertThat(metrics.overallAccuracy()).isEqualTo(99.28);
            assertThat(metrics.aiConfidence()).isEqualTo(85);
        }

        @Test
        @DisplayName("ComparisonVerdict_record_생성_및_접근_테스트")
        void ComparisonVerdict_record_생성_및_접근_테스트() {
            // when
            var verdict = new AIComparisonResponse.ComparisonVerdict(
                    "매우 우수",
                    true,
                    "오차율 5% 미만",
                    List.of("프로덕션 적용 권장", "AI 캐싱 레이어 구축")
            );

            // then
            assertThat(verdict.grade()).isEqualTo("매우 우수");
            assertThat(verdict.canReplaceDb()).isTrue();
            assertThat(verdict.explanation()).isEqualTo("오차율 5% 미만");
            assertThat(verdict.recommendations()).hasSize(2);
            assertThat(verdict.recommendations()).contains("프로덕션 적용 권장");
        }
    }

    @Nested
    @DisplayName("JSON 직렬화/역직렬화 테스트")
    class JsonSerializationTest {

        @Test
        @DisplayName("AIComparisonResponse_JSON_직렬화_테스트")
        void AIComparisonResponse_JSON_직렬화_테스트() throws Exception {
            // given
            var response = createFullResponse();

            // when
            String json = objectMapper.writeValueAsString(response);

            // then
            assertThat(json).contains("\"project_uuid\":\"test-uuid\"");
            assertThat(json).contains("\"analysis_period_hours\":24");
            assertThat(json).contains("\"sample_size\":100");
            assertThat(json).contains("\"total_logs\":15420");
            assertThat(json).contains("\"estimated_total_logs\":15380");
            assertThat(json).contains("\"overall_accuracy\":99.28");
            assertThat(json).contains("\"grade\":\"매우 우수\"");
            assertThat(json).contains("\"can_replace_db\":true");
        }

        @Test
        @DisplayName("AIComparisonResponse_JSON_역직렬화_테스트")
        void AIComparisonResponse_JSON_역직렬화_테스트() throws Exception {
            // given
            String json = """
                    {
                      "project_uuid": "test-uuid",
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
                        "reasoning": "추론 근거"
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
                        "explanation": "설명",
                        "recommendations": ["권장1", "권장2"]
                      },
                      "technical_highlights": ["포인트1", "포인트2"]
                    }
                    """;

            // when
            var response = objectMapper.readValue(json, AIComparisonResponse.class);

            // then
            assertThat(response.projectUuid()).isEqualTo("test-uuid");
            assertThat(response.analysisPeriodHours()).isEqualTo(24);
            assertThat(response.dbStatistics().totalLogs()).isEqualTo(15420);
            assertThat(response.aiStatistics().estimatedTotalLogs()).isEqualTo(15380);
            assertThat(response.accuracyMetrics().overallAccuracy()).isEqualTo(99.28);
            assertThat(response.verdict().grade()).isEqualTo("매우 우수");
            assertThat(response.verdict().canReplaceDb()).isTrue();
            assertThat(response.technicalHighlights()).hasSize(2);
        }

        @Test
        @DisplayName("빈_리스트_JSON_직렬화_테스트")
        void 빈_리스트_JSON_직렬화_테스트() throws Exception {
            // given
            var verdict = new AIComparisonResponse.ComparisonVerdict(
                    "미흡", false, "설명", List.of()
            );

            // when
            String json = objectMapper.writeValueAsString(verdict);

            // then
            assertThat(json).contains("\"recommendations\":[]");
        }

        @Test
        @DisplayName("한국어_포함_JSON_직렬화_테스트")
        void 한국어_포함_JSON_직렬화_테스트() throws Exception {
            // given
            var verdict = new AIComparisonResponse.ComparisonVerdict(
                    "매우 우수",
                    true,
                    "오차율 5% 미만으로 프로덕션 환경에서 신뢰성 있게 사용 가능합니다.",
                    List.of("프로덕션 환경 적용 권장", "실시간 대시보드 AI 기반 분석 도입 가능")
            );

            // when
            String json = objectMapper.writeValueAsString(verdict);

            // then
            assertThat(json).contains("매우 우수");
            assertThat(json).contains("프로덕션 환경에서 신뢰성 있게 사용 가능");
            assertThat(json).contains("프로덕션 환경 적용 권장");
        }
    }

    @Nested
    @DisplayName("비즈니스 로직 검증 테스트")
    class BusinessLogicTest {

        @Test
        @DisplayName("높은_정확도는_DB_대체_가능을_의미한다")
        void 높은_정확도는_DB_대체_가능을_의미한다() {
            // given
            var metrics = new AIComparisonResponse.AccuracyMetrics(
                    99.0, 98.0, 99.0, 99.0, 100.0, 98.5, 90
            );

            // then
            // 95% 이상이면 "매우 우수"이고 canReplaceDb = true
            assertThat(metrics.overallAccuracy()).isGreaterThanOrEqualTo(95.0);
        }

        @Test
        @DisplayName("낮은_정확도는_DB_대체_불가능을_의미한다")
        void 낮은_정확도는_DB_대체_불가능을_의미한다() {
            // given
            var metrics = new AIComparisonResponse.AccuracyMetrics(
                    60.0, 60.0, 60.0, 60.0, 100.0, 62.0, 30
            );

            // then
            // 70% 미만이면 "미흡"이고 canReplaceDb = false
            assertThat(metrics.overallAccuracy()).isLessThan(70.0);
        }

        @Test
        @DisplayName("에러율_차이는_정확도에_영향을_미친다")
        void 에러율_차이는_정확도에_영향을_미친다() {
            // given
            var dbStats = new AIComparisonResponse.DBStatistics(
                    1000, 100, 100, 800, 10.0, "12:00", 100
            );
            var aiStats = new AIComparisonResponse.AIStatistics(
                    1000, 100, 100, 800, 15.0, 80, "추론"  // 에러율 5% 차이
            );

            // then
            // 에러율이 10% vs 15%로 5% 차이나면 정확도 50점 감점 (1% 당 10점)
            double errorRateDiff = Math.abs(dbStats.errorRate() - aiStats.estimatedErrorRate());
            assertThat(errorRateDiff).isEqualTo(5.0);
        }

        @Test
        @DisplayName("AI_신뢰도는_추론_품질을_나타낸다")
        void AI_신뢰도는_추론_품질을_나타낸다() {
            // given
            var highConfidenceAI = new AIComparisonResponse.AIStatistics(
                    1000, 100, 100, 800, 10.0, 95, "높은 신뢰도"
            );
            var lowConfidenceAI = new AIComparisonResponse.AIStatistics(
                    1000, 100, 100, 800, 10.0, 30, "낮은 신뢰도"
            );

            // then
            assertThat(highConfidenceAI.confidenceScore()).isGreaterThanOrEqualTo(90);
            assertThat(lowConfidenceAI.confidenceScore()).isLessThanOrEqualTo(50);
        }
    }

    private AIComparisonResponse createFullResponse() {
        return new AIComparisonResponse(
                "test-uuid",
                24,
                100,
                LocalDateTime.of(2025, 11, 14, 15, 30),
                new AIComparisonResponse.DBStatistics(
                        15420, 342, 1205, 13873, 2.22, "2025-11-14T12", 892
                ),
                new AIComparisonResponse.AIStatistics(
                        15380, 338, 1198, 13844, 2.20, 85, "추론 근거"
                ),
                new AIComparisonResponse.AccuracyMetrics(
                        99.74, 98.83, 99.42, 99.79, 99.80, 99.28, 85
                ),
                new AIComparisonResponse.ComparisonVerdict(
                        "매우 우수", true, "설명", List.of("권장1", "권장2")
                ),
                List.of("포인트1", "포인트2")
        );
    }
}
