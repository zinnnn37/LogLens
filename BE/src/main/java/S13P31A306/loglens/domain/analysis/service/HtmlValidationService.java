package S13P31A306.loglens.domain.analysis.service;

import S13P31A306.loglens.domain.analysis.dto.response.ValidationResult;

/**
 * HTML 검증 서비스
 */
public interface HtmlValidationService {

    /**
     * HTML 유효성 검증
     *
     * @param html 검증할 HTML 문자열
     * @return 검증 결과
     */
    ValidationResult validate(String html);

    /**
     * HTML Sanitization (XSS 방어)
     *
     * @param html 정제할 HTML 문자열
     * @return 정제된 HTML
     */
    String sanitize(String html);
}
