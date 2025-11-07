package S13P31A306.loglens.domain.log.service.impl;

import S13P31A306.loglens.domain.log.constants.LogErrorCode;
import S13P31A306.loglens.domain.log.dto.ai.AiAnalysisDto;
import S13P31A306.loglens.domain.log.dto.ai.AiAnalysisResponse;
import S13P31A306.loglens.domain.log.dto.internal.LogSearchResult;
import S13P31A306.loglens.domain.log.dto.internal.TraceLogSearchResult;
import S13P31A306.loglens.domain.log.dto.request.LogSearchRequest;
import S13P31A306.loglens.domain.log.dto.response.LogDetailResponse;
import S13P31A306.loglens.domain.log.dto.response.LogPageResponse;
import S13P31A306.loglens.domain.log.dto.response.LogResponse;
import S13P31A306.loglens.domain.log.dto.response.PaginationResponse;
import S13P31A306.loglens.domain.log.dto.response.TraceLogResponse;
import S13P31A306.loglens.domain.log.entity.Log;
import S13P31A306.loglens.domain.log.mapper.LogMapper;
import S13P31A306.loglens.domain.log.repository.LogRepository;
import S13P31A306.loglens.domain.log.service.LogService;
import S13P31A306.loglens.global.client.AiServiceClient;
import S13P31A306.loglens.global.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    private static final String LOG_PREFIX = "[LogService]";

    private final LogRepository logRepository;
    private final LogMapper logMapper;
    private final ObjectMapper objectMapper; // For cursor encoding/decoding
    private final AiServiceClient aiServiceClient;

    @Override
    public LogPageResponse getLogs(LogSearchRequest request) {
        log.info("{} 로그 목록 조회 시작: projectUuid={}", LOG_PREFIX, request.getProjectUuid());

        LogSearchResult result = searchLogs(request.getProjectUuid(), request);
        List<LogResponse> logResponses = mapToLogResponses(result);
        PaginationResponse pagination = createPaginationResponse(result);

        LogPageResponse response = LogPageResponse.builder()
                .logs(logResponses)
                .pagination(pagination)
                .build();

        log.info("{} 로그 목록 조회 완료: 조회된 로그 수={}, hasNext={}",
                LOG_PREFIX, logResponses.size(), result.hasNext());

        return response;
    }

    @Override
    public TraceLogResponse getLogsByTraceId(LogSearchRequest request) {
        log.info("{} Trace ID로 로그 조회 시작: projectUuid={}, traceId={}",
                LOG_PREFIX, request.getProjectUuid(), request.getTraceId());

        TraceLogSearchResult result = searchLogsByTraceId(request.getProjectUuid(), request);
        List<LogResponse> logResponses = mapToLogResponses(result);

        TraceLogResponse response = TraceLogResponse.builder()
                .traceId(request.getTraceId())
                .summary(result.summary())
                .logs(logResponses)
                .build();

        log.info("{} Trace ID로 로그 조회 완료: 로그 수={}, 전체 로그 수={}",
                LOG_PREFIX, logResponses.size(), result.summary().getTotalLogs());

        return response;
    }

    private LogSearchResult searchLogs(String projectUuid, LogSearchRequest request) {
        log.debug("{} OpenSearch에서 로그 조회: projectUuid={}", LOG_PREFIX, projectUuid);
        LogSearchResult result = logRepository.findWithCursor(projectUuid, request);
        if (Objects.isNull(result)) {
            return new LogSearchResult(Collections.emptyList(), false, null);
        }
        log.debug("{} OpenSearch 조회 완료: 로그 개수={}", LOG_PREFIX, result.logs().size());
        return result;
    }

    private TraceLogSearchResult searchLogsByTraceId(String projectUuid, LogSearchRequest request) {
        log.debug("{} OpenSearch에서 Trace ID로 로그 조회: projectUuid={}, traceId={}", LOG_PREFIX, projectUuid,
                request.getTraceId());
        TraceLogSearchResult result = logRepository.findByTraceId(projectUuid, request);
        log.debug("{} OpenSearch 조회 완료: 로그 개수={}", LOG_PREFIX, result.logs().size());
        return result;
    }

    private List<LogResponse> mapToLogResponses(LogSearchResult result) {
        return result.logs().stream()
                .map(logMapper::toLogResponse)
                .toList();
    }

    private List<LogResponse> mapToLogResponses(TraceLogSearchResult result) {
        return result.logs().stream()
                .map(logMapper::toLogResponse)
                .toList();
    }

    private PaginationResponse createPaginationResponse(LogSearchResult result) {
        String nextCursor = result.hasNext() ? encodeCursor(result.sortValues()) : null;
        return PaginationResponse.builder()
                .hasNext(result.hasNext())
                .nextCursor(nextCursor)
                .size(result.logs().size())
                .build();
    }

    /**
     * 커서 인코딩 (페이지네이션용)
     *
     * @param sortValues OpenSearch의 sort 값
     * @return Base64로 인코딩된 커서 문자열
     */
    private String encodeCursor(Object[] sortValues) {
        if (Objects.isNull(sortValues)) {
            return null;
        }
        try {
            String encoded = Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(sortValues));
            log.debug("{} 커서 인코딩 완료", LOG_PREFIX);
            return encoded;
        } catch (Exception e) {
            log.error("{} 커서 인코딩 실패: sortValues={}", LOG_PREFIX, sortValues, e);
            throw new BusinessException(LogErrorCode.CURSOR_ENCODING_FAILED, null, e);
        }
    }

    @Override
    public LogDetailResponse getLogDetail(Long logId, String projectUuid) {
        log.info("{} 로그 상세 조회 시작: logId={}, projectUuid={}", LOG_PREFIX, logId, projectUuid);

        // 1. OpenSearch에서 로그 조회
        Log log = logRepository.findByLogId(logId, projectUuid)
                .orElseThrow(() -> {
                    LogServiceImpl.log.warn("{} 로그를 찾을 수 없음: logId={}, projectUuid={}",
                            LOG_PREFIX, logId, projectUuid);
                    return new BusinessException(LogErrorCode.LOG_NOT_FOUND);
                });

        // 2. 기본 로그 정보로 LogDetailResponse 빌드 시작
        LogDetailResponse.LogDetailResponseBuilder builder = LogDetailResponse.builder()
                .logId(log.getLogId())
                .traceId(log.getTraceId())
                .logLevel(log.getLogLevel())
                .sourceType(log.getSourceType())
                .message(log.getMessage())
                .timestamp(!Objects.isNull(log.getTimestamp()) ? log.getTimestamp().toLocalDateTime() : null)
                .logger(log.getLogger())
                .layer(log.getLayer())
                .comment(log.getComment())
                .serviceName(log.getServiceName())
                .className(log.getClassName())
                .methodName(log.getMethodName())
                .threadName(log.getThreadName())
                .requesterIp(log.getRequesterIp())
                .duration(log.getDuration())
                .stackTrace(log.getStackTrace())
                .logDetails(log.getLogDetails());

        // 3. AI 분석 결과 확인 및 처리
        AiAnalysisDto analysis = null;
        Boolean fromCache = null;
        Long similarLogId = null;
        Double similarityScore = null;

        // 3-1. OpenSearch에 저장된 aiAnalysis 확인
        Map<String, Object> aiAnalysisMap = log.getAiAnalysis();
        if (aiAnalysisMap != null && !aiAnalysisMap.isEmpty()) {
            LogServiceImpl.log.info("{} OpenSearch에 저장된 AI 분석 결과 사용: logId={}", LOG_PREFIX, logId);
            try {
                analysis = objectMapper.convertValue(aiAnalysisMap, AiAnalysisDto.class);
                fromCache = true;
            } catch (Exception e) {
                LogServiceImpl.log.error("{} AI 분석 결과 변환 실패: logId={}", LOG_PREFIX, logId, e);
            }
        }

        // 3-2. AI 분석이 없으면 AI 서비스 호출
        if (Objects.isNull(analysis)) {
            LogServiceImpl.log.info("{} AI 서비스 호출하여 분석 수행: logId={}", LOG_PREFIX, logId);
            try {
                AiAnalysisResponse aiResponse = aiServiceClient.analyzeLog(logId, projectUuid);
                if (aiResponse != null && aiResponse.getAnalysis() != null) {
                    analysis = aiResponse.getAnalysis();
                    fromCache = aiResponse.getFromCache();
                    similarLogId = aiResponse.getSimilarLogId();
                    similarityScore = aiResponse.getSimilarityScore();
                    LogServiceImpl.log.info("{} AI 분석 완료: logId={}, fromCache={}", LOG_PREFIX, logId, fromCache);
                } else {
                    LogServiceImpl.log.warn("{} AI 분석 결과가 null: logId={}", LOG_PREFIX, logId);
                }
            } catch (Exception e) {
                LogServiceImpl.log.error("{} AI 서비스 호출 실패, 분석 없이 로그만 반환: logId={}", LOG_PREFIX, logId, e);
            }
        }

        // 4. AI 분석 결과를 포함한 응답 반환
        LogDetailResponse response = builder
                .analysis(analysis)
                .fromCache(fromCache)
                .similarLogId(similarLogId)
                .similarityScore(similarityScore)
                .build();

        LogServiceImpl.log.info("{} 로그 상세 조회 완료: logId={}, hasAnalysis={}", LOG_PREFIX, logId, analysis != null);
        return response;
    }
}
