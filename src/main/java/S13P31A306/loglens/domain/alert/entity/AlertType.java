package S13P31A306.loglens.domain.alert.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 알림 타입 Enum
 */
@Getter
@RequiredArgsConstructor
public enum AlertType {
    ERROR_THRESHOLD("에러 발생 건수 임계값"),
    LATENCY("응답 시간 임계값"),
    ERROR_RATE("에러율 임계값");

    private final String description;
}
