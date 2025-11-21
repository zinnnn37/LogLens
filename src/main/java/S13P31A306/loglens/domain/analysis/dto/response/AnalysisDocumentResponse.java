package S13P31A306.loglens.domain.analysis.dto.response;

import S13P31A306.loglens.domain.analysis.constants.DocumentFormat;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 분석 문서 생성 응답
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalysisDocumentResponse {

    /**
     * 문서 ID (DB 기본키)
     */
    private Integer documentId;

    /**
     * 프로젝트별 문서 번호 (1부터 시작)
     */
    private Integer documentNumber;

    /**
     * 프로젝트 UUID (프로젝트 분석)
     */
    private String projectUuid;

    /**
     * 로그 ID (에러 분석)
     */
    private Long logId;

    /**
     * 문서 형식
     */
    private DocumentFormat format;

    /**
     * 문서 내용 (HTML, Markdown, JSON 형식인 경우)
     */
    private String content;

    /**
     * PDF 다운로드 URL (PDF 형식인 경우)
     */
    private String downloadUrl;

    /**
     * 파일명 (PDF 형식인 경우)
     */
    private String fileName;

    /**
     * 파일 크기 (bytes, PDF 형식인 경우)
     */
    private Long fileSize;

    /**
     * 만료 시간 (PDF 형식인 경우)
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;

    /**
     * 문서 메타데이터
     */
    private DocumentMetadata documentMetadata;

    /**
     * HTML 검증 상태
     */
    private String validationStatus;

    /**
     * 캐시 TTL (초)
     */
    private Integer cacheTtl;
}
