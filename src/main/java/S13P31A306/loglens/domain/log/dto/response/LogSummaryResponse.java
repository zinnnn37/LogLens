package S13P31A306.loglens.domain.log.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogSummaryResponse {
    @Schema(description = "전체 로그 개수", example = "1523")
    private long totalLogs;

    @Schema(description = "로그 수집 기간 (밀리초)", example = "3600000")
    private long durationMs;

    @Schema(description = "로그 시작 시간", example = "2024-01-15T10:30:00.000Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private LocalDateTime startTime;

    @Schema(description = "로그 종료 시간", example = "2024-01-15T11:30:00.000Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private LocalDateTime endTime;

    @Schema(description = "ERROR 레벨 로그 개수", example = "23")
    private long errorCount;

    @Schema(description = "WARN 레벨 로그 개수", example = "145")
    private long warnCount;

    @Schema(description = "INFO 레벨 로그 개수", example = "1355")
    private long infoCount;
}
