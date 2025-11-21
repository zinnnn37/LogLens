package S13P31A306.loglens.domain.analysis.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI 문서 생성 스타일 선호도
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StylePreferences {

    /**
     * CSS 프레임워크 (tailwind, bootstrap)
     */
    @Builder.Default
    @JsonProperty("css_framework")
    private String cssFramework = "tailwind";

    /**
     * 차트 라이브러리 (chartjs, d3)
     */
    @Builder.Default
    @JsonProperty("chart_library")
    private String chartLibrary = "chartjs";

    /**
     * 색상 테마 (blue, green, red)
     */
    @Builder.Default
    @JsonProperty("color_scheme")
    private String colorScheme = "blue";
}
