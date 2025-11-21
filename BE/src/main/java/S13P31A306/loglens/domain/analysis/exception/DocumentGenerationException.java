package S13P31A306.loglens.domain.analysis.exception;

import S13P31A306.loglens.global.exception.BusinessException;
import S13P31A306.loglens.domain.analysis.constants.AnalysisErrorCode;

/**
 * 문서 생성 실패 예외
 */
public class DocumentGenerationException extends BusinessException {

    public DocumentGenerationException(String message) {
        super(AnalysisErrorCode.DOCUMENT_GENERATION_ERROR, message);
    }

    public DocumentGenerationException(String message, Throwable cause) {
        super(AnalysisErrorCode.DOCUMENT_GENERATION_ERROR, message, cause);
    }
}
