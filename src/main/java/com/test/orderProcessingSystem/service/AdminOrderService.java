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
import com.test.orderProcessingSystem.exception.ResourceNotFoundException;
import com.test.orderProcessingSystem.repository.OrderDetailsRepository;
import com.test.orderProcessingSystem.repository.OrderHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final OrderHistoryRepository orderHistoryRepository;
    private final OrderDetailsRepository orderDetailsRepository;

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
        orderHistory.setOrderStatus(request.getOrderStatus());
        OrderHistory savedOrder = orderHistoryRepository.save(orderHistory);
        log.info("Order status updated orderId={} old={} new={}",
                orderId, oldStatus, savedOrder.getOrderStatus());
        return toAdminOrderDetailResponse(savedOrder);
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
