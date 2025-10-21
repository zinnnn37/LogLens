package a306.dependency_logger_starter.dependency;

import a306.dependency_logger_starter.dependency.client.DependencyLogSender;
import a306.dependency_logger_starter.dependency.dto.Component;
import a306.dependency_logger_starter.dependency.dto.DependencyRelation;
import a306.dependency_logger_starter.dependency.dto.ProjectDependencyInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * 애플리케이션 시작 완료 후 의존성 수집 (Batch 방식)
 */
@Slf4j
@RequiredArgsConstructor
public class DependencyCollector {

    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;
    private final DependencyLogSender sender;

    @Value("${spring.application.name:unknown-project}")
    private String projectName;

    /**
     * 애플리케이션 준비 완료 후 실행
     */
    @EventListener(ApplicationReadyEvent.class)
    public void collectDependencies() {
        log.info("🚀 의존성 수집 시작...");

        Map<String, Component> componentMap = new LinkedHashMap<>();
        List<DependencyRelation> relations = new ArrayList<>();

        // 1. Controller 수집
        collectBeansWithAnnotation(RestController.class, componentMap, relations);

        // 2. Service 수집
        collectBeansWithAnnotation(Service.class, componentMap, relations);

        // 3. Repository 수집
        collectBeansWithAnnotation(Repository.class, componentMap, relations);

        log.info("✅ 의존성 수집 완료! (컴포넌트: {}, 관계: {})",
                componentMap.size(), relations.size());

        // 4. 한 번에 전송
        ProjectDependencyInfo projectInfo = new ProjectDependencyInfo(
                projectName,
                new ArrayList<>(componentMap.values()),
                relations
        );

        try {
            log.debug("전송 데이터: {}", objectMapper.writeValueAsString(projectInfo));
        } catch (Exception e) {
            log.error("JSON 변환 실패", e);
        }

        sender.sendProjectDependencies(projectInfo);
    }

    /**
     * 특정 어노테이션이 붙은 Bean들 수집
     */
    private void collectBeansWithAnnotation(
            Class<? extends java.lang.annotation.Annotation> annotationClass,
            Map<String, Component> componentMap,
            List<DependencyRelation> relations) {

        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(annotationClass);

        // ===== 디버깅: beans 전체 출력 =====
        log.info("🔍 {} 어노테이션으로 찾은 Bean 개수: {}",
                annotationClass.getSimpleName(), beans.size());

        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            String beanName = entry.getKey();
            Object bean = entry.getValue();
            Class<?> beanClass = bean.getClass();
            Class<?> userClass = ClassUtils.getUserClass(beanClass);

            log.info("  📦 Bean Name: {}", beanName);
            log.info("     - Bean Class: {}", beanClass.getName());
            log.info("     - User Class: {}", userClass.getName());
            log.info("     - Is Proxy?: {}", isProxyClass(userClass));
        }
        log.info("==========================================");
        // ===== 디버깅 끝 =====

        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            Object bean = entry.getValue();
            Class<?> targetClass = ClassUtils.getUserClass(bean.getClass());

            // 프록시인 경우 인터페이스 찾기
            if (isProxyClass(targetClass)) {
                log.debug("⚠️ 프록시 감지: {} → 인터페이스 추출 시도", targetClass.getSimpleName());

                // 프록시의 인터페이스들 확인
                Class<?>[] interfaces = bean.getClass().getInterfaces();
                targetClass = null;

                for (Class<?> intf : interfaces) {
                    // Spring 내부 인터페이스 제외
                    if (!intf.getName().startsWith("org.springframework") &&
                            !intf.getName().startsWith("java.")) {
                        targetClass = intf;
                        log.debug("  ✅ 인터페이스 찾음: {}", targetClass.getName());
                        break;
                    }
                }

                if (targetClass == null) {
                    log.warn("  ❌ 유효한 인터페이스를 찾지 못함. 스킵.");
                    continue;
                }
            }

            String componentKey = getComponentKey(targetClass);

            // 중복 체크
            boolean alreadyCollected = componentMap.containsKey(componentKey);

            if (alreadyCollected) {
                log.debug("⏭️ 이미 수집된 컴포넌트 발견: {} → 의존성만 추가 분석", targetClass.getSimpleName());
            } else {
                log.info("🔍 의존성 수집: {}", targetClass.getSimpleName());

                // 컴포넌트 생성
                Component component = new Component(
                        targetClass.getSimpleName(),
                        targetClass.getSimpleName(),
                        targetClass.getPackage().getName(),
                        LayerDetector.detectLayer(targetClass)
                );

                componentMap.put(componentKey, component);
                log.info("  📦 컴포넌트: {} ({})", component.name(), component.layer());
            }

            // 의존성 수집 (중복이어도 항상 실행!)
            Component currentComponent = componentMap.get(componentKey);
            List<Component> dependencies = collectDependenciesForBean(bean, targetClass, componentMap);

            log.info("  ➡️ 의존성 개수: {}", dependencies.size());

