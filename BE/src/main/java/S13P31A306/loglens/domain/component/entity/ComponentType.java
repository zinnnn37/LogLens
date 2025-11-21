package S13P31A306.loglens.domain.component.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ComponentType {
    FE("프론트엔드"),
    BE("백엔드"),
    INFRA("인프라");

    private final String description;
}
