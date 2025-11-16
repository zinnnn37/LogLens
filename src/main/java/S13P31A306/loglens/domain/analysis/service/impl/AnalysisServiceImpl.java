package S13P31A306.loglens.domain.analysis.service.impl;

import S13P31A306.loglens.domain.analysis.constants.AnalysisErrorCode;
import S13P31A306.loglens.domain.analysis.constants.DocumentType;
import S13P31A306.loglens.domain.analysis.dto.ai.AiHtmlDocumentRequest;
import S13P31A306.loglens.domain.analysis.dto.ai.AiHtmlDocumentResponse;
import S13P31A306.loglens.domain.analysis.dto.ai.StylePreferences;
import S13P31A306.loglens.domain.analysis.dto.request.ErrorAnalysisRequest;
import S13P31A306.loglens.domain.analysis.dto.request.ProjectAnalysisRequest;
import S13P31A306.loglens.domain.analysis.dto.response.AnalysisDocumentDetailResponse;
import S13P31A306.loglens.domain.analysis.dto.response.AnalysisDocumentResponse;
import S13P31A306.loglens.domain.analysis.dto.response.AnalysisDocumentSummary;
import S13P31A306.loglens.domain.analysis.dto.response.DocumentMetadata;
import S13P31A306.loglens.domain.analysis.dto.response.DocumentSummary;
import S13P31A306.loglens.domain.analysis.dto.response.TimeRange;
import S13P31A306.loglens.domain.analysis.entity.AnalysisDocument;
import S13P31A306.loglens.domain.analysis.mapper.AnalysisMapper;
import S13P31A306.loglens.domain.analysis.repository.AnalysisDocumentRepository;
import S13P31A306.loglens.domain.analysis.service.AnalysisService;
import S13P31A306.loglens.domain.analysis.service.DocumentGenerationService;
import S13P31A306.loglens.domain.dashboard.dto.response.DashboardOverviewResponse;
import S13P31A306.loglens.domain.dashboard.service.DashboardService;
import S13P31A306.loglens.domain.log.dto.request.LogSearchRequest;
import S13P31A306.loglens.domain.log.dto.response.LogDetailResponse;
import S13P31A306.loglens.domain.log.dto.response.LogPageResponse;
import S13P31A306.loglens.domain.log.entity.Log;
import S13P31A306.loglens.domain.log.repository.LogRepository;
import S13P31A306.loglens.domain.log.service.LogService;
import S13P31A306.loglens.domain.project.entity.Project;
import S13P31A306.loglens.domain.project.service.ProjectService;
import S13P31A306.loglens.domain.project.validator.ProjectValidator;
import S13P31A306.loglens.global.client.AiServiceClient;
import S13P31A306.loglens.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 분석 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    private final ProjectValidator projectValidator;
    private final DashboardService dashboardService;
    private final LogService logService;
    private final LogRepository logRepository;
    private final DocumentGenerationService documentGenerationService;
    private final AiServiceClient aiServiceClient;
    private final AnalysisMapper analysisMapper;
    private final AnalysisDocumentRepository analysisDocumentRepository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    @Transactional
    public AnalysisDocumentResponse generateProjectAnalysisDocument(
            String projectUuid,
            ProjectAnalysisRequest request
    ) {
        log.info("Starting project analysis document generation for project: {}", projectUuid);

        // 1. 프로젝트 정보 조회
        Project project = projectValidator.validateProjectExists(projectUuid);

        // 2. 시간 범위 설정
        LocalDateTime startTime = request.getStartTime();
        LocalDateTime endTime = request.getEndTime() != null ? request.getEndTime() : LocalDateTime.now();

        // 3. 대시보드 데이터 수집
        Map<String, Object> analysisData = collectProjectData(projectUuid, project, startTime, endTime, request);

        // 4. 문서 형식에 따라 생성
        AnalysisDocumentResponse response = documentGenerationService.generateDocument(
                projectUuid,
                null,
                request.getFormat(),
                DocumentType.PROJECT_ANALYSIS,
                analysisData,
                convertOptionsToMap(request.getOptions())
        );

        // 5. 문서 저장
        saveAnalysisDocument(project.getId(), projectUuid, null, DocumentType.PROJECT_ANALYSIS, response);

        return response;
    }

    @Override
    @Transactional
    public AnalysisDocumentResponse generateErrorAnalysisDocument(
            Long logId,
            ErrorAnalysisRequest request
    ) {
        log.info("Starting error analysis document generation for log: {}", logId);

        // 1. 프로젝트 조회
        Project project = projectValidator.validateProjectExists(request.getProjectUuid());

        // 2. 에러 분석 데이터 수집 (내부에서 로그 조회 및 AI 분석 수행)
        Map<String, Object> analysisData = collectErrorData(logId, request.getProjectUuid(), request);

        // 3. 문서 형식에 따라 생성
        AnalysisDocumentResponse response = documentGenerationService.generateDocument(
                request.getProjectUuid(),
                logId,
                request.getFormat(),
                DocumentType.ERROR_ANALYSIS,
                analysisData,
                convertErrorOptionsToMap(request.getOptions())
        );

        // 4. 문서 저장
        saveAnalysisDocument(project.getId(), request.getProjectUuid(), logId, DocumentType.ERROR_ANALYSIS, response);

        return response;
    }

    /**
     * 프로젝트 분석 데이터 수집
     */
    private Map<String, Object> collectProjectData(
            String projectUuid,
            Project project,
            LocalDateTime startTime,
            LocalDateTime endTime,
            ProjectAnalysisRequest request
    ) {
        Map<String, Object> data = new HashMap<>();

        // 프로젝트 기본 정보
        Map<String, Object> projectInfo = new HashMap<>();
        projectInfo.put("name", project.getProjectName());
        projectInfo.put("uuid", projectUuid);
        projectInfo.put("description", project.getDescription());
        data.put("projectInfo", projectInfo);

        // 시간 범위
        Map<String, String> timeRange = new HashMap<>();
        if (startTime != null) {
            timeRange.put("startTime", startTime.format(FORMATTER));
        }
        timeRange.put("endTime", endTime.format(FORMATTER));
        data.put("timeRange", timeRange);

        // 대시보드 통계 조회
        try {
            String startTimeStr = startTime != null ? startTime.format(FORMATTER) : null;
            String endTimeStr = endTime.format(FORMATTER);

            DashboardOverviewResponse overview = dashboardService.getStatisticsOverview(
                    projectUuid,
                    startTimeStr,
                    endTimeStr
            );

            Map<String, Object> metrics = new HashMap<>();
            if (overview != null && overview.summary() != null) {
                metrics.put("totalLogs", ((Number) overview.summary().totalLogs()).longValue());
                metrics.put("errorCount", ((Number) overview.summary().errorCount()).longValue());
                metrics.put("warnCount", ((Number) overview.summary().warnCount()).longValue());
                metrics.put("infoCount", ((Number) overview.summary().infoCount()).longValue());
                metrics.put("avgResponseTime", ((Number) overview.summary().avgResponseTime()).doubleValue());
            }
            data.put("metrics", metrics);

        } catch (Exception e) {
            log.warn("Failed to collect dashboard metrics: {}", e.getMessage());
            data.put("metrics", new HashMap<>());
        }

        // 최근 에러 로그 조회
        try {
            LogSearchRequest logSearchRequest = LogSearchRequest.builder()
                    .projectUuid(projectUuid)
                    .logLevel(List.of("ERROR"))
                    .size(10)
                    .build();

            LogPageResponse topErrors = logService.getLogs(logSearchRequest);
            data.put("topErrors", topErrors.getLogs());

        } catch (Exception e) {
            log.warn("Failed to collect top errors: {}", e.getMessage());
        }

        return data;
    }

    /**
     * 에러 분석 데이터 수집
     */
    private Map<String, Object> collectErrorData(
            Long logId,
            String projectUuid,
            ErrorAnalysisRequest request
    ) {
        Map<String, Object> data = new HashMap<>();

        // 1. Log 엔티티 직접 조회 (모든 로그 필드 포함)
        Log logEntity = logRepository.findByLogId(logId, projectUuid)
                .orElseThrow(() -> new BusinessException(
                        AnalysisErrorCode.LOG_NOT_FOUND,
                        "로그를 찾을 수 없습니다: " + logId
                ));

        // 2. 에러 로그 기본 정보
        Map<String, Object> errorInfo = new HashMap<>();
        errorInfo.put("logId", logId);
        errorInfo.put("level", logEntity.getLogLevel().name());
        errorInfo.put("message", logEntity.getMessage());
        errorInfo.put("stackTrace", logEntity.getStackTrace());
        errorInfo.put("timestamp", logEntity.getTimestamp());
        errorInfo.put("componentName", logEntity.getComponentName());
        errorInfo.put("traceId", logEntity.getTraceId());
        data.put("errorLog", errorInfo);

        // 3. 관련 로그 조회 (동일 traceId)
        if (request.getOptions().getIncludeRelatedLogs() && logEntity.getTraceId() != null) {
            try {
                LogSearchRequest traceRequest = LogSearchRequest.builder()
                        .projectUuid(projectUuid)
                        .traceId(logEntity.getTraceId())
                        .size(request.getOptions().getMaxRelatedLogs())
                        .build();

                LogPageResponse relatedLogs = logService.getLogs(traceRequest);
                data.put("relatedLogs", relatedLogs.getLogs());

            } catch (Exception e) {
                log.warn("Failed to collect related logs: {}", e.getMessage());
            }
        }

        // 4. AI 분석 결과 조회
        try {
            LogDetailResponse aiAnalysis = logService.getLogDetail(logId, projectUuid);
            if (aiAnalysis.getAnalysis() != null) {
                Map<String, Object> existingAnalysis = new HashMap<>();
                existingAnalysis.put("summary", aiAnalysis.getAnalysis().getSummary());
                existingAnalysis.put("errorCause", aiAnalysis.getAnalysis().getErrorCause());
                existingAnalysis.put("solution", aiAnalysis.getAnalysis().getSolution());
                existingAnalysis.put("tags", aiAnalysis.getAnalysis().getTags());
                data.put("existingAnalysis", existingAnalysis);
            }
        } catch (Exception e) {
            log.warn("Failed to get AI analysis: {}", e.getMessage());
        }

        return data;
    }

    /**
     * AnalysisOptions를 Map으로 변환
     */
    private Map<String, Object> convertOptionsToMap(S13P31A306.loglens.domain.analysis.dto.request.AnalysisOptions options) {
        Map<String, Object> optionsMap = new HashMap<>();
        if (options != null) {
            optionsMap.put("includeComponents", options.getIncludeComponents());
            optionsMap.put("includeAlerts", options.getIncludeAlerts());
            optionsMap.put("includeDependencies", options.getIncludeDependencies());
            optionsMap.put("includeCharts", options.getIncludeCharts());
            optionsMap.put("darkMode", options.getDarkMode());
        }
        return optionsMap;
    }

    /**
     * ErrorAnalysisOptions를 Map으로 변환
     */
    private Map<String, Object> convertErrorOptionsToMap(S13P31A306.loglens.domain.analysis.dto.request.ErrorAnalysisOptions options) {
        Map<String, Object> optionsMap = new HashMap<>();
        if (options != null) {
            optionsMap.put("includeRelatedLogs", options.getIncludeRelatedLogs());
            optionsMap.put("includeSimilarErrors", options.getIncludeSimilarErrors());
            optionsMap.put("includeImpactAnalysis", options.getIncludeImpactAnalysis());
            optionsMap.put("includeCodeExamples", options.getIncludeCodeExamples());
            optionsMap.put("maxRelatedLogs", options.getMaxRelatedLogs());
        }
        return optionsMap;
    }

    /**
     * 분석 문서 저장
     */
    private void saveAnalysisDocument(
            Integer projectId,
            String projectUuid,
            Long logId,
            DocumentType documentType,
            AnalysisDocumentResponse response
    ) {
        try {
            DocumentMetadata metadata = response.getDocumentMetadata();
            DocumentSummary summary = metadata != null ? metadata.getSummary() : null;

            AnalysisDocument document = AnalysisDocument.builder()
                    .projectId(projectId)
                    .documentType(documentType)
                    .title(metadata != null ? metadata.getTitle() : documentType.name() + " Document")
                    .content(response.getContent())
                    .logId(logId)
                    .validationStatus(response.getValidationStatus())
                    .healthScore(summary != null ? summary.getHealthScore() : null)
                    .totalIssues(summary != null ? summary.getTotalIssues() : null)
                    .criticalIssues(summary != null ? summary.getCriticalIssues() : null)
                    .wordCount(metadata != null ? metadata.getWordCount() : null)
                    .estimatedReadingTime(metadata != null ? metadata.getEstimatedReadingTime() : null)
                    .build();

            analysisDocumentRepository.save(document);
            log.info("Analysis document saved with id: {}", document.getId());

        } catch (Exception e) {
            log.warn("Failed to save analysis document: {}", e.getMessage());
            // 저장 실패해도 문서 생성은 성공으로 처리
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AnalysisDocumentSummary> getAnalysisDocuments(
            String projectUuid,
            Pageable pageable
    ) {
        log.info("Fetching analysis documents for project: {}", projectUuid);

        // 프로젝트 존재 확인
        Project project = projectValidator.validateProjectExists(projectUuid);

        // 문서 목록 조회
        Page<AnalysisDocument> documents = analysisDocumentRepository.findByProjectIdOrderByCreatedAtDesc(
                project.getId(), pageable
        );

        // DTO 변환
        return documents.map(doc -> AnalysisDocumentSummary.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .documentType(doc.getDocumentType())
                .validationStatus(doc.getValidationStatus())
                .healthScore(doc.getHealthScore())
                .totalIssues(doc.getTotalIssues())
                .criticalIssues(doc.getCriticalIssues())
                .wordCount(doc.getWordCount())
                .estimatedReadingTime(doc.getEstimatedReadingTime())
                .logId(doc.getLogId())
                .createdAt(doc.getCreatedAt())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public AnalysisDocumentDetailResponse getAnalysisDocumentById(
            Integer documentId,
            String projectUuid
    ) {
        log.info("Fetching analysis document: {} for project: {}", documentId, projectUuid);

        // 프로젝트 존재 확인
        Project project = projectValidator.validateProjectExists(projectUuid);

        // 문서 조회
        AnalysisDocument document = analysisDocumentRepository.findByIdAndProjectId(documentId, project.getId())
                .orElseThrow(() -> new BusinessException(
                        AnalysisErrorCode.DOCUMENT_NOT_FOUND,
                        "문서를 찾을 수 없습니다: " + documentId
                ));

        // DTO 변환
        return AnalysisDocumentDetailResponse.builder()
                .id(document.getId())
                .projectUuid(projectUuid)
                .documentType(document.getDocumentType())
                .title(document.getTitle())
                .content(document.getContent())
                .logId(document.getLogId())
                .validationStatus(document.getValidationStatus())
                .healthScore(document.getHealthScore())
                .totalIssues(document.getTotalIssues())
                .criticalIssues(document.getCriticalIssues())
                .wordCount(document.getWordCount())
                .estimatedReadingTime(document.getEstimatedReadingTime())
                .createdAt(document.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void deleteAnalysisDocument(
            Integer documentId,
            String projectUuid
    ) {
        log.info("Deleting analysis document: {} for project: {}", documentId, projectUuid);

        // 프로젝트 존재 확인
        Project project = projectValidator.validateProjectExists(projectUuid);

        // 문서 조회
        AnalysisDocument document = analysisDocumentRepository.findByIdAndProjectId(documentId, project.getId())
                .orElseThrow(() -> new BusinessException(
                        AnalysisErrorCode.DOCUMENT_NOT_FOUND,
                        "문서를 찾을 수 없습니다: " + documentId
                ));

        // 삭제
        analysisDocumentRepository.delete(document);
        log.info("Analysis document deleted: {}", documentId);
    }
}