            // 관계 추가
            for (Component dep : dependencies) {
                DependencyRelation relation = new DependencyRelation(
                        currentComponent.name(),
                        dep.name()
                );

                // 중복 관계 체크
                boolean relationExists = relations.stream()
                        .anyMatch(r -> r.from().equals(relation.from()) && r.to().equals(relation.to()));

                if (!relationExists) {
                    relations.add(relation);
                    log.info("    - {} → {}", currentComponent.name(), dep.name());
                }
            }
        }
    }

    /**
     * 개별 Bean의 의존성 수집
     */
    private List<Component> collectDependenciesForBean(
            Object bean,
            Class<?> targetClass,
            Map<String, Component> componentMap) {

        List<Component> dependencies = new ArrayList<>();

        // 생성자 파라미터 분석 (final 필드 지원)
        log.debug("  🔧 생성자 파라미터 분석 시작...");
        dependencies.addAll(collectFromConstructor(targetClass, componentMap));

        return dependencies;
    }

    /**
     * 생성자 파라미터에서 의존성 수집
     */
    private List<Component> collectFromConstructor(
            Class<?> targetClass,
            Map<String, Component> componentMap) {

        List<Component> dependencies = new ArrayList<>();
        Constructor<?>[] constructors = targetClass.getDeclaredConstructors();

        for (Constructor<?> constructor : constructors) {
            Parameter[] parameters = constructor.getParameters();

            log.debug("    🏗️ 생성자 파라미터 {} 개", parameters.length);

            for (Parameter param : parameters) {
                Class<?> paramType = param.getType();
                String typeName = paramType.getSimpleName();

                log.debug("      🔍 파라미터 타입: {}", typeName);

                if (!isServiceOrRepositoryType(typeName, paramType)) {
                    log.debug("        ❌ Service/Repository 아님");
                    continue;
                }

                try {
                    Object bean = applicationContext.getBean(paramType);
                    Component dep = createDependencyComponent(bean, paramType, componentMap);

                    if (dep != null) {
                        dependencies.add(dep);
                        log.info("        ✅ 의존성 추가: {} ({})", dep.name(), dep.layer());
                    }

                } catch (Exception e) {
                    log.debug("        ⚠️ Bean 조회 실패: {} - {}", typeName, e.getMessage());
                }
            }
        }

        return dependencies;
    }

    /**
     * 의존성 Component 생성
     */
    private Component createDependencyComponent(
            Object bean,
            Class<?> interfaceType,
            Map<String, Component> componentMap) {

        Class<?> depClass = ClassUtils.getUserClass(bean.getClass());

        // 프록시인 경우 인터페이스 정보 사용
        if (isProxyClass(depClass)) {
            log.debug("        ⚠️ 프록시 감지 → 인터페이스 사용: {}", interfaceType.getSimpleName());

            Component dep = new Component(
                    interfaceType.getSimpleName(),
                    interfaceType.getSimpleName(),
                    interfaceType.getPackage().getName(),
                    LayerDetector.detectLayer(interfaceType)
            );

            // componentMap에 추가
            String key = getComponentKey(interfaceType);
            componentMap.putIfAbsent(key, dep);

            return dep;
        }

        // 일반 클래스
        Component dep = new Component(
                depClass.getSimpleName(),
                depClass.getSimpleName(),
                depClass.getPackage().getName(),
                LayerDetector.detectLayer(depClass)
        );

        // componentMap에 추가
        String key = getComponentKey(depClass);
        componentMap.putIfAbsent(key, dep);

        return dep;
    }

    /**
     * 컴포넌트 고유 키 생성
     */
    private String getComponentKey(Class<?> clazz) {
        return clazz.getPackage().getName() + "." + clazz.getSimpleName();
    }

    /**
     * Service/Repository 체크 (인스턴스 기반)
     */
    private boolean isServiceOrRepository(String typeName, Object instance) {
        if (typeName.contains("Service") || typeName.contains("Repository")) {
            return true;
        }

        Class<?> clazz = ClassUtils.getUserClass(instance.getClass());
        return AnnotationUtils.findAnnotation(clazz, Service.class) != null
                || AnnotationUtils.findAnnotation(clazz, Repository.class) != null;
    }

    /**
     * Service/Repository 체크 (타입 기반)
     */
    private boolean isServiceOrRepositoryType(String typeName, Class<?> type) {
        if (typeName.contains("Service") || typeName.contains("Repository")) {
            log.debug("        ✅ 타입명으로 Service/Repository 확인: {}", typeName);
            return true;
        }

        boolean hasServiceAnnotation = AnnotationUtils.findAnnotation(type, Service.class) != null;
        boolean hasRepoAnnotation = AnnotationUtils.findAnnotation(type, Repository.class) != null;

        if (hasServiceAnnotation || hasRepoAnnotation) {
            log.debug("        ✅ 어노테이션으로 Service/Repository 확인: {}", typeName);
            return true;
        }

        if (isJpaRepository(type)) {
            log.debug("        ✅ JpaRepository 상속으로 Repository 확인: {}", typeName);
            return true;
        }

        return false;
    }

    /**
     * JpaRepository 상속 체크
     */
    private boolean isJpaRepository(Class<?> type) {
        try {
            for (Class<?> interfaceClass : type.getInterfaces()) {
                String interfaceName = interfaceClass.getName();
                if (interfaceName.contains("JpaRepository") ||
                        interfaceName.contains("CrudRepository") ||
                        interfaceName.contains("Repository")) {
                    return true;
                }
                if (isJpaRepository(interfaceClass)) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.debug("        JpaRepository 체크 실패: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 프록시 클래스 체크
     */
    private boolean isProxyClass(Class<?> clazz) {
        String className = clazz.getSimpleName();
        return className.contains("$$")
                || className.contains("$Proxy")
                || className.startsWith("$")
                || clazz.getPackage() != null && clazz.getPackage().getName().startsWith("jdk.proxy");
    }
}
