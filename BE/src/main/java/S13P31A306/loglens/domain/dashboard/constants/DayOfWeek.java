package S13P31A306.loglens.domain.dashboard.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum DayOfWeek {
    MONDAY(1, "월요일"),
    TUESDAY(2, "화요일"),
    WEDNESDAY(3, "수요일"),
    THURSDAY(4, "목요일"),
    FRIDAY(5, "금요일"),
    SATURDAY(6, "토요일"),
    SUNDAY(7, "일요일");

    private final int value;
    private final String koreanName;

    public static DayOfWeek fromValue(int value) {
        return Arrays.stream(values())
                .filter(day -> day.value == value)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid day of week value: " + value));
    }
}
