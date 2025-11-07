package S13P31A306.loglens.domain.log.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Log {

    private String id; // _id from OpenSearch

    // ========== 핵심 식별 필드 ==========
    @JsonProperty("log_id")
    private Long logId;

    @JsonProperty("project_uuid")
    private String projectUuid;

    @JsonProperty("timestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private OffsetDateTime timestamp;

    @JsonProperty("requester_ip")
    private String requesterIp;

    // ========== 서비스 및 로깅 컨텍스트 ==========
    @JsonProperty("service_name")
    private String serviceName;

    private String logger;

    @JsonProperty("source_type")
    private SourceType sourceType;

    private String layer;

    // ========== 로그 내용 ==========
    @JsonProperty("log_level")
    @JsonAlias("level")
    private LogLevel logLevel;

    private String message;

    private String comment;

    // ========== 메서드 및 스레드 정보 ==========
    @JsonProperty("class_name")
    private String className;

    @JsonProperty("method_name")
    private String methodName;

    @JsonProperty("thread_name")
    private String threadName;

    @JsonProperty("component_name")
    private String componentName;

    // ========== 트레이싱 ==========
    @JsonProperty("trace_id")
    @JsonAlias({"trace_id", "traceId"})
    private String traceId;

    // ========== 성능 ==========
    private Integer duration;

    // ========== 에러 정보 ==========
    @JsonProperty("stacktrace")
    private String stackTrace;

    // ========== 로그 상세 ==========
    @JsonProperty("log_details")
    private Map<String, Object> logDetails;

    // ========== AI 분석 ==========
    @JsonProperty("log_vector")
    private float[] logVector;

    @JsonProperty("ai_analysis")
    private Map<String, Object> aiAnalysis;

    // ========== 시스템 필드 ==========
    @JsonProperty("indexed_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private OffsetDateTime indexedAt;
}
