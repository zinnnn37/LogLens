package S13P31A306.loglens.domain.log.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SourceType {
    FE("프론트엔드 로그"),
    BE("백엔드 로그"),
    INFRA("인프라 로그");

    private final String description;
}
