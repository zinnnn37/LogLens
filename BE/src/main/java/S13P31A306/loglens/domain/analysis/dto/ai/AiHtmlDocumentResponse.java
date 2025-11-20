package S13P31A306.loglens.domain.analysis.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("html_content")
    private String htmlContent;

    /**
     * 문서 메타데이터
     */
    @JsonProperty("metadata")
    private AiDocumentMetadata metadata;

    /**
     * 검증 상태
     */
    @JsonProperty("validation_status")
    private AiValidationStatus validationStatus;
}
