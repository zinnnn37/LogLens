package S13P31A306.loglens.domain.alert.dto;

import S13P31A306.loglens.domain.alert.entity.AlertHistory;

import java.time.LocalDateTime;

/**
 * 알림 이력 응답 DTO
 */
public record AlertHistoryResponse(
        Integer id,
        String alertMessage,
        LocalDateTime alertTime,
        String resolvedYN,
        String logReference,
        String projectUuid
) {
    /**
     * AlertHistory 엔티티로부터 생성
     */
    public static AlertHistoryResponse from(AlertHistory alertHistory, String projectUuid) {
        return new AlertHistoryResponse(
                alertHistory.getId(),
                alertHistory.getAlertMessage(),
                alertHistory.getAlertTime(),
                alertHistory.getResolvedYN(),
                alertHistory.getLogReference(),
                projectUuid
        );
    }
}
