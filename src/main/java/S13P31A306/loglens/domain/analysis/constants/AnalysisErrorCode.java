package S13P31A306.loglens.domain.analysis.constants;

import S13P31A306.loglens.global.constants.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 분석 문서 생성 API 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum AnalysisErrorCode implements ErrorCode {

    INVALID_TIME_RANGE("AN400-1", "시간 범위가 유효하지 않습니다", 400),
    INVALID_FORMAT("AN400-2", "지원하지 않는 문서 형식입니다", 400),
    INVALID_OPTIONS("AN400-3", "잘못된 옵션 설정입니다", 400),

    LOG_NOT_FOUND("AN404-1", "로그를 찾을 수 없습니다", 404),
    PDF_FILE_NOT_FOUND("AN404-2", "PDF 파일이 존재하지 않거나 만료되었습니다", 404),

    HTML_VALIDATION_FAILED("AN500-1", "HTML 검증에 실패했습니다", 500),
    AI_SERVICE_ERROR("AN500-2", "AI 서비스 오류가 발생했습니다", 500),
    DOCUMENT_GENERATION_ERROR("AN500-3", "문서 생성 중 오류가 발생했습니다", 500),
    PDF_CONVERSION_ERROR("AN500-4", "PDF 변환 중 오류가 발생했습니다", 500);

    private final String code;
    private final String message;
    private final int status;
}
