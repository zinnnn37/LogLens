package S13P31A306.loglens.domain.analysis.controller.impl;

import S13P31A306.loglens.domain.analysis.constants.AnalysisErrorCode;
import S13P31A306.loglens.domain.analysis.constants.DocumentFormat;
import S13P31A306.loglens.domain.analysis.dto.request.AnalysisOptions;
import S13P31A306.loglens.domain.analysis.dto.request.ErrorAnalysisOptions;
import S13P31A306.loglens.domain.analysis.dto.request.ErrorAnalysisRequest;
import S13P31A306.loglens.domain.analysis.dto.request.ProjectAnalysisRequest;
import S13P31A306.loglens.domain.analysis.dto.response.AnalysisDocumentResponse;
import S13P31A306.loglens.domain.analysis.dto.response.DocumentMetadata;
import S13P31A306.loglens.domain.analysis.service.AnalysisService;
import S13P31A306.loglens.domain.analysis.service.DocumentGenerationService;
import S13P31A306.loglens.domain.analysis.validator.AnalysisValidator;
import S13P31A306.loglens.global.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AnalysisController 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)  // Security 필터 비활성화
@ActiveProfiles("test")
@DisplayName("AnalysisController 테스트")
public class AnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AnalysisService analysisService;

    @MockitoBean
    private DocumentGenerationService documentGenerationService;

    @MockitoBean
    private AnalysisValidator analysisValidator;

    private static final String PROJECT_UUID = "test-project-uuid";
    private static final Long LOG_ID = 123L;

    @Nested
    @DisplayName("프로젝트 분석 보고서 생성 API 테스트")
    class GenerateProjectAnalysisReportTest {

        @Test
        @DisplayName("POST_/api/analysis/projects/{projectUuid}/reports_HTML_문서_생성_성공")
        void POST_프로젝트_분석_HTML_문서_생성_성공() throws Exception {
            // given
            ProjectAnalysisRequest request = ProjectAnalysisRequest.builder()
                    .format(DocumentFormat.HTML)
                    .options(AnalysisOptions.builder()
                            .includeComponents(true)
                            .includeCharts(true)
                            .build())
                    .build();

            AnalysisDocumentResponse response = AnalysisDocumentResponse.builder()
                    .projectUuid(PROJECT_UUID)
                    .format(DocumentFormat.HTML)
                    .content("<html><body><h1>Project Analysis Report</h1></body></html>")
                    .validationStatus("VALID")
                    .documentMetadata(DocumentMetadata.builder()
                            .title("프로젝트 종합 분석 보고서")
                            .generatedAt(LocalDateTime.now())
                            .build())
                    .build();

            doNothing().when(analysisValidator).validateProjectAnalysisRequest(
                    eq(PROJECT_UUID), any(ProjectAnalysisRequest.class), any()
            );
            given(analysisService.generateProjectAnalysisDocument(eq(PROJECT_UUID), any(ProjectAnalysisRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/analysis/projects/{projectUuid}/reports", PROJECT_UUID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.projectUuid").value(PROJECT_UUID))
                    .andExpect(jsonPath("$.data.format").value("HTML"))
                    .andExpect(jsonPath("$.data.content").exists())
                    .andExpect(jsonPath("$.data.validationStatus").value("VALID"))
                    .andExpect(jsonPath("$.data.documentMetadata.title").value("프로젝트 종합 분석 보고서"));

            verify(analysisValidator).validateProjectAnalysisRequest(
                    eq(PROJECT_UUID), any(ProjectAnalysisRequest.class), any()
            );
            verify(analysisService).generateProjectAnalysisDocument(
                    eq(PROJECT_UUID), any(ProjectAnalysisRequest.class)
            );
        }

        @Test
        @DisplayName("POST_/api/analysis/projects/{projectUuid}/reports_PDF_문서_생성_성공")
        void POST_프로젝트_분석_PDF_문서_생성_성공() throws Exception {
            // given
            ProjectAnalysisRequest request = ProjectAnalysisRequest.builder()
                    .format(DocumentFormat.PDF)
                    .options(AnalysisOptions.builder().build())
                    .build();

            AnalysisDocumentResponse response = AnalysisDocumentResponse.builder()
                    .projectUuid(PROJECT_UUID)
                    .format(DocumentFormat.PDF)
                    .downloadUrl("/api/analysis/downloads/test-file-id")
                    .fileName("project-analysis-20251114.pdf")
                    .fileSize(102400L)
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .documentMetadata(DocumentMetadata.builder()
                            .title("프로젝트 종합 분석 보고서")
                            .generatedAt(LocalDateTime.now())
                            .build())
                    .build();

            doNothing().when(analysisValidator).validateProjectAnalysisRequest(
                    eq(PROJECT_UUID), any(ProjectAnalysisRequest.class), any()
            );
            given(analysisService.generateProjectAnalysisDocument(eq(PROJECT_UUID), any(ProjectAnalysisRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/analysis/projects/{projectUuid}/reports", PROJECT_UUID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.projectUuid").value(PROJECT_UUID))
                    .andExpect(jsonPath("$.data.format").value("PDF"))
                    .andExpect(jsonPath("$.data.downloadUrl").value("/api/analysis/downloads/test-file-id"))
                    .andExpect(jsonPath("$.data.fileName").value("project-analysis-20251114.pdf"))
                    .andExpect(jsonPath("$.data.fileSize").value(102400));

            verify(analysisService).generateProjectAnalysisDocument(
                    eq(PROJECT_UUID), any(ProjectAnalysisRequest.class)
            );
        }

        @Test
        @DisplayName("POST_/api/analysis/projects/{projectUuid}/reports_시간_범위_지정_성공")
        void POST_프로젝트_분석_시간_범위_지정_성공() throws Exception {
            // given
            LocalDateTime startTime = LocalDateTime.of(2025, 11, 1, 0, 0);
            LocalDateTime endTime = LocalDateTime.of(2025, 11, 14, 23, 59);

            ProjectAnalysisRequest request = ProjectAnalysisRequest.builder()
                    .startTime(startTime)
                    .endTime(endTime)
                    .format(DocumentFormat.MARKDOWN)
                    .options(AnalysisOptions.builder().build())
                    .build();

            AnalysisDocumentResponse response = AnalysisDocumentResponse.builder()
                    .projectUuid(PROJECT_UUID)
                    .format(DocumentFormat.MARKDOWN)
                    .content("# Project Analysis Report\n\n...")
                    .build();

            doNothing().when(analysisValidator).validateProjectAnalysisRequest(
                    eq(PROJECT_UUID), any(ProjectAnalysisRequest.class), any()
            );
            given(analysisService.generateProjectAnalysisDocument(eq(PROJECT_UUID), any(ProjectAnalysisRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/analysis/projects/{projectUuid}/reports", PROJECT_UUID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.format").value("MARKDOWN"));

            verify(analysisValidator).validateProjectAnalysisRequest(
                    eq(PROJECT_UUID), any(ProjectAnalysisRequest.class), any()
            );
        }

        @Test
        @DisplayName("POST_/api/analysis/projects/{projectUuid}/reports_검증_실패_시_에러_반환")
        void POST_프로젝트_분석_검증_실패_시_에러_반환() throws Exception {
            // given
            ProjectAnalysisRequest request = ProjectAnalysisRequest.builder()
                    .startTime(LocalDateTime.now())
                    .endTime(LocalDateTime.now().minusDays(1))  // 잘못된 시간 범위
                    .format(DocumentFormat.HTML)
                    .build();

            doThrow(new BusinessException(AnalysisErrorCode.INVALID_TIME_RANGE, "시작 시간이 종료 시간보다 늦을 수 없습니다"))
                    .when(analysisValidator).validateProjectAnalysisRequest(
                            eq(PROJECT_UUID), any(ProjectAnalysisRequest.class), any()
                    );

            // when & then
            mockMvc.perform(post("/api/analysis/projects/{projectUuid}/reports", PROJECT_UUID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(analysisValidator).validateProjectAnalysisRequest(
                    eq(PROJECT_UUID), any(ProjectAnalysisRequest.class), any()
            );
            verify(analysisService, never()).generateProjectAnalysisDocument(any(), any());
        }
    }

    @Nested
    @DisplayName("에러 분석 보고서 생성 API 테스트")
    class GenerateErrorAnalysisReportTest {

        @Test
        @DisplayName("POST_/api/analysis/errors/{logId}/reports_HTML_문서_생성_성공")
        void POST_에러_분석_HTML_문서_생성_성공() throws Exception {
            // given
            ErrorAnalysisRequest request = ErrorAnalysisRequest.builder()
                    .projectUuid(PROJECT_UUID)
                    .format(DocumentFormat.HTML)
                    .options(ErrorAnalysisOptions.builder()
                            .includeRelatedLogs(true)
                            .maxRelatedLogs(10)
                            .build())
                    .build();

            AnalysisDocumentResponse response = AnalysisDocumentResponse.builder()
                    .projectUuid(PROJECT_UUID)
                    .logId(LOG_ID)
                    .format(DocumentFormat.HTML)
                    .content("<html><body><h1>Error Analysis Report</h1></body></html>")
                    .validationStatus("VALID")
                    .documentMetadata(DocumentMetadata.builder()
                            .title("에러 상세 분석 보고서")
                            .generatedAt(LocalDateTime.now())
                            .build())
                    .build();

            doNothing().when(analysisValidator).validateErrorAnalysisRequest(
                    eq(LOG_ID), any(ErrorAnalysisRequest.class), any()
            );
            given(analysisService.generateErrorAnalysisDocument(eq(LOG_ID), any(ErrorAnalysisRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/analysis/errors/{logId}/reports", LOG_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.projectUuid").value(PROJECT_UUID))
                    .andExpect(jsonPath("$.data.logId").value(LOG_ID))
                    .andExpect(jsonPath("$.data.format").value("HTML"))
                    .andExpect(jsonPath("$.data.validationStatus").value("VALID"))
                    .andExpect(jsonPath("$.data.documentMetadata.title").value("에러 상세 분석 보고서"));

            verify(analysisValidator).validateErrorAnalysisRequest(
                    eq(LOG_ID), any(ErrorAnalysisRequest.class), any()
            );
            verify(analysisService).generateErrorAnalysisDocument(
                    eq(LOG_ID), any(ErrorAnalysisRequest.class)
            );
        }

        @Test
        @DisplayName("POST_/api/analysis/errors/{logId}/reports_PDF_문서_생성_성공")
        void POST_에러_분석_PDF_문서_생성_성공() throws Exception {
            // given
            ErrorAnalysisRequest request = ErrorAnalysisRequest.builder()
                    .projectUuid(PROJECT_UUID)
                    .format(DocumentFormat.PDF)
                    .options(ErrorAnalysisOptions.builder().build())
                    .build();

            AnalysisDocumentResponse response = AnalysisDocumentResponse.builder()
                    .projectUuid(PROJECT_UUID)
                    .logId(LOG_ID)
                    .format(DocumentFormat.PDF)
                    .downloadUrl("/api/analysis/downloads/error-file-id")
                    .fileName("error-analysis-123-20251114.pdf")
                    .fileSize(51200L)
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .build();

            doNothing().when(analysisValidator).validateErrorAnalysisRequest(
                    eq(LOG_ID), any(ErrorAnalysisRequest.class), any()
            );
            given(analysisService.generateErrorAnalysisDocument(eq(LOG_ID), any(ErrorAnalysisRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/analysis/errors/{logId}/reports", LOG_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.format").value("PDF"))
                    .andExpect(jsonPath("$.data.downloadUrl").value("/api/analysis/downloads/error-file-id"))
                    .andExpect(jsonPath("$.data.fileName").value("error-analysis-123-20251114.pdf"));

            verify(analysisService).generateErrorAnalysisDocument(
                    eq(LOG_ID), any(ErrorAnalysisRequest.class)
            );
        }

        @Test
        @DisplayName("POST_/api/analysis/errors/{logId}/reports_로그_없음_에러_반환")
        void POST_에러_분석_로그_없음_에러_반환() throws Exception {
            // given
            ErrorAnalysisRequest request = ErrorAnalysisRequest.builder()
                    .projectUuid(PROJECT_UUID)
                    .format(DocumentFormat.HTML)
                    .options(ErrorAnalysisOptions.builder().build())
                    .build();

            doThrow(new BusinessException(AnalysisErrorCode.LOG_NOT_FOUND, "로그를 찾을 수 없습니다"))
                    .when(analysisValidator).validateErrorAnalysisRequest(
                            eq(LOG_ID), any(ErrorAnalysisRequest.class), any()
                    );

            // when & then
            mockMvc.perform(post("/api/analysis/errors/{logId}/reports", LOG_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNotFound());

            verify(analysisValidator).validateErrorAnalysisRequest(
                    eq(LOG_ID), any(ErrorAnalysisRequest.class), any()
            );
            verify(analysisService, never()).generateErrorAnalysisDocument(any(), any());
        }

        @Test
        @DisplayName("POST_/api/analysis/errors/{logId}/reports_MARKDOWN_형식_생성_성공")
        void POST_에러_분석_MARKDOWN_형식_생성_성공() throws Exception {
            // given
            ErrorAnalysisRequest request = ErrorAnalysisRequest.builder()
                    .projectUuid(PROJECT_UUID)
                    .format(DocumentFormat.MARKDOWN)
                    .options(ErrorAnalysisOptions.builder().build())
                    .build();

            AnalysisDocumentResponse response = AnalysisDocumentResponse.builder()
                    .projectUuid(PROJECT_UUID)
                    .logId(LOG_ID)
                    .format(DocumentFormat.MARKDOWN)
                    .content("# Error Analysis Report\n\n## Error Details\n...")
                    .build();

            doNothing().when(analysisValidator).validateErrorAnalysisRequest(
                    eq(LOG_ID), any(ErrorAnalysisRequest.class), any()
            );
            given(analysisService.generateErrorAnalysisDocument(eq(LOG_ID), any(ErrorAnalysisRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/analysis/errors/{logId}/reports", LOG_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.format").value("MARKDOWN"))
                    .andExpect(jsonPath("$.data.content").exists());

            verify(analysisService).generateErrorAnalysisDocument(
                    eq(LOG_ID), any(ErrorAnalysisRequest.class)
            );
        }

        @Test
        @DisplayName("POST_/api/analysis/errors/{logId}/reports_JSON_형식_생성_성공")
        void POST_에러_분석_JSON_형식_생성_성공() throws Exception {
            // given
            ErrorAnalysisRequest request = ErrorAnalysisRequest.builder()
                    .projectUuid(PROJECT_UUID)
                    .format(DocumentFormat.JSON)
                    .options(ErrorAnalysisOptions.builder().build())
                    .build();

            AnalysisDocumentResponse response = AnalysisDocumentResponse.builder()
                    .projectUuid(PROJECT_UUID)
                    .logId(LOG_ID)
                    .format(DocumentFormat.JSON)
                    .content("{\"errorLog\": {\"logId\": 123}}")
                    .build();

            doNothing().when(analysisValidator).validateErrorAnalysisRequest(
                    eq(LOG_ID), any(ErrorAnalysisRequest.class), any()
            );
            given(analysisService.generateErrorAnalysisDocument(eq(LOG_ID), any(ErrorAnalysisRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/analysis/errors/{logId}/reports", LOG_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.format").value("JSON"));

            verify(analysisService).generateErrorAnalysisDocument(
                    eq(LOG_ID), any(ErrorAnalysisRequest.class)
            );
        }
    }

    @Nested
    @DisplayName("PDF 다운로드 API 테스트")
    class DownloadPdfDocumentTest {

        @Test
        @DisplayName("GET_/api/analysis/downloads/{fileId}_PDF_다운로드_성공")
        void GET_PDF_다운로드_성공() throws Exception {
            // given
            String fileId = "test-file-id";
            byte[] pdfContent = "PDF content".getBytes();
            Resource resource = new ByteArrayResource(pdfContent);

            given(documentGenerationService.getPdfFile(fileId))
                    .willReturn(resource);

            // when & then
            mockMvc.perform(get("/api/analysis/downloads/{fileId}", fileId))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", "attachment; filename=\"" + fileId + ".pdf\""))
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF));

            verify(documentGenerationService).getPdfFile(fileId);
        }

        @Test
        @DisplayName("GET_/api/analysis/downloads/{fileId}_파일_없음_에러")
        void GET_PDF_파일_없음_에러() throws Exception {
            // given
            String fileId = "non-existent-file";

            given(documentGenerationService.getPdfFile(fileId))
                    .willThrow(new BusinessException(AnalysisErrorCode.PDF_FILE_NOT_FOUND, "PDF 파일을 찾을 수 없습니다"));

            // when & then
            mockMvc.perform(get("/api/analysis/downloads/{fileId}", fileId))
                    .andDo(print())
                    .andExpect(status().isNotFound());

            verify(documentGenerationService).getPdfFile(fileId);
        }
    }
}
