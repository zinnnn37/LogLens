package S13P31A306.loglens.global.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 필드나 매개변수에 이 어노테이션을 적용하면 로그 출력 시 해당 값이 제외됩니다.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcludeValue {
}
