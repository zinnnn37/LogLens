package S13P31A306.loglens.domain.log.service.impl;

import static S13P31A306.loglens.domain.log.entity.LogLevel.ERROR;
import static S13P31A306.loglens.domain.log.entity.LogLevel.INFO;
import static S13P31A306.loglens.domain.log.entity.LogLevel.WARN;
import static S13P31A306.loglens.domain.log.entity.SourceType.BE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import S13P31A306.loglens.domain.log.constants.LogErrorCode;
import S13P31A306.loglens.domain.log.dto.ai.AiAnalysisDto;
import S13P31A306.loglens.domain.log.dto.ai.AiAnalysisResponse;
import S13P31A306.loglens.domain.log.dto.internal.LogSearchResult;
import S13P31A306.loglens.domain.log.dto.internal.TraceLogSearchResult;
import S13P31A306.loglens.domain.log.dto.request.LogSearchRequest;
import S13P31A306.loglens.domain.log.dto.response.LogDetailResponse;
import S13P31A306.loglens.domain.log.dto.response.LogPageResponse;
import S13P31A306.loglens.domain.log.dto.response.LogResponse;
import S13P31A306.loglens.domain.log.dto.response.LogSummaryResponse;
import S13P31A306.loglens.domain.log.dto.response.TraceLogResponse;
import S13P31A306.loglens.domain.log.entity.Log;
import S13P31A306.loglens.domain.log.entity.LogLevel;
import S13P31A306.loglens.domain.log.mapper.LogMapper;
import S13P31A306.loglens.domain.log.repository.LogRepository;
import S13P31A306.loglens.global.client.AiServiceClient;
import S13P31A306.loglens.global.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class LogServiceImplTest {

    private LogServiceImpl logService;

    @Mock
    private LogRepository logRepository;

    @Mock
    private LogMapper logMapper;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private AiServiceClient aiServiceClient;

    @Mock
    private ScheduledExecutorService sseScheduler;

    @Mock
    private ScheduledFuture<?> scheduledFuture;

    private LogSearchRequest baseRequest;
    private static final long SSE_TIMEOUT = 300000L; // 5분

    @BeforeEach
    void setup() {
        logService = new LogServiceImpl(
                logRepository,
                logMapper,
                objectMapper,
                aiServiceClient,
                sseScheduler,
                SSE_TIMEOUT
        );

        baseRequest = LogSearchRequest.builder()
                .projectUuid("550e8400-e29b-41d4-a716-446655440000")
                .size(50)
                .sort("TIMESTAMP,DESC")
                .build();
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
                .logId(Long.parseLong(logId.replace("log-", "")))
                .timestamp(LocalDateTime.of(2024, 1, 15, 12, 0, 0))
                .sourceType(BE)
                .logLevel(logLevel)
                .message(message)
                .traceId("trace-abc-123")
                .build();
    }

    @Nested
    @DisplayName("로그 상세 조회 테스트")
    class GetLogDetailTest {

        @Test
        @DisplayName("OpenSearch에_AI_분석_결과가_있으면_해당_결과를_반환한다")
        void OpenSearch에_AI_분석_결과가_있으면_해당_결과를_반환한다() {
            // given
            Long logId = 1234567890L;
            String projectUuid = "550e8400-e29b-41d4-a716-446655440000";

            Log log = createLog("log-1", "NullPointerException occurred", ERROR);
            log.setLogId(logId);

            // OpenSearch에 저장된 AI 분석 결과
            Map<String, Object> aiAnalysisMap = new HashMap<>();
            aiAnalysisMap.put("summary", "NULL 참조 에러 발생");
            aiAnalysisMap.put("error_cause", "NULL 체크 없이 메서드 호출");
            aiAnalysisMap.put("solution", "NULL 체크 추가");
            aiAnalysisMap.put("tags", List.of("NULL_POINTER"));
            aiAnalysisMap.put("analysis_type", "TRACE_BASED");
            log.setAiAnalysis(aiAnalysisMap);

            AiAnalysisDto analysisDto = AiAnalysisDto.builder()
                    .summary("NULL 참조 에러 발생")
                    .errorCause("NULL 체크 없이 메서드 호출")
                    .solution("NULL 체크 추가")
                    .tags(List.of("NULL_POINTER"))
                    .analysisType("TRACE_BASED")
                    .build();

            given(logRepository.findByLogId(logId, projectUuid)).willReturn(Optional.of(log));
            given(objectMapper.convertValue(aiAnalysisMap, AiAnalysisDto.class)).willReturn(analysisDto);

            // when
            LogDetailResponse result = logService.getLogDetail(logId, projectUuid);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getLogId()).isEqualTo(logId);
            assertThat(result.getMessage()).isEqualTo("NullPointerException occurred");
            assertThat(result.getAnalysis()).isNotNull();
            assertThat(result.getAnalysis().getSummary()).isEqualTo("NULL 참조 에러 발생");
            assertThat(result.getFromCache()).isTrue();

            verify(logRepository).findByLogId(logId, projectUuid);
            verify(aiServiceClient, never()).analyzeLog(any(), anyString());
        }

        @Test
        @DisplayName("AI_분석_결과가_없으면_AI_서비스를_호출한다")
        void AI_분석_결과가_없으면_AI_서비스를_호출한다() {
            // given
            Long logId = 1234567890L;
            String projectUuid = "550e8400-e29b-41d4-a716-446655440000";

            Log log = createLog("log-1", "Database connection timeout", ERROR);
            log.setLogId(logId);
            log.setAiAnalysis(null); // AI 분석 결과 없음

            AiAnalysisDto analysisDto = AiAnalysisDto.builder()
                    .summary("데이터베이스 연결 타임아웃")
                    .errorCause("커넥션 풀 고갈")
                    .solution("커넥션 풀 크기 증가")
                    .tags(List.of("DATABASE", "TIMEOUT"))
                    .analysisType("SINGLE")
                    .build();

            AiAnalysisResponse aiResponse = AiAnalysisResponse.builder()
                    .logId(logId)
                    .analysis(analysisDto)
                    .fromCache(false)
                    .build();

            given(logRepository.findByLogId(logId, projectUuid)).willReturn(Optional.of(log));
            given(aiServiceClient.analyzeLog(logId, projectUuid)).willReturn(aiResponse);

            // when
            LogDetailResponse result = logService.getLogDetail(logId, projectUuid);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getLogId()).isEqualTo(logId);
            assertThat(result.getAnalysis()).isNotNull();
            assertThat(result.getAnalysis().getSummary()).isEqualTo("데이터베이스 연결 타임아웃");
            assertThat(result.getFromCache()).isFalse();

            verify(logRepository).findByLogId(logId, projectUuid);
            verify(aiServiceClient).analyzeLog(logId, projectUuid);
        }

        @Test
        @DisplayName("AI_서비스_호출_실패_시_로그_정보만_반환한다")
        void AI_서비스_호출_실패_시_로그_정보만_반환한다() {
            // given
            Long logId = 1234567890L;
            String projectUuid = "550e8400-e29b-41d4-a716-446655440000";

            Log log = createLog("log-1", "Test error", ERROR);
            log.setLogId(logId);
            log.setAiAnalysis(null);

            given(logRepository.findByLogId(logId, projectUuid)).willReturn(Optional.of(log));
            given(aiServiceClient.analyzeLog(logId, projectUuid)).willReturn(null); // AI 서비스 실패

            // when
            LogDetailResponse result = logService.getLogDetail(logId, projectUuid);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getLogId()).isEqualTo(logId);
            assertThat(result.getMessage()).isEqualTo("Test error");
            assertThat(result.getAnalysis()).isNull(); // 분석 결과 없음
            assertThat(result.getFromCache()).isNull();

            verify(logRepository).findByLogId(logId, projectUuid);
            verify(aiServiceClient).analyzeLog(logId, projectUuid);
        }

        @Test
        @DisplayName("로그가_존재하지_않으면_LOG_NOT_FOUND_예외를_발생시킨다")
        void 로그가_존재하지_않으면_LOG_NOT_FOUND_예외를_발생시킨다() {
            // given
            Long logId = 9999999999L;
            String projectUuid = "550e8400-e29b-41d4-a716-446655440000";

            given(logRepository.findByLogId(logId, projectUuid)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> logService.getLogDetail(logId, projectUuid))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", LogErrorCode.LOG_NOT_FOUND);

            verify(logRepository).findByLogId(logId, projectUuid);
            verify(aiServiceClient, never()).analyzeLog(any(), anyString());
        }

        @Test
        @DisplayName("AI_분석_응답에_유사도_정보가_포함되어_있으면_반환한다")
        void AI_분석_응답에_유사도_정보가_포함되어_있으면_반환한다() {
            // given
            Long logId = 1234567890L;
            String projectUuid = "550e8400-e29b-41d4-a716-446655440000";

            Log log = createLog("log-1", "Error message", ERROR);
            log.setLogId(logId);
            log.setAiAnalysis(null);

            AiAnalysisDto analysisDto = AiAnalysisDto.builder()
                    .summary("유사 에러")
                    .build();

            AiAnalysisResponse aiResponse = AiAnalysisResponse.builder()
                    .logId(logId)
                    .analysis(analysisDto)
                    .fromCache(true)
                    .similarLogId(1234567800L)
                    .similarityScore(0.92)
                    .build();

            given(logRepository.findByLogId(logId, projectUuid)).willReturn(Optional.of(log));
            given(aiServiceClient.analyzeLog(logId, projectUuid)).willReturn(aiResponse);

            // when
            LogDetailResponse result = logService.getLogDetail(logId, projectUuid);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getSimilarLogId()).isEqualTo(1234567800L);
            assertThat(result.getSimilarityScore()).isEqualTo(0.92);
            assertThat(result.getFromCache()).isTrue();

            verify(aiServiceClient).analyzeLog(logId, projectUuid);
        }
    }

    @Nested
    @DisplayName("SSE 로그 스트리밍 테스트")
    class StreamLogsTest {

        @Test
        void SSE_연결을_생성하고_스케줄러를_시작한다() {
            // given
            given(sseScheduler.scheduleAtFixedRate(any(Runnable.class), eq(0L), eq(5L), eq(TimeUnit.SECONDS)))
                    .willReturn((ScheduledFuture) scheduledFuture);

            // when
            SseEmitter result = logService.streamLogs(baseRequest);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTimeout()).isEqualTo(SSE_TIMEOUT); // 5분

            verify(sseScheduler).scheduleAtFixedRate(any(Runnable.class), eq(0L), eq(5L), eq(TimeUnit.SECONDS));
        }

        @Test
        void 스케줄러가_정상적으로_로그를_조회한다() {
            // given
            Log log1 = createLog("log-1", "Test message 1", ERROR);
            Log log2 = createLog("log-2", "Test message 2", WARN);
            List<Log> logs = Arrays.asList(log1, log2);
            LogSearchResult searchResult = new LogSearchResult(logs, false, null);

            given(logRepository.findWithCursor(any(), any())).willReturn(searchResult);
            given(logMapper.toLogResponse(any(Log.class))).willAnswer(inv -> {
                Log log = inv.getArgument(0);
                return createLogResponse(log.getId(), log.getMessage(), log.getLogLevel());
            });

            // 스케줄러가 즉시 실행되도록 설정
            doAnswer(invocation -> {
                Runnable task = invocation.getArgument(0);
                task.run(); // 즉시 실행
                return (ScheduledFuture) scheduledFuture;
            }).when(sseScheduler).scheduleAtFixedRate(any(Runnable.class), eq(0L), eq(5L), eq(TimeUnit.SECONDS));

            // when
            SseEmitter result = logService.streamLogs(baseRequest);

            // then
            assertThat(result).isNotNull();
            verify(logRepository).findWithCursor(any(), any());
            verify(logMapper, times(2)).toLogResponse(any(Log.class));
        }

        @Test
        void 로그가_없을_때_heartbeat를_전송한다() {
            // given
            LogSearchResult emptyResult = new LogSearchResult(Collections.emptyList(), false, null);
            given(logRepository.findWithCursor(any(), any())).willReturn(emptyResult);

            // 스케줄러가 즉시 실행되도록 설정
            doAnswer(invocation -> {
                Runnable task = invocation.getArgument(0);
                task.run(); // 즉시 실행
                return (ScheduledFuture) scheduledFuture;
            }).when(sseScheduler).scheduleAtFixedRate(any(Runnable.class), eq(0L), eq(5L), eq(TimeUnit.SECONDS));

            // when
            SseEmitter result = logService.streamLogs(baseRequest);

            // then
            assertThat(result).isNotNull();
            verify(logRepository).findWithCursor(any(), any());
        }

        @Test
        void startTime이_null이면_현재_시간_기준으로_조회한다() {
            // given
            baseRequest.setStartTime(null);
            LogSearchResult emptyResult = new LogSearchResult(Collections.emptyList(), false, null);
            given(logRepository.findWithCursor(any(), any())).willReturn(emptyResult);

            doAnswer(invocation -> {
                Runnable task = invocation.getArgument(0);
                task.run();
                return (ScheduledFuture) scheduledFuture;
            }).when(sseScheduler).scheduleAtFixedRate(any(Runnable.class), eq(0L), eq(5L), eq(TimeUnit.SECONDS));

            // when
            SseEmitter result = logService.streamLogs(baseRequest);

            // then
            assertThat(result).isNotNull();
            verify(logRepository).findWithCursor(any(), any());
        }

        @Test
        void 필터_조건이_적용된_로그를_조회한다() {
            // given
            baseRequest.setLogLevel(List.of("ERROR", "WARN"));
            baseRequest.setSourceType(List.of("BE"));
            baseRequest.setKeyword("exception");

            Log log1 = createLog("log-1", "Exception occurred", ERROR);
            List<Log> logs = Collections.singletonList(log1);
            LogSearchResult searchResult = new LogSearchResult(logs, false, null);

            given(logRepository.findWithCursor(any(), any())).willReturn(searchResult);
            given(logMapper.toLogResponse(any(Log.class))).willAnswer(inv -> {
                Log log = inv.getArgument(0);
                return createLogResponse(log.getId(), log.getMessage(), log.getLogLevel());
            });

            doAnswer(invocation -> {
                Runnable task = invocation.getArgument(0);
                task.run();
                return (ScheduledFuture) scheduledFuture;
            }).when(sseScheduler).scheduleAtFixedRate(any(Runnable.class), eq(0L), eq(5L), eq(TimeUnit.SECONDS));

            // when
            SseEmitter result = logService.streamLogs(baseRequest);

            // then
            assertThat(result).isNotNull();
            verify(logRepository).findWithCursor(any(), any());
            verify(logMapper).toLogResponse(any(Log.class));
        }

        @Test
        void 마지막_timestamp를_추적하여_새로운_로그만_조회한다() {
            // given
            LocalDateTime initialTime = LocalDateTime.of(2024, 1, 15, 12, 0, 0);
            baseRequest.setStartTime(initialTime);

            Log log1 = createLog("log-1", "Message 1", INFO);
            log1.setTimestamp(OffsetDateTime.of(2024, 1, 15, 12, 0, 10, 0, ZoneOffset.UTC));

            LogSearchResult firstResult = new LogSearchResult(List.of(log1), false, null);
            LogSearchResult secondResult = new LogSearchResult(Collections.emptyList(), false, null);

            given(logRepository.findWithCursor(any(), any()))
                    .willReturn(firstResult)
                    .willReturn(secondResult);

            given(logMapper.toLogResponse(any(Log.class))).willAnswer(inv -> {
                Log log = inv.getArgument(0);
                return LogResponse.builder()
                        .logId(1L)
                        .timestamp(LocalDateTime.of(2024, 1, 15, 12, 0, 10))
                        .message(log.getMessage())
                        .logLevel(log.getLogLevel())
                        .build();
            });

            doAnswer(invocation -> {
                Runnable task = invocation.getArgument(0);
                task.run(); // 첫 번째 실행
                return (ScheduledFuture) scheduledFuture;
            }).when(sseScheduler).scheduleAtFixedRate(any(Runnable.class), eq(0L), eq(5L), eq(TimeUnit.SECONDS));

            // when
            SseEmitter result = logService.streamLogs(baseRequest);

            // then
            assertThat(result).isNotNull();
            verify(logRepository).findWithCursor(any(), any());
        }
    }
}
