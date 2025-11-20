package S13P31A306.loglens.domain.dependency.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record DependencyGraphBatchRequest(
//        @NotBlank(message = "프로젝트 이름은 필수입니다")
//        String projectName,

        @NotEmpty(message = "의존성 목록은 비어있을 수 없습니다")
        @Valid
        List<DependencyRelationRequest> dependencies,
        List<String> databases
) {
}
