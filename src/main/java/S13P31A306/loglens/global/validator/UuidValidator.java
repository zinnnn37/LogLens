package S13P31A306.loglens.global.validator;

import S13P31A306.loglens.global.annotation.ValidUuid;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * UUID 형식 검증 Validator
 * RFC 4122 표준 UUID 형식을 검증합니다.
 */
public class UuidValidator implements ConstraintValidator<ValidUuid, String> {

    /**
     * UUID 정규표현식 패턴
     * 형식: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
     * 예: 123e4567-e89b-12d3-a456-426614174000
     */
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );

    private boolean nullable;

    @Override
    public void initialize(ValidUuid constraintAnnotation) {
        this.nullable = constraintAnnotation.nullable();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (Objects.isNull(value)) {
            return nullable;
        }
        if (value.trim().isEmpty()) {
            return false;
        }
        return UUID_PATTERN.matcher(value).matches();
    }
}
