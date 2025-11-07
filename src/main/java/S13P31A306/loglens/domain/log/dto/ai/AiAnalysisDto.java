package S13P31A306.loglens.domain.log.dto.ai;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 로그 분석 결과 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAnalysisDto {

    @Schema(description = "로그 요약 (1-2문장)", example = "데이터베이스 연결 풀이 고갈되어 타임아웃이 발생했습니다.")
    private String summary;

    @Schema(description = "에러 원인 분석", example = "동시 요청이 급증하면서 커넥션 풀의 모든 연결이 사용 중이 되었고...")
    @JsonProperty("error_cause")
    private String errorCause;

    @Schema(description = "해결 방안", example = "1. [우선순위: 높음] 커넥션 풀 크기 증가...")
    private String solution;

    @Schema(description = "분류 태그", example = "[\"DATABASE\", \"CONNECTION_POOL\", \"TIMEOUT\"]")
    private List<String> tags;

    @Schema(description = "분석 타입", example = "TRACE_BASED")
    @JsonProperty("analysis_type")
    private String analysisType;

    @Schema(description = "대상 타입", example = "LOG")
    @JsonProperty("target_type")
    private String targetType;

    @Schema(description = "분석 시각 (UTC)", example = "2025-01-15T10:30:45.123Z")
    @JsonProperty("analyzed_at")
    private LocalDateTime analyzedAt;
}
