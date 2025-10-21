package a306.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 메서드 실행을 추적하는 어노테이션
 * Controller, Service, Repository 메서드에 사용
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogMethodExecution {

    /**
     * 파라미터 로깅 여부
     */
    boolean logParams() default true;

    /**
     * 응답 로깅 여부
     */
    boolean logResponse() default true;

    /**
     * 실행 시간 로깅 여부
     */
    boolean logExecutionTime() default true;
}
