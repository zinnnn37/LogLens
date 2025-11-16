package S13P31A306.loglens.domain.analysis.service;

import S13P31A306.loglens.domain.analysis.constants.DocumentFormat;
import S13P31A306.loglens.domain.analysis.dto.request.ErrorAnalysisRequest;
import S13P31A306.loglens.domain.analysis.dto.request.ProjectAnalysisRequest;
import S13P31A306.loglens.domain.analysis.dto.response.AnalysisDocumentDetailResponse;
import S13P31A306.loglens.domain.analysis.dto.response.AnalysisDocumentResponse;
import S13P31A306.loglens.domain.analysis.dto.response.AnalysisDocumentSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    /**
     * 프로젝트별 문서 목록 조회
     *
     * @param projectUuid 프로젝트 UUID
     * @param pageable    페이지 정보
     * @return 문서 목록 (페이지네이션)
     */
    Page<AnalysisDocumentSummary> getAnalysisDocuments(
            String projectUuid,
            Pageable pageable
    );

    /**
     * 문서 상세 조회
     *
     * @param documentId  문서 ID
     * @param projectUuid 프로젝트 UUID
     * @return 문서 상세 정보
     */
    AnalysisDocumentDetailResponse getAnalysisDocumentById(
            Integer documentId,
            String projectUuid
    );

    /**
     * 문서 삭제
     *
     * @param documentId  문서 ID
     * @param projectUuid 프로젝트 UUID
     */
    void deleteAnalysisDocument(
            Integer documentId,
            String projectUuid
    );
}
