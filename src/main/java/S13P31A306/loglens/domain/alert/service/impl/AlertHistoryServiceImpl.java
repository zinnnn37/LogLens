package S13P31A306.loglens.domain.alert.service.impl;

import S13P31A306.loglens.domain.alert.dto.AlertHistoryResponse;
import S13P31A306.loglens.domain.alert.entity.AlertHistory;
import S13P31A306.loglens.domain.alert.exception.AlertErrorCode;
import S13P31A306.loglens.domain.alert.repository.AlertHistoryRepository;
import S13P31A306.loglens.domain.alert.service.AlertHistoryService;
import S13P31A306.loglens.domain.project.repository.ProjectMemberRepository;
import S13P31A306.loglens.domain.project.repository.ProjectRepository;
import S13P31A306.loglens.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static S13P31A306.loglens.domain.project.constants.ProjectErrorCode.PROJECT_NOT_FOUND;
import static S13P31A306.loglens.global.constants.GlobalErrorCode.FORBIDDEN;

/**
 * 알림 이력 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlertHistoryServiceImpl implements AlertHistoryService {

    private static final String LOG_PREFIX = "[AlertHistoryService]";
    private static final int POLLING_INTERVAL = 5; // 5초 간격으로 새 알림 확인

    private final AlertHistoryRepository alertHistoryRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    @Qualifier("sseScheduler")
    private final ScheduledExecutorService sseScheduler;

    @Qualifier("sseTimeout")
    private final long sseTimeout;

    @Override
    public List<AlertHistoryResponse> getAlertHistories(Integer projectId, Integer userId, String resolvedYN) {
        log.info("{} 알림 이력 조회 시작: projectId={}, resolvedYN={}", LOG_PREFIX, projectId, resolvedYN);

        // 1. 프로젝트 존재 여부 확인
        projectRepository.findById(projectId)
                .orElseThrow(() -> {
                    log.warn("{} 프로젝트 없음: projectId={}", LOG_PREFIX, projectId);
                    return new BusinessException(PROJECT_NOT_FOUND);
                });

        // 2. 사용자의 프로젝트 접근 권한 확인
        validateProjectAccess(projectId, userId);

        // 3. 알림 이력 조회
        List<AlertHistory> histories;
        if (resolvedYN != null && !resolvedYN.isBlank()) {
            histories = alertHistoryRepository
                    .findByProjectIdAndResolvedYNOrderByAlertTimeDesc(projectId, resolvedYN);
        } else {
            histories = alertHistoryRepository
                    .findByProjectIdOrderByAlertTimeDesc(projectId);
        }

        log.info("{} 알림 이력 조회 완료: count={}", LOG_PREFIX, histories.size());

        return histories.stream()
                .map(AlertHistoryResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AlertHistoryResponse markAsRead(Integer alertId, Integer userId) {
        log.info("{} 알림 읽음 처리 시작: alertId={}", LOG_PREFIX, alertId);

        // 1. 알림 존재 여부 확인
        AlertHistory alertHistory = alertHistoryRepository.findById(alertId)
                .orElseThrow(() -> {
                    log.warn("{} 알림 이력 없음: alertId={}", LOG_PREFIX, alertId);
                    return new BusinessException(AlertErrorCode.ALERT_HISTORY_NOT_FOUND);
                });

        // 2. 사용자의 프로젝트 접근 권한 확인
        validateProjectAccess(alertHistory.getProjectId(), userId);

        // 3. 읽음 처리 (멱등성 보장 - 이미 읽은 경우에도 정상 처리)
        if (!"Y".equals(alertHistory.getResolvedYN())) {
            alertHistory.markAsRead();
            log.info("{} 알림이 읽음 처리되었습니다: alertId={}", LOG_PREFIX, alertId);
        } else {
            log.info("{} 이미 읽은 알림입니다: alertId={}", LOG_PREFIX, alertId);
        }

        return AlertHistoryResponse.from(alertHistory);
    }

    @Override
    public long getUnreadCount(Integer projectId, Integer userId) {
        log.info("{} 읽지 않은 알림 개수 조회: projectId={}", LOG_PREFIX, projectId);

        // 1. 프로젝트 존재 여부 확인
        projectRepository.findById(projectId)
                .orElseThrow(() -> {
                    log.warn("{} 프로젝트 없음: projectId={}", LOG_PREFIX, projectId);
                    return new BusinessException(PROJECT_NOT_FOUND);
                });

        // 2. 사용자의 프로젝트 접근 권한 확인
        validateProjectAccess(projectId, userId);

        // 3. 읽지 않은 알림 개수 조회
        long count = alertHistoryRepository.countByProjectIdAndResolvedYN(projectId, "N");

        log.info("{} 읽지 않은 알림 개수: {}", LOG_PREFIX, count);

        return count;
    }

    @Override
    public SseEmitter streamAlerts(Integer projectId, Integer userId) {
        log.info("{} 실시간 알림 스트리밍 시작: projectId={}, userId={}", LOG_PREFIX, projectId, userId);

        // 1. 프로젝트 존재 여부 확인
        projectRepository.findById(projectId)
                .orElseThrow(() -> {
                    log.warn("{} 프로젝트 없음: projectId={}", LOG_PREFIX, projectId);
                    return new BusinessException(PROJECT_NOT_FOUND);
                });

        // 2. 사용자의 프로젝트 접근 권한 확인
        validateProjectAccess(projectId, userId);

        // 3. SseEmitter 생성
        SseEmitter emitter = new SseEmitter(sseTimeout);

        // 마지막으로 전송한 알림의 timestamp를 추적하기 위한 변수
        LocalDateTime[] lastTimestamp = {LocalDateTime.now()};

        // 스케줄러를 저장하여 연결 종료 시 취소할 수 있도록 함
        ScheduledFuture<?>[] scheduledFutureHolder = new ScheduledFuture<?>[1];

        scheduledFutureHolder[0] = sseScheduler.scheduleAtFixedRate(() -> {
            try {
                // 마지막 시간 이후의 새로운 알림 조회
                List<AlertHistory> newAlerts = alertHistoryRepository
                        .findByProjectIdAndAlertTimeAfterOrderByAlertTimeDesc(projectId, lastTimestamp[0]);

                if (!newAlerts.isEmpty()) {
                    List<AlertHistoryResponse> responses = newAlerts.stream()
                            .map(AlertHistoryResponse::from)
                            .collect(Collectors.toList());

                    // SSE로 데이터 전송
                    emitter.send(SseEmitter.event()
                            .name("alert-update")
                            .data(responses));

                    // 마지막 timestamp 업데이트
                    lastTimestamp[0] = newAlerts.get(0).getAlertTime();

                    log.debug("{} 새로운 알림 전송: 개수={}", LOG_PREFIX, responses.size());
                } else {
                    // 새 알림이 없으면 heartbeat 전송
                    emitter.send(SseEmitter.event()
                            .name("heartbeat")
                            .data("No new alerts"));
                    log.debug("{} Heartbeat 전송", LOG_PREFIX);
                }
            } catch (Exception e) {
                // IOException은 클라이언트 연결 끊김으로 정상적인 상황
                if (e instanceof java.io.IOException) {
                    log.debug("{} 클라이언트 연결 종료됨", LOG_PREFIX);
                    if (Objects.nonNull(scheduledFutureHolder[0])) {
                        scheduledFutureHolder[0].cancel(true);
                    }
                    emitter.complete();
                } else {
                    log.error("{} 알림 스트리밍 중 오류 발생", LOG_PREFIX, e);
                    emitter.completeWithError(e);
                }
            }
        }, 0, POLLING_INTERVAL, TimeUnit.SECONDS);

        // 연결 종료 시 스케줄러 정리
        emitter.onCompletion(() -> {
            if (Objects.nonNull(scheduledFutureHolder[0])) {
                scheduledFutureHolder[0].cancel(true);
            }
            log.info("{} SSE 연결 정상 종료: projectId={}", LOG_PREFIX, projectId);
        });

        emitter.onTimeout(() -> {
            if (Objects.nonNull(scheduledFutureHolder[0])) {
                scheduledFutureHolder[0].cancel(true);
            }
            log.info("{} SSE 연결 타임아웃: projectId={}", LOG_PREFIX, projectId);
            emitter.complete();
        });

        emitter.onError((e) -> {
            if (Objects.nonNull(scheduledFutureHolder[0])) {
                scheduledFutureHolder[0].cancel(true);
            }
            // IOException은 클라이언트가 연결을 끊은 정상적인 상황
            if (e instanceof java.io.IOException) {
                log.debug("{} SSE 클라이언트 연결 종료: projectId={}", LOG_PREFIX, projectId);
            } else {
                log.error("{} SSE 연결 오류: projectId={}", LOG_PREFIX, projectId, e);
            }
        });

        return emitter;
    }

    /**
     * 프로젝트 접근 권한 검증
     */
    private void validateProjectAccess(Integer projectId, Integer userId) {
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            log.warn("{} 접근 권한 없음: projectId={}, userId={}", LOG_PREFIX, projectId, userId);
            throw new BusinessException(FORBIDDEN);
        }
    }
}
