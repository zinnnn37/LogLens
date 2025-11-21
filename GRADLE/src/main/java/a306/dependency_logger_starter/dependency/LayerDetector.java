package a306.dependency_logger_starter.dependency;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

/**
 * Layer 감지 유틸리티
 *
 * 전략: Annotation 우선 → 인터페이스 체크
 */
public class LayerDetector {

    /**
     * Layer 감지
     *
     * @param clazz 판단할 클래스
     * @return CONTROLLER, SERVICE, REPOSITORY, COMPONENT, UNKNOWN
     */
    public static String detectLayer(Class<?> clazz) {
        // 1순위: Annotation 체크
        if (clazz.isAnnotationPresent(RestController.class)) {
            return "CONTROLLER";
        }
        if (clazz.isAnnotationPresent(Service.class)) {
            return "SERVICE";
        }
        if (clazz.isAnnotationPresent(Repository.class)) {
            return "REPOSITORY";
        }
        if (clazz.isAnnotationPresent(Component.class)) {
            return "COMPONENT";
        }

        // 2순위: 인터페이스 체크 (JpaRepository 등)
        String fromInterface = inferFromInterfaces(clazz);
        if (!fromInterface.equals("UNKNOWN")) {
            return fromInterface;
        }

        return "UNKNOWN";
    }

    /**
     * 인터페이스로 Layer 추론 (JpaRepository, CrudRepository 등)
     */
    private static String inferFromInterfaces(Class<?> clazz) {
        try {
            for (Class<?> interfaceClass : clazz.getInterfaces()) {
                String interfaceName = interfaceClass.getName();

                // JpaRepository, CrudRepository 등
                if (interfaceName.contains("Repository")) {
                    return "REPOSITORY";
                }

                // 재귀적으로 상위 인터페이스 체크
                String result = inferFromInterfaces(interfaceClass);
                if (!result.equals("UNKNOWN")) {
                    return result;
                }
            }
        } catch (Exception e) {
            // 무시
        }
        return "UNKNOWN";
    }
}
