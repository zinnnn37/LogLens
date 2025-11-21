package S13P31A306.loglens.domain.dependency.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 의존성 관계 요청 DTO
 * from → to 관계를 나타냄
 */
public record DependencyRelationRequest(
        @NotBlank(message = "호출하는 컴포넌트는 필수입니다")
        String from,

        @NotBlank(message = "호출받는 컴포넌트는 필수입니다")
        String to
) {
}
