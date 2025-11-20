package S13P31A306.loglens.domain.analysis.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 에러 분석 문서 생성 옵션
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorAnalysisOptions {

    /**
     * 관련 로그 포함 여부 (동일 traceId)
     */
    @Builder.Default
    private Boolean includeRelatedLogs = true;

    /**
     * 유사 에러 분석 포함 여부
     */
    @Builder.Default
    private Boolean includeSimilarErrors = true;

    /**
     * 영향 범위 분석 포함 여부
     */
    @Builder.Default
    private Boolean includeImpactAnalysis = true;

    /**
     * 코드 예시 포함 여부
     */
    @Builder.Default
    private Boolean includeCodeExamples = true;

    /**
     * 관련 로그 최대 개수
     */
    @Builder.Default
    private Integer maxRelatedLogs = 10;
}
