package S13P31A306.loglens.domain.log.dto.response;

import S13P31A306.loglens.domain.log.dto.ai.AiAnalysisDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//@formatter:off
/**
 * 로그 상세 정보 응답 DTO
 * AI 분석 관련 필드만 포함
 */
//@formatter:on
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogDetailResponse {

    // ========== AI 분석 결과 ==========
    @Schema(description = "AI 분석 결과", nullable = true)
    private AiAnalysisDto analysis;

    @Schema(description = "캐시된 분석 결과 여부", example = "true", nullable = true)
    private Boolean fromCache;

    @Schema(description = "유사 로그 ID (캐시 사용 시)", example = "1234567800", nullable = true)
    private Long similarLogId;

    @Schema(description = "유사도 점수 (캐시 사용 시, 0.0~1.0)", example = "0.85", nullable = true)
    private Double similarityScore;
}
