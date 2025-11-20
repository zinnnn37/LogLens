package S13P31A306.loglens.global.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidationErrorDetail(
        String field,
        String rejectedValue,
        String code,
        String reason
) {
}
