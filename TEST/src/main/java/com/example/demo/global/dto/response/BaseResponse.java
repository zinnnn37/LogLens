package com.example.demo.global.dto.response;

import jakarta.validation.constraints.NotNull;

public interface BaseResponse {
    @NotNull
    String code();

    @NotNull
    String message();

    @NotNull
    int status();

    @NotNull
    String timestamp();
}
