package S13P31A306.loglens.domain.component.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ComponentLayer {
    CONTROLLER("컨트롤러"),
    SERVICE("서비스"),
    REPOSITORY("레포지토리");

    private final String description;
}
