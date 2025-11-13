package S13P31A306.loglens.domain.log.dto.response;

import S13P31A306.loglens.domain.log.entity.LogLevel;
import S13P31A306.loglens.domain.log.entity.SourceType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//@formatter:off
/**
 * 로그 목록 조회용 응답 DTO
 * OpenSearch 실제 데이터 구조를 기반으로 한 로그 정보
 */
//@formatter:on
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogResponse {
    // ========== 기본 식별 정보 ==========
    @Schema(description = "로그 ID (고유 식별자)", example = "4275513421")
    private Long logId;

    @Schema(description = "Trace ID", example = "6beef638-92dd-4c42-9657-5ed08579cd92")
    private String traceId;

    // ========== 로그 기본 정보 ==========
    @Schema(description = "로그 레벨", example = "ERROR")
    private LogLevel logLevel;

    @Schema(description = "소스 타입", example = "BE")
    private SourceType sourceType;

    @Schema(description = "로그 메시지", example = "Request received: existsByEmail")
    private String message;

    @Schema(description = "로그 발생 시간", example = "2025-11-12T20:14:08.090Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private LocalDateTime timestamp;

    @Schema(description = "로거 이름 (패키지 포함 전체 클래스명)", example = "com.example.demo.domain.user.repository.UserJpaRepository")
    private String logger;

    @Schema(description = "레이어 정보", example = "Repository")
    private String layer;

    @Schema(description = "코멘트 (thread, app, pid 정보)", example = "thread: http-nio-8081-exec-4, app: demo, pid: 49983")
    private String comment;

    // ========== 서비스 및 실행 정보 ==========
    @Schema(description = "서비스 이름", example = "Loglens")
    private String serviceName;

    @Schema(description = "메서드 이름", example = "existsByEmail", nullable = true)
    private String methodName;

    @Schema(description = "스레드 이름", example = "http-nio-8081-exec-4")
    private String threadName;

    @Schema(description = "요청자 IP 주소", example = "127.0.0.1")
    private String requesterIp;

    @Schema(description = "실행 시간 (밀리초)", example = "2", nullable = true)
    private Integer duration;

    // ========== 상세 정보 ==========
    @Schema(description = "로그 상세 정보 (request_body, response_body, exception_type 등 추가 메타데이터)", nullable = true)
    private Map<String, Object> logDetails;
}
