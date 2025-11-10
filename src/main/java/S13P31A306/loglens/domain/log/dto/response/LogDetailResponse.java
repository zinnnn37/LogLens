package S13P31A306.loglens.domain.log.dto.response;

import S13P31A306.loglens.domain.log.dto.ai.AiAnalysisDto;
import S13P31A306.loglens.domain.log.entity.LogLevel;
import S13P31A306.loglens.domain.log.entity.SourceType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 로그 상세 정보 응답 DTO
 * 기본 로그 정보 + 상세 정보 + AI 분석 결과를 포함합니다.
 */
@Data
@Builder
public class LogDetailResponse {

    // ========== 기본 로그 정보 ==========
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

    // ========== 상세 정보 ==========
    @Schema(description = "서비스 이름", example = "loglens-api")
    private String serviceName;

    @Schema(description = "클래스 이름", example = "S13P31A306.loglens.domain.user.service.UserServiceImpl")
    private String className;

    @Schema(description = "메서드 이름", example = "getUserById")
    private String methodName;

    @Schema(description = "스레드 이름", example = "http-nio-8080-exec-5")
    private String threadName;

    @Schema(description = "요청자 IP 주소", example = "192.168.1.100")
    private String requesterIp;

    @Schema(description = "실행 시간 (밀리초)", example = "1250")
    private Integer duration;

    @Schema(description = "스택 트레이스")
    private String stackTrace;

    @Schema(description = "로그 상세 정보 (추가 메타데이터)")
    private Map<String, Object> logDetails;

    // ========== AI 분석 결과 ==========
    @Schema(description = "AI 분석 결과 (분석되지 않은 경우 null)")
    private AiAnalysisDto analysis;

    @Schema(description = "캐시된 분석 결과 여부", example = "true")
    private Boolean fromCache;

    @Schema(description = "유사 로그 ID (캐시 사용 시)", example = "1234567800")
    private Long similarLogId;

    @Schema(description = "유사도 점수 (캐시 사용 시, 0.0~1.0)", example = "0.85")
    private Double similarityScore;
}
