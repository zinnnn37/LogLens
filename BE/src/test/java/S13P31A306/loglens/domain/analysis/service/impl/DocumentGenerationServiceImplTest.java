package S13P31A306.loglens.domain.analysis.service.impl;

import S13P31A306.loglens.domain.analysis.constants.DocumentFormat;
import S13P31A306.loglens.domain.analysis.constants.DocumentType;
import S13P31A306.loglens.domain.analysis.dto.ai.AiDocumentMetadata;
import S13P31A306.loglens.domain.analysis.dto.ai.AiHtmlDocumentRequest;
import S13P31A306.loglens.domain.analysis.dto.ai.AiHtmlDocumentResponse;
import S13P31A306.loglens.domain.analysis.dto.response.AnalysisDocumentResponse;
import S13P31A306.loglens.domain.analysis.dto.response.ValidationResult;
import S13P31A306.loglens.domain.analysis.exception.AiServiceException;
import S13P31A306.loglens.domain.analysis.exception.DocumentGenerationException;
import S13P31A306.loglens.domain.analysis.mapper.AnalysisMapper;
import S13P31A306.loglens.domain.analysis.service.HtmlValidationService;
import S13P31A306.loglens.global.client.AiServiceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * DocumentGenerationServiceImpl 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentGenerationServiceImpl 테스트")
class DocumentGenerationServiceImplTest {

    @InjectMocks
    private DocumentGenerationServiceImpl documentGenerationService;

    @Mock
    private AiServiceClient aiServiceClient;

    @Mock
    private HtmlValidationService htmlValidationService;

    @Mock
    private AnalysisMapper analysisMapper;

    private ObjectMapper objectMapper;

    @TempDir
    Path tempDir;

    private static final String PROJECT_UUID = "test-project-uuid";
    private static final Long LOG_ID = 123L;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        ReflectionTestUtils.setField(documentGenerationService, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(documentGenerationService, "pdfStoragePath", tempDir.toString());
    }

    @Nested
    @DisplayName("generateDocument 메서드 테스트")
    class GenerateDocumentTest {

        private Map<String, Object> testData;
        private Map<String, Object> testOptions;

        @BeforeEach
        void setUp() {
            testData = new HashMap<>();
            testData.put("projectInfo", Map.of("name", "Test Project", "uuid", PROJECT_UUID));
            testOptions = new HashMap<>();
        }

        @Test
        @DisplayName("HTML_형식으로_문서를_생성한다")
        void HTML_형식으로_문서를_생성한다() {
            // given
            String validHtml = "<!DOCTYPE html><html><head><title>Test</title></head><body><h1>Report</h1></body></html>";

            AiHtmlDocumentResponse aiResponse = AiHtmlDocumentResponse.builder()
                    .htmlContent(validHtml)
                    .metadata(AiDocumentMetadata.builder()
                            .healthScore(85)
                            .totalIssues(10)
                            .criticalIssues(2)
                            .build())
                    .build();

            ValidationResult validationResult = ValidationResult.builder()
                    .isValid(true)
                    .errors(Collections.emptyList())
                    .warnings(Collections.emptyList())
                    .build();

            when(aiServiceClient.generateProjectAnalysisHtml(any(AiHtmlDocumentRequest.class)))
                    .thenReturn(aiResponse);
            when(htmlValidationService.validate(validHtml))
                    .thenReturn(validationResult);

            // when
            AnalysisDocumentResponse response = documentGenerationService.generateDocument(
                    PROJECT_UUID, null, DocumentFormat.HTML, DocumentType.PROJECT_ANALYSIS, testData, testOptions
            );

            // then
            assertThat(response).isNotNull();
            assertThat(response.getFormat()).isEqualTo(DocumentFormat.HTML);
            assertThat(response.getContent()).isEqualTo(validHtml);
            assertThat(response.getValidationStatus()).isEqualTo("VALID");

            verify(aiServiceClient).generateProjectAnalysisHtml(any(AiHtmlDocumentRequest.class));
            verify(htmlValidationService).validate(validHtml);
        }

