package S13P31A306.loglens.domain.analysis.service.impl;

import S13P31A306.loglens.domain.analysis.constants.DocumentFormat;
import S13P31A306.loglens.domain.analysis.constants.DocumentType;
import S13P31A306.loglens.domain.analysis.dto.request.AnalysisOptions;
import S13P31A306.loglens.domain.analysis.dto.request.ErrorAnalysisOptions;
import S13P31A306.loglens.domain.analysis.dto.request.ErrorAnalysisRequest;
import S13P31A306.loglens.domain.analysis.dto.request.ProjectAnalysisRequest;
import S13P31A306.loglens.domain.analysis.dto.response.AnalysisDocumentResponse;
import S13P31A306.loglens.domain.analysis.mapper.AnalysisMapper;
import S13P31A306.loglens.domain.analysis.service.DocumentGenerationService;
import S13P31A306.loglens.domain.dashboard.dto.response.DashboardOverviewResponse;
import S13P31A306.loglens.domain.dashboard.service.DashboardService;
import S13P31A306.loglens.domain.log.dto.ai.AiAnalysisDto;
import S13P31A306.loglens.domain.log.dto.request.LogSearchRequest;
import S13P31A306.loglens.domain.log.dto.response.LogDetailResponse;
import S13P31A306.loglens.domain.log.dto.response.LogPageResponse;
import S13P31A306.loglens.domain.log.dto.response.PaginationResponse;
import S13P31A306.loglens.domain.log.entity.Log;
import S13P31A306.loglens.domain.log.entity.LogLevel;
import S13P31A306.loglens.domain.log.repository.LogRepository;
import S13P31A306.loglens.domain.log.service.LogService;
import S13P31A306.loglens.domain.project.entity.Project;
import S13P31A306.loglens.domain.project.validator.ProjectValidator;
import S13P31A306.loglens.global.client.AiServiceClient;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AnalysisServiceImpl 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AnalysisServiceImpl 테스트")
class AnalysisServiceImplTest {

    @InjectMocks
    private AnalysisServiceImpl analysisService;

    @Mock
    private ProjectValidator projectValidator;

    @Mock
    private DashboardService dashboardService;

    @Mock
    private LogService logService;

    @Mock
    private LogRepository logRepository;

    @Mock
    private DocumentGenerationService documentGenerationService;

    @Mock
    private AiServiceClient aiServiceClient;

    @Mock
    private AnalysisMapper analysisMapper;

    private static final String PROJECT_UUID = "test-project-uuid";
    private static final Long LOG_ID = 123L;
    private static final String TRACE_ID = "test-trace-id";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Nested
    @DisplayName("generateProjectAnalysisDocument 메서드 테스트")
    class GenerateProjectAnalysisDocumentTest {

        private Project mockProject;
        private ProjectAnalysisRequest request;
        private DashboardOverviewResponse mockDashboard;
        private LogPageResponse mockTopErrors;
        private AnalysisDocumentResponse expectedResponse;

        @BeforeEach
        void setUp() {
            // Mock Project
            mockProject = mock(Project.class);
            when(mockProject.getProjectName()).thenReturn("Test Project");
            when(mockProject.getDescription()).thenReturn("Test Description");

            // Mock Dashboard Response
            DashboardOverviewResponse.Summary summary = new DashboardOverviewResponse.Summary(
                    1000, 50, 30, 20, 250
            );
            DashboardOverviewResponse.Period period = new DashboardOverviewResponse.Period(
                    LocalDateTime.now().minusDays(7).format(FORMATTER),
                    LocalDateTime.now().format(FORMATTER)
            );
            mockDashboard = DashboardOverviewResponse.builder()
                    .projectUuid(PROJECT_UUID)
                    .period(period)
                    .summary(summary)
                    .build();

            // Mock Top Errors
            mockTopErrors = LogPageResponse.builder()
                    .logs(Collections.emptyList())
                    .pagination(PaginationResponse.builder()
                            .size(10)
                            .hasNext(false)
                            .nextCursor(null)
                            .build())
                    .build();

            // Expected Response
            expectedResponse = AnalysisDocumentResponse.builder()
                    .projectUuid(PROJECT_UUID)
                    .format(DocumentFormat.HTML)
                    .content("<html>Test Report</html>")
                    .build();

            // Request
            request = ProjectAnalysisRequest.builder()
                    .format(DocumentFormat.HTML)
                    .options(AnalysisOptions.builder().build())
                    .build();
        }

