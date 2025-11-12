package S13P31A306.loglens.domain.alert.dto;

import S13P31A306.loglens.domain.alert.entity.AlertType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 알림 설정 수정 요청 DTO (부분 업데이트 지원)
 */
@Schema(description = "알림 설정 수정 요청. 제공된 필드만 수정됩니다 (부분 업데이트)")
public record AlertConfigUpdateRequest(
        @Schema(description = "알림 설정 ID", example = "1", required = true)
        @NotNull(message = "알림 설정 ID는 필수입니다.")
        Integer alertConfigId,

        @Schema(description = "알림 타입 (ERROR_THRESHOLD: 에러 발생 건수 임계값, LATENCY: 응답 시간 임계값, ERROR_RATE: 에러율 임계값). 미입력 시 기존 값 유지",
                example = "LATENCY",
                allowableValues = {"ERROR_THRESHOLD", "LATENCY", "ERROR_RATE"})
        AlertType alertType,

        @Schema(description = "임계값 (1-255 사이의 정수). 미입력 시 기존 값 유지",
                example = "100",
                minimum = "1",
                maximum = "255")
        @Min(value = 1, message = "임계값은 1 이상이어야 합니다.")
        @Max(value = 255, message = "임계값은 255 이하여야 합니다.")
        Integer thresholdValue,

        @Schema(description = "활성화 여부 (Y: 활성, N: 비활성). 미입력 시 기존 값 유지",
                example = "N",
                allowableValues = {"Y", "N"})
        String activeYN
) {
}
