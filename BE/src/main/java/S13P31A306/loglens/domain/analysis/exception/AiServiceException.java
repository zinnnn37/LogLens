package S13P31A306.loglens.domain.analysis.exception;

import S13P31A306.loglens.global.exception.BusinessException;
import S13P31A306.loglens.domain.analysis.constants.AnalysisErrorCode;

/**
 * AI 서비스 호출 실패 예외
 */
public class AiServiceException extends BusinessException {

    public AiServiceException(String message) {
        super(AnalysisErrorCode.AI_SERVICE_ERROR, message);
    }

    public AiServiceException(String message, Throwable cause) {
        super(AnalysisErrorCode.AI_SERVICE_ERROR, message, cause);
    }
}