        @Test
        @DisplayName("PDF_형식으로_문서를_생성한다")
        void PDF_형식으로_문서를_생성한다() {
            // given
            String validHtml = "<!DOCTYPE html><html><head><title>Test</title></head><body><h1>Report</h1></body></html>";

            AiHtmlDocumentResponse aiResponse = AiHtmlDocumentResponse.builder()
                    .htmlContent(validHtml)
                    .metadata(AiDocumentMetadata.builder().build())
                    .build();

            ValidationResult validationResult = ValidationResult.builder()
                    .isValid(true)
                    .errors(Collections.emptyList())
                    .build();

            when(aiServiceClient.generateProjectAnalysisHtml(any(AiHtmlDocumentRequest.class)))
                    .thenReturn(aiResponse);
            when(htmlValidationService.validate(validHtml))
                    .thenReturn(validationResult);

            // when
            AnalysisDocumentResponse response = documentGenerationService.generateDocument(
                    PROJECT_UUID, null, DocumentFormat.PDF, DocumentType.PROJECT_ANALYSIS, testData, testOptions
            );

            // then
            assertThat(response).isNotNull();
            assertThat(response.getFormat()).isEqualTo(DocumentFormat.PDF);
            assertThat(response.getDownloadUrl()).isNotNull();
            assertThat(response.getDownloadUrl()).startsWith("/api/analysis/downloads/");
            assertThat(response.getFileName()).contains("project-analysis");
            assertThat(response.getFileSize()).isGreaterThan(0L);
            assertThat(response.getExpiresAt()).isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("MARKDOWN_형식으로_문서를_생성한다")
        void MARKDOWN_형식으로_문서를_생성한다() {
            // when
            AnalysisDocumentResponse response = documentGenerationService.generateDocument(
                    PROJECT_UUID, null, DocumentFormat.MARKDOWN, DocumentType.PROJECT_ANALYSIS, testData, testOptions
            );

            // then
            assertThat(response).isNotNull();
            assertThat(response.getFormat()).isEqualTo(DocumentFormat.MARKDOWN);
            assertThat(response.getContent()).isNotNull();
            assertThat(response.getContent()).contains("# 프로젝트 분석 보고서");
            assertThat(response.getContent()).contains("Test Project");
        }

        @Test
        @DisplayName("JSON_형식으로_문서를_생성한다")
        void JSON_형식으로_문서를_생성한다() {
            // when
            AnalysisDocumentResponse response = documentGenerationService.generateDocument(
                    PROJECT_UUID, null, DocumentFormat.JSON, DocumentType.PROJECT_ANALYSIS, testData, testOptions
            );

            // then
            assertThat(response).isNotNull();
            assertThat(response.getFormat()).isEqualTo(DocumentFormat.JSON);
            assertThat(response.getContent()).isNotNull();
            assertThat(response.getContent()).contains("projectInfo");
        }
    }

    @Nested
    @DisplayName("HTML 생성 로직 테스트")
    class HtmlGenerationTest {

        private Map<String, Object> testData;
        private Map<String, Object> testOptions;

        @BeforeEach
        void setUp() {
            testData = new HashMap<>();
            testData.put("projectInfo", Map.of("name", "Test Project"));
            testOptions = new HashMap<>();
        }

        @Test
        @DisplayName("첫_시도에서_유효한_HTML을_생성하면_재시도하지_않는다")
        void 첫_시도에서_유효한_HTML을_생성하면_재시도하지_않는다() {
            // given
            String validHtml = "<!DOCTYPE html><html><head><title>Test</title></head><body><h1>Report</h1></body></html>";

            AiHtmlDocumentResponse aiResponse = AiHtmlDocumentResponse.builder()
                    .htmlContent(validHtml)
                    .metadata(AiDocumentMetadata.builder().build())
                    .build();

            ValidationResult validationResult = ValidationResult.builder()
                    .isValid(true)
                    .errors(Collections.emptyList())
                    .build();

            when(aiServiceClient.generateProjectAnalysisHtml(any(AiHtmlDocumentRequest.class)))
                    .thenReturn(aiResponse);
            when(htmlValidationService.validate(validHtml))
                    .thenReturn(validationResult);

            // when
            documentGenerationService.generateDocument(
                    PROJECT_UUID, null, DocumentFormat.HTML, DocumentType.PROJECT_ANALYSIS, testData, testOptions
            );

            // then
            verify(aiServiceClient, times(1)).generateProjectAnalysisHtml(any(AiHtmlDocumentRequest.class));
            verify(aiServiceClient, never()).regenerateWithFeedback(any(), anyList());
        }

