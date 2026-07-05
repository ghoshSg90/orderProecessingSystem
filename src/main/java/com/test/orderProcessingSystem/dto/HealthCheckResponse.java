package com.test.orderProcessingSystem.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
@Data
public class HealthCheckResponse {
    private STATUS status;

    public static enum STATUS{
        UP, DOWN
    }
}