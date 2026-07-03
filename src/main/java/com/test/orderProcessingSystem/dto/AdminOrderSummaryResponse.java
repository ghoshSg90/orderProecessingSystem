package com.test.orderProcessingSystem.dto;

import com.test.orderProcessingSystem.entity.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminOrderSummaryResponse {
    private Long orderId;
    private Long userId;
    private OrderStatus orderStatus;
    private Double totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
