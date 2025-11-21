package S13P31A306.loglens.domain.flow.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "컴포넌트 정보 (간소화)")
public record FlowComponentInfo(
        @Schema(description = "컴포넌트 ID", example = "1")
        Integer id,

        @Schema(description = "컴포넌트 이름", example = "AuthController")
        String name,

        @Schema(description = "레이어", example = "CONTROLLER")
        String layer
) {
}
