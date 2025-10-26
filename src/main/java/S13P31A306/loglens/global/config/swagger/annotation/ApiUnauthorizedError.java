package S13P31A306.loglens.global.config.swagger.annotation;

import S13P31A306.loglens.global.dto.response.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ApiResponse(
        responseCode = "401",
        description = "인증 실패 또는 토큰 없음/만료",
        content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                        name = "UnauthorizedExample",
                        summary = "인증 실패 예시",
                        value = """
                                {
                                  "code": 401,
                                  "message": "인증이 필요합니다.",
                                  "status": "UNAUTHORIZED",
                                  "timestamp": "2025-09-04T15:16:30Z"
                                }
                                """
                )
        )
)
public @interface ApiUnauthorizedError {
}
