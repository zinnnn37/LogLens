package S13P31A306.loglens.domain.auth.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserSortField {
    CREATED_AT("createdAt", "생성일"),
    NAME("name", "이름"),
    EMAIL("email", "이메일");

    private final String fieldName;
    private final String displayName;
}
