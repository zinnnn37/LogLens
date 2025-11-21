package S13P31A306.loglens.domain.analysis.service;

import S13P31A306.loglens.domain.analysis.constants.DocumentFormat;
import S13P31A306.loglens.domain.analysis.constants.DocumentType;
import S13P31A306.loglens.domain.analysis.dto.response.AnalysisDocumentResponse;
import org.springframework.core.io.Resource;

import java.util.Map;

/**
 * 문서 생성 서비스
 */
public interface DocumentGenerationService {

    /**
     * 분석 문서 생성
     *
     * @param projectUuid  프로젝트 UUID
     * @param logId        로그 ID (에러 분석인 경우)
     * @param format       문서 형식
     * @param documentType 문서 타입
     * @param data         분석 데이터
     * @param options      생성 옵션
     * @return 생성된 문서 응답
     */
    AnalysisDocumentResponse generateDocument(
            String projectUuid,
            Long logId,
            DocumentFormat format,
            DocumentType documentType,
            Map<String, Object> data,
            Map<String, Object> options
    );

    /**
     * PDF 파일 조회
     *
     * @param fileId 파일 ID
     * @return PDF 파일 리소스
     */
    Resource getPdfFile(String fileId);
}
