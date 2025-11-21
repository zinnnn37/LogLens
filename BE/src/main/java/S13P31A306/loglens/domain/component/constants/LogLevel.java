package S13P31A306.loglens.domain.component.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 로그 레벨 Enum
 */
@Getter
@RequiredArgsConstructor
public enum LogLevel {
    ERROR("ERROR"),
    WARN("WARN"),
    INFO("INFO"),
    DEBUG("DEBUG"),
    TRACE("TRACE");

    private final String level;
}
