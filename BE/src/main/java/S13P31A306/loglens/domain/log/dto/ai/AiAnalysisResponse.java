package S13P31A306.loglens.domain.log.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 서비스 응답 DTO (GET /api/v1/logs/{log_id}/analysis)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAnalysisResponse {

    @Schema(description = "분석된 로그 ID", example = "1234567890")
    @JsonProperty("log_id")
    private Long logId;

    @Schema(description = "AI 분석 결과")
    private AiAnalysisDto analysis;

    @Schema(description = "캐시된 결과 여부", example = "true")
    @JsonProperty("from_cache")
    private Boolean fromCache;

    @Schema(description = "유사한 로그 ID (캐시 사용 시)", example = "1234567800")
    @JsonProperty("similar_log_id")
    private Long similarLogId;

    @Schema(description = "유사도 점수 (0.0~1.0, 캐시 사용 시)", example = "0.85")
    @JsonProperty("similarity_score")
    private Double similarityScore;
}