        @Test
        @DisplayName("프로젝트_분석_문서를_성공적으로_생성한다")
        void 프로젝트_분석_문서를_성공적으로_생성한다() {
            // given
            when(projectValidator.validateProjectExists(PROJECT_UUID))
                    .thenReturn(mockProject);
            when(dashboardService.getStatisticsOverview(anyString(), any(), anyString()))
                    .thenReturn(mockDashboard);
            when(logService.getLogs(any(LogSearchRequest.class)))
                    .thenReturn(mockTopErrors);
            when(documentGenerationService.generateDocument(
                    anyString(), any(), any(DocumentFormat.class), any(DocumentType.class), anyMap(), anyMap()
            ))
                    .thenReturn(expectedResponse);

            // when
            AnalysisDocumentResponse response = analysisService.generateProjectAnalysisDocument(
                    PROJECT_UUID, request
            );

            // then
            assertThat(response).isNotNull();
            assertThat(response.getProjectUuid()).isEqualTo(PROJECT_UUID);
            assertThat(response.getFormat()).isEqualTo(DocumentFormat.HTML);

            verify(projectValidator).validateProjectExists(PROJECT_UUID);
            verify(dashboardService).getStatisticsOverview(anyString(), any(), anyString());
            verify(logService).getLogs(any(LogSearchRequest.class));
            verify(documentGenerationService).generateDocument(
                    eq(PROJECT_UUID),
                    isNull(),
                    eq(DocumentFormat.HTML),
                    eq(DocumentType.PROJECT_ANALYSIS),
                    anyMap(),
                    anyMap()
            );
        }

        @Test
        @DisplayName("시간_범위가_지정되면_해당_범위의_데이터를_수집한다")
        void 시간_범위가_지정되면_해당_범위의_데이터를_수집한다() {
            // given
            LocalDateTime startTime = LocalDateTime.now().minusDays(7);
            LocalDateTime endTime = LocalDateTime.now();

            ProjectAnalysisRequest requestWithTime = ProjectAnalysisRequest.builder()
                    .startTime(startTime)
                    .endTime(endTime)
                    .format(DocumentFormat.HTML)
                    .options(AnalysisOptions.builder().build())
                    .build();

            when(projectValidator.validateProjectExists(PROJECT_UUID))
                    .thenReturn(mockProject);
            when(dashboardService.getStatisticsOverview(anyString(), anyString(), anyString()))
                    .thenReturn(mockDashboard);
            when(logService.getLogs(any(LogSearchRequest.class)))
                    .thenReturn(mockTopErrors);
            when(documentGenerationService.generateDocument(
                    anyString(), any(), any(DocumentFormat.class), any(DocumentType.class), anyMap(), anyMap()
            ))
                    .thenReturn(expectedResponse);

            // when
            analysisService.generateProjectAnalysisDocument(PROJECT_UUID, requestWithTime);

            // then
            ArgumentCaptor<String> startTimeCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> endTimeCaptor = ArgumentCaptor.forClass(String.class);

            verify(dashboardService).getStatisticsOverview(
                    eq(PROJECT_UUID),
                    startTimeCaptor.capture(),
                    endTimeCaptor.capture()
            );

            assertThat(startTimeCaptor.getValue()).isEqualTo(startTime.format(FORMATTER));
            assertThat(endTimeCaptor.getValue()).isEqualTo(endTime.format(FORMATTER));
        }

