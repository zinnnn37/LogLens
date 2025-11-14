package S13P31A306.loglens.domain.analysis.dto.ai;

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
    private Integer wordCount;

    /**
     * 예상 읽기 시간
     */
    private String estimatedReadingTime;

    /**
     * 생성된 섹션 목록
     */
    private List<String> sectionsGenerated;

    /**
     * 포함된 차트 목록
     */
    private List<String> chartsIncluded;

    /**
     * 사용된 CSS 라이브러리
     */
    private List<String> cssLibrariesUsed;

    /**
     * 사용된 JS 라이브러리
     */
    private List<String> jsLibrariesUsed;

    /**
     * 생성 시간 (초)
     */
    private Double generationTime;

    /**
     * 건강 점수 (프로젝트 분석)
     */
    private Integer healthScore;

    /**
     * 심각한 이슈 개수
     */
    private Integer criticalIssues;

    /**
     * 전체 이슈 개수
     */
    private Integer totalIssues;

    /**
     * 권장사항 개수
     */
    private Integer recommendations;

    /**
     * 에러 심각도 (에러 분석)
     */
    private String severity;

    /**
     * 근본 원인 (에러 분석)
     */
    private String rootCause;

    /**
     * 영향받은 사용자 수 (에러 분석)
     */
    private Integer affectedUsers;
}
