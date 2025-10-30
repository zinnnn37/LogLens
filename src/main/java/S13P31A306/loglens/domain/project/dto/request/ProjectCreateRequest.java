package S13P31A306.loglens.domain.project.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectCreateRequest(
        @NotBlank(message = "PROJECT_NAME_REQUIRED")
        @Size(min = 2, max = 100)
        @Schema(description = "프로젝트 이름 (2~100자, 중복 불가)")
        String projectName,

        @Size(max = 500, message = "INVALID_DESCRIPTION_LENGTH")
        @Schema(description = "프로젝트 설명 (Optional)")
        String description
) {
}