        @Test
        @DisplayName("종료_시간이_없으면_현재_시간을_사용한다")
        void 종료_시간이_없으면_현재_시간을_사용한다() {
            // given
            LocalDateTime startTime = LocalDateTime.now().minusDays(7);

            ProjectAnalysisRequest requestWithoutEndTime = ProjectAnalysisRequest.builder()
                    .startTime(startTime)
                    .format(DocumentFormat.HTML)
                    .options(AnalysisOptions.builder().build())
                    .build();

            when(projectValidator.validateProjectExists(PROJECT_UUID))
                    .thenReturn(mockProject);
            when(dashboardService.getStatisticsOverview(anyString(), anyString(), anyString()))
                    .thenReturn(mockDashboard);
            when(logService.getLogs(any(LogSearchRequest.class)))
                    .thenReturn(mockTopErrors);
            when(documentGenerationService.generateDocument(
                    anyString(), any(), any(DocumentFormat.class), any(DocumentType.class), anyMap(), anyMap()
            ))
                    .thenReturn(expectedResponse);

            // when
            analysisService.generateProjectAnalysisDocument(PROJECT_UUID, requestWithoutEndTime);

            // then
            verify(dashboardService).getStatisticsOverview(
                    eq(PROJECT_UUID),
                    anyString(),
                    anyString()
            );
        }

        @Test
        @DisplayName("대시보드_데이터_수집_실패_시에도_문서를_생성한다")
        void 대시보드_데이터_수집_실패_시에도_문서를_생성한다() {
            // given
            when(projectValidator.validateProjectExists(PROJECT_UUID))
                    .thenReturn(mockProject);
            when(dashboardService.getStatisticsOverview(anyString(), any(), anyString()))
                    .thenThrow(new RuntimeException("Dashboard service error"));
            when(logService.getLogs(any(LogSearchRequest.class)))
                    .thenReturn(mockTopErrors);
            when(documentGenerationService.generateDocument(
                    anyString(), any(), any(DocumentFormat.class), any(DocumentType.class), anyMap(), anyMap()
            ))
                    .thenReturn(expectedResponse);

            // when
            AnalysisDocumentResponse response = analysisService.generateProjectAnalysisDocument(
                    PROJECT_UUID, request
            );

            // then
            assertThat(response).isNotNull();
            verify(documentGenerationService).generateDocument(
                    anyString(), any(), any(), any(), anyMap(), anyMap()
            );
        }

        @Test
        @DisplayName("최근_에러_로그_수집_실패_시에도_문서를_생성한다")
        void 최근_에러_로그_수집_실패_시에도_문서를_생성한다() {
            // given
            when(projectValidator.validateProjectExists(PROJECT_UUID))
                    .thenReturn(mockProject);
            when(dashboardService.getStatisticsOverview(anyString(), any(), anyString()))
                    .thenReturn(mockDashboard);
            when(logService.getLogs(any(LogSearchRequest.class)))
                    .thenThrow(new RuntimeException("Log service error"));
            when(documentGenerationService.generateDocument(
                    anyString(), any(), any(DocumentFormat.class), any(DocumentType.class), anyMap(), anyMap()
            ))
                    .thenReturn(expectedResponse);

            // when
            AnalysisDocumentResponse response = analysisService.generateProjectAnalysisDocument(
                    PROJECT_UUID, request
            );

            // then
            assertThat(response).isNotNull();
            verify(documentGenerationService).generateDocument(
                    anyString(), any(), any(), any(), anyMap(), anyMap()
            );
        }

        @Test
        @DisplayName("수집된_데이터에_프로젝트_정보가_포함된다")
        void 수집된_데이터에_프로젝트_정보가_포함된다() {
            // given
            when(projectValidator.validateProjectExists(PROJECT_UUID))
                    .thenReturn(mockProject);
            when(dashboardService.getStatisticsOverview(anyString(), any(), anyString()))
                    .thenReturn(mockDashboard);
            when(logService.getLogs(any(LogSearchRequest.class)))
                    .thenReturn(mockTopErrors);
            when(documentGenerationService.generateDocument(
                    anyString(), any(), any(DocumentFormat.class), any(DocumentType.class), anyMap(), anyMap()
            ))
                    .thenReturn(expectedResponse);

            // when
            analysisService.generateProjectAnalysisDocument(PROJECT_UUID, request);

            // then
            ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);
            verify(documentGenerationService).generateDocument(
                    anyString(), any(), any(), any(), dataCaptor.capture(), anyMap()
            );

            Map<String, Object> capturedData = dataCaptor.getValue();
            assertThat(capturedData).containsKey("projectInfo");

            Map<String, Object> projectInfo = (Map<String, Object>) capturedData.get("projectInfo");
            assertThat(projectInfo.get("name")).isEqualTo("Test Project");
            assertThat(projectInfo.get("uuid")).isEqualTo(PROJECT_UUID);
        }

