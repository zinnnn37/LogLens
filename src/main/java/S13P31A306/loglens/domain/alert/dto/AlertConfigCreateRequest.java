package S13P31A306.loglens.domain.alert.dto;

import S13P31A306.loglens.domain.alert.entity.AlertType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 알림 설정 생성 요청 DTO
 */
public record AlertConfigCreateRequest(
        @NotNull(message = "프로젝트 ID는 필수입니다.")
        Integer projectId,

        @NotNull(message = "알림 타입은 필수입니다.")
        AlertType alertType,

        @NotNull(message = "임계값은 필수입니다.")
        @Min(value = 1, message = "임계값은 1 이상이어야 합니다.")
        @Max(value = 255, message = "임계값은 255 이하여야 합니다.")
        Integer thresholdValue,

        String activeYN
) {
    /**
     * activeYN 기본값 처리
     */
    public AlertConfigCreateRequest {
        if (activeYN == null || activeYN.isBlank()) {
            activeYN = "Y";
        }
    }
}
