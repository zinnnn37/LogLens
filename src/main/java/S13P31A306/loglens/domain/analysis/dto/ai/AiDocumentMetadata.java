package S13P31A306.loglens.domain.analysis.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 생성 문서 메타데이터
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiDocumentMetadata {

    /**
     * 단어 수
     */
    @JsonProperty("word_count")
    private Integer wordCount;

    /**
     * 예상 읽기 시간
     */
    @JsonProperty("estimated_reading_time")
    private String estimatedReadingTime;

    /**
     * 생성된 섹션 목록
     */
    @JsonProperty("sections_generated")
    private List<String> sectionsGenerated;

    /**
     * 포함된 차트 목록
     */
    @JsonProperty("charts_included")
    private List<String> chartsIncluded;

    /**
     * 사용된 CSS 라이브러리
     */
    @JsonProperty("css_libraries_used")
    private List<String> cssLibrariesUsed;

    /**
     * 사용된 JS 라이브러리
     */
    @JsonProperty("js_libraries_used")
    private List<String> jsLibrariesUsed;

    /**
     * 생성 시간 (초)
     */
    @JsonProperty("generation_time")
    private Double generationTime;

    /**
     * 건강 점수 (프로젝트 분석)
     */
    @JsonProperty("health_score")
    private Integer healthScore;

    /**
     * 심각한 이슈 개수
     */
    @JsonProperty("critical_issues")
    private Integer criticalIssues;

    /**
     * 전체 이슈 개수
     */
    @JsonProperty("total_issues")
    private Integer totalIssues;

    /**
     * 권장사항 개수
     */
    @JsonProperty("recommendations")
    private Integer recommendations;

    /**
     * 에러 심각도 (에러 분석)
     */
    @JsonProperty("severity")
    private String severity;

    /**
     * 근본 원인 (에러 분석)
     */
    @JsonProperty("root_cause")
    private String rootCause;

    /**
     * 영향받은 사용자 수 (에러 분석)
     */
    @JsonProperty("affected_users")
    private Integer affectedUsers;
}
