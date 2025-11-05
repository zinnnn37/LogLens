package S13P31A306.loglens.domain.project.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectCreateRequest(
        @NotBlank(message = "PROJECT_NAME_REQUIRED")
        @Size(min = 2, max = 100, message = "INVALID_PROJECT_NAME_LENGTH")
        @Schema(description = "프로젝트 이름 (2~100자, 중복 불가)", example = "LogLens")
        String projectName,

        @Size(max = 500, message = "INVALID_DESCRIPTION_LENGTH")
        @Schema(description = "프로젝트 설명 (Optional)", example = "로그 수집 및 분석 프로젝트")
        String description
) {
}
