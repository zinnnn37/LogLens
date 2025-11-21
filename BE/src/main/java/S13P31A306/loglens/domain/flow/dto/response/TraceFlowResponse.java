package S13P31A306.loglens.domain.flow.dto.response;

import S13P31A306.loglens.domain.dependency.dto.response.DependencyGraphResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Trace 요청 흐름 응답")
public record TraceFlowResponse(
        @Schema(description = "추적 ID")
        String traceId,

        @Schema(description = "프로젝트 UUID")
        String projectUuid,

        @Schema(description = "흐름 요약 정보")
        FlowSummary summary,

        @Schema(description = "시간순 실행 흐름")
        List<TimelineEntry> timeline,

        @Schema(description = "사용된 컴포넌트 목록")
        List<FlowComponentInfo> components,

        @Schema(description = "컴포넌트 의존성 그래프")
        DependencyGraphResponse graph
) {
}
