package S13P31A306.loglens.domain.log.constants;

import java.util.Objects;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 로그 정렬 필드
 */
@Getter
@RequiredArgsConstructor
public enum SortField {
    TIMESTAMP("timestamp");

    private final String value;

    /**
     * 문자열로부터 SortField를 찾습니다.
     *
     * @param value 정렬 필드 문자열
     * @return 일치하는 SortField, 없으면 null
     */
    public static SortField fromString(String value) {
        if (Objects.isNull(value)) {
            return null;
        }
        for (SortField field : values()) {
            if (field.name().equalsIgnoreCase(value)) {
                return field;
            }
        }
        return null;
    }
}
