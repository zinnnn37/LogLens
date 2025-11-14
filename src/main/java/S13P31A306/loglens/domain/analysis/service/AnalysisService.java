package S13P31A306.loglens.domain.analysis.service;

import S13P31A306.loglens.domain.analysis.constants.DocumentFormat;
import S13P31A306.loglens.domain.analysis.dto.request.ErrorAnalysisRequest;
import S13P31A306.loglens.domain.analysis.dto.request.ProjectAnalysisRequest;
import S13P31A306.loglens.domain.analysis.dto.response.AnalysisDocumentResponse;

/**
 * 분석 문서 생성 서비스
 */
public interface AnalysisService {

    /**
     * 프로젝트 전체 분석 문서 생성
     *
     * @param projectUuid 프로젝트 UUID
     * @param request     분석 요청
     * @return 생성된 문서 응답
     */
    AnalysisDocumentResponse generateProjectAnalysisDocument(
            String projectUuid,
            ProjectAnalysisRequest request
    );

    /**
     * 에러 상세 분석 문서 생성
     *
     * @param logId   로그 ID
     * @param request 분석 요청
     * @return 생성된 문서 응답
     */
    AnalysisDocumentResponse generateErrorAnalysisDocument(
            Long logId,
            ErrorAnalysisRequest request
    );
}
