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
    @Schema(description = "로그 ID", example = "12345")
    private Long logId;

    @Schema(description = "프로젝트 UUID", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef")
    private String projectUuid;

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

    @Schema(description = "요청자 IP", example = "192.168.1.1")
    private String requesterIp;

    @Schema(description = "서비스 이름", example = "user-service")
    private String serviceName;

    @Schema(description = "클래스 이름", example = "UserService")
    private String className;

    @Schema(description = "메서드 이름", example = "getUser")
    private String methodName;

    @Schema(description = "스레드 이름", example = "http-nio-8080-exec-1")
    private String threadName;

    @Schema(description = "스택 트레이스", example = "java.lang.NullPointerException: ...")
    private String stackTrace;

    @Schema(description = "컴포넌트 이름", example = "database")
    private String componentName;

    @Schema(description = "처리 시간 (ms)", example = "150")
    private Integer duration;
}
