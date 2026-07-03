package com.test.orderProcessingSystem.dto;

import com.test.orderProcessingSystem.entity.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusRequest {
    @NotNull
    private OrderStatus orderStatus;
}