        @Test
        @DisplayName("첫_시도_실패_후_피드백과_함께_재생성을_시도한다")
        void 첫_시도_실패_후_피드백과_함께_재생성을_시도한다() {
            // given
            String invalidHtml = "<div>Incomplete HTML</div>";
            String validHtml = "<!DOCTYPE html><html><head><title>Test</title></head><body><h1>Report</h1></body></html>";

            AiHtmlDocumentResponse firstResponse = AiHtmlDocumentResponse.builder()
                    .htmlContent(invalidHtml)
                    .metadata(AiDocumentMetadata.builder().build())
                    .build();

            AiHtmlDocumentResponse secondResponse = AiHtmlDocumentResponse.builder()
                    .htmlContent(validHtml)
                    .metadata(AiDocumentMetadata.builder().build())
                    .build();

            ValidationResult invalidResult = ValidationResult.builder()
                    .isValid(false)
                    .errors(List.of("Missing <html> tag", "Missing <head> tag"))
                    .build();

            ValidationResult validResult = ValidationResult.builder()
                    .isValid(true)
                    .errors(Collections.emptyList())
                    .build();

            when(aiServiceClient.generateProjectAnalysisHtml(any(AiHtmlDocumentRequest.class)))
                    .thenReturn(firstResponse)
                    .thenReturn(secondResponse);
            when(aiServiceClient.regenerateWithFeedback(any(AiHtmlDocumentRequest.class), anyList()))
                    .thenReturn(secondResponse);
            when(htmlValidationService.validate(invalidHtml))
                    .thenReturn(invalidResult);
            when(htmlValidationService.validate(validHtml))
                    .thenReturn(validResult);

            // when
            AnalysisDocumentResponse response = documentGenerationService.generateDocument(
                    PROJECT_UUID, null, DocumentFormat.HTML, DocumentType.PROJECT_ANALYSIS, testData, testOptions
            );

            // then
            assertThat(response.getContent()).isEqualTo(validHtml);
            assertThat(response.getValidationStatus()).isEqualTo("VALID");

            verify(aiServiceClient, times(2)).generateProjectAnalysisHtml(any(AiHtmlDocumentRequest.class));
        }

        @Test
        @DisplayName("모든_재시도_실패_시_fallback_HTML을_반환한다")
        void 모든_재시도_실패_시_fallback_HTML을_반환한다() {
            // given
            String invalidHtml = "<div>Invalid</div>";

            AiHtmlDocumentResponse aiResponse = AiHtmlDocumentResponse.builder()
                    .htmlContent(invalidHtml)
                    .metadata(AiDocumentMetadata.builder().build())
                    .build();

            ValidationResult invalidResult = ValidationResult.builder()
                    .isValid(false)
                    .errors(List.of("Missing required tags"))
                    .build();

            when(aiServiceClient.generateProjectAnalysisHtml(any(AiHtmlDocumentRequest.class)))
                    .thenReturn(aiResponse);
            when(aiServiceClient.regenerateWithFeedback(any(AiHtmlDocumentRequest.class), anyList()))
                    .thenReturn(aiResponse);
            when(htmlValidationService.validate(invalidHtml))
                    .thenReturn(invalidResult);

            // when
            AnalysisDocumentResponse response = documentGenerationService.generateDocument(
                    PROJECT_UUID, null, DocumentFormat.HTML, DocumentType.PROJECT_ANALYSIS, testData, testOptions
            );

            // then
            assertThat(response).isNotNull();
            assertThat(response.getValidationStatus()).isEqualTo("FALLBACK");
            assertThat(response.getContent()).contains("<!DOCTYPE html>");
            assertThat(response.getContent()).contains("프로젝트 분석 보고서");
        }

