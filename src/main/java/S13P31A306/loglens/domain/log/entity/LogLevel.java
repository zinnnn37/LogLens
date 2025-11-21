package S13P31A306.loglens.domain.log.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LogLevel {
    INFO("정보 로그"),
    WARN("경고 로그"),
    ERROR("오류 로그");

    private final String description;
}
