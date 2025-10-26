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
        responseCode = "500",
        description = "서버 내부 오류",
        content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(
                        name = "InternalServerErrorExample",
                        summary = "서버 오류 예시",
                        value = """
                                {
                                  "code": G500,
                                  "message": "서버 내부 오류가 발생했습니다.",
                                  "status": 500,
                                  "timestamp": "2025-09-04T16:35:00Z"
                                }
                                """
                )
        )
)
public @interface ApiInternalServerError {
}