        @Test
        @DisplayName("AI가_null_응답을_반환하면_재시도한다")
        void AI가_null_응답을_반환하면_재시도한다() {
            // given
            String validHtml = "<!DOCTYPE html><html><head><title>Test</title></head><body><h1>Report</h1></body></html>";

            AiHtmlDocumentResponse validResponse = AiHtmlDocumentResponse.builder()
                    .htmlContent(validHtml)
                    .metadata(AiDocumentMetadata.builder().build())
                    .build();

            ValidationResult validResult = ValidationResult.builder()
                    .isValid(true)
                    .errors(Collections.emptyList())
                    .build();

            when(aiServiceClient.generateProjectAnalysisHtml(any(AiHtmlDocumentRequest.class)))
                    .thenReturn(null)
                    .thenReturn(validResponse);
            when(htmlValidationService.validate(validHtml))
                    .thenReturn(validResult);

            // when
            AnalysisDocumentResponse response = documentGenerationService.generateDocument(
                    PROJECT_UUID, null, DocumentFormat.HTML, DocumentType.PROJECT_ANALYSIS, testData, testOptions
            );

            // then
            assertThat(response.getContent()).isEqualTo(validHtml);
            verify(aiServiceClient, times(2)).generateProjectAnalysisHtml(any(AiHtmlDocumentRequest.class));
        }

        @Test
        @DisplayName("AI_서비스_예외_발생_시_모든_재시도_후_fallback을_반환한다")
        void AI_서비스_예외_발생_시_모든_재시도_후_fallback을_반환한다() {
            // given
            when(aiServiceClient.generateProjectAnalysisHtml(any(AiHtmlDocumentRequest.class)))
                    .thenThrow(new AiServiceException("AI service error"));

            // when
            AnalysisDocumentResponse response = documentGenerationService.generateDocument(
                    PROJECT_UUID, null, DocumentFormat.HTML, DocumentType.PROJECT_ANALYSIS, testData, testOptions
            );

            // then
            assertThat(response).isNotNull();
            assertThat(response.getValidationStatus()).isEqualTo("FALLBACK");
            assertThat(response.getContent()).contains("<!DOCTYPE html>");
        }

        @Test
        @DisplayName("에러_분석_HTML_생성_시_적절한_AI_메서드를_호출한다")
        void 에러_분석_HTML_생성_시_적절한_AI_메서드를_호출한다() {
            // given
            String validHtml = "<!DOCTYPE html><html><head><title>Error</title></head><body><h1>Error Analysis</h1></body></html>";

            AiHtmlDocumentResponse aiResponse = AiHtmlDocumentResponse.builder()
                    .htmlContent(validHtml)
                    .metadata(AiDocumentMetadata.builder().build())
                    .build();

            ValidationResult validationResult = ValidationResult.builder()
                    .isValid(true)
                    .errors(Collections.emptyList())
                    .build();

            when(aiServiceClient.generateErrorAnalysisHtml(any(AiHtmlDocumentRequest.class)))
                    .thenReturn(aiResponse);
            when(htmlValidationService.validate(validHtml))
                    .thenReturn(validationResult);

            // when
            documentGenerationService.generateDocument(
                    PROJECT_UUID, LOG_ID, DocumentFormat.HTML, DocumentType.ERROR_ANALYSIS, testData, testOptions
            );

            // then
            verify(aiServiceClient).generateErrorAnalysisHtml(any(AiHtmlDocumentRequest.class));
            verify(aiServiceClient, never()).generateProjectAnalysisHtml(any());
        }
    }

    @Nested
    @DisplayName("PDF 생성 로직 테스트")
    class PdfGenerationTest {

        private Map<String, Object> testData;
        private Map<String, Object> testOptions;

