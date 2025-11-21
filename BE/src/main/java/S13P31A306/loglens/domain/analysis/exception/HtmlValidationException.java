package S13P31A306.loglens.domain.analysis.exception;

import S13P31A306.loglens.global.exception.BusinessException;
import S13P31A306.loglens.domain.analysis.constants.AnalysisErrorCode;

/**
 * HTML 검증 실패 예외
 */
public class HtmlValidationException extends BusinessException {

    public HtmlValidationException(String message) {
        super(AnalysisErrorCode.HTML_VALIDATION_FAILED, message);
    }

    public HtmlValidationException(String message, Throwable cause) {
        super(AnalysisErrorCode.HTML_VALIDATION_FAILED, message, cause);
    }
}
