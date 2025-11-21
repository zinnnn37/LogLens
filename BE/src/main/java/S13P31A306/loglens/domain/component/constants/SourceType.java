package S13P31A306.loglens.domain.component.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 소스 타입 Enum (Backend/Frontend 구분)
 */
@Getter
@RequiredArgsConstructor
public enum SourceType {
    BACKEND("BE", "백엔드"),
    FRONTEND("FE", "프론트엔드");

    private final String type;
    private final String description;
}
