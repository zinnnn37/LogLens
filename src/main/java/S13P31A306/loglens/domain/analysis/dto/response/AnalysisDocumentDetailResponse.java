package S13P31A306.loglens.domain.analysis.dto.response;

import S13P31A306.loglens.domain.analysis.constants.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 분석 문서 상세 조회 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisDocumentDetailResponse {

    private Integer id;
    private String projectUuid;
    private DocumentType documentType;
    private String title;
    private String content;
    private Long logId;
    private String validationStatus;
    private Integer healthScore;
    private Integer totalIssues;
    private Integer criticalIssues;
    private Integer wordCount;
    private String estimatedReadingTime;
    private LocalDateTime createdAt;
}
