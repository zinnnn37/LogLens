package S13P31A306.loglens.domain.analysis.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI HTML 문서 생성 응답
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiHtmlDocumentResponse {

    /**
     * 생성된 HTML 컨텐츠
     */
    private String htmlContent;

    /**
     * 문서 메타데이터
     */
    private AiDocumentMetadata metadata;

    /**
     * 검증 상태
     */
    private AiValidationStatus validationStatus;
}
