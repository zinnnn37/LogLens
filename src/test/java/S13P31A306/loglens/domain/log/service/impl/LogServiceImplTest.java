package S13P31A306.loglens.domain.log.service.impl;

import static S13P31A306.loglens.domain.log.entity.LogLevel.*;
import static S13P31A306.loglens.domain.log.entity.SourceType.BE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import S13P31A306.loglens.domain.log.dto.internal.LogSearchResult;
import S13P31A306.loglens.domain.log.dto.internal.TraceLogSearchResult;
import S13P31A306.loglens.domain.log.dto.request.LogSearchRequest;
import S13P31A306.loglens.domain.log.dto.response.LogPageResponse;
import S13P31A306.loglens.domain.log.dto.response.LogResponse;
import S13P31A306.loglens.domain.log.dto.response.LogSummaryResponse;
import S13P31A306.loglens.domain.log.dto.response.TraceLogResponse;
import S13P31A306.loglens.domain.log.entity.Log;
import S13P31A306.loglens.domain.log.entity.LogLevel;
import S13P31A306.loglens.domain.log.mapper.LogMapper;
import S13P31A306.loglens.domain.log.repository.LogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LogServiceImplTest {

    @InjectMocks
    private LogServiceImpl logService;

    @Mock
    private LogRepository logRepository;

    @Mock
    private LogMapper logMapper;

    @Mock
    private ObjectMapper objectMapper;

    private LogSearchRequest baseRequest;

    @BeforeEach
    void setup() {
        baseRequest = new LogSearchRequest();
        baseRequest.setProjectUuid("550e8400-e29b-41d4-a716-446655440000");
        baseRequest.setSize(50);
        baseRequest.setSort("TIMESTAMP,DESC");
    }

    @Test
    void getLogs_정상_요청이면_로그_목록과_페이지네이션_정보를_반환한다() throws Exception {
        // given
        Log log1 = createLog("log-1", "Test message 1", ERROR);
        Log log2 = createLog("log-2", "Test message 2", WARN);

        List<Log> logs = Arrays.asList(log1, log2);
        Object[] sortValues = new Object[]{1705312800000L, "log-2"};
        LogSearchResult searchResult = new LogSearchResult(logs, true, sortValues);

        given(logRepository.findWithCursor(any(), any())).willReturn(searchResult);
        given(logMapper.toLogResponse(log1)).willReturn(createLogResponse("log-1", "Test message 1", ERROR));
        given(logMapper.toLogResponse(log2)).willReturn(createLogResponse("log-2", "Test message 2", WARN));
        given(objectMapper.writeValueAsBytes(any())).willReturn("[1705312800000,\"log-2\"]".getBytes());

        // when
        LogPageResponse result = logService.getLogs(baseRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getLogs()).hasSize(2);
        assertThat(result.getPagination().isHasNext()).isTrue();
        assertThat(result.getPagination().getNextCursor()).isNotNull();

        verify(logRepository).findWithCursor(any(), any());
        verify(logMapper, times(2)).toLogResponse(any(Log.class));
    }

    @Test
    void getLogs_다음_페이지가_없으면_nextCursor가_null이다() {
        // given
        Log log1 = createLog("log-1", "Test message 1", ERROR);
        List<Log> logs = Collections.singletonList(log1);
        LogSearchResult searchResult = new LogSearchResult(logs, false, null);

        given(logRepository.findWithCursor(any(), any())).willReturn(searchResult);
        given(logMapper.toLogResponse(log1)).willReturn(createLogResponse("log-1", "Test message 1", ERROR));

        // when
        LogPageResponse result = logService.getLogs(baseRequest);

        // then
        assertThat(result.getLogs()).hasSize(1);
        assertThat(result.getPagination().isHasNext()).isFalse();
        assertThat(result.getPagination().getNextCursor()).isNull();

        verify(logRepository).findWithCursor(any(), any());
        verify(logMapper).toLogResponse(log1);
    }

    @Test
    void getLogs_로그가_없으면_빈_리스트를_반환한다() {
        // given
        LogSearchResult searchResult = new LogSearchResult(Collections.emptyList(), false, null);
        given(logRepository.findWithCursor(any(), any())).willReturn(searchResult);

        // when
        LogPageResponse result = logService.getLogs(baseRequest);

        // then
        assertThat(result.getLogs()).isEmpty();
        assertThat(result.getPagination().getSize()).isEqualTo(0);

        verify(logRepository).findWithCursor(any(), any());
    }

    @Test
    void getLogsByTraceId_정상_요청이면_Trace_ID에_대한_로그_목록과_요약_정보를_반환한다() {
        // given
        Log log1 = createLog("log-1", "Test message 1", ERROR);
        Log log2 = createLog("log-2", "Test message 2", WARN);
        Log log3 = createLog("log-3", "Test message 3", INFO);

        List<Log> logs = Arrays.asList(log1, log2, log3);

        LogSummaryResponse summary = LogSummaryResponse.builder()
                .totalLogs(3).errorCount(1L).warnCount(1L).infoCount(1L)
                .build();

        TraceLogSearchResult searchResult = new TraceLogSearchResult(logs, summary);
        given(logRepository.findByTraceId(any(), any())).willReturn(searchResult);
        given(logMapper.toLogResponse(any(Log.class))).willAnswer(inv -> {
            Log log = inv.getArgument(0);
            return createLogResponse(log.getId(), log.getMessage(), log.getLogLevel());
        });

        baseRequest.setTraceId("trace-abc-123");

        // when
        TraceLogResponse result = logService.getLogsByTraceId(baseRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getLogs()).hasSize(3);
        assertThat(result.getSummary().getErrorCount()).isEqualTo(1);
        assertThat(result.getSummary().getWarnCount()).isEqualTo(1);
        assertThat(result.getSummary().getInfoCount()).isEqualTo(1);

        verify(logRepository).findByTraceId(any(), any());
    }

    @Test
    void getLogsByTraceId_로그가_없으면_빈_리스트를_반환한다() {
        // given
        LogSummaryResponse summary = LogSummaryResponse.builder()
                .totalLogs(0).errorCount(0L).warnCount(0L).infoCount(0L)
                .build();

        TraceLogSearchResult searchResult = new TraceLogSearchResult(Collections.emptyList(), summary);
        given(logRepository.findByTraceId(any(), any())).willReturn(searchResult);

        baseRequest.setTraceId("trace-nonexistent");

        // when
        TraceLogResponse result = logService.getLogsByTraceId(baseRequest);

        // then
        assertThat(result.getLogs()).isEmpty();
        assertThat(result.getSummary().getTotalLogs()).isZero();

        verify(logRepository).findByTraceId(any(), any());
    }

    private Log createLog(String id, String message, LogLevel logLevel) {
        Log log = new Log();
        log.setId(id);
        log.setLogId(1L);
        log.setProjectUuid("550e8400-e29b-41d4-a716-446655440000");
        log.setTimestamp(OffsetDateTime.of(2024, 1, 15, 12, 0, 0, 0, ZoneOffset.UTC));
        log.setServiceName("test-service");
        log.setSourceType(BE);
        log.setLogLevel(logLevel);
        log.setMessage(message);
        log.setTraceId("trace-abc-123");
        return log;
    }

    private LogResponse createLogResponse(String logId, String message, LogLevel logLevel) {
        return LogResponse.builder()
                .logId(logId)
                .timestamp(LocalDateTime.of(2024, 1, 15, 12, 0, 0))
                .sourceType(BE)
                .logLevel(logLevel)
                .message(message)
                .traceId("trace-abc-123")
                .build();
    }
}
