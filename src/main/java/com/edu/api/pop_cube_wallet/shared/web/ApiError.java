package com.edu.api.pop_cube_wallet.shared.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Standardized API error response body.
 */
@Schema(description = "Standardized error response returned by all endpoints on failure")
public record ApiError(

        @Schema(description = "HTTP status code", example = "400")
        int status,

        @Schema(description = "Error category", example = "Bad Request")
        String error,

        @Schema(description = "Human-readable error detail", example = "CPF must have 11 digits")
        String message,

        @Schema(description = "UTC timestamp of the error", example = "2026-02-20T14:30:00.000")
        LocalDateTime timestamp
) {
    public static ApiError of(int status, String error, String message) {
        return new ApiError(status, error, message, LocalDateTime.now(ZoneOffset.UTC));
    }
}
