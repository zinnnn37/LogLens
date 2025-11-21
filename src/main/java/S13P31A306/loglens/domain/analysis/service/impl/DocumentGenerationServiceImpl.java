package S13P31A306.loglens.domain.analysis.service.impl;

import S13P31A306.loglens.domain.analysis.constants.DocumentFormat;
import S13P31A306.loglens.domain.analysis.constants.DocumentType;
import S13P31A306.loglens.domain.analysis.dto.ai.AiHtmlDocumentRequest;
import S13P31A306.loglens.domain.analysis.dto.ai.AiHtmlDocumentResponse;
import S13P31A306.loglens.domain.analysis.dto.ai.StylePreferences;
import S13P31A306.loglens.domain.analysis.dto.response.AnalysisDocumentResponse;
import S13P31A306.loglens.domain.analysis.dto.response.DocumentMetadata;
import S13P31A306.loglens.domain.analysis.dto.response.DocumentSummary;
import S13P31A306.loglens.domain.analysis.dto.response.TimeRange;
import S13P31A306.loglens.domain.analysis.dto.response.ValidationResult;
import S13P31A306.loglens.domain.analysis.exception.AiServiceException;
import S13P31A306.loglens.domain.analysis.exception.DocumentGenerationException;
import S13P31A306.loglens.domain.analysis.mapper.AnalysisMapper;
import S13P31A306.loglens.domain.analysis.service.DocumentGenerationService;
import S13P31A306.loglens.domain.analysis.service.HtmlValidationService;
import S13P31A306.loglens.global.client.AiServiceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * 문서 생성 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentGenerationServiceImpl implements DocumentGenerationService {

    private final AiServiceClient aiServiceClient;
    private final HtmlValidationService htmlValidationService;
    private final AnalysisMapper analysisMapper;
    private final ObjectMapper objectMapper;

    @Value("${analysis.pdf.storage-path:/tmp/loglens/pdfs}")
    private String pdfStoragePath;

    private static final int MAX_RETRY = 2;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public AnalysisDocumentResponse generateDocument(
            String projectUuid,
            Long logId,
            DocumentFormat format,
            DocumentType documentType,
            Map<String, Object> data,
            Map<String, Object> options
    ) {
        log.info("Generating {} document in {} format", documentType, format);

        return switch (format) {
            case HTML -> generateHtmlDocument(projectUuid, logId, documentType, data, options);
            case PDF -> generatePdfDocument(projectUuid, logId, documentType, data, options);
            case MARKDOWN -> generateMarkdownDocument(projectUuid, logId, documentType, data, options);
            case JSON -> generateJsonDocument(projectUuid, logId, documentType, data, options);
        };
    }

    @Override
    public Resource getPdfFile(String fileId) {
        Path filePath = Paths.get(pdfStoragePath, fileId + ".pdf");

        if (!Files.exists(filePath)) {
            throw new DocumentGenerationException("PDF 파일을 찾을 수 없습니다: " + fileId);
        }

        return new FileSystemResource(filePath);
    }

    /**
     * HTML 문서 생성 (AI 사용)
     */
    private AnalysisDocumentResponse generateHtmlDocument(
            String projectUuid,
            Long logId,
            DocumentType documentType,
            Map<String, Object> data,
            Map<String, Object> options
    ) {
        // 1. AI 요청 생성
        AiHtmlDocumentRequest aiRequest = buildAiRequest(
                projectUuid, logId, documentType, DocumentFormat.HTML, data, options
        );

        // 2. AI에게 HTML 생성 요청 (재시도 로직 포함)
        AiHtmlDocumentResponse aiResponse = null;
        ValidationResult validation = null;

        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                // AI 호출
                aiResponse = callAiService(aiRequest, documentType);

                if (aiResponse == null || aiResponse.getHtmlContent() == null) {
                    log.warn("AI returned null response on attempt {}", attempt);
                    if (attempt == MAX_RETRY) {
                        throw new AiServiceException("AI 서비스가 응답하지 않습니다");
                    }
                    continue;
                }

                // 3. HTML 검증
                validation = htmlValidationService.validate(aiResponse.getHtmlContent());

                if (validation.getIsValid()) {
                    log.info("HTML validation passed on attempt {}", attempt);
                    break;
                }

                log.warn("HTML validation failed on attempt {}: {}", attempt, validation.getErrors());

                // 재시도: 이전 검증 에러 피드백 포함
                if (attempt < MAX_RETRY) {
                    aiResponse = aiServiceClient.regenerateWithFeedback(aiRequest, validation.getErrors());
                }

            } catch (Exception e) {
                log.error("AI service error on attempt {}: {}", attempt, e.getMessage());
                if (attempt == MAX_RETRY) {
                    log.error("All attempts failed, using fallback HTML");
                    return buildFallbackResponse(projectUuid, logId, documentType, data);
                }
            }
        }

        // 4. 최종 검증 실패 시 fallback
        if (validation == null || !validation.getIsValid()) {
            log.warn("HTML validation failed after {} attempts, using fallback", MAX_RETRY);
            return buildFallbackResponse(projectUuid, logId, documentType, data);
        }

        // 5. Sanitization (선택적)
        // String safeHtml = htmlValidationService.sanitize(aiResponse.getHtmlContent());

        // 6. 응답 생성
        return buildHtmlResponse(projectUuid, logId, aiResponse, DocumentFormat.HTML);
    }

    /**
     * PDF 문서 생성 (HTML을 PDF로 변환)
     */
    private AnalysisDocumentResponse generatePdfDocument(
            String projectUuid,
            Long logId,
            DocumentType documentType,
            Map<String, Object> data,
            Map<String, Object> options
    ) {
        // 1. 먼저 HTML 생성
        AnalysisDocumentResponse htmlDoc = generateHtmlDocument(
                projectUuid, logId, documentType, data, options
        );

        // 2. HTML → PDF 변환
        try {
            byte[] pdfBytes = convertHtmlToPdf(htmlDoc.getContent());

            // 3. 임시 파일로 저장
            String fileId = UUID.randomUUID().toString();
            String fileName = generateFileName(documentType, projectUuid, logId);
            Path savedPath = savePdfFile(fileId, pdfBytes);

            // 4. 다운로드 URL 생성
            String downloadUrl = "/api/analysis/downloads/" + fileId;

            return AnalysisDocumentResponse.builder()
                    .projectUuid(projectUuid)
                    .logId(logId)
                    .format(DocumentFormat.PDF)
                    .downloadUrl(downloadUrl)
                    .fileName(fileName)
                    .fileSize((long) pdfBytes.length)
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .documentMetadata(htmlDoc.getDocumentMetadata())
                    .validationStatus("PDF_GENERATED")
                    .build();

        } catch (Exception e) {
            log.error("PDF conversion failed: {}", e.getMessage(), e);
            throw new DocumentGenerationException("PDF 생성 실패", e);
        }
    }

    /**
     * Markdown 문서 생성
     */
    private AnalysisDocumentResponse generateMarkdownDocument(
            String projectUuid,
            Long logId,
            DocumentType documentType,
            Map<String, Object> data,
            Map<String, Object> options
    ) {
        StringBuilder md = new StringBuilder();

        // 프로젝트 분석
        if (documentType == DocumentType.PROJECT_ANALYSIS) {
            md.append("# 프로젝트 분석 보고서\n\n");

            Map<String, Object> projectInfo = (Map<String, Object>) data.get("projectInfo");
            if (projectInfo != null) {
                md.append("## 프로젝트 정보\n\n");
                md.append("- **프로젝트명**: ").append(projectInfo.get("name")).append("\n");
                md.append("- **UUID**: ").append(projectInfo.get("uuid")).append("\n\n");
            }

            Map<String, Object> metrics = (Map<String, Object>) data.get("metrics");
            if (metrics != null) {
                md.append("## 주요 메트릭\n\n");
                md.append("- **총 로그 수**: ").append(metrics.get("totalLogs")).append("\n");
                md.append("- **에러 수**: ").append(metrics.get("errorCount")).append("\n");
                md.append("- **경고 수**: ").append(metrics.get("warnCount")).append("\n");
                md.append("- **평균 응답 시간**: ").append(metrics.get("avgResponseTime")).append("ms\n\n");
            }
        }
        // 에러 분석
        else {
            md.append("# 에러 상세 분석 보고서\n\n");

            Map<String, Object> errorLog = (Map<String, Object>) data.get("errorLog");
            if (errorLog != null) {
                md.append("## 에러 정보\n\n");
                md.append("- **로그 ID**: ").append(errorLog.get("logId")).append("\n");
                md.append("- **레벨**: ").append(errorLog.get("level")).append("\n");
                md.append("- **메시지**: ").append(errorLog.get("message")).append("\n");
                md.append("- **발생 시간**: ").append(errorLog.get("timestamp")).append("\n\n");
            }
        }

        md.append("---\n\n");
        md.append("*생성 시간: ").append(LocalDateTime.now().format(FORMATTER)).append("*\n");

        return AnalysisDocumentResponse.builder()
                .projectUuid(projectUuid)
                .logId(logId)
                .format(DocumentFormat.MARKDOWN)
                .content(md.toString())
                .documentMetadata(buildSimpleMetadata(documentType))
                .build();
    }

    /**
     * JSON 문서 생성
     */
    private AnalysisDocumentResponse generateJsonDocument(
            String projectUuid,
            Long logId,
            DocumentType documentType,
            Map<String, Object> data,
            Map<String, Object> options
    ) {
        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(data);

            return AnalysisDocumentResponse.builder()
                    .projectUuid(projectUuid)
                    .logId(logId)
                    .format(DocumentFormat.JSON)
                    .content(json)
                    .documentMetadata(buildSimpleMetadata(documentType))
                    .build();

        } catch (Exception e) {
            log.error("JSON generation failed: {}", e.getMessage(), e);
            throw new DocumentGenerationException("JSON 생성 실패", e);
        }
    }

    /**
     * AI 요청 생성
     */
    private AiHtmlDocumentRequest buildAiRequest(
            String projectUuid,
            Long logId,
            DocumentType documentType,
            DocumentFormat format,
            Map<String, Object> data,
            Map<String, Object> options
    ) {
        return AiHtmlDocumentRequest.builder()
                .projectUuid(projectUuid)
                .logId(logId)
                .documentType(documentType)
                .format(format)
                .data(data)
                .options(options)
                .stylePreferences(StylePreferences.builder()
                        .cssFramework("tailwind")
                        .chartLibrary("chartjs")
                        .colorScheme("blue")
                        .build())
                .build();
    }

    /**
     * AI 서비스 호출
     */
    private AiHtmlDocumentResponse callAiService(
            AiHtmlDocumentRequest request,
            DocumentType documentType
    ) {
        return switch (documentType) {
            case PROJECT_ANALYSIS -> aiServiceClient.generateProjectAnalysisHtml(request);
            case ERROR_ANALYSIS -> aiServiceClient.generateErrorAnalysisHtml(request);
        };
    }

    /**
     * HTML → PDF 변환
     */
    private byte[] convertHtmlToPdf(String html) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        }
    }

    /**
     * PDF 파일 저장
     */
    private Path savePdfFile(String fileId, byte[] pdfBytes) throws IOException {
        Path directory = Paths.get(pdfStoragePath);
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }

        Path filePath = directory.resolve(fileId + ".pdf");
        Files.write(filePath, pdfBytes);

        log.info("PDF file saved: {}", filePath);
        return filePath;
    }

    /**
     * 파일명 생성
     */
    private String generateFileName(DocumentType documentType, String projectUuid, Long logId) {
        String prefix = documentType == DocumentType.PROJECT_ANALYSIS ? "project-analysis" : "error-analysis";
        String identifier = logId != null ? logId.toString() : projectUuid.substring(0, 8);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        return String.format("%s-%s-%s.pdf", prefix, identifier, timestamp);
    }

    /**
     * HTML 응답 생성
     */
    private AnalysisDocumentResponse buildHtmlResponse(
            String projectUuid,
            Long logId,
            AiHtmlDocumentResponse aiResponse,
            DocumentFormat format
    ) {
        DocumentSummary summary = null;
        if (aiResponse.getMetadata() != null) {
            summary = DocumentSummary.builder()
                    .healthScore(aiResponse.getMetadata().getHealthScore())
                    .totalIssues(aiResponse.getMetadata().getTotalIssues())
                    .criticalIssues(aiResponse.getMetadata().getCriticalIssues())
                    .recommendations(aiResponse.getMetadata().getRecommendations())
                    .severity(aiResponse.getMetadata().getSeverity())
                    .rootCause(aiResponse.getMetadata().getRootCause())
                    .affectedUsers(aiResponse.getMetadata().getAffectedUsers())
                    .build();
        }

        DocumentMetadata metadata = DocumentMetadata.builder()
                .title(logId != null ? "에러 상세 분석 보고서" : "프로젝트 종합 분석 보고서")
                .generatedAt(LocalDateTime.now())
                .summary(summary)
                .wordCount(aiResponse.getMetadata() != null ? aiResponse.getMetadata().getWordCount() : null)
                .estimatedReadingTime(aiResponse.getMetadata() != null ? aiResponse.getMetadata().getEstimatedReadingTime() : null)
                .build();

        return AnalysisDocumentResponse.builder()
                .projectUuid(projectUuid)
                .logId(logId)
                .format(format)
                .content(aiResponse.getHtmlContent())
                .documentMetadata(metadata)
                .validationStatus("VALID")
                .cacheTtl(3600)
                .build();
    }

    /**
     * Fallback HTML 응답 생성
     */
    private AnalysisDocumentResponse buildFallbackResponse(
            String projectUuid,
            Long logId,
            DocumentType documentType,
            Map<String, Object> data
    ) {
        String fallbackHtml = generateFallbackHtml(documentType, data);

        return AnalysisDocumentResponse.builder()
                .projectUuid(projectUuid)
                .logId(logId)
                .format(DocumentFormat.HTML)
                .content(fallbackHtml)
                .documentMetadata(buildSimpleMetadata(documentType))
                .validationStatus("FALLBACK")
                .build();
    }

    /**
     * Fallback HTML 생성 (간단한 템플릿)
     */
    private String generateFallbackHtml(DocumentType documentType, Map<String, Object> data) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html lang='ko'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>분석 보고서</title>");
        html.append("<style>");
        html.append("body { font-family: sans-serif; padding: 20px; max-width: 1200px; margin: 0 auto; }");
        html.append("h1 { color: #2563eb; }");
        html.append(".metric { display: inline-block; margin: 10px; padding: 15px; background: #f3f4f6; border-radius: 8px; }");
        html.append(".metric-value { font-size: 24px; font-weight: bold; color: #1f2937; }");
        html.append(".metric-label { font-size: 14px; color: #6b7280; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");

        if (documentType == DocumentType.PROJECT_ANALYSIS) {
            html.append("<h1>📊 프로젝트 분석 보고서</h1>");

            Map<String, Object> projectInfo = (Map<String, Object>) data.get("projectInfo");
            if (projectInfo != null) {
                html.append("<h2>프로젝트: ").append(projectInfo.get("name")).append("</h2>");
            }

            Map<String, Object> metrics = (Map<String, Object>) data.get("metrics");
            if (metrics != null) {
                html.append("<div class='metrics'>");
                html.append("<div class='metric'><div class='metric-value'>")
                        .append(metrics.get("totalLogs"))
                        .append("</div><div class='metric-label'>총 로그</div></div>");
                html.append("<div class='metric'><div class='metric-value'>")
                        .append(metrics.get("errorCount"))
                        .append("</div><div class='metric-label'>에러</div></div>");
                html.append("</div>");
            }
        } else {
            html.append("<h1>🔍 에러 상세 분석 보고서</h1>");

            Map<String, Object> errorLog = (Map<String, Object>) data.get("errorLog");
            if (errorLog != null) {
                html.append("<p><strong>로그 ID:</strong> ").append(errorLog.get("logId")).append("</p>");
                html.append("<p><strong>레벨:</strong> ").append(errorLog.get("level")).append("</p>");
                html.append("<p><strong>메시지:</strong> ").append(errorLog.get("message")).append("</p>");
            }
        }

        html.append("<hr>");
        html.append("<p><small>생성 시간: ").append(LocalDateTime.now().format(FORMATTER)).append("</small></p>");
        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    /**
     * 간단한 메타데이터 생성
     */
    private DocumentMetadata buildSimpleMetadata(DocumentType documentType) {
        return DocumentMetadata.builder()
                .title(documentType == DocumentType.PROJECT_ANALYSIS ? "프로젝트 분석 보고서" : "에러 분석 보고서")
                .generatedAt(LocalDateTime.now())
                .build();
    }
}
