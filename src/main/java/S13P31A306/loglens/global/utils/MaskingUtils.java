package S13P31A306.loglens.global.utils;

import S13P31A306.loglens.global.annotation.ExcludeFromLogging;
import S13P31A306.loglens.global.annotation.Sensitive;
import S13P31A306.loglens.global.constants.LogMessages;
import S13P31A306.loglens.global.constants.SystemMessages;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * `@Sensitive`는 마스킹 처리, `@ExcludeFromLogging`은 로그 제외 처리를 수행하는 필드 전용 유틸리티 클래스입니다.
 */
public final class MaskingUtils {

    private static final String NULL_TEXT = "null";
    private static final String FIELD_FORMAT = "%s=%s";

    private MaskingUtils() {
        throw new IllegalStateException(SystemMessages.UTILITY_CLASS_ERROR.message());
    }

    /**
     * 대상 객체의 필드 중 @Sensitive 어노테이션이 붙은 항목을 마스킹 처리하여 문자열로 반환합니다.
     */
    public static String mask(final Object target) {
        if (Objects.isNull(target)) {
            return NULL_TEXT;
        }

        Class<?> clazz = target.getClass();
        // 파라미터가 단순 타입일 경우, 리플렉션 없이 바로 문자열로 변환
        if (isSimpleType(clazz)) {
            return String.valueOf(target);
        }

        Field[] fields = clazz.getDeclaredFields();
        return getString(target, clazz, fields);
    }

    /**
     * 주어진 클래스가 단순 타입(Primitive, Wrapper, String)인지 확인합니다.
     */
    private static boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive() ||
                clazz.equals(String.class) ||
                Number.class.isAssignableFrom(clazz) ||
                clazz.equals(Boolean.class) ||
                clazz.equals(Character.class);
    }

    /**
     * 객체의 필드 정보를 문자열로 변환하여 반환합니다.
     */
    private static String getString(final Object target, final Class<?> clazz, final Field[] fields) {
        StringJoiner result = new StringJoiner(", ", clazz.getSimpleName() + "[", "]");

        for (Field field : fields) {
            result.add(formatField(field, target));
        }

        return result.toString();
    }

    // @formatter:off
    /**
     * 필드를 문자열로 변환합니다.
     *
     * @return 필드명=값 형태의 문자열. 예: fieldName=value
     *   - 제외된 필드인 경우: fieldName=<excluded>
     *   - 민감 정보인 경우: fieldName=****
     *   - 일반 필드인 경우: fieldName={실제값}
     *   - 접근 오류 시: fieldName=<error>
     */
    // @formatter:on
    private static String formatField(final Field field, final Object target) {
        field.setAccessible(true); // private 필드에 접근 가능하도록 설정
        try {
            if (isExcluded(field)) {
                return formatExcluded(field);
            }

            Object value = field.get(target);
            String displayValue = isSensitive(field) ? LogMessages.LOG_MASKED_VALUE.message() : String.valueOf(value);
            return formatKeyValue(field.getName(), displayValue);
        } catch (IllegalAccessException e) {
            return formatKeyValue(field.getName(), LogMessages.LOG_ERROR_VALUE.message());
        }
    }

    private static boolean isExcluded(final Field field) {
        return field.isAnnotationPresent(ExcludeFromLogging.class);
    }

    private static boolean isSensitive(final Field field) {
        return field.isAnnotationPresent(Sensitive.class);
    }

    private static String formatExcluded(final Field field) {
        return formatKeyValue(field.getName(), LogMessages.LOG_EXCLUDED_VALUE.message());
    }

    private static String formatKeyValue(final String key, final String value) {
        return String.format(FIELD_FORMAT, key, value);
    }
}

