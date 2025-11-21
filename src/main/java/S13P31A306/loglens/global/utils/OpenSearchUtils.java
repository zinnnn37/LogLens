package S13P31A306.loglens.global.utils;

import java.util.Objects;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OpenSearchUtils {

    /**
     * 프로젝트별 인덱스 패턴을 반환
     *
     * @param projectUuid 프로젝트 UUID (하이픈 포함)
     * @return "{projectUuid_with_underscores}_*" 형식의 인덱스 패턴
     */
    public static String getProjectIndexPattern(String projectUuid) {
        if (Objects.isNull(projectUuid) || projectUuid.isBlank()) {
            throw new IllegalArgumentException("Project UUID는 null이거나 비어있을 수 없습니다.");
        }
        return projectUuid.replace('-', '_') + "_*";
    }
}
