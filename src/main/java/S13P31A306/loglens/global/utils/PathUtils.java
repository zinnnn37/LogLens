package S13P31A306.loglens.global.utils;

public final class PathUtils {
    private PathUtils() {
        throw new IllegalStateException();
    }

    /**
     * 경로 문자열에서 마지막 필드명만 추출
     *
     * @param path 점(.)으로 구분된 경로 문자열 (예: "method.param.field")
     * @return 마지막 필드명 (예: "field")
     */
    public static String extractLeafProperty(final String path) {
        final var lastDot = path.lastIndexOf('.');
        return (lastDot != -1) ? path.substring(lastDot + 1) : path;
    }
}
