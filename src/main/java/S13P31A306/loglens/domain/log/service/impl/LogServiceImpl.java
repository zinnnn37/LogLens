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
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    private static final String LOG_PREFIX = "[LogService]";
    private static final long POLLING_INTERVAL = 5; // 5초
    private static final int MAX_IDLE_POLLS = 3; // 빈 응답 3회 후 폴링 주기 감소 시작

    private final LogRepository logRepository;
    private final LogMapper logMapper;
    private final ObjectMapper objectMapper; // For cursor encoding/decoding
    private final AiServiceClient aiServiceClient;
    private final ScheduledExecutorService sseScheduler;
    private final long sseTimeout;

    @Override
    public LogPageResponse getLogs(LogSearchRequest request) {
        log.info("{} 로그 목록 조회 시작: projectUuid={}", LOG_PREFIX, request.getProjectUuid());

        // 기본값 설정
        applyDefaults(request);

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

        // 기본값 설정
        applyDefaults(request);

        TraceLogSearchResult result = searchLogsByTraceId(request.getProjectUuid(), request);
        List<LogResponse> logResponses = mapToLogResponses(result);

        TraceLogResponse response = TraceLogResponse.builder()
                .traceId(request.getTraceId())
                .summary(result.summary())
                .logs(logResponses)
                .build();

        log.info("{} Trace ID로 로그 조회 완료: 로그 수={}, 전체 로그 수={}",
                LOG_PREFIX, logResponses.size(),
                Objects.nonNull(result.summary()) ? result.summary().getTotalLogs() : 0);

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
        if (Objects.isNull(result) || Objects.isNull(result.logs())) {
            return new TraceLogSearchResult(Collections.emptyList(), null);
        }
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
        Log logEntity = logRepository.findByLogId(logId, projectUuid)
                .orElseThrow(() -> {
                    log.warn("{} 로그를 찾을 수 없음: logId={}, projectUuid={}",
                            LOG_PREFIX, logId, projectUuid);
                    return new BusinessException(LogErrorCode.LOG_NOT_FOUND);
                });

        // 2. AI 분석 결과 확인 및 처리
        AiAnalysisDto analysis = null;
        Boolean fromCache = null;
        Long similarLogId = null;
        Double similarityScore = null;

        // 2-1. OpenSearch에 저장된 aiAnalysis 확인
        Map<String, Object> aiAnalysisMap = logEntity.getAiAnalysis();
        if (aiAnalysisMap != null && !aiAnalysisMap.isEmpty()) {
            log.info("{} OpenSearch에 저장된 AI 분석 결과 사용: logId={}", LOG_PREFIX, logId);
            try {
                analysis = objectMapper.convertValue(aiAnalysisMap, AiAnalysisDto.class);
                fromCache = true;
            } catch (Exception e) {
                log.error("{} AI 분석 결과 변환 실패: logId={}", LOG_PREFIX, logId, e);
            }
        }

        // 2-2. AI 분석이 없으면 AI 서비스 호출
        if (Objects.isNull(analysis)) {
            log.info("{} AI 서비스 호출하여 분석 수행: logId={}", LOG_PREFIX, logId);
            try {
                AiAnalysisResponse aiResponse = aiServiceClient.analyzeLog(logId, projectUuid);
                if (aiResponse != null && aiResponse.getAnalysis() != null) {
                    analysis = aiResponse.getAnalysis();
                    fromCache = aiResponse.getFromCache();
                    similarLogId = aiResponse.getSimilarLogId();
                    similarityScore = aiResponse.getSimilarityScore();
                    log.info("{} AI 분석 완료: logId={}, fromCache={}", LOG_PREFIX, logId, fromCache);
                } else {
                    log.warn("{} AI 분석 결과가 null: logId={}", LOG_PREFIX, logId);
                }
            } catch (Exception e) {
                log.error("{} AI 서비스 호출 실패, 분석 없이 로그만 반환: logId={}", LOG_PREFIX, logId, e);
            }
        }

        // 3. LogDetailResponse 생성 (AI 분석 결과만 포함)
        LogDetailResponse response = LogDetailResponse.builder()
                .analysis(analysis)
                .fromCache(fromCache)
                .similarLogId(similarLogId)
                .similarityScore(similarityScore)
                .build();

        log.info("{} 로그 상세 조회 완료: logId={}, hasAnalysis={}", LOG_PREFIX, logId, analysis != null);
        return response;
    }

    @Override
    public SseEmitter streamLogs(LogSearchRequest request) {
        log.info("{} 실시간 로그 스트리밍 시작: projectUuid={}", LOG_PREFIX, request.getProjectUuid());

        SseEmitter emitter = new SseEmitter(sseTimeout);

        // AtomicReference를 사용하여 thread-safe하게 변수 관리
        AtomicReference<LocalDateTime> lastTimestamp = new AtomicReference<>(request.getStartTime());
        AtomicReference<ScheduledFuture<?>> scheduledFutureHolder = new AtomicReference<>();
        AtomicInteger emptyPollCount = new AtomicInteger(0); // 빈 응답 카운터 (OpenSearch 부하 관리용)

        scheduledFutureHolder.set(sseScheduler.scheduleAtFixedRate(() -> {
            try {
                // OpenSearch 부하 관리: 빈 응답이 일정 횟수 이상이면 스킵
                int emptyCount = emptyPollCount.get();
                if (emptyCount >= MAX_IDLE_POLLS && emptyCount % 2 != 0) {
                    // 3회 이상 빈 응답 후, 홀수 번째는 스킵하여 폴링 주기를 실질적으로 2배로 늘림
                    emptyPollCount.incrementAndGet();
                    emitter.send(SseEmitter.event()
                            .name("heartbeat")
                            .data("No new logs"));
                    log.debug("{} OpenSearch 부하 감소를 위한 폴링 스킵 (emptyCount: {})", LOG_PREFIX, emptyCount);
                    return;
                }

                // 검색 조건 복사 및 시작 시간 업데이트
                LogSearchRequest pollingRequest = createPollingRequest(request, lastTimestamp.get());

                // 새로운 로그 조회
                LogSearchResult result = searchLogs(pollingRequest.getProjectUuid(), pollingRequest);

                if (!result.logs().isEmpty()) {
                    List<LogResponse> logResponses = mapToLogResponses(result);

                    // SSE로 데이터 전송
                    emitter.send(SseEmitter.event()
                            .name("log-update")
                            .data(logResponses));

                    // 마지막 timestamp 업데이트 (중복 방지를 위해 1 나노초 추가)
                    if (!logResponses.isEmpty()) {
                        LocalDateTime lastLogTime = logResponses.getLast().getTimestamp();
                        lastTimestamp.set(lastLogTime.plusNanos(1));
                    }

                    // 빈 응답 카운터 리셋
                    emptyPollCount.set(0);

                    log.debug("{} 새로운 로그 전송: 개수={}", LOG_PREFIX, logResponses.size());
                } else {
                    // 빈 응답 카운터 증가
                    emptyPollCount.incrementAndGet();

                    // heartbeat 전송 (연결 유지)
                    emitter.send(SseEmitter.event()
                            .name("heartbeat")
                            .data("No new logs"));
                    log.debug("{} Heartbeat 전송 (빈 응답 카운트: {})", LOG_PREFIX, emptyPollCount.get());
                }
            } catch (Exception e) {
                // 모든 예외에서 스케줄러 정리 (메모리 누수 방지)
                cleanupScheduler(scheduledFutureHolder.get());

                if (e instanceof java.io.IOException) {
                    log.debug("{} 클라이언트 연결 종료됨", LOG_PREFIX);
                    emitter.complete();
                } else {
                    log.error("{} 로그 스트리밍 중 오류 발생", LOG_PREFIX, e);
                    emitter.completeWithError(e);
                }
            }
        }, 0, POLLING_INTERVAL, TimeUnit.SECONDS));

        // 연결 종료 시 스케줄러 정리
        emitter.onCompletion(() -> {
            cleanupScheduler(scheduledFutureHolder.get());
            log.info("{} SSE 연결 정상 종료: projectUuid={}", LOG_PREFIX, request.getProjectUuid());
        });

        emitter.onTimeout(() -> {
            cleanupScheduler(scheduledFutureHolder.get());
            log.info("{} SSE 연결 타임아웃: projectUuid={}", LOG_PREFIX, request.getProjectUuid());
            emitter.complete();
        });

        emitter.onError((e) -> {
            cleanupScheduler(scheduledFutureHolder.get());
            // IOException은 클라이언트가 연결을 끊은 정상적인 상황
            if (e instanceof java.io.IOException) {
                log.debug("{} SSE 클라이언트 연결 종료: projectUuid={}", LOG_PREFIX, request.getProjectUuid());
            } else {
                log.error("{} SSE 연결 오류: projectUuid={}", LOG_PREFIX, request.getProjectUuid(), e);
            }
        });

        return emitter;
    }

    /**
     * 스케줄러 정리 헬퍼 메서드 (중복 코드 제거)
     */
    private void cleanupScheduler(ScheduledFuture<?> future) {
        if (Objects.nonNull(future) && !future.isCancelled()) {
            future.cancel(true);
        }
    }

    /**
     * LogSearchRequest에 기본값 적용
     *
     * @param request 로그 검색 요청
     */
    private void applyDefaults(LogSearchRequest request) {
        if (Objects.isNull(request.getSize())) {
            request.setSize(100);
        }
        if (Objects.isNull(request.getSort()) || request.getSort().isBlank()) {
            request.setSort("TIMESTAMP,DESC");
        }
    }

    /**
     * 폴링용 검색 요청 생성 마지막 조회 시간 이후의 로그만 조회하도록 설정
     */
    private LogSearchRequest createPollingRequest(LogSearchRequest original, LocalDateTime lastTimestamp) {
        LogSearchRequest pollingRequest = LogSearchRequest.builder()
                .projectUuid(original.getProjectUuid())
                .size(original.getSize())
                .logLevel(original.getLogLevel())
                .sourceType(original.getSourceType())
                .keyword(original.getKeyword())
                .sort(original.getSort())
                .build();

        // 마지막 조회 시간 이후의 로그만 조회
        if (Objects.nonNull(lastTimestamp)) {
            pollingRequest.setStartTime(lastTimestamp);
        } else if (Objects.nonNull(original.getStartTime())) {
            pollingRequest.setStartTime(original.getStartTime());
        }

        pollingRequest.setEndTime(original.getEndTime());

        return pollingRequest;
    }
}
