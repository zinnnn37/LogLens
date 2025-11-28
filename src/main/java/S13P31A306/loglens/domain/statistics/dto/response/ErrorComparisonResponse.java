package S13P31A306.loglens.domain.statistics.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * ERROR 로그 비교 검증 응답
 * Vector KNN 샘플링을 활용한 ERROR 통계 비교 결과
 */
@Schema(description = "ERROR 로그 비교 검증 응답")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ErrorComparisonResponse(

        @Schema(description = "프로젝트 UUID", example = "3a73c7d4-8176-3929-b72f-d5b921daae67")
        String projectUuid,

        @Schema(description = "분석 기간 (시간)", example = "24")
        Integer analysisPeriodHours,

        @Schema(description = "사용된 샘플 크기", example = "100")
        Integer sampleSize,

        @Schema(description = "분석 시점")
        LocalDateTime analyzedAt,

        @Schema(description = "DB ERROR 통계 (Ground Truth)")
        DBErrorStats dbErrorStats,

        @Schema(description = "AI ERROR 추정 결과")
        AIErrorStats aiErrorStats,

        @Schema(description = "정확도 지표")
        ErrorAccuracyMetrics accuracyMetrics,

        @Schema(description = "Vector 샘플링 정보")
        VectorGroupingInfo vectorAnalysis
) {

    /**
     * DB에서 조회한 ERROR 통계
     */
    @Schema(description = "DB ERROR 통계 (Ground Truth)")
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record DBErrorStats(
            @Schema(description = "ERROR 로그 수", example = "342")
            Integer totalErrors,

            @Schema(description = "ERROR 비율 (%)", example = "2.22")
            Double errorRate,

            @Schema(description = "ERROR 최다 발생 시간", example = "2025-11-14T12")
            String peakErrorHour,

            @Schema(description = "최다 시간 ERROR 수", example = "45")
            Integer peakErrorCount
    ) {}

    /**
     * AI 추정 ERROR 통계
     */
    @Schema(description = "AI ERROR 추정 결과")
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record AIErrorStats(
            @Schema(description = "추정 ERROR 로그 수", example = "338")
            Integer estimatedTotalErrors,

            @Schema(description = "추정 ERROR 비율 (%)", example = "2.20")
            Double estimatedErrorRate,

            @Schema(description = "AI 신뢰도 (0-100)", example = "85")
            Integer confidenceScore,

            @Schema(description = "추론 근거", example = "샘플 100개 중 ERROR 패턴 분석 기반 추론")
            String reasoning
    ) {}

    /**
     * ERROR 정확도 메트릭
     */
    @Schema(description = "ERROR 정확도 지표")
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ErrorAccuracyMetrics(
            @Schema(description = "ERROR 수 일치율 (%)", example = "98.83")
            Double errorCountAccuracy,

            @Schema(description = "ERROR 비율 정확도 (%)", example = "99.80")
            Double errorRateAccuracy,

            @Schema(description = "종합 정확도 (%)", example = "99.28")
            Double overallAccuracy
    ) {}

    /**
     * Vector KNN 그룹핑 정보
     */
    @Schema(description = "Vector 샘플링 정보")
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record VectorGroupingInfo(
            @Schema(description = "log_vector 있는 ERROR 개수", example = "280")
            Integer vectorizedErrorCount,

            @Schema(description = "벡터화율 (%)", example = "81.87")
            Double vectorizationRate,

            @Schema(description = "샘플링 방법 (vector_knn 또는 random_fallback)", example = "vector_knn")
            String samplingMethod,

            @Schema(description = "샘플 분포 설명", example = "5 clusters with avg 20 samples each")
            String sampleDistribution
    ) {}
}
