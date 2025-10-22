package S13P31A306.loglens.global.utils;

import S13P31A306.loglens.global.annotation.ExcludeValue;
import S13P31A306.loglens.global.annotation.Sensitive;
import S13P31A306.loglens.global.constants.LogMessages;
import S13P31A306.loglens.global.constants.SystemMessages;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * 메서드 시그니처 정보를 처리하는 유틸리티 클래스
 */
public final class MethodSignatureUtils {

    private static final String ARGUMENT_SEPARATOR = ", ";
    private static final String METHOD_SIGNATURE_FORMAT = "%s.%s(%s)";
    private static final String PARAMETER_FORMAT = "%s=%s";

    private MethodSignatureUtils() {
        throw new IllegalStateException(SystemMessages.UTILITY_CLASS_ERROR.message());
    }

    /**
     * 메서드 시그니처 문자열 `클래스명.메서드명(파라미터들)`을 생성합니다.
     */
    public static String formatMethodSignature(final ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        String arguments = formatArgumentsWithExclusions(joinPoint, signature);

        return String.format(METHOD_SIGNATURE_FORMAT, className, methodName, arguments);
    }

    /**
     * 메서드의 매개변수 값들을 문자열로 포맷팅합니다
     */
    private static String formatArgumentsWithExclusions(final ProceedingJoinPoint joinPoint,
                                                        final MethodSignature signature) {
        Object[] args = joinPoint.getArgs();
        Annotation[][] parameterAnnotations = signature.getMethod().getParameterAnnotations();
        String[] parameterNames = signature.getParameterNames();

        return formatArgumentList(args, parameterAnnotations, parameterNames).toString();
    }

    /**
     * 매개변수 배열을 문자열로 변환합니다
     */
    private static StringJoiner formatArgumentList(final Object[] args, final Annotation[][] parameterAnnotations,
                                                   final String[] parameterNames) {
        StringJoiner joiner = new StringJoiner(ARGUMENT_SEPARATOR);

        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (!isValidArgument(arg)) {
                continue;
            }

            Annotation[] annotations = parameterAnnotations[i];
            String argString;

            if (isExcluded(annotations)) {
                argString = LogMessages.LOG_EXCLUDED_VALUE.message();
            } else if (isSensitive(annotations)) {
                argString = LogMessages.LOG_MASKED_VALUE.message();
            } else {
                argString = convertArgToString(arg);
            }

            joiner.add(String.format(PARAMETER_FORMAT, parameterNames[i], argString));
        }
        return joiner;
    }

    /**
     * 주어진 파라미터에 @ExcludeFromLogging 어노테이션이 있는지 검사합니다.
     */
    private static boolean isExcluded(final Annotation[] annotations) {
        return Arrays.stream(annotations).anyMatch(a ->
                a.annotationType() == ExcludeValue.class || a.annotationType() == AuthenticationPrincipal.class
        );
    }

    /**
     * 주어진 파라미터에 @Sensitive 어노테이션이 있는지 검사합니다.
     */
    private static boolean isSensitive(final Annotation[] annotations) {
        return Arrays.stream(annotations).anyMatch(a -> a.annotationType() == Sensitive.class);
    }

    /**
     * 유효(null이 아니고 spring/apache 패키지가 아닌 경우)한 인자인지 확인합니다.
     */
    private static boolean isValidArgument(final Object arg) {
        return Objects.isNull(arg) || isNotSpringOrApachePackage(arg.getClass().getName());
    }

    /**
     * 주어진 클래스가 스프링 또는 아파치 패키지에 속하는지 확인합니다.
     */
    private static boolean isNotSpringOrApachePackage(final String className) {
        return !className.startsWith(LogMessages.SPRING_PACKAGE.message())
                && !className.startsWith(LogMessages.APACHE_PACKAGE.message());
    }

    /**
     * 인자를 문자열로 변환하고 마스킹 처리합니다.
     */
    private static String convertArgToString(final Object arg) {
        try {
            // MaskingUtils를 통한 마스킹 처리
            return MaskingUtils.mask(arg);
        } catch (Exception e) {
            return LogMessages.LOG_ARG_CONVERSION_FAILED.message();
        }
    }
}
