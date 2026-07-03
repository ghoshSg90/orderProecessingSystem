package com.test.orderProcessingSystem.dto;

import com.test.orderProcessingSystem.entity.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDetailResponse {
    private Long orderId;
    private OrderStatus orderStatus;
    private Double totalAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private AddressResponse shippingAddress;
    private List<OrderItemResponse> items;
}
