package S13P31A306.loglens.domain.alert.service;

import S13P31A306.loglens.domain.alert.dto.AlertHistoryResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 알림 이력 서비스 인터페이스
 */
public interface AlertHistoryService {

    /**
     * 알림 이력 조회
     *
     * @param projectUuid 프로젝트 UUID
     * @param userId      사용자 ID
     * @param resolvedYN  읽음 여부 필터 (null이면 전체 조회)
     * @return 알림 이력 목록
     */
    List<AlertHistoryResponse> getAlertHistories(String projectUuid, Integer userId, String resolvedYN);

    /**
     * 알림 읽음 처리
     *
     * @param alertId 알림 ID
     * @param userId  사용자 ID
     * @return 읽음 처리된 알림
     */
    AlertHistoryResponse markAsRead(Integer alertId, Integer userId);

    /**
     * 읽지 않은 알림 개수 조회
     *
     * @param projectUuid 프로젝트 UUID
     * @param userId      사용자 ID
     * @return 읽지 않은 알림 개수
     */
    long getUnreadCount(String projectUuid, Integer userId);

    /**
     * 실시간 알림 스트리밍
     * SSE를 통해 새로운 알림을 실시간으로 전송합니다.
     *
     * @param projectUuid 프로젝트 UUID
     * @param userId      사용자 ID
     * @return SseEmitter
     */
    SseEmitter streamAlerts(String projectUuid, Integer userId);
}
