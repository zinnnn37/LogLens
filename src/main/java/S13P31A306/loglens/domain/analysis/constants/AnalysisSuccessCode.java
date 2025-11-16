package S13P31A306.loglens.domain.analysis.constants;

import S13P31A306.loglens.global.constants.SuccessCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 분석 문서 생성 API 성공 코드
 */
@Getter
@RequiredArgsConstructor
public enum AnalysisSuccessCode implements SuccessCode {

    ANALYSIS_DOCUMENT_CREATED("AN200-1", "분석 문서가 성공적으로 생성되었습니다", 200),
    PDF_DOWNLOAD_READY("AN200-2", "PDF 다운로드 준비가 완료되었습니다", 200),
    DOCUMENTS_RETRIEVED("AN200-3", "문서 목록을 성공적으로 조회했습니다", 200),
    DOCUMENT_DETAIL_RETRIEVED("AN200-4", "문서 상세 정보를 성공적으로 조회했습니다", 200),
    DOCUMENT_DELETED("AN200-5", "문서가 성공적으로 삭제되었습니다", 200);

    private final String code;
    private final String message;
    private final int status;
}
