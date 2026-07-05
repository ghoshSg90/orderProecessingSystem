package com.test.orderProcessingSystem.controller;

import com.test.orderProcessingSystem.dto.AdminOrderDetailResponse;
import com.test.orderProcessingSystem.dto.AdminOrderSummaryResponse;
import com.test.orderProcessingSystem.dto.ErrorResponse;
import com.test.orderProcessingSystem.dto.OrderDetailLineResponse;
import com.test.orderProcessingSystem.dto.UpdateOrderStatusRequest;
import com.test.orderProcessingSystem.entity.enums.OrderStatus;
import com.test.orderProcessingSystem.security.SecurityUtils;
import com.test.orderProcessingSystem.service.AdminOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Admin Order Services", description = " - APIs for support executives to manage all orders")
@Slf4j
@RestController
@RequestMapping("/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @Operation(summary = "List all orders",
            description = "Returns a paginated list of all orders, optionally filtered by status.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK - orders retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AdminOrderSummaryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - invalid status value",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PagedModel<AdminOrderSummaryResponse>> listAllOrders(
            @Parameter(description = "Optional order status filter (PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED)")
            @RequestParam(required = false) OrderStatus status,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        log.info("Admin accessed order list adminId={}", SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(new PagedModel<>(adminOrderService.listAllOrders(status, pageable)));
    }

    @Operation(summary = "Get any order",
            description = "Returns full details of any order, including line items.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK - order retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AdminOrderDetailResponse.class))),
            @ApiResponse(responseCode = "404", description = "Not found - order does not exist",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{orderId}")
    public ResponseEntity<AdminOrderDetailResponse> getOrder(
            @Parameter(required = true, description = "Id of the order") @PathVariable Long orderId) {
        return ResponseEntity.ok(adminOrderService.getOrder(orderId));
    }

    @Operation(summary = "Get an order detail line",
            description = "Returns a single ORDER_DETAILS line for the given order.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK - order detail retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderDetailLineResponse.class))),
            @ApiResponse(responseCode = "404", description = "Not found - order detail not found for this order",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{orderId}/{orderDetailsId}")
    public ResponseEntity<OrderDetailLineResponse> getOrderDetailLine(
            @Parameter(required = true, description = "Id of the order") @PathVariable Long orderId,
            @Parameter(required = true, description = "Id of the order-detail line") @PathVariable Long orderDetailsId) {
        return ResponseEntity.ok(adminOrderService.getOrderDetailLine(orderId, orderDetailsId));
    }

    @Operation(summary = "Update order status",
            description = "Transitions an order to a new status (e.g. PROCESSING, SHIPPED, DELIVERED).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK - status updated",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AdminOrderDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - invalid status value",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Not found - order does not exist",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<AdminOrderDetailResponse> updateOrderStatus(
            @Parameter(required = true, description = "Id of the order") @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(adminOrderService.updateOrderStatus(orderId, request));
    }
}
