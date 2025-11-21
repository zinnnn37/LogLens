package S13P31A306.loglens.domain.log.constants;

import java.util.Objects;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 로그 정렬 방향
 */
@Getter
@RequiredArgsConstructor
public enum SortDirection {
    ASC("asc"),
    DESC("desc");

    private final String value;

    /**
     * 문자열로부터 SortDirection을 찾습니다.
     *
     * @param value 정렬 방향 문자열
     * @return 일치하는 SortDirection, 없으면 null
     */
    public static SortDirection fromString(String value) {
        if (Objects.isNull(value)) {
            return null;
        }
        for (SortDirection direction : values()) {
            if (direction.name().equalsIgnoreCase(value)) {
                return direction;
            }
        }
        return null;
    }
}
