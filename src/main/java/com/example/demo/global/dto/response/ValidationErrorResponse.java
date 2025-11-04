package com.example.demo.global.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidationErrorResponse(
        String path,
        List<ValidationErrorDetail> errors
) {
}
