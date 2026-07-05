package com.test.orderProcessingSystem.service;

import com.test.orderProcessingSystem.dto.AddressResponse;
import com.test.orderProcessingSystem.dto.AdminOrderDetailResponse;
import com.test.orderProcessingSystem.dto.AdminOrderSummaryResponse;
import com.test.orderProcessingSystem.dto.OrderDetailLineResponse;
import com.test.orderProcessingSystem.dto.OrderItemResponse;
import com.test.orderProcessingSystem.dto.UpdateOrderStatusRequest;
import com.test.orderProcessingSystem.entity.Address;
import com.test.orderProcessingSystem.entity.OrderDetails;
import com.test.orderProcessingSystem.entity.OrderHistory;
import com.test.orderProcessingSystem.entity.enums.OrderStatus;
import com.test.orderProcessingSystem.exception.BadRequestException;
import com.test.orderProcessingSystem.exception.ResourceNotFoundException;
import com.test.orderProcessingSystem.repository.OrderDetailsRepository;
import com.test.orderProcessingSystem.repository.OrderHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final OrderHistoryRepository orderHistoryRepository;
    private final OrderDetailsRepository orderDetailsRepository;

    // Allowed manual (admin) status transitions. A status mapped to an empty set is terminal.
    //   PENDING    -> PROCESSING, CANCELLED
    //   PROCESSING -> SHIPPED (only)
    //   SHIPPED    -> DELIVERED, PROCESSING (the latter to correct a premature shipment)
    //   DELIVERED  -> (terminal)
    //   CANCELLED  -> (terminal)
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            OrderStatus.PENDING, EnumSet.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED),
            OrderStatus.PROCESSING, EnumSet.of(OrderStatus.SHIPPED),
            OrderStatus.SHIPPED, EnumSet.of(OrderStatus.DELIVERED, OrderStatus.PROCESSING),
            OrderStatus.DELIVERED, EnumSet.noneOf(OrderStatus.class),
            OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class)
    );

    @Transactional(readOnly = true)
    public Page<AdminOrderSummaryResponse> listAllOrders(OrderStatus statusFilter, Pageable pageable) {
        log.debug("Fetching orders (admin) page={} size={} status={}",
                pageable.getPageNumber(), pageable.getPageSize(), statusFilter);
        Page<OrderHistory> orders = statusFilter == null
                ? orderHistoryRepository.findAll(pageable)
                : orderHistoryRepository.findByOrderStatus(statusFilter, pageable);

        return orders.map(this::toAdminOrderSummaryResponse);
    }

    @Transactional(readOnly = true)
    public AdminOrderDetailResponse getOrder(Long orderId) {
        log.debug("Fetching order (admin) orderId={}", orderId);
        OrderHistory orderHistory = orderHistoryRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        return toAdminOrderDetailResponse(orderHistory);
    }

    @Transactional(readOnly = true)
    public OrderDetailLineResponse getOrderDetailLine(Long orderId, Long orderDetailsId) {
        log.debug("Fetching order detail line orderId={} orderDetailsId={}", orderId, orderDetailsId);
        OrderDetails orderDetails = orderDetailsRepository
                .findByOrderDetailsIdAndOrderHistory_OrderId(orderDetailsId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order detail not found with id: " + orderDetailsId + " for order: " + orderId));

        return toOrderDetailLineResponse(orderDetails);
    }

    @Transactional
    public AdminOrderDetailResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request) {
        OrderHistory orderHistory = orderHistoryRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        OrderStatus oldStatus = orderHistory.getOrderStatus();
        OrderStatus newStatus = request.getOrderStatus();

        validateTransition(orderId, oldStatus, newStatus);

        orderHistory.setOrderStatus(newStatus);
        OrderHistory savedOrder = orderHistoryRepository.save(orderHistory);
        log.info("Order status updated orderId={} old={} new={}",
                orderId, oldStatus, savedOrder.getOrderStatus());
        return toAdminOrderDetailResponse(savedOrder);
    }

    /**
     * Validates a manual (admin) status change against {@link #ALLOWED_TRANSITIONS}, throwing a 400
     * with a state-specific message when the transition is not permitted. Setting a status to itself
     * is treated as a no-op and allowed.
     */
    private void validateTransition(Long orderId, OrderStatus from, OrderStatus to) {
        if (from == to) {
            return;
        }
        if (ALLOWED_TRANSITIONS.getOrDefault(from, EnumSet.noneOf(OrderStatus.class)).contains(to)) {
            return;
        }
        // Not allowed — surface the most helpful message for the current state.
        if (from == OrderStatus.DELIVERED) {
            throw new BadRequestException(
                    "Order " + orderId + " is already delivered and cannot be moved to another state");
        }
        if (from == OrderStatus.CANCELLED) {
            throw new BadRequestException(
                    "Order " + orderId + " is cancelled and cannot be moved to another state");
        }
        if (from == OrderStatus.PROCESSING && to == OrderStatus.DELIVERED) {
            throw new BadRequestException("Order " + orderId + " can only be moved to shipped");
        }
        throw new BadRequestException(
                "Order " + orderId + " cannot be moved from " + from + " to " + to);
    }

    @Transactional
    public int movePendingOrdersToProcessing() {
        List<OrderHistory> pendingOrders = orderHistoryRepository.findByOrderStatus(OrderStatus.PENDING);
        if (!pendingOrders.isEmpty()) {
            log.info("Pending orders found count={}", pendingOrders.size());
        }
        pendingOrders.forEach(order -> {
            log.debug("Order moved orderId={} status=PENDING->PROCESSING", order.getOrderId());
            order.setOrderStatus(OrderStatus.PROCESSING);
        });
        orderHistoryRepository.saveAll(pendingOrders);
        return pendingOrders.size();
    }

    private AdminOrderSummaryResponse toAdminOrderSummaryResponse(OrderHistory orderHistory) {
        return AdminOrderSummaryResponse.builder()
                .orderId(orderHistory.getOrderId())
                .userId(orderHistory.getUser().getUserId())
                .orderStatus(orderHistory.getOrderStatus())
                .totalAmount(orderHistory.getTotalAmount())
                .createdAt(orderHistory.getCreatedAt())
                .updatedAt(orderHistory.getUpdatedAt())
                .build();
    }

    private AdminOrderDetailResponse toAdminOrderDetailResponse(OrderHistory orderHistory) {
        return AdminOrderDetailResponse.builder()
                .orderId(orderHistory.getOrderId())
                .userId(orderHistory.getUser().getUserId())
                .orderStatus(orderHistory.getOrderStatus())
                .totalAmount(orderHistory.getTotalAmount())
                .createdAt(orderHistory.getCreatedAt())
                .updatedAt(orderHistory.getUpdatedAt())
                .shippingAddress(toAddressResponse(orderHistory.getShippingAddress()))
                .items(orderHistory.getOrderDetails().stream()
                        .map(item -> OrderItemResponse.builder()
                                .orderDetailsId(item.getOrderDetailsId())
                                .productId(item.getProductDetails().getProductId())
                                .productName(item.getProductDetails().getName())
                                .quantity(item.getQuantity())
                                .priceAtPurchase(item.getPriceAtPurchase())
                                .subtotal(item.getSubtotal())
                                .build())
                        .toList())
                .build();
    }

    private OrderDetailLineResponse toOrderDetailLineResponse(OrderDetails orderDetails) {
        return OrderDetailLineResponse.builder()
                .orderDetailsId(orderDetails.getOrderDetailsId())
                .orderId(orderDetails.getOrderHistory().getOrderId())
                .productId(orderDetails.getProductDetails().getProductId())
                .productName(orderDetails.getProductDetails().getName())
                .quantity(orderDetails.getQuantity())
                .priceAtPurchase(orderDetails.getPriceAtPurchase())
                .subtotal(orderDetails.getSubtotal())
                .build();
    }

    private AddressResponse toAddressResponse(Address address) {
        return AddressResponse.builder()
                .addressId(address.getAddressId())
                .line1(address.getLine1())
                .line2(address.getLine2())
                .city(address.getCity())
                .state(address.getState())
                .country(address.getCountry())
                .postalCode(address.getPostalCode())
                .addressType(address.getAddressType())
                .build();
    }
}
