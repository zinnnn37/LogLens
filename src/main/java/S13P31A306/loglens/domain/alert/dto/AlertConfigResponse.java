package S13P31A306.loglens.domain.alert.dto;

import S13P31A306.loglens.domain.alert.entity.AlertType;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 알림 설정 응답 DTO
 */
@Schema(description = "알림 설정 응답")
public record AlertConfigResponse(
        @Schema(description = "알림 설정 ID", example = "1")
        Integer id,

        @Schema(description = "알림 타입 (ERROR_THRESHOLD: 에러 발생 건수 임계값, LATENCY: 응답 시간 임계값, ERROR_RATE: 에러율 임계값)",
                example = "ERROR_THRESHOLD")
        AlertType alertType,

        @Schema(description = "임계값 (1-255)", example = "10", minimum = "1", maximum = "255")
        Integer thresholdValue,

        @Schema(description = "활성화 여부 (Y: 활성, N: 비활성)", example = "Y", allowableValues = {"Y", "N"})
        String activeYN,

        @Schema(description = "프로젝트 UUID", example = "9911573f-8a1d-3b96-98b4-5a0def93513b")
        String projectUuid,

        @Schema(description = "프로젝트 이름", example = "Loglens")
        String projectName
) {
}
