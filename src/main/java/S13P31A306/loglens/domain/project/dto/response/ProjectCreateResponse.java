package S13P31A306.loglens.domain.project.dto.response;

import S13P31A306.loglens.global.annotation.Sensitive;
import io.swagger.v3.oas.annotations.media.Schema;

public record ProjectCreateResponse(
        @Schema(description = "프로젝트 ID", example = "42")
        int projectId,

        @Schema(description = "프로젝트 이름", example = "LogLens")
        String projectName,

        @Schema(description = "프로젝트 설명", example = "로그 수집 및 분석 프로젝트")
        String description,

        @Sensitive
        @Schema(description = "프로젝트 uuid KEY", example = "pk_1a2b3c4d5e6f")
        String projectUuid,

        @Schema(description = "프로젝트 생성 시간", example = "2025-10-29T10:30:00")
        String createdAt,

        @Schema(description = "프로젝트 업데이트 시간", example = "2025-10-29T10:30:00")
        String updatedAt
) {
}
