package S13P31A306.loglens.domain.project.dto.response;

import S13P31A306.loglens.global.annotation.Sensitive;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프로젝트 연결 상태 응답 DTO")
public record ProjectConnectionResponse(
        @Sensitive
        @Schema(description = "프로젝트 UUID", example = "48d96cd7-bf8d-38f5-891c-9c2f6430d871")
        String projectUuid,

        @Schema(description = "연결 상태", example = "true")
        boolean isConnected
) {
}
