package S13P31A306.loglens.domain.log.dto.response;

import S13P31A306.loglens.domain.log.entity.LogLevel;
import S13P31A306.loglens.domain.log.entity.SourceType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

//@formatter:off
/**
 * 로그 목록 조회용 응답 DTO
 * OpenSearch 실제 데이터 구조를 기반으로 한 간단한 로그 정보
 *
 * LogDetailResponse와의 차이점:
 * - 목록 조회에 필요한 핵심 필드만 포함
 * - comment, stackTrace, logDetails, AI 분석 관련 필드는 제외 (상세 조회 시에만 제공)
 */
//@formatter:on
@Data
@Builder
public class LogResponse {
    @Schema(description = "로그 ID (고유 식별자)", example = "4275513421")
    private Long logId;

    @Schema(description = "Trace ID", example = "6beef638-92dd-4c42-9657-5ed08579cd92")
    private String traceId;

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

    @Schema(description = "서비스 이름", example = "Loglens")
    private String serviceName;

    @Schema(description = "메서드 이름", example = "existsByEmail", nullable = true)
    private String methodName;

    @Schema(description = "스레드 이름", example = "http-nio-8081-exec-4")
    private String threadName;

    @Schema(description = "요청자 IP", example = "127.0.0.1")
    private String requesterIp;

    @Schema(description = "처리 시간 (밀리초)", example = "2", nullable = true)
    private Integer duration;
}
