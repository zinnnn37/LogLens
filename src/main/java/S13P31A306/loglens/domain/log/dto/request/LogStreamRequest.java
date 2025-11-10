package S13P31A306.loglens.domain.log.dto.request;

import S13P31A306.loglens.global.annotation.Sensitive;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

import lombok.Builder;
import lombok.Data;

// @formatter:off
/**
 * SSE 로그 스트리밍 요청 DTO
 * - 실시간 로그 스트리밍에 필요한 필드만 포함
 * - sort는 항상 TIMESTAMP,DESC 고정
 */
// @formatter:on
@Data
@Builder
public class LogStreamRequest {

    @Sensitive
    @Schema(description = "프로젝트 UUID", example = "9911573f-8a1d-3b96-98b4-5a0def93513b", required = true)
    private String projectUuid;

    @Schema(description = "페이지 크기", example = "50", defaultValue = "100")
    private Integer size = 100;

    @Schema(description = "로그 레벨 필터", example = "[\"ERROR\", \"WARN\"]")
    private List<String> logLevel;

    @Schema(description = "소스 타입 필터", example = "[\"BE\"]")
    private List<String> sourceType;

    @Schema(description = "검색 키워드", example = "NullPointerException")
    private String keyword;

    /**
     * LogStreamRequest를 LogSearchRequest로 변환 SSE 스트리밍에 필요한 필드만 설정하고 sort는 TIMESTAMP,DESC로 고정
     */
    public LogSearchRequest toLogSearchRequest() {
        return LogSearchRequest.builder()
                .projectUuid(this.projectUuid)
                .size(this.size)
                .logLevel(this.logLevel)
                .sourceType(this.sourceType)
                .keyword(this.keyword)
                .sort("TIMESTAMP,DESC")
                .build();
    }
}
