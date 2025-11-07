package S13P31A306.loglens.domain.log.dto.response;

import S13P31A306.loglens.domain.log.entity.LogLevel;
import S13P31A306.loglens.domain.log.entity.SourceType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogResponse {
    @Schema(description = "로그 ID (고유 식별자)", example = "1234567890")
    private Long logId;

    @Schema(description = "Trace ID", example = "trace-abc-123")
    private String traceId;

    @Schema(description = "로그 레벨", example = "ERROR")
    private LogLevel logLevel;

    @Schema(description = "소스 타입", example = "BE")
    private SourceType sourceType;

    @Schema(description = "로그 메시지", example = "NullPointerException occurred in UserService")
    private String message;

    @Schema(description = "로그 발생 시간", example = "2024-01-15T10:30:45.123Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private LocalDateTime timestamp;

    @Schema(description = "로거 이름", example = "com.example.UserService")
    private String logger;

    @Schema(description = "레이어 정보", example = "Service")
    private String layer;

    @Schema(description = "사용자 코멘트", example = "이 에러는 무시해도 됨")
    private String comment;
}
