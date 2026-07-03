package com.test.orderProcessingSystem.controller;

import com.test.orderProcessingSystem.dto.AdminOrderDetailResponse;
import com.test.orderProcessingSystem.dto.AdminOrderSummaryResponse;
import com.test.orderProcessingSystem.dto.OrderDetailLineResponse;
import com.test.orderProcessingSystem.dto.UpdateOrderStatusRequest;
import com.test.orderProcessingSystem.entity.enums.OrderStatus;
import com.test.orderProcessingSystem.security.SecurityUtils;
import com.test.orderProcessingSystem.service.AdminOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping
    public ResponseEntity<PagedModel<AdminOrderSummaryResponse>> listAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 10) Pageable pageable) {
        log.info("Admin accessed order list adminId={}", SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(new PagedModel<>(adminOrderService.listAllOrders(status, pageable)));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<AdminOrderDetailResponse> getOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(adminOrderService.getOrder(orderId));
    }

    @GetMapping("/{orderId}/{orderDetailsId}")
    public ResponseEntity<OrderDetailLineResponse> getOrderDetailLine(
            @PathVariable Long orderId,
            @PathVariable Long orderDetailsId) {
        return ResponseEntity.ok(adminOrderService.getOrderDetailLine(orderId, orderDetailsId));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<AdminOrderDetailResponse> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(adminOrderService.updateOrderStatus(orderId, request));
    }
}