        @Test
        @DisplayName("수집된_데이터에_메트릭_정보가_포함된다")
        void 수집된_데이터에_메트릭_정보가_포함된다() {
            // given
            when(projectValidator.validateProjectExists(PROJECT_UUID))
                    .thenReturn(mockProject);
            when(dashboardService.getStatisticsOverview(anyString(), any(), anyString()))
                    .thenReturn(mockDashboard);
            when(logService.getLogs(any(LogSearchRequest.class)))
                    .thenReturn(mockTopErrors);
            when(documentGenerationService.generateDocument(
                    anyString(), any(), any(DocumentFormat.class), any(DocumentType.class), anyMap(), anyMap()
            ))
                    .thenReturn(expectedResponse);

            // when
            analysisService.generateProjectAnalysisDocument(PROJECT_UUID, request);

            // then
            ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);
            verify(documentGenerationService).generateDocument(
                    anyString(), any(), any(), any(), dataCaptor.capture(), anyMap()
            );

            Map<String, Object> capturedData = dataCaptor.getValue();
            assertThat(capturedData).containsKey("metrics");

            Map<String, Object> metrics = (Map<String, Object>) capturedData.get("metrics");
            assertThat(metrics.get("totalLogs")).isEqualTo(1000L);
            assertThat(metrics.get("errorCount")).isEqualTo(50L);
            assertThat(metrics.get("avgResponseTime")).isEqualTo(250.0);
        }

        @Test
        @DisplayName("PDF_형식으로_문서를_생성한다")
        void PDF_형식으로_문서를_생성한다() {
            // given
            ProjectAnalysisRequest pdfRequest = ProjectAnalysisRequest.builder()
                    .format(DocumentFormat.PDF)
                    .options(AnalysisOptions.builder().build())
                    .build();

            AnalysisDocumentResponse pdfResponse = AnalysisDocumentResponse.builder()
                    .projectUuid(PROJECT_UUID)
                    .format(DocumentFormat.PDF)
                    .downloadUrl("/api/analysis/downloads/test-file-id")
                    .build();

            when(projectValidator.validateProjectExists(PROJECT_UUID))
                    .thenReturn(mockProject);
            when(dashboardService.getStatisticsOverview(anyString(), any(), anyString()))
                    .thenReturn(mockDashboard);
            when(logService.getLogs(any(LogSearchRequest.class)))
                    .thenReturn(mockTopErrors);
            when(documentGenerationService.generateDocument(
                    anyString(), any(), any(DocumentFormat.class), any(DocumentType.class), anyMap(), anyMap()
            ))
                    .thenReturn(pdfResponse);

            // when
            AnalysisDocumentResponse response = analysisService.generateProjectAnalysisDocument(
                    PROJECT_UUID, pdfRequest
            );

            // then
            assertThat(response.getFormat()).isEqualTo(DocumentFormat.PDF);
            verify(documentGenerationService).generateDocument(
                    eq(PROJECT_UUID),
                    isNull(),
                    eq(DocumentFormat.PDF),
                    eq(DocumentType.PROJECT_ANALYSIS),
                    anyMap(),
                    anyMap()
            );
        }
    }

    @Nested
    @DisplayName("generateErrorAnalysisDocument 메서드 테스트")
    class GenerateErrorAnalysisDocumentTest {

        private ErrorAnalysisRequest request;
        private Log mockLog;
        private LogDetailResponse mockAiAnalysis;
        private LogPageResponse mockRelatedLogs;
        private AnalysisDocumentResponse expectedResponse;

        @BeforeEach
        void setUp() {
            // Mock Log Entity
            mockLog = new Log();
            mockLog.setLogId(LOG_ID);
            mockLog.setLogLevel(LogLevel.ERROR);
            mockLog.setMessage("NullPointerException occurred");
            mockLog.setStackTrace("java.lang.NullPointerException: null\n\tat com.example.Service.method(Service.java:42)");
            mockLog.setTimestamp(OffsetDateTime.of(2025, 11, 14, 10, 0, 0, 0, ZoneOffset.UTC));
            mockLog.setComponentName("UserService");
            mockLog.setTraceId(TRACE_ID);

            // Mock AI Analysis Response
            AiAnalysisDto aiAnalysisDto = AiAnalysisDto.builder()
                    .summary("Error summary")
                    .errorCause("NullPointerException")
                    .solution("Check null values")
                    .tags(List.of("NPE", "critical"))
                    .build();

            mockAiAnalysis = LogDetailResponse.builder()
                    .analysis(aiAnalysisDto)
                    .fromCache(false)
                    .build();

            // Mock Related Logs
            mockRelatedLogs = LogPageResponse.builder()
                    .logs(Collections.emptyList())
                    .pagination(PaginationResponse.builder()
                            .size(10)
                            .hasNext(false)
                            .nextCursor(null)
                            .build())
                    .build();

            // Expected Response
            expectedResponse = AnalysisDocumentResponse.builder()
                    .projectUuid(PROJECT_UUID)
                    .logId(LOG_ID)
                    .format(DocumentFormat.HTML)
                    .content("<html>Error Analysis</html>")
                    .build();

            // Request
            request = ErrorAnalysisRequest.builder()
                    .projectUuid(PROJECT_UUID)
                    .format(DocumentFormat.HTML)
                    .options(ErrorAnalysisOptions.builder()
                            .includeRelatedLogs(true)
                            .maxRelatedLogs(10)
                            .build())
                    .build();
        }

        @Test
        @DisplayName("에러_분석_문서를_성공적으로_생성한다")
        void 에러_분석_문서를_성공적으로_생성한다() {
            // given
            when(logRepository.findByLogId(LOG_ID, PROJECT_UUID))
                    .thenReturn(Optional.of(mockLog));
            when(logService.getLogDetail(LOG_ID, PROJECT_UUID))
                    .thenReturn(mockAiAnalysis);
            when(logService.getLogs(any(LogSearchRequest.class)))
                    .thenReturn(mockRelatedLogs);
            when(documentGenerationService.generateDocument(
                    anyString(), anyLong(), any(DocumentFormat.class), any(DocumentType.class), anyMap(), anyMap()
            ))
                    .thenReturn(expectedResponse);

            // when
            AnalysisDocumentResponse response = analysisService.generateErrorAnalysisDocument(
                    LOG_ID, request
            );

            // then
            assertThat(response).isNotNull();
            assertThat(response.getProjectUuid()).isEqualTo(PROJECT_UUID);
            assertThat(response.getLogId()).isEqualTo(LOG_ID);
            assertThat(response.getFormat()).isEqualTo(DocumentFormat.HTML);

            verify(logService).getLogDetail(LOG_ID, PROJECT_UUID);
            verify(documentGenerationService).generateDocument(
                    eq(PROJECT_UUID),
                    eq(LOG_ID),
                    eq(DocumentFormat.HTML),
                    eq(DocumentType.ERROR_ANALYSIS),
                    anyMap(),
                    anyMap()
            );
        }

        @Test
        @DisplayName("관련_로그_포함_옵션이_true면_관련_로그를_조회한다")
        void 관련_로그_포함_옵션이_true면_관련_로그를_조회한다() {
            // given
            when(logRepository.findByLogId(LOG_ID, PROJECT_UUID))
                    .thenReturn(Optional.of(mockLog));
            when(logService.getLogDetail(LOG_ID, PROJECT_UUID))
                    .thenReturn(mockAiAnalysis);
            when(logService.getLogs(any(LogSearchRequest.class)))
                    .thenReturn(mockRelatedLogs);
            when(documentGenerationService.generateDocument(
                    anyString(), anyLong(), any(DocumentFormat.class), any(DocumentType.class), anyMap(), anyMap()
            ))
                    .thenReturn(expectedResponse);

            // when
            analysisService.generateErrorAnalysisDocument(LOG_ID, request);

            // then
            ArgumentCaptor<LogSearchRequest> searchCaptor = ArgumentCaptor.forClass(LogSearchRequest.class);
            verify(logService).getLogs(searchCaptor.capture());

            LogSearchRequest capturedSearch = searchCaptor.getValue();
            assertThat(capturedSearch.getProjectUuid()).isEqualTo(PROJECT_UUID);
            assertThat(capturedSearch.getTraceId()).isEqualTo(TRACE_ID);
            assertThat(capturedSearch.getSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("관련_로그_포함_옵션이_false면_관련_로그를_조회하지_않는다")
        void 관련_로그_포함_옵션이_false면_관련_로그를_조회하지_않는다() {
            // given
            ErrorAnalysisRequest requestWithoutRelated = ErrorAnalysisRequest.builder()
                    .projectUuid(PROJECT_UUID)
                    .format(DocumentFormat.HTML)
                    .options(ErrorAnalysisOptions.builder()
                            .includeRelatedLogs(false)
                            .build())
                    .build();

            when(logRepository.findByLogId(LOG_ID, PROJECT_UUID))
                    .thenReturn(Optional.of(mockLog));
            when(logService.getLogDetail(LOG_ID, PROJECT_UUID))
                    .thenReturn(mockAiAnalysis);
            when(documentGenerationService.generateDocument(
                    anyString(), anyLong(), any(DocumentFormat.class), any(DocumentType.class), anyMap(), anyMap()
            ))
                    .thenReturn(expectedResponse);

            // when
            analysisService.generateErrorAnalysisDocument(LOG_ID, requestWithoutRelated);

            // then
            verify(logService, never()).getLogs(any(LogSearchRequest.class));
        }

        @Test
        @DisplayName("traceId가_null이면_관련_로그를_조회하지_않는다")
        void traceId가_null이면_관련_로그를_조회하지_않는다() {
            // given
            Log logWithoutTrace = new Log();
            logWithoutTrace.setLogId(LOG_ID);
            logWithoutTrace.setLogLevel(LogLevel.ERROR);
            logWithoutTrace.setMessage("Error message");
            logWithoutTrace.setStackTrace("Stack trace");
            logWithoutTrace.setTimestamp(OffsetDateTime.of(2025, 11, 14, 10, 0, 0, 0, ZoneOffset.UTC));
            logWithoutTrace.setComponentName("Service");
            logWithoutTrace.setTraceId(null); // traceId가 null

            when(logRepository.findByLogId(LOG_ID, PROJECT_UUID))
                    .thenReturn(Optional.of(logWithoutTrace));
            when(logService.getLogDetail(LOG_ID, PROJECT_UUID))
                    .thenReturn(mockAiAnalysis);
            when(documentGenerationService.generateDocument(
                    anyString(), anyLong(), any(DocumentFormat.class), any(DocumentType.class), anyMap(), anyMap()
            ))
                    .thenReturn(expectedResponse);

            // when
            analysisService.generateErrorAnalysisDocument(LOG_ID, request);

            // then
            verify(logService, never()).getLogs(any(LogSearchRequest.class));
        }

        @Test
        @DisplayName("관련_로그_조회_실패_시에도_문서를_생성한다")
        void 관련_로그_조회_실패_시에도_문서를_생성한다() {
            // given
            when(logRepository.findByLogId(LOG_ID, PROJECT_UUID))
                    .thenReturn(Optional.of(mockLog));
            when(logService.getLogDetail(LOG_ID, PROJECT_UUID))
                    .thenReturn(mockAiAnalysis);
            when(logService.getLogs(any(LogSearchRequest.class)))
                    .thenThrow(new RuntimeException("Log service error"));
            when(documentGenerationService.generateDocument(
                    anyString(), anyLong(), any(DocumentFormat.class), any(DocumentType.class), anyMap(), anyMap()
            ))
                    .thenReturn(expectedResponse);

            // when
            AnalysisDocumentResponse response = analysisService.generateErrorAnalysisDocument(
                    LOG_ID, request
            );

            // then
            assertThat(response).isNotNull();
            verify(documentGenerationService).generateDocument(
                    anyString(), anyLong(), any(), any(), anyMap(), anyMap()
            );
        }

        @Test
        @DisplayName("수집된_데이터에_에러_로그_정보가_포함된다")
        void 수집된_데이터에_에러_로그_정보가_포함된다() {
            // given
            when(logRepository.findByLogId(LOG_ID, PROJECT_UUID))
                    .thenReturn(Optional.of(mockLog));
            when(logService.getLogDetail(LOG_ID, PROJECT_UUID))
                    .thenReturn(mockAiAnalysis);
            when(logService.getLogs(any(LogSearchRequest.class)))
                    .thenReturn(mockRelatedLogs);
            when(documentGenerationService.generateDocument(
                    anyString(), anyLong(), any(DocumentFormat.class), any(DocumentType.class), anyMap(), anyMap()
            ))
                    .thenReturn(expectedResponse);

            // when
            analysisService.generateErrorAnalysisDocument(LOG_ID, request);

            // then
            ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);
            verify(documentGenerationService).generateDocument(
                    anyString(), anyLong(), any(), any(), dataCaptor.capture(), anyMap()
            );

            Map<String, Object> capturedData = dataCaptor.getValue();
            assertThat(capturedData).containsKey("errorLog");

            Map<String, Object> errorInfo = (Map<String, Object>) capturedData.get("errorLog");
            assertThat(errorInfo.get("logId")).isEqualTo(LOG_ID);
            assertThat(errorInfo.get("level")).isEqualTo("ERROR");
            assertThat(errorInfo.get("message")).isEqualTo("NullPointerException occurred");
            assertThat(errorInfo.get("traceId")).isEqualTo(TRACE_ID);
        }

        @Test
        @DisplayName("기존_분석_결과가_있으면_데이터에_포함한다")
        void 기존_분석_결과가_있으면_데이터에_포함한다() {
            // given
            when(logRepository.findByLogId(LOG_ID, PROJECT_UUID))
                    .thenReturn(Optional.of(mockLog));
            when(logService.getLogDetail(LOG_ID, PROJECT_UUID))
                    .thenReturn(mockAiAnalysis);
            when(logService.getLogs(any(LogSearchRequest.class)))
                    .thenReturn(mockRelatedLogs);
            when(documentGenerationService.generateDocument(
                    anyString(), anyLong(), any(DocumentFormat.class), any(DocumentType.class), anyMap(), anyMap()
            ))
                    .thenReturn(expectedResponse);

            // when
            analysisService.generateErrorAnalysisDocument(LOG_ID, request);

            // then
            ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);
            verify(documentGenerationService).generateDocument(
                    anyString(), anyLong(), any(), any(), dataCaptor.capture(), anyMap()
            );

            Map<String, Object> capturedData = dataCaptor.getValue();
            assertThat(capturedData).containsKey("existingAnalysis");

            Map<String, Object> existingAnalysis = (Map<String, Object>) capturedData.get("existingAnalysis");
            assertThat(existingAnalysis.get("summary")).isEqualTo("Error summary");
            assertThat(existingAnalysis.get("errorCause")).isEqualTo("NullPointerException");
            assertThat(existingAnalysis.get("solution")).isEqualTo("Check null values");
        }

        @Test
        @DisplayName("PDF_형식으로_에러_분석_문서를_생성한다")
        void PDF_형식으로_에러_분석_문서를_생성한다() {
            // given
            ErrorAnalysisRequest pdfRequest = ErrorAnalysisRequest.builder()
                    .projectUuid(PROJECT_UUID)
                    .format(DocumentFormat.PDF)
                    .options(ErrorAnalysisOptions.builder().build())
                    .build();

            AnalysisDocumentResponse pdfResponse = AnalysisDocumentResponse.builder()
                    .projectUuid(PROJECT_UUID)
                    .logId(LOG_ID)
                    .format(DocumentFormat.PDF)
                    .downloadUrl("/api/analysis/downloads/test-file-id")
                    .build();

            when(logRepository.findByLogId(LOG_ID, PROJECT_UUID))
                    .thenReturn(Optional.of(mockLog));
            when(logService.getLogDetail(LOG_ID, PROJECT_UUID))
                    .thenReturn(mockAiAnalysis);
            when(documentGenerationService.generateDocument(
                    anyString(), anyLong(), any(DocumentFormat.class), any(DocumentType.class), anyMap(), anyMap()
            ))
                    .thenReturn(pdfResponse);

            // when
            AnalysisDocumentResponse response = analysisService.generateErrorAnalysisDocument(
                    LOG_ID, pdfRequest
            );

            // then
            assertThat(response.getFormat()).isEqualTo(DocumentFormat.PDF);
            verify(documentGenerationService).generateDocument(
                    eq(PROJECT_UUID),
                    eq(LOG_ID),
                    eq(DocumentFormat.PDF),
                    eq(DocumentType.ERROR_ANALYSIS),
                    anyMap(),
                    anyMap()
            );
        }
    }
}
