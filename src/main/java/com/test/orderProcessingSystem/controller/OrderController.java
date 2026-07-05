package com.test.orderProcessingSystem.controller;

import com.test.orderProcessingSystem.dto.CreateOrderRequest;
import com.test.orderProcessingSystem.dto.ErrorResponse;
import com.test.orderProcessingSystem.dto.OrderDetailResponse;
import com.test.orderProcessingSystem.dto.OrderItemResponse;
import com.test.orderProcessingSystem.dto.OrderSummaryResponse;
import com.test.orderProcessingSystem.security.SecurityUtils;
import com.test.orderProcessingSystem.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Customer Order Services", description = " - APIs for customers to place, view and cancel orders")
@Slf4j
@RestController
@RequestMapping("/v1/users/")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Create an order",
            description = "Places a new order with one or more items for the authenticated customer.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created - order placed successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - validation failed or insufficient stock",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot order on behalf of another customer",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Not found - user, address, or product not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{userId}/orders")
    public ResponseEntity<OrderDetailResponse> createOrder(
            @Parameter(required = true, description = "Id of the caller's own account") @PathVariable Long userId,
            @Valid @RequestBody CreateOrderRequest request) {
        requireSelf(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(userId, request));
    }

    @Operation(summary = "List own orders",
            description = "Returns a paginated list of the authenticated customer's orders.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK - orders retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderSummaryResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot access another customer's orders",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Not found - user does not exist",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{userId}/orders")
    public ResponseEntity<PagedModel<OrderSummaryResponse>> listOrders(
            @Parameter(required = true, description = "Id of the caller's own account") @PathVariable Long userId,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        requireSelf(userId);
        log.info("Customer fetched own orders userId={}", userId);
        return ResponseEntity.ok(new PagedModel<>(orderService.listOrdersForUser(userId, pageable)));
    }

    @Operation(summary = "Get order details",
            description = "Returns one of the customer's orders with all its line items.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK - order retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderDetailResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot access another customer's orders",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Not found - order not found for this user",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{userId}/orders/{orderId}")
    public ResponseEntity<OrderDetailResponse> getOrder(
            @Parameter(required = true, description = "Id of the caller's own account") @PathVariable Long userId,
            @Parameter(required = true, description = "Id of the order to retrieve") @PathVariable Long orderId) {
        requireSelf(userId);
        return ResponseEntity.ok(orderService.getOrderForUser(userId, orderId));
    }

    @Operation(summary = "List order items",
            description = "Returns a paginated list of the products (ORDER_DETAILS) in one of the customer's orders.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK - order items retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderItemResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot access another customer's orders",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Not found - order not found for this user",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{userId}/orders/{orderId}/items")
    public ResponseEntity<PagedModel<OrderItemResponse>> getOrderItems(
            @Parameter(required = true, description = "Id of the caller's own account") @PathVariable Long userId,
            @Parameter(required = true, description = "Id of the order") @PathVariable Long orderId,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        requireSelf(userId);
        return ResponseEntity.ok(new PagedModel<>(orderService.getOrderItemsForUser(userId, orderId, pageable)));
    }

    @Operation(summary = "Cancel an order",
            description = "Cancels one of the customer's orders. Allowed only while the order is still PENDING.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK - order cancelled",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - order is no longer PENDING",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot cancel another customer's order",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Not found - order not found for this user",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{userId}/orders/{orderId}/cancel")
    public ResponseEntity<OrderDetailResponse> cancelOrder(
            @Parameter(required = true, description = "Id of the caller's own account") @PathVariable Long userId,
            @Parameter(required = true, description = "Id of the order to cancel") @PathVariable Long orderId) {
        requireSelf(userId);
        return ResponseEntity.ok(orderService.cancelOrder(userId, orderId));
    }

    private void requireSelf(Long userId) {
        if (!userId.equals(SecurityUtils.getCurrentUserId())) {
            throw new AccessDeniedException("You are not allowed to access another customer's orders");
        }
    }
}
