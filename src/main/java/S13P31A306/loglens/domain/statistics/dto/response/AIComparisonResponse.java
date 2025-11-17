package S13P31A306.loglens.domain.statistics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AI vs DB 통계 비교 검증 응답
 * LLM이 DB 쿼리를 대체할 수 있는 역량을 검증하는 결과
 */
@Schema(description = "AI vs DB 통계 비교 검증 응답")
public record AIComparisonResponse(

        @Schema(description = "프로젝트 UUID", example = "3a73c7d4-8176-3929-b72f-d5b921daae67")
        String projectUuid,

        @Schema(description = "분석 기간 (시간)", example = "24")
        Integer analysisPeriodHours,

        @Schema(description = "사용된 샘플 크기", example = "100")
        Integer sampleSize,

        @Schema(description = "분석 시점")
        LocalDateTime analyzedAt,

        @Schema(description = "DB 직접 조회 결과 (Ground Truth)")
        DBStatistics dbStatistics,

        @Schema(description = "AI(LLM) 추론 결과")
        AIStatistics aiStatistics,

        @Schema(description = "정확도 지표")
        AccuracyMetrics accuracyMetrics,

        @Schema(description = "검증 결론")
        ComparisonVerdict verdict,

        @Schema(description = "기술적 어필 포인트")
        List<String> technicalHighlights
) {

    /**
     * DB 직접 조회 통계
     */
    @Schema(description = "DB 직접 조회 통계 (Ground Truth)")
    public record DBStatistics(
            @Schema(description = "총 로그 수", example = "15420")
            Integer totalLogs,

            @Schema(description = "ERROR 로그 수", example = "342")
            Integer errorCount,

            @Schema(description = "WARN 로그 수", example = "1205")
            Integer warnCount,

            @Schema(description = "INFO 로그 수", example = "13873")
            Integer infoCount,

            @Schema(description = "에러율 (%)", example = "2.22")
            Double errorRate,

            @Schema(description = "피크 시간", example = "2025-11-14T12")
            String peakHour,

            @Schema(description = "피크 시간 로그 수", example = "892")
            Integer peakCount
    ) {}

    /**
     * AI(LLM) 추론 통계
     */
    @Schema(description = "AI(LLM) 추론 통계")
    public record AIStatistics(
            @Schema(description = "추론한 총 로그 수", example = "15380")
            Integer estimatedTotalLogs,

            @Schema(description = "추론한 ERROR 로그 수", example = "338")
            Integer estimatedErrorCount,

            @Schema(description = "추론한 WARN 로그 수", example = "1198")
            Integer estimatedWarnCount,

            @Schema(description = "추론한 INFO 로그 수", example = "13844")
            Integer estimatedInfoCount,

            @Schema(description = "추론한 에러율 (%)", example = "2.20")
            Double estimatedErrorRate,

            @Schema(description = "AI 신뢰도 (0-100)", example = "85")
            Integer confidenceScore,

            @Schema(description = "추론 근거", example = "샘플 100개 중 ERROR 2.2% 비율을 전체에 적용")
            String reasoning
    ) {}

    /**
     * 정확도 지표
     */
    @Schema(description = "정확도 지표")
    public record AccuracyMetrics(
            @Schema(description = "총 로그 수 일치율 (%)", example = "99.74")
            Double totalLogsAccuracy,

            @Schema(description = "ERROR 수 일치율 (%)", example = "98.83")
            Double errorCountAccuracy,

            @Schema(description = "WARN 수 일치율 (%)", example = "99.42")
            Double warnCountAccuracy,

            @Schema(description = "INFO 수 일치율 (%)", example = "99.79")
            Double infoCountAccuracy,

            @Schema(description = "에러율 정확도 (%)", example = "99.80")
            Double errorRateAccuracy,

            @Schema(description = "종합 정확도 (%)", example = "99.28")
            Double overallAccuracy,

            @Schema(description = "AI 자체 신뢰도", example = "85")
            Integer aiConfidence
    ) {}

    /**
     * 검증 결론
     */
    @Schema(description = "검증 결론")
    public record ComparisonVerdict(
            @Schema(description = "등급", example = "매우 우수")
            String grade,

            @Schema(description = "DB 대체 가능 여부", example = "true")
            Boolean canReplaceDb,

            @Schema(description = "설명", example = "오차율 5% 미만으로 프로덕션 환경에서 신뢰성 있게 사용 가능합니다.")
            String explanation,

            @Schema(description = "권장 사항")
            List<String> recommendations
    ) {}
}
