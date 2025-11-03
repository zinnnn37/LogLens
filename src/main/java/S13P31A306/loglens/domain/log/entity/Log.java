package S13P31A306.loglens.domain.log.entity;

import S13P31A306.loglens.domain.component.entity.ComponentType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Log {
    // This field will be populated from the OpenSearch document's _id
    private String id;

    // ========== 핵심 식별 필드 (Core Identification) ==========
    @JsonProperty("log_id")
    private Long logId;  // OpenSearch의 고유 로그 ID (Auto-increment)

    @JsonProperty("project_uuid")
    private String projectUuid;  // 프로젝트 UUID (UUID 문자열)

    private LocalDateTime timestamp;  // 로그 발생 시각 (ISO 8601)

    @JsonProperty("requester_ip")
    private String requesterIp;  // 요청자 IP 주소

    // ========== 서비스 및 로깅 컨텍스트 (Service Context) ==========
    @JsonProperty("service_name")
    private String serviceName;  // 서비스명 (예: user-service)

    private String logger;  // 로거 전체 경로

    @JsonProperty("source_type")
    private ComponentType sourceType;  // FE/BE/INFRA

    private String layer;  // Controller/Service/Repository

    // ========== 로그 내용 (Log Content) ==========
    @JsonProperty("log_level")
    private LogLevel logLevel;  // 로그 레벨 (INFO/WARN/ERROR만 허용)

    private String message;  // 로그 메시지

    private String comment;  // 추가 코멘트

    // ========== 메서드 및 스레드 정보 (Method & Thread) ==========
    @JsonProperty("class_name")
    private String className;  // 클래스명

    @JsonProperty("method_name")
    private String methodName;  // 메서드명

    @JsonProperty("thread_name")
    private String threadName;  // 스레드명 (선택)

    // ========== 트레이싱 (Tracing) ==========
    @JsonProperty("trace_id")
    private String traceId;  // 분산 트레이싱 ID

    // ========== 성능 (Performance) ==========
    private Integer duration;  // 실행 시간 (밀리초)

    // ========== 에러 정보 (Error Information) ==========
    @JsonProperty("stack_trace")
    private String stackTrace;  // 스택 트레이스 (에러 발생 시)

    // ========== 로그 상세 정보 (Log Details - Nested Object) ==========
    @JsonProperty("log_details")
    private Map<String, Object> logDetails;  // HTTP 요청/응답, 추가 정보 등

    // ========== AI 분석 결과 (AI Analysis - Nested Object) ==========
    @JsonProperty("log_vector")
    private float[] logVector;  // 임베딩 벡터 (1536차원)

    @JsonProperty("ai_analysis")
    private Map<String, Object> aiAnalysis;  // AI 분석 결과

    // ========== 시스템 필드 (System Fields) ==========
    @JsonProperty("indexed_at")
    private LocalDateTime indexedAt;  // OpenSearch 인덱싱 시각
}
