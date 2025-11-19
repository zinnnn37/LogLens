package S13P31A306.loglens.domain.alert.dto;

import S13P31A306.loglens.domain.log.dto.response.LogResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 알림 이력 응답 DTO
 */
@Schema(description = "알림 이력 응답")
public record AlertHistoryResponse(
        @Schema(description = "알림 이력 ID", example = "1")
        Integer id,

        @Schema(description = "알림 메시지", example = "에러 발생 건수가 임계값(10건)을 초과했습니다.")
        String alertMessage,

        @Schema(description = "알림 발생 시간", example = "2025-11-12T13:25:00")
        LocalDateTime alertTime,

        @Schema(description = "읽음 여부 (Y: 읽음, N: 읽지 않음)", example = "N", allowableValues = {"Y", "N"})
        String resolvedYN,

        @Schema(description = "관련 로그 참조 정보 (JSON 형식)",
                example = "{\"logId\": 12345, \"traceId\": \"abc-123\", \"errorCount\": 15}")
        String logReference,

        @Schema(description = "알림 레벨", example = "ERROR", allowableValues = {"ERROR", "WARN", "INFO"})
        String alertLevel,

        @Schema(description = "트레이스 ID", example = "abc-123-xyz")
        String traceId,

        @Schema(description = "프로젝트 UUID", example = "9911573f-8a1d-3b96-98b4-5a0def93513b")
        String projectUuid,

        @Schema(description = "알림 발생 원인 로그 목록")
        List<LogResponse> relatedLogs,

        @Schema(description = "관련 Trace ID 목록")
        List<String> relatedTraceIds
) {
}
