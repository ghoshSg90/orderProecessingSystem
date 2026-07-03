package com.test.orderProcessingSystem.controller;

import com.test.orderProcessingSystem.dto.CreateOrderRequest;
import com.test.orderProcessingSystem.dto.OrderDetailResponse;
import com.test.orderProcessingSystem.dto.OrderSummaryResponse;
import com.test.orderProcessingSystem.security.SecurityUtils;
import com.test.orderProcessingSystem.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/users/")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/{userId}/orders")
    public ResponseEntity<OrderDetailResponse> createOrder(
            @PathVariable Long userId,
            @Valid @RequestBody CreateOrderRequest request) {
        requireSelf(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(userId, request));
    }

    @GetMapping("/{userId}/orders")
    public ResponseEntity<List<OrderSummaryResponse>> listOrders(@PathVariable Long userId) {
        requireSelf(userId);
        return ResponseEntity.ok(orderService.listOrdersForUser(userId));
    }

    @GetMapping("/{userId}/orders/{orderId}")
    public ResponseEntity<OrderDetailResponse> getOrder(
            @PathVariable Long userId,
            @PathVariable Long orderId) {
        requireSelf(userId);
        return ResponseEntity.ok(orderService.getOrderForUser(userId, orderId));
    }

    @PutMapping("/{userId}/orders/{orderId}/cancel")
    public ResponseEntity<OrderDetailResponse> cancelOrder(
            @PathVariable Long userId,
            @PathVariable Long orderId) {
        requireSelf(userId);
        return ResponseEntity.ok(orderService.cancelOrder(userId, orderId));
    }

    private void requireSelf(Long userId) {
        if (!userId.equals(SecurityUtils.getCurrentUserId())) {
            throw new AccessDeniedException("You are not allowed to access another customer's orders");
        }
    }
}
