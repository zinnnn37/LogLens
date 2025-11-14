package S13P31A306.loglens.domain.analysis.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 자체 검증 상태
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiValidationStatus {

    /**
     * 유효한 HTML 여부
     */
    private Boolean isValidHtml;

    /**
     * 필수 섹션 포함 여부
     */
    private Boolean hasRequiredSections;

    /**
     * 경고 메시지 목록
     */
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
}