        @BeforeEach
        void setUp() {
            testData = new HashMap<>();
            testOptions = new HashMap<>();
        }

        @Test
        @DisplayName("유효한_HTML을_PDF로_변환하고_파일로_저장한다")
        void 유효한_HTML을_PDF로_변환하고_파일로_저장한다() throws IOException {
            // given
            String validHtml = "<!DOCTYPE html><html><head><title>Test</title></head><body><h1>Report</h1></body></html>";

            AiHtmlDocumentResponse aiResponse = AiHtmlDocumentResponse.builder()
                    .htmlContent(validHtml)
                    .metadata(AiDocumentMetadata.builder().build())
                    .build();

            ValidationResult validationResult = ValidationResult.builder()
                    .isValid(true)
                    .errors(Collections.emptyList())
                    .build();

            when(aiServiceClient.generateProjectAnalysisHtml(any(AiHtmlDocumentRequest.class)))
                    .thenReturn(aiResponse);
            when(htmlValidationService.validate(validHtml))
                    .thenReturn(validationResult);

            // when
            AnalysisDocumentResponse response = documentGenerationService.generateDocument(
                    PROJECT_UUID, null, DocumentFormat.PDF, DocumentType.PROJECT_ANALYSIS, testData, testOptions
            );

            // then
            assertThat(response).isNotNull();
            assertThat(response.getFormat()).isEqualTo(DocumentFormat.PDF);
            assertThat(response.getDownloadUrl()).isNotNull();

            // PDF 파일이 실제로 저장되었는지 확인
            String fileId = response.getDownloadUrl().substring(response.getDownloadUrl().lastIndexOf("/") + 1);
            Path pdfPath = tempDir.resolve(fileId + ".pdf");
            assertThat(Files.exists(pdfPath)).isTrue();
        }

        @Test
        @DisplayName("PDF_파일명에_문서_유형과_타임스탬프가_포함된다")
        void PDF_파일명에_문서_유형과_타임스탬프가_포함된다() {
            // given
            String validHtml = "<!DOCTYPE html><html><head><title>Test</title></head><body><h1>Report</h1></body></html>";

            AiHtmlDocumentResponse aiResponse = AiHtmlDocumentResponse.builder()
                    .htmlContent(validHtml)
                    .metadata(AiDocumentMetadata.builder().build())
                    .build();

            ValidationResult validationResult = ValidationResult.builder()
                    .isValid(true)
                    .errors(Collections.emptyList())
                    .build();

            when(aiServiceClient.generateErrorAnalysisHtml(any(AiHtmlDocumentRequest.class)))
                    .thenReturn(aiResponse);
            when(htmlValidationService.validate(validHtml))
                    .thenReturn(validationResult);

            // when
            AnalysisDocumentResponse response = documentGenerationService.generateDocument(
                    PROJECT_UUID, LOG_ID, DocumentFormat.PDF, DocumentType.ERROR_ANALYSIS, testData, testOptions
            );

            // then
            assertThat(response.getFileName()).contains("error-analysis");
            assertThat(response.getFileName()).contains(LOG_ID.toString());
            assertThat(response.getFileName()).endsWith(".pdf");
        }

