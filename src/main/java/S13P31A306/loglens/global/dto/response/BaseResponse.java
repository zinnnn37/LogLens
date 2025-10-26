package S13P31A306.loglens.global.dto.response;

import jakarta.validation.constraints.NotNull;

public interface BaseResponse {
    @NotNull
    int code();

    @NotNull
    String message();

    @NotNull
    String timestamp();
}
