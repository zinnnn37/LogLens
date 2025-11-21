package S13P31A306.loglens.global.annotation;

import S13P31A306.loglens.global.validator.UuidValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * UUID 형식 검증 어노테이션
 * 문자열이 올바른 UUID 형식인지 검증합니다.
 *
 * 사용 예시:
 * <pre>
 * {@code
 * public record SomeRequest(
 *     @ValidUuid
 *     String projectUuid
 * ) {}
 * }
 * </pre>
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UuidValidator.class)
@Documented
public @interface ValidUuid {

    String message() default "유효하지 않은 UUID 형식입니다";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * null 값을 허용할지 여부
     * true일 경우 null은 유효한 값으로 간주
     */
    boolean nullable() default false;
}