        @Test
        @DisplayName("HTML_변환_실패_시_DocumentGenerationException을_발생시킨다")
        void HTML_변환_실패_시_DocumentGenerationException을_발생시킨다() {
            // given
            // 잘못된 HTML로 PDF 변환 실패 유도 (OpenHTMLtoPDF는 유효하지 않은 HTML에서 예외 발생 가능)
            String malformedHtml = "Not valid HTML at all <<<<";

            AiHtmlDocumentResponse aiResponse = AiHtmlDocumentResponse.builder()
                    .htmlContent(malformedHtml)
                    .metadata(AiDocumentMetadata.builder().build())
                    .build();

            ValidationResult validationResult = ValidationResult.builder()
                    .isValid(true) // 검증은 통과하지만 PDF 변환에서 실패
                    .errors(Collections.emptyList())
                    .build();

            when(aiServiceClient.generateProjectAnalysisHtml(any(AiHtmlDocumentRequest.class)))
                    .thenReturn(aiResponse);
            when(htmlValidationService.validate(malformedHtml))
                    .thenReturn(validationResult);

            // when & then
            assertThatThrownBy(() -> documentGenerationService.generateDocument(
                    PROJECT_UUID, null, DocumentFormat.PDF, DocumentType.PROJECT_ANALYSIS, testData, testOptions
            ))
                    .isInstanceOf(DocumentGenerationException.class)
                    .satisfies(ex -> {
                        S13P31A306.loglens.global.exception.BusinessException be = (S13P31A306.loglens.global.exception.BusinessException) ex;
                        assertThat((String) be.getDetails()).contains("PDF 생성 실패");
                    });
        }
    }

    @Nested
    @DisplayName("getPdfFile 메서드 테스트")
    class GetPdfFileTest {

        @Test
        @DisplayName("존재하는_PDF_파일을_반환한다")
        void 존재하는_PDF_파일을_반환한다() throws IOException {
            // given
            String fileId = "test-file-id";
            Path pdfPath = tempDir.resolve(fileId + ".pdf");
            Files.write(pdfPath, "PDF content".getBytes());

            // when
            Resource resource = documentGenerationService.getPdfFile(fileId);

            // then
            assertThat(resource).isNotNull();
            assertThat(resource.exists()).isTrue();
            assertThat(resource.getFilename()).isEqualTo(fileId + ".pdf");
        }

        @Test
        @DisplayName("존재하지_않는_파일_요청_시_예외를_발생시킨다")
        void 존재하지_않는_파일_요청_시_예외를_발생시킨다() {
            // given
            String nonExistentFileId = "non-existent-file";

            // when & then
            assertThatThrownBy(() -> documentGenerationService.getPdfFile(nonExistentFileId))
                    .isInstanceOf(DocumentGenerationException.class)
                    .satisfies(ex -> {
                        S13P31A306.loglens.global.exception.BusinessException be = (S13P31A306.loglens.global.exception.BusinessException) ex;
                        assertThat((String) be.getDetails()).contains("PDF 파일을 찾을 수 없습니다");
                    });
        }
    }

    @Nested
    @DisplayName("Markdown 생성 로직 테스트")
    class MarkdownGenerationTest {

        @Test
        @DisplayName("프로젝트_분석_Markdown을_생성한다")
        void 프로젝트_분석_Markdown을_생성한다() {
            // given
            Map<String, Object> testData = new HashMap<>();
            testData.put("projectInfo", Map.of("name", "Test Project", "uuid", PROJECT_UUID));
            testData.put("metrics", Map.of(
                    "totalLogs", 1000,
                    "errorCount", 50,
                    "warnCount", 30,
                    "avgResponseTime", 250
            ));

            // when
            AnalysisDocumentResponse response = documentGenerationService.generateDocument(
                    PROJECT_UUID, null, DocumentFormat.MARKDOWN, DocumentType.PROJECT_ANALYSIS, testData, new HashMap<>()
            );

            // then
            assertThat(response.getContent()).contains("# 프로젝트 분석 보고서");
            assertThat(response.getContent()).contains("Test Project");
            assertThat(response.getContent()).contains("총 로그 수");
            assertThat(response.getContent()).contains("1000");
            assertThat(response.getContent()).contains("에러 수");
            assertThat(response.getContent()).contains("50");
        }

        @Test
        @DisplayName("에러_분석_Markdown을_생성한다")
        void 에러_분석_Markdown을_생성한다() {
            // given
            Map<String, Object> testData = new HashMap<>();
            testData.put("errorLog", Map.of(
                    "logId", LOG_ID,
                    "level", "ERROR",
                    "message", "NullPointerException occurred",
                    "timestamp", "2025-11-14T10:00:00"
            ));

            // when
            AnalysisDocumentResponse response = documentGenerationService.generateDocument(
                    PROJECT_UUID, LOG_ID, DocumentFormat.MARKDOWN, DocumentType.ERROR_ANALYSIS, testData, new HashMap<>()
            );

            // then
            assertThat(response.getContent()).contains("# 에러 상세 분석 보고서");
            assertThat(response.getContent()).contains("로그 ID");
            assertThat(response.getContent()).contains(LOG_ID.toString());
            assertThat(response.getContent()).contains("ERROR");
            assertThat(response.getContent()).contains("NullPointerException occurred");
        }
    }

