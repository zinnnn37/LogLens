package S13P31A306.loglens.domain.analysis.constants;

/**
 * 분석 문서 출력 형식
 */
public enum DocumentFormat {
    /**
     * HTML 형식 (AI가 생성한 인터랙티브 HTML)
     */
    HTML,

    /**
     * PDF 형식 (다운로드 가능)
     */
    PDF,

    /**
     * Markdown 형식
     */
    MARKDOWN,

    /**
     * JSON 형식 (구조화된 데이터)
     */
    JSON
}
