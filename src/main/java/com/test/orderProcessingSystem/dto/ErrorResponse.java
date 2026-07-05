package com.test.orderProcessingSystem.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standard error payload returned by {@code GlobalExceptionHandler} and the security handlers.
 * Documented here so it can be referenced as the schema for error responses in the API docs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard error response")
public class ErrorResponse {

    @Schema(description = "When the error occurred", example = "2026-07-04T10:15:30")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP status code", example = "404")
    private int status;

    @Schema(description = "Human-readable error message", example = "Order not found with id: 9999 for user: 2")
    private String message;
}
