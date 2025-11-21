package S13P31A306.loglens.domain.analysis.dto.response;

import S13P31A306.loglens.domain.analysis.constants.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 분석 문서 목록 조회용 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisDocumentSummary {

    private Integer id;
    private Integer documentNumber;
    private String title;
    private DocumentType documentType;
    private String validationStatus;
    private Integer healthScore;
    private Integer totalIssues;
    private Integer criticalIssues;
    private Integer wordCount;
    private String estimatedReadingTime;
    private Long logId;
    private LocalDateTime createdAt;
}
