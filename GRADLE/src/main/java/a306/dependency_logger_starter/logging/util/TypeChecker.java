package a306.dependency_logger_starter.logging.util;

/**
 * 타입 체크 유틸리티
 */
public final class TypeChecker {

    private TypeChecker() {
        throw new IllegalStateException();
    }

    /**
     * Primitive 또는 Wrapper 타입 체크
     */
    public static boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return clazz.isPrimitive() ||
                clazz == Boolean.class ||
                clazz == Byte.class ||
                clazz == Short.class ||
                clazz == Integer.class ||
                clazz == Long.class ||
                clazz == Float.class ||
                clazz == Double.class ||
                clazz == Character.class;
    }

    /**
     * Framework 클래스 체크
     */
    public static boolean isFrameworkClass(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }

        String className = clazz.getName();

        return className.startsWith("javax.servlet.") ||
                className.startsWith("jakarta.servlet.") ||
                className.startsWith("org.springframework.web.") ||
                className.startsWith("org.springframework.http.") ||
                className.startsWith("org.springframework.ui.") ||
                className.startsWith("org.springframework.validation.");
    }

    /**
     * Entity 체크 (JPA)
     */
    public static boolean isEntity(Class<?> clazz) {
        try {
            // Jakarta (Spring Boot 3.x)
            Class<?> entityAnnotation = Class.forName("jakarta.persistence.Entity");
            if (clazz.isAnnotationPresent((Class) entityAnnotation)) {
                return true;
            }
        } catch (ClassNotFoundException e) {
            // Jakarta 없음
        }

        try {
            // Javax (Spring Boot 2.x)
            Class<?> entityAnnotation = Class.forName("javax.persistence.Entity");
            if (clazz.isAnnotationPresent((Class) entityAnnotation)) {
                return true;
            }
        } catch (ClassNotFoundException e) {
            // Javax 없음
        }

        return false;
    }

    /**
     * DTO/POJO 체크
     * Java/Spring 기본 패키지가 아니고 Entity도 아닌 경우 DTO로 간주
     */
    public static boolean isDto(Class<?> clazz) {
        String packageName = clazz.getPackage() != null ? clazz.getPackage().getName() : "";

        // Java/Spring 기본 패키지 제외
        if (packageName.startsWith("java.") ||
                packageName.startsWith("javax.") ||
                packageName.startsWith("jakarta.") ||
                packageName.startsWith("org.springframework.")) {
            return false;
        }

        // Entity가 아니면 DTO로 간주
        return !isEntity(clazz);
    }
}
