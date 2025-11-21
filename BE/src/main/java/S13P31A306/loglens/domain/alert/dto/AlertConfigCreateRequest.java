package S13P31A306.loglens.domain.alert.dto;

import S13P31A306.loglens.domain.alert.entity.AlertType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 알림 설정 생성 요청 DTO
 */
@Schema(description = "알림 설정 생성 요청")
public record AlertConfigCreateRequest(
        @Schema(description = "프로젝트 UUID", example = "9911573f-8a1d-3b96-98b4-5a0def93513b", required = true)
        @NotNull(message = "프로젝트 UUID는 필수입니다.")
        String projectUuid,

        @Schema(description = "알림 타입 (ERROR_THRESHOLD: 에러 발생 건수 임계값, LATENCY: 응답 시간 임계값, ERROR_RATE: 에러율 임계값)",
                example = "ERROR_THRESHOLD",
                required = true,
                allowableValues = {"ERROR_THRESHOLD", "LATENCY", "ERROR_RATE"})
        @NotNull(message = "알림 타입은 필수입니다.")
        AlertType alertType,

        @Schema(description = "임계값 (1-255 사이의 정수)",
                example = "10",
                required = true,
                minimum = "1",
                maximum = "255")
        @NotNull(message = "임계값은 필수입니다.")
        @Min(value = 1, message = "임계값은 1 이상이어야 합니다.")
        @Max(value = 255, message = "임계값은 255 이하여야 합니다.")
        Integer thresholdValue,

        @Schema(description = "활성화 여부 (Y: 활성, N: 비활성). 미입력 시 기본값 'Y'",
                example = "Y",
                allowableValues = {"Y", "N"},
                defaultValue = "Y")
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
