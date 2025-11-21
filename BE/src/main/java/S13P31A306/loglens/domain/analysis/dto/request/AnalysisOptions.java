package S13P31A306.loglens.domain.analysis.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 분석 문서 생성 옵션
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisOptions {

    /**
     * 컴포넌트 분석 포함 여부
     */
    @Builder.Default
    private Boolean includeComponents = true;

    /**
     * 알림 히스토리 포함 여부
     */
    @Builder.Default
    private Boolean includeAlerts = true;

    /**
     * 의존성 그래프 포함 여부
     */
    @Builder.Default
    private Boolean includeDependencies = true;

    /**
     * 차트 포함 여부 (Chart.js 사용)
     */
    @Builder.Default
    private Boolean includeCharts = true;

    /**
     * 다크모드 스타일 적용 여부
     */
    @Builder.Default
    private Boolean darkMode = false;
}
