package com.test.orderProcessingSystem.service;

import com.test.orderProcessingSystem.dto.AddressResponse;
import com.test.orderProcessingSystem.dto.CreateOrderRequest;
import com.test.orderProcessingSystem.dto.OrderDetailResponse;
import com.test.orderProcessingSystem.dto.OrderItemRequest;
import com.test.orderProcessingSystem.dto.OrderItemResponse;
import com.test.orderProcessingSystem.dto.OrderSummaryResponse;
import com.test.orderProcessingSystem.entity.Address;
import com.test.orderProcessingSystem.entity.OrderDetails;
import com.test.orderProcessingSystem.entity.OrderHistory;
import com.test.orderProcessingSystem.entity.ProductDetails;
import com.test.orderProcessingSystem.entity.ProductInventory;
import com.test.orderProcessingSystem.entity.User;
import com.test.orderProcessingSystem.entity.enums.OrderStatus;
import com.test.orderProcessingSystem.exception.BadRequestException;
import com.test.orderProcessingSystem.exception.ResourceNotFoundException;
import com.test.orderProcessingSystem.repository.AddressRepository;
import com.test.orderProcessingSystem.repository.OrderHistoryRepository;
import com.test.orderProcessingSystem.repository.ProductDetailsRepository;
import com.test.orderProcessingSystem.repository.ProductInventoryRepository;
import com.test.orderProcessingSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderHistoryRepository orderHistoryRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final ProductDetailsRepository productDetailsRepository;
    private final ProductInventoryRepository productInventoryRepository;

    @Transactional
    public OrderDetailResponse createOrder(Long userId, CreateOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Address shippingAddress = addressRepository.findByAddressIdAndUser_UserId(request.getShippingAddressId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address not found with id: " + request.getShippingAddressId() + " for user: " + userId));

        OrderHistory orderHistory = new OrderHistory();
        orderHistory.setOrderStatus(OrderStatus.PENDING);
        orderHistory.setUser(user);
        orderHistory.setShippingAddress(shippingAddress);

        List<OrderDetails> orderDetailsList = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()) {
            ProductDetails productDetails = productDetailsRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found with id: " + itemRequest.getProductId()));

            ProductInventory productInventory = productInventoryRepository
                    .findByProductDetails_ProductId(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Inventory not found for product id: " + itemRequest.getProductId()));

            if (productInventory.getTotalAvailableUnits() < itemRequest.getQuantity()) {
                throw new BadRequestException("Insufficient stock for product: " + productDetails.getName());
            }

            productInventory.setTotalAvailableUnits(
                    productInventory.getTotalAvailableUnits() - itemRequest.getQuantity());
            productInventoryRepository.save(productInventory);

            BigDecimal priceAtPurchase = BigDecimal.valueOf(productDetails.getPricePerUnit());
            BigDecimal subtotal = priceAtPurchase.multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            orderDetailsList.add(OrderDetails.builder()
                    .orderHistory(orderHistory)
                    .productDetails(productDetails)
                    .quantity(itemRequest.getQuantity())
                    .priceAtPurchase(priceAtPurchase)
                    .subtotal(subtotal)
                    .build());
        }

        orderHistory.setTotalAmount(totalAmount.doubleValue());
        orderHistory.setOrderDetails(orderDetailsList);

        OrderHistory savedOrder = orderHistoryRepository.save(orderHistory);
        return toOrderDetailResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> listOrdersForUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        return orderHistoryRepository.findByUser_UserId(userId).stream()
                .map(this::toOrderSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderForUser(Long userId, Long orderId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        OrderHistory orderHistory = orderHistoryRepository.findByOrderIdAndUser_UserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + orderId + " for user: " + userId));

        return toOrderDetailResponse(orderHistory);
    }

    @Transactional
    public OrderDetailResponse cancelOrder(Long userId, Long orderId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        OrderHistory orderHistory = orderHistoryRepository.findByOrderIdAndUser_UserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + orderId + " for user: " + userId));

        if (orderHistory.getOrderStatus() != OrderStatus.PENDING) {
            throw new BadRequestException(
                    "Order cannot be cancelled because it is already " + orderHistory.getOrderStatus());
        }

        orderHistory.getOrderDetails().forEach(item -> {
            ProductInventory productInventory = productInventoryRepository
                    .findByProductDetails_ProductId(item.getProductDetails().getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Inventory not found for product id: " + item.getProductDetails().getProductId()));
            productInventory.setTotalAvailableUnits(
                    productInventory.getTotalAvailableUnits() + item.getQuantity());
            productInventoryRepository.save(productInventory);
        });

        orderHistory.setOrderStatus(OrderStatus.CANCELLED);
        OrderHistory savedOrder = orderHistoryRepository.save(orderHistory);
        return toOrderDetailResponse(savedOrder);
    }

    private OrderDetailResponse toOrderDetailResponse(OrderHistory orderHistory) {
        return OrderDetailResponse.builder()
                .orderId(orderHistory.getOrderId())
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

    private OrderSummaryResponse toOrderSummaryResponse(OrderHistory orderHistory) {
        return OrderSummaryResponse.builder()
                .orderId(orderHistory.getOrderId())
                .orderStatus(orderHistory.getOrderStatus())
                .totalAmount(orderHistory.getTotalAmount())
                .createdAt(orderHistory.getCreatedAt())
                .updatedAt(orderHistory.getUpdatedAt())
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