    @Nested
    @DisplayName("JSON 생성 로직 테스트")
    class JsonGenerationTest {

        @Test
        @DisplayName("데이터를_JSON_형식으로_변환한다")
        void 데이터를_JSON_형식으로_변환한다() {
            // given
            Map<String, Object> testData = new HashMap<>();
            testData.put("projectInfo", Map.of("name", "Test Project"));
            testData.put("metrics", Map.of("totalLogs", 1000));

            // when
            AnalysisDocumentResponse response = documentGenerationService.generateDocument(
                    PROJECT_UUID, null, DocumentFormat.JSON, DocumentType.PROJECT_ANALYSIS, testData, new HashMap<>()
            );

            // then
            assertThat(response.getContent()).isNotNull();
            assertThat(response.getContent()).contains("\"projectInfo\"");
            assertThat(response.getContent()).contains("\"Test Project\"");
            assertThat(response.getContent()).contains("\"metrics\"");
            assertThat(response.getContent()).contains("\"totalLogs\"");
            assertThat(response.getContent()).contains("1000");
        }
    }

    @Nested
    @DisplayName("Fallback HTML 생성 테스트")
    class FallbackHtmlTest {

        @Test
        @DisplayName("프로젝트_분석_fallback_HTML을_생성한다")
        void 프로젝트_분석_fallback_HTML을_생성한다() {
            // given
            Map<String, Object> testData = new HashMap<>();
            testData.put("projectInfo", Map.of("name", "Test Project"));
            testData.put("metrics", Map.of("totalLogs", 1000, "errorCount", 50));

            when(aiServiceClient.generateProjectAnalysisHtml(any(AiHtmlDocumentRequest.class)))
                    .thenThrow(new AiServiceException("AI service unavailable"));

            // when
            AnalysisDocumentResponse response = documentGenerationService.generateDocument(
                    PROJECT_UUID, null, DocumentFormat.HTML, DocumentType.PROJECT_ANALYSIS, testData, new HashMap<>()
            );

            // then
            assertThat(response.getValidationStatus()).isEqualTo("FALLBACK");
            assertThat(response.getContent()).contains("<!DOCTYPE html>");
            assertThat(response.getContent()).contains("프로젝트 분석 보고서");
            assertThat(response.getContent()).contains("Test Project");
            assertThat(response.getContent()).contains("1000");
        }

        @Test
        @DisplayName("에러_분석_fallback_HTML을_생성한다")
        void 에러_분석_fallback_HTML을_생성한다() {
            // given
            Map<String, Object> testData = new HashMap<>();
            testData.put("errorLog", Map.of(
                    "logId", LOG_ID,
                    "level", "ERROR",
                    "message", "Test error message"
            ));

            when(aiServiceClient.generateErrorAnalysisHtml(any(AiHtmlDocumentRequest.class)))
                    .thenThrow(new AiServiceException("AI service unavailable"));

            // when
            AnalysisDocumentResponse response = documentGenerationService.generateDocument(
                    PROJECT_UUID, LOG_ID, DocumentFormat.HTML, DocumentType.ERROR_ANALYSIS, testData, new HashMap<>()
            );

            // then
            assertThat(response.getValidationStatus()).isEqualTo("FALLBACK");
            assertThat(response.getContent()).contains("에러 상세 분석 보고서");
            assertThat(response.getContent()).contains(LOG_ID.toString());
            assertThat(response.getContent()).contains("ERROR");
            assertThat(response.getContent()).contains("Test error message");
        }
    }
}
