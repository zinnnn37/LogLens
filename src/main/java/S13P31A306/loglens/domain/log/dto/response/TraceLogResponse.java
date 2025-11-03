package S13P31A306.loglens.domain.log.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TraceLogResponse {

    @Schema(description = "Trace ID", example = "abc123def456")
    private String traceId;

    @Schema(description = "Trace 로그 요약 정보")
    private LogSummaryResponse summary;

    @Schema(description = "Trace에 포함된 로그 목록")
    private List<LogResponse> logs;
}
