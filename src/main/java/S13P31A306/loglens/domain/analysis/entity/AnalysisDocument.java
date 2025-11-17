package S13P31A306.loglens.domain.analysis.entity;

import S13P31A306.loglens.domain.analysis.constants.DocumentType;
import S13P31A306.loglens.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI 분석 문서 엔티티
 * 생성된 프로젝트/에러 분석 문서를 저장
 */
@Entity
@Table(name = "analysis_documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AnalysisDocument extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @NotNull
    @Column(name = "project_id", nullable = false, columnDefinition = "INT")
    private Integer projectId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    @NotNull
    @Column(name = "title", nullable = false, length = 256)
    private String title;

    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "log_id")
    private Long logId;

    @Column(name = "validation_status", length = 50)
    private String validationStatus;

    @Column(name = "health_score")
    private Integer healthScore;

    @Column(name = "total_issues")
    private Integer totalIssues;

    @Column(name = "critical_issues")
    private Integer criticalIssues;

    @Column(name = "word_count")
    private Integer wordCount;

    @Column(name = "estimated_reading_time", length = 50)
    private String estimatedReadingTime;

    @Column(name = "document_number")
    private Integer documentNumber;
}
