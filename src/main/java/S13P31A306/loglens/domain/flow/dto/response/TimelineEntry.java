package S13P31A306.loglens.domain.flow.dto.response;

import S13P31A306.loglens.domain.log.dto.response.LogResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "타임라인 항목")
public record TimelineEntry(
        @Schema(description = "실행 순서", example = "1")
        Integer sequence,

        @Schema(description = "컴포넌트 ID", example = "1")
        Integer componentId,

        @Schema(description = "컴포넌트 이름", example = "AuthController")
        String componentName,

        @Schema(description = "레이어", example = "CONTROLLER")
        String layer,

        @Schema(description = "시작 시간 (첫 요청)")
        LocalDateTime startTime,

        @Schema(description = "종료 시간 (마지막 응답)")
        LocalDateTime endTime,

        @Schema(description = "소요 시간 (ms)", example = "22")
        Long duration,

        @Schema(description = "이 컴포넌트 구간의 모든 로그들")
        List<LogResponse> logs
) {
}
