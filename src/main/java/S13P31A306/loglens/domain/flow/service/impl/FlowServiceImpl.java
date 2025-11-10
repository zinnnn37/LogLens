package S13P31A306.loglens.domain.flow.service.impl;

import S13P31A306.loglens.domain.flow.dto.response.TraceLogsResponse;
import S13P31A306.loglens.domain.flow.service.FlowService;
import S13P31A306.loglens.domain.log.constants.LogErrorCode;
import S13P31A306.loglens.domain.log.dto.request.LogSearchRequest;
import S13P31A306.loglens.domain.log.dto.response.LogResponse;
import S13P31A306.loglens.domain.log.dto.response.TraceLogResponse;
import S13P31A306.loglens.domain.log.service.LogService;
import S13P31A306.loglens.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FlowServiceImpl implements FlowService {
    private static final String LOG_PREFIX = "[FlowService]";
    private final LogService logService;

    @Override
    public TraceLogsResponse getTraceLogsById(String traceId, String projectUuid) {
        log.info("{} Trace 로그 조회 시작: traceId={}, projectUuid={}", LOG_PREFIX, traceId, projectUuid);

        // LogService를 이용하여 trace_id로 로그 검색
        LogSearchRequest searchRequest = LogSearchRequest.builder()
                .projectUuid(projectUuid)
                .traceId(traceId)
                .sort("TIMESTAMP,ASC")
                .size(1000)
                .build();

        TraceLogResponse traceLogResponse = logService.getLogsByTraceId(searchRequest);
        List<LogResponse> logs = traceLogResponse.getLogs();

        if (logs.isEmpty()) {
            log.warn("{} Trace ID에 해당하는 로그를 찾을 수 없음: traceId={}", LOG_PREFIX, traceId);
            throw new BusinessException(LogErrorCode.LOG_NOT_FOUND);
        }

        // request: 가장 빠른 로그 (첫 번째)
        LogResponse request = logs.getFirst();

        // response: 가장 느린 로그 (마지막)
        LogResponse response = logs.getLast();

        // duration 계산 (밀리초)
        Long duration = Duration.between(
                request.getTimestamp(),
                response.getTimestamp()
        ).toMillis();

        // status 판단: ERROR 로그가 하나라도 있으면 ERROR, 없으면 SUCCESS
        String status = logs.stream()
                .anyMatch(log -> "ERROR".equals(log.getLogLevel()))
                ? "ERROR"
                : "SUCCESS";

        log.info("{} Trace 로그 조회 완료: traceId={}, 로그 수={}, duration={}ms, status={}",
                LOG_PREFIX, traceId, logs.size(), duration, status);

        return new TraceLogsResponse(
                traceId,
                projectUuid,
                request,
                response,
                duration,
                status,
                logs
        );
    }
}
