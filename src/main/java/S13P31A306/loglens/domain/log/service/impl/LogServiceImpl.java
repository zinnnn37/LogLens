package S13P31A306.loglens.domain.log.service.impl;

import S13P31A306.loglens.domain.log.constants.LogErrorCode;
import S13P31A306.loglens.domain.log.dto.internal.LogSearchResult;
import S13P31A306.loglens.domain.log.dto.internal.TraceLogSearchResult;
import S13P31A306.loglens.domain.log.dto.request.LogSearchRequest;
import S13P31A306.loglens.domain.log.dto.response.LogPageResponse;
import S13P31A306.loglens.domain.log.dto.response.LogResponse;
import S13P31A306.loglens.domain.log.dto.response.PaginationResponse;
import S13P31A306.loglens.domain.log.dto.response.TraceLogResponse;
import S13P31A306.loglens.domain.log.mapper.LogMapper;
import S13P31A306.loglens.domain.log.repository.LogRepository;
import S13P31A306.loglens.domain.log.service.LogService;
import S13P31A306.loglens.global.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
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
                .collect(Collectors.toList());
    }

    private List<LogResponse> mapToLogResponses(TraceLogSearchResult result) {
        return result.logs().stream()
                .map(logMapper::toLogResponse)
                .collect(Collectors.toList());
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
}
