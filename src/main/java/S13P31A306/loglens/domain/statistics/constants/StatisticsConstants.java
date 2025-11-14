package S13P31A306.loglens.domain.statistics.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 통계 도메인 상수
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StatisticsConstants {

    // ========== 로그 추이 관련 상수 ==========

    /**
     * 조회 기간 (24시간)
     */
    public static final int TREND_HOURS = 24;

    /**
     * 시간 간격 (3시간)
     */
    public static final int INTERVAL_HOURS = 3;

    /**
     * 데이터 포인트 개수 (8개)
     */
    public static final int DATA_POINTS = 8;

    /**
     * 시각 표시 포맷 (HH:mm)
     */
    public static final String TIME_FORMAT = "HH:mm";

    /**
     * 기본 타임존
     */
    public static final String DEFAULT_TIMEZONE = "Asia/Seoul";
}
