package S13P31A306.loglens.domain.log.dto.response;

import S13P31A306.loglens.domain.log.dto.ai.AiAnalysisDto;
import S13P31A306.loglens.domain.log.entity.LogLevel;
import S13P31A306.loglens.domain.log.entity.SourceType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

//@formatter:off
/**
 * 로그 상세 정보 응답 DTO
 * OpenSearch 실제 데이터 구조를 기반으로 한 로그 상세 정보
 *
 * LogResponse와의 차이점:
 * - comment: 코멘트 (thread, app, pid 정보) 추가
 * - stackTrace: 스택 트레이스 추가
 * - logDetails: 로그 상세 정보 (request_body, response_body 등) 추가
 * - analysis: AI 분석 결과 추가
 * - fromCache, similarLogId, similarityScore: AI 분석 캐싱 관련 정보 추가
 */
//@formatter:on
@Data
@Builder
public class LogDetailResponse {

    // ========== 기본 식별 정보 (LogResponse와 공통) ==========
    @Schema(description = "로그 ID (고유 식별자)", example = "4275513421")
    private Long logId;

    @Schema(description = "Trace ID", example = "6beef638-92dd-4c42-9657-5ed08579cd92")
    private String traceId;

    // ========== 로그 기본 정보 (LogResponse와 공통) ==========
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

    // ========== 상세 정보 전용: comment ==========
    @Schema(description = "코멘트 (thread, app, pid 정보)", example = "thread: http-nio-8081-exec-4, app: demo, pid: 49983")
    private String comment;

    // ========== 서비스 및 실행 정보 (LogResponse와 공통) ==========
    @Schema(description = "서비스 이름", example = "Loglens")
    private String serviceName;

//    @Schema(description = "클래스 이름", example = "com.example.demo.domain.user.repository.UserJpaRepository")
//    private String className;

    @Schema(description = "메서드 이름", example = "existsByEmail", nullable = true)
    private String methodName;

    @Schema(description = "스레드 이름", example = "http-nio-8081-exec-4")
    private String threadName;

    @Schema(description = "요청자 IP 주소", example = "127.0.0.1")
    private String requesterIp;

    @Schema(description = "실행 시간 (밀리초)", example = "2", nullable = true)
    private Integer duration;

    // ========== 상세 정보 전용: logDetails ==========
    @Schema(description = "로그 상세 정보 (request_body, response_body, exception_type 등 추가 메타데이터)", nullable = true)
    private Map<String, Object> logDetails;

    // ========== 상세 정보 전용: AI 분석 결과 ==========
    @Schema(description = "AI 분석 결과", nullable = true)
    private AiAnalysisDto analysis;

    @Schema(description = "캐시된 분석 결과 여부", example = "true", nullable = true)
    private Boolean fromCache;

    @Schema(description = "유사 로그 ID (캐시 사용 시)", example = "1234567800", nullable = true)
    private Long similarLogId;

    @Schema(description = "유사도 점수 (캐시 사용 시, 0.0~1.0)", example = "0.85", nullable = true)
    private Double similarityScore;
}
