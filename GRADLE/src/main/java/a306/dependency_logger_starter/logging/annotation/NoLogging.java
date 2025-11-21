package a306.dependency_logger_starter.logging.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

//@formatter:off
/**
 * 메서드 또는 클래스 단위로 로깅을 제외하는 어노테이션
 * 이 어노테이션이 붙은 메서드나 클래스는 MethodLoggingAspect에서 로깅되지 않습니다.
 *
 * <p>사용 예시:</p>
 * <pre>
 * // 메서드 단위 적용
 * {@code @NoLogging}
 * public void sensitiveMethod() { ... }
 *
 * // 클래스 단위 적용 (클래스 내 모든 메서드에 적용)
 * {@code @NoLogging}
 * {@code @Service}
 * public class SensitiveService { ... }
 * </pre>
 */
//@formatter:on
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NoLogging {
}
