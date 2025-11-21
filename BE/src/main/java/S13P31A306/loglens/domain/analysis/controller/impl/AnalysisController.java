package S13P31A306.loglens.domain.analysis.controller.impl;

import S13P31A306.loglens.domain.analysis.constants.AnalysisSuccessCode;
import S13P31A306.loglens.domain.analysis.controller.AnalysisApi;
import S13P31A306.loglens.domain.analysis.dto.request.ErrorAnalysisRequest;
import S13P31A306.loglens.domain.analysis.dto.request.ProjectAnalysisRequest;
import S13P31A306.loglens.domain.analysis.dto.response.AnalysisDocumentDetailResponse;
import S13P31A306.loglens.domain.analysis.dto.response.AnalysisDocumentResponse;
import S13P31A306.loglens.domain.analysis.dto.response.AnalysisDocumentSummary;
import S13P31A306.loglens.domain.analysis.service.AnalysisService;
import S13P31A306.loglens.domain.analysis.service.DocumentGenerationService;
import S13P31A306.loglens.domain.analysis.validator.AnalysisValidator;
import S13P31A306.loglens.global.dto.response.ApiResponseFactory;
import S13P31A306.loglens.global.dto.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 분석 문서 생성 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController implements AnalysisApi {

    private final AnalysisService analysisService;
    private final DocumentGenerationService documentGenerationService;
    private final AnalysisValidator analysisValidator;

    @PostMapping("/projects/{projectUuid}/reports")
    @Override
    public ResponseEntity<? extends BaseResponse> generateProjectAnalysisReport(
            @PathVariable String projectUuid,
            @RequestBody ProjectAnalysisRequest request,
            UserDetails userDetails
    ) {
        log.info("📊 Generating project analysis report: projectUuid={}, format={}",
                projectUuid, request.getFormat());

        // 검증
        analysisValidator.validateProjectAnalysisRequest(projectUuid, request, userDetails);

        // 문서 생성
        AnalysisDocumentResponse response = analysisService.generateProjectAnalysisDocument(
                projectUuid,
                request
        );

        log.info("✅ Project analysis report generated successfully: projectUuid={}, format={}, validationStatus={}",
                projectUuid, request.getFormat(), response.getValidationStatus());

        return ApiResponseFactory.success(
                AnalysisSuccessCode.ANALYSIS_DOCUMENT_CREATED,
                response
        );
    }

    @PostMapping("/errors/{logId}/reports")
    @Override
    public ResponseEntity<? extends BaseResponse> generateErrorAnalysisReport(
            @PathVariable Long logId,
            @RequestBody ErrorAnalysisRequest request,
            UserDetails userDetails
    ) {
        log.info("🔍 Generating error analysis report: logId={}, projectUuid={}, format={}",
                logId, request.getProjectUuid(), request.getFormat());

        // 검증
        analysisValidator.validateErrorAnalysisRequest(logId, request, userDetails);

        // 문서 생성
        AnalysisDocumentResponse response = analysisService.generateErrorAnalysisDocument(
                logId,
                request
        );

        log.info("✅ Error analysis report generated successfully: logId={}, format={}, validationStatus={}",
                logId, request.getFormat(), response.getValidationStatus());

        return ApiResponseFactory.success(
                AnalysisSuccessCode.ANALYSIS_DOCUMENT_CREATED,
                response
        );
    }

    @GetMapping("/downloads/{fileId}")
    @Override
    public ResponseEntity<Resource> downloadPdfDocument(@PathVariable String fileId) {
        log.info("📥 Downloading PDF document: fileId={}", fileId);

        // PDF 파일 조회
        Resource resource = documentGenerationService.getPdfFile(fileId);

        // 파일명 추출
        String filename = fileId + ".pdf";

        log.info("✅ PDF document download ready: fileId={}, filename={}", fileId, filename);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @GetMapping("/projects/{projectUuid}/documents")
    @Override
    public ResponseEntity<? extends BaseResponse> getAnalysisDocuments(
            @PathVariable String projectUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            UserDetails userDetails
    ) {
        log.info("📄 Fetching analysis documents: projectUuid={}, page={}, size={}", projectUuid, page, size);

        // 프로젝트 접근 권한 검증
        analysisValidator.validateProjectAccess(projectUuid, userDetails);

        // 문서 목록 조회
        Pageable pageable = PageRequest.of(page, size);
        Page<AnalysisDocumentSummary> documents = analysisService.getAnalysisDocuments(projectUuid, pageable);

        log.info("✅ Analysis documents retrieved: projectUuid={}, totalElements={}", projectUuid, documents.getTotalElements());

        return ApiResponseFactory.success(
                AnalysisSuccessCode.DOCUMENTS_RETRIEVED,
                documents
        );
    }

    @GetMapping("/projects/{projectUuid}/documents/{documentId}")
    @Override
    public ResponseEntity<? extends BaseResponse> getAnalysisDocumentById(
            @PathVariable String projectUuid,
            @PathVariable Integer documentId,
            UserDetails userDetails
    ) {
        log.info("📄 Fetching analysis document detail: projectUuid={}, documentId={}", projectUuid, documentId);

        // 프로젝트 접근 권한 검증
        analysisValidator.validateProjectAccess(projectUuid, userDetails);

        // 문서 상세 조회
        AnalysisDocumentDetailResponse document = analysisService.getAnalysisDocumentById(documentId, projectUuid);

        log.info("✅ Analysis document retrieved: documentId={}, title={}", documentId, document.getTitle());

        return ApiResponseFactory.success(
                AnalysisSuccessCode.DOCUMENT_DETAIL_RETRIEVED,
                document
        );
    }

    @DeleteMapping("/projects/{projectUuid}/documents/{documentId}")
    @Override
    public ResponseEntity<? extends BaseResponse> deleteAnalysisDocument(
            @PathVariable String projectUuid,
            @PathVariable Integer documentId,
            UserDetails userDetails
    ) {
        log.info("🗑️ Deleting analysis document: projectUuid={}, documentId={}", projectUuid, documentId);

        // 프로젝트 접근 권한 검증
        analysisValidator.validateProjectAccess(projectUuid, userDetails);

        // 문서 삭제
        analysisService.deleteAnalysisDocument(documentId, projectUuid);

        log.info("✅ Analysis document deleted: documentId={}", documentId);

        return ApiResponseFactory.success(
                AnalysisSuccessCode.DOCUMENT_DELETED
        );
    }
}
