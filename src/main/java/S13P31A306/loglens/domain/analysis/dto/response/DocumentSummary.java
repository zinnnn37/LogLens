package S13P31A306.loglens.domain.analysis.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 문서 요약 정보
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentSummary {

    /**
     * 건강 점수 (0-100)
     */
    private Integer healthScore;

    /**
     * 전체 이슈 개수
     */
    private Integer totalIssues;

    /**
     * 심각한 이슈 개수
     */
    private Integer criticalIssues;

    /**
     * 권장사항 개수
     */
    private Integer recommendations;

    /**
     * 에러 심각도 (에러 분석용)
     */
    private String severity;

    /**
     * 근본 원인 요약 (에러 분석용)
     */
    private String rootCause;

    /**
     * 영향받은 사용자 수 (에러 분석용)
     */
    private Integer affectedUsers;
}
