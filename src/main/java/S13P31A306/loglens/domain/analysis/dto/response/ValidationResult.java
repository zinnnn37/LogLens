package S13P31A306.loglens.domain.analysis.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * HTML 검증 결과
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {

    /**
     * 검증 통과 여부
     */
    private Boolean isValid;

    /**
     * 검증 에러 목록
     */
    @Builder.Default
    private List<String> errors = new ArrayList<>();

    /**
     * 검증 경고 목록
     */
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
}
