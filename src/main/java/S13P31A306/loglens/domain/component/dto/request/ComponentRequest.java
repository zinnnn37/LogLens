package S13P31A306.loglens.domain.component.dto.request;

import S13P31A306.loglens.domain.component.entity.ComponentLayer;
import S13P31A306.loglens.domain.component.entity.ComponentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ComponentRequest(
        @NotBlank(message = "컴포넌트 이름은 필수입니다")
        String name,

        String classType,

        @NotNull(message = "컴포넌트 타입은 필수입니다")
        ComponentType componentType,

        String packageName,

        ComponentLayer layer,

        Boolean highLevel,

        @NotBlank(message = "기술 스택은 필수입니다")
        String technology
) {}
