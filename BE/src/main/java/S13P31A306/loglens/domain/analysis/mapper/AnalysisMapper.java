package S13P31A306.loglens.domain.analysis.mapper;

import S13P31A306.loglens.domain.analysis.constants.DocumentFormat;
import S13P31A306.loglens.domain.analysis.dto.ai.AiDocumentMetadata;
import S13P31A306.loglens.domain.analysis.dto.ai.AiHtmlDocumentResponse;
import S13P31A306.loglens.domain.analysis.dto.response.AnalysisDocumentResponse;
import S13P31A306.loglens.domain.analysis.dto.response.DocumentMetadata;
import S13P31A306.loglens.domain.analysis.dto.response.DocumentSummary;
import S13P31A306.loglens.domain.analysis.dto.response.TimeRange;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDateTime;

/**
 * 분석 도메인 Mapper
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AnalysisMapper {

    /**
     * AI 응답을 프로젝트 분석 문서 응답으로 변환
     */
    @Mapping(source = "aiResponse.htmlContent", target = "content")
    @Mapping(source = "projectUuid", target = "projectUuid")
    @Mapping(source = "format", target = "format")
    @Mapping(target = "validationStatus", constant = "VALID")
    @Mapping(target = "cacheTtl", constant = "3600")
    AnalysisDocumentResponse toProjectDocumentResponse(
            AiHtmlDocumentResponse aiResponse,
            String projectUuid,
            DocumentFormat format,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    /**
     * AI 응답을 에러 분석 문서 응답으로 변환
     */
    @Mapping(source = "aiResponse.htmlContent", target = "content")
    @Mapping(source = "logId", target = "logId")
    @Mapping(source = "format", target = "format")
    @Mapping(target = "validationStatus", constant = "VALID")
    @Mapping(target = "cacheTtl", constant = "3600")
    AnalysisDocumentResponse toErrorDocumentResponse(
            AiHtmlDocumentResponse aiResponse,
            Long logId,
            DocumentFormat format
    );

    /**
     * AI 문서 메타데이터를 일반 문서 메타데이터로 변환
     */
    @Mapping(source = "aiMetadata", target = ".")
    DocumentMetadata toDocumentMetadata(
            AiDocumentMetadata aiMetadata,
            String title,
            LocalDateTime generatedAt,
            TimeRange dataRange
    );

    /**
     * AI 메타데이터를 문서 요약으로 변환 (프로젝트 분석용)
     */
    @Mapping(source = "healthScore", target = "healthScore")
    @Mapping(source = "totalIssues", target = "totalIssues")
    @Mapping(source = "criticalIssues", target = "criticalIssues")
    @Mapping(source = "recommendations", target = "recommendations")
    DocumentSummary toProjectSummary(AiDocumentMetadata aiMetadata);

    /**
     * AI 메타데이터를 문서 요약으로 변환 (에러 분석용)
     */
    @Mapping(source = "severity", target = "severity")
    @Mapping(source = "rootCause", target = "rootCause")
    @Mapping(source = "affectedUsers", target = "affectedUsers")
    DocumentSummary toErrorSummary(AiDocumentMetadata aiMetadata);
}
