package a306.dependency_logger_starter.logging.util;

import a306.dependency_logger_starter.logging.annotation.ExcludeValue;
import a306.dependency_logger_starter.logging.annotation.Sensitive;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 값 처리 및 변환 유틸리티
 * 로깅을 위한 값 변환, 마스킹, 트렁케이션 처리
 */
@Slf4j
public final class ValueProcessor {

    private static final int MAX_STRING_LENGTH = 500;
    private static final int MAX_FIELD_LENGTH = 200;

    private static final String MASKED_VALUE = "****";
    private static final String EXCLUDED_VALUE = "<excluded>";

    private ValueProcessor() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 값 처리 (타입별 분기)
     */
    public static Object processValue(Object value) {
        if (value == null) {
            return null;
        }

        Class<?> valueClass = value.getClass();

        // 1. Primitive/Wrapper/String
        if (TypeChecker.isPrimitiveOrWrapper(valueClass) || value instanceof String) {
            return truncateString(String.valueOf(value), MAX_STRING_LENGTH);
        }

        // 2. Enum
        if (value instanceof Enum) {
            return ((Enum<?>) value).name();
        }

        // 3. Collection
        if (value instanceof Collection) {
            return processCollection((Collection<?>) value);
        }

        // 4. Map
        if (value instanceof Map) {
            return processMap((Map<?, ?>) value);
        }

        // 5. Array
        if (valueClass.isArray()) {
            return processArray(value);
        }

        // 6. Entity
        if (TypeChecker.isEntity(valueClass)) {
            return processEntity(value);
        }

        // 7. DTO/POJO
        if (TypeChecker.isDto(valueClass)) {
            return processDtoWithAnnotations(value);
        }

        // 8. 기타
        return truncateString(value.toString(), MAX_FIELD_LENGTH);
    }

    /**
     * Collection 처리
     * 3개 미만: 전부 표시
     * 3개 이상: 앞 3개만 표시 + "... (N more items)"
     */
    private static Object processCollection(Collection<?> collection) {
        if (collection.isEmpty()) {
            return List.of();
        }

        int size = collection.size();

        // 3개 미만이면 전부 표시
        if (size < 3) {
            List<Object> processed = new ArrayList<>();
            for (Object item : collection) {
                processed.add(processValue(item));
            }
            return processed;
        }

        // 3개 이상이면 앞 3개만 표시
        List<Object> processed = new ArrayList<>();
        int count = 0;

        for (Object item : collection) {
            if (count >= 3) {
                break;
            }
            processed.add(processValue(item));
            count++;
        }

        // 나머지 개수 표시
        processed.add("... (" + (size - 3) + " more items)");

        return processed;
    }

    /**
     * Map 처리
     * 3개 미만: 전부 표시
     * 3개 이상: 앞 3개만 표시 + "... (N more entries)"
     */
    private static Object processMap(Map<?, ?> map) {
        if (map.isEmpty()) {
            return Map.of();
        }

        int size = map.size();

        // 3개 미만이면 전부 표시
        if (size < 3) {
            Map<String, Object> processed = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                Object value = entry.getValue();
                processed.put(key, processValue(value));
            }
            return processed;
        }

        // 3개 이상이면 앞 3개만 표시
        Map<String, Object> processed = new LinkedHashMap<>();
        int count = 0;

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (count >= 3) {
                break;
            }
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            processed.put(key, processValue(value));
            count++;
        }

        // 나머지 개수 표시
        processed.put("...", "(" + (size - 3) + " more entries)");

        return processed;
    }

    /**
     * Array 처리
     * 3개 미만: 전부 표시
     * 3개 이상: 앞 3개만 표시 + "... (N more items)"
     */
    private static Object processArray(Object array) {
        int length = Array.getLength(array);

        if (length == 0) {
            return List.of();
        }

        List<Object> processed = new ArrayList<>();

        // 3개 미만이면 전부 표시
        if (length < 3) {
            for (int i = 0; i < length; i++) {
                Object item = Array.get(array, i);
                processed.add(processValue(item));
            }
            return processed;
        }

        // 3개 이상이면 앞 3개만 표시
        for (int i = 0; i < 3; i++) {
            Object item = Array.get(array, i);
            processed.add(processValue(item));
        }

        // 나머지 개수 표시
        processed.add("... (" + (length - 3) + " more items)");

        return processed;
    }

    /**
     * Entity 처리 (ID만 표시)
     */
    private static Object processEntity(Object entity) {
        try {
            Method getIdMethod = entity.getClass().getMethod("getId");
            Object id = getIdMethod.invoke(entity);
            return entity.getClass().getSimpleName() + "(id=" + id + ")";
        } catch (Exception e) {
            return entity.getClass().getSimpleName() + "(?)";
        }
    }

    /**
     * DTO를 Map으로 변환 (필드 어노테이션 체크)
     * @Sensitive, @ExcludeValue 어노테이션 처리
     */
    private static Object processDtoWithAnnotations(Object dto) {
        try {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            Class<?> clazz = dto.getClass();
            Field[] fields = clazz.getDeclaredFields();

            for (Field field : fields) {
                field.setAccessible(true);
                String fieldName = field.getName();

                // @ExcludeValue 체크
                if (field.isAnnotationPresent(ExcludeValue.class)) {
                    sanitized.put(fieldName, EXCLUDED_VALUE);
                    continue;
                }

                // @Sensitive 체크
                if (field.isAnnotationPresent(Sensitive.class)) {
                    sanitized.put(fieldName, MASKED_VALUE);
                    continue;
                }

                // 일반 필드
                try {
                    Object fieldValue = field.get(dto);
                    sanitized.put(fieldName, sanitizeFieldValue(fieldValue));
                } catch (IllegalAccessException e) {
                    sanitized.put(fieldName, "<error>");
                }
            }

            return sanitized;

        } catch (Exception e) {
            log.debug("DTO 변환 실패: {}", dto.getClass().getName(), e);
            return truncateString(dto.toString(), MAX_FIELD_LENGTH);
        }
    }

    /**
     * 필드 값 정리 (중첩 객체 간략화)
     */
    private static Object sanitizeFieldValue(Object value) {
        if (value == null) {
            return null;
        }

        // Primitive/Wrapper/String
        if (TypeChecker.isPrimitiveOrWrapper(value.getClass()) || value instanceof String) {
            return truncateString(String.valueOf(value), MAX_STRING_LENGTH);
        }

        // Collection
        if (value instanceof Collection) {
            Collection<?> collection = (Collection<?>) value;
            if (collection.size() > 10) {
                return collection.getClass().getSimpleName() + "[" + collection.size() + " items]";
            }
            return value;
        }

        // Map
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            if (map.size() > 10) {
                return "Map[" + map.size() + " entries]";
            }
            return value;
        }

        // 중첩 DTO
        return value.getClass().getSimpleName() + "[nested]";
    }

    /**
     * 문자열 자르기
     */
    public static String truncateString(String str, int maxLength) {
        if (str == null) {
            return null;
        }

        if (str.length() <= maxLength) {
            return str;
        }

        return str.substring(0, maxLength) + "... [truncated]";
    }

    /**
     * 마스킹 값 반환
     */
    public static String getMaskedValue() {
        return MASKED_VALUE;
    }

    /**
     * 제외 값 반환
     */
    public static String getExcludedValue() {
        return EXCLUDED_VALUE;
    }
}
