package S13P31A306.loglens.domain.auth.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserSortField {
    CREATED_AT("CREATED_AT", "생성일"),
    NAME("NAME", "이름"),
    EMAIL("EMAIL", "이메일");

    private final String fieldName;
    private final String displayName;
}
