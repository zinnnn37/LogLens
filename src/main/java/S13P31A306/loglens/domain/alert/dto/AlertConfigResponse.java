package S13P31A306.loglens.domain.alert.dto;

import S13P31A306.loglens.domain.alert.entity.AlertConfig;
import S13P31A306.loglens.domain.alert.entity.AlertType;

import java.time.LocalDateTime;

/**
 * 알림 설정 응답 DTO
 */
public record AlertConfigResponse(
        Integer id,
        AlertType alertType,
        Integer thresholdValue,
        String activeYN,
        String projectUuid,
        String projectName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * AlertConfig 엔티티로부터 생성
     */
    public static AlertConfigResponse from(AlertConfig alertConfig, String projectName, String projectUuid) {
        return new AlertConfigResponse(
                alertConfig.getId(),
                alertConfig.getAlertType(),
                alertConfig.getThresholdValue(),
                alertConfig.getActiveYN(),
                projectUuid,
                projectName,
                null, // createdAt은 BaseTimeEntity가 아니므로 null
                null  // updatedAt은 BaseTimeEntity가 아니므로 null
        );
    }
}
