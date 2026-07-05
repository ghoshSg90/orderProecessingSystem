package com.test.orderProcessingSystem.service;

import com.test.orderProcessingSystem.dto.AdminOrderDetailResponse;
import com.test.orderProcessingSystem.dto.AdminOrderSummaryResponse;
import com.test.orderProcessingSystem.dto.OrderDetailLineResponse;
import com.test.orderProcessingSystem.dto.UpdateOrderStatusRequest;
import com.test.orderProcessingSystem.entity.Address;
import com.test.orderProcessingSystem.entity.OrderDetails;
import com.test.orderProcessingSystem.entity.OrderHistory;
import com.test.orderProcessingSystem.entity.ProductDetails;
import com.test.orderProcessingSystem.entity.User;
import com.test.orderProcessingSystem.entity.enums.AddressType;
import com.test.orderProcessingSystem.entity.enums.OrderStatus;
import com.test.orderProcessingSystem.exception.BadRequestException;
import com.test.orderProcessingSystem.exception.ResourceNotFoundException;
import com.test.orderProcessingSystem.repository.OrderDetailsRepository;
import com.test.orderProcessingSystem.repository.OrderHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceTest {

    @Mock
    private OrderHistoryRepository orderHistoryRepository;
    @Mock
    private OrderDetailsRepository orderDetailsRepository;

    @InjectMocks
    private AdminOrderService adminOrderService;

    private static final Long USER_ID = 2L;
    private static final Long ORDER_ID = 10L;
    private static final Long ORDER_DETAILS_ID = 500L;
    private static final Long PRODUCT_ID = 1L;

    private User user;
    private Address address;
    private ProductDetails productDetails;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(USER_ID);
        user.setUserName("sghosh");

        address = new Address();
        address.setAddressId(1L);
        address.setLine1("45 Lake View Road");
        address.setCity("Bengaluru");
        address.setState("Karnataka");
        address.setCountry("India");
        address.setPostalCode("560001");
        address.setAddressType(AddressType.HOME);
        address.setUser(user);

        productDetails = new ProductDetails();
        productDetails.setProductId(PRODUCT_ID);
        productDetails.setName("Clean Code");
        productDetails.setPricePerUnit(45.99);
    }

    private OrderHistory orderWithStatus(OrderStatus status) {
        OrderHistory order = new OrderHistory();
        order.setOrderId(ORDER_ID);
        order.setOrderStatus(status);
        order.setUser(user);
        order.setShippingAddress(address);
        order.setTotalAmount(91.98);

        OrderDetails item = OrderDetails.builder()
                .orderDetailsId(ORDER_DETAILS_ID)
                .orderHistory(order)
                .productDetails(productDetails)
                .quantity(2)
                .priceAtPurchase(BigDecimal.valueOf(45.99))
                .subtotal(BigDecimal.valueOf(91.98))
                .build();

        order.setOrderDetails(new ArrayList<>(List.of(item)));
        return order;
    }

    // ---------- listAllOrders ----------

    @Test
    void listAllOrders_noFilter_usesPagedFindAll() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<OrderHistory> page = new PageImpl<>(
                List.of(orderWithStatus(OrderStatus.PENDING), orderWithStatus(OrderStatus.SHIPPED)),
                pageable, 2);
        when(orderHistoryRepository.findAll(pageable)).thenReturn(page);

        Page<AdminOrderSummaryResponse> result = adminOrderService.listAllOrders(null, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(10);
        verify(orderHistoryRepository).findAll(pageable);
        verify(orderHistoryRepository, never())
                .findByOrderStatus(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void listAllOrders_withFilter_usesPagedFindByStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<OrderHistory> page = new PageImpl<>(List.of(orderWithStatus(OrderStatus.PENDING)), pageable, 1);
        when(orderHistoryRepository.findByOrderStatus(OrderStatus.PENDING, pageable)).thenReturn(page);

        Page<AdminOrderSummaryResponse> result = adminOrderService.listAllOrders(OrderStatus.PENDING, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.getContent().get(0).getUserId()).isEqualTo(USER_ID);
        verify(orderHistoryRepository).findByOrderStatus(OrderStatus.PENDING, pageable);
        verify(orderHistoryRepository, never()).findAll(org.mockito.ArgumentMatchers.any(Pageable.class));
    }

    // ---------- getOrder ----------

    @Test
    void getOrder_success_returnsFullDetail() {
        when(orderHistoryRepository.findById(ORDER_ID)).thenReturn(Optional.of(orderWithStatus(OrderStatus.PENDING)));

        AdminOrderDetailResponse response = adminOrderService.getOrder(ORDER_ID);

        assertThat(response.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(response.getUserId()).isEqualTo(USER_ID);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getShippingAddress().getCity()).isEqualTo("Bengaluru");
    }

    @Test
    void getOrder_notFound_throws() {
        when(orderHistoryRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminOrderService.getOrder(ORDER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found");
    }

    // ---------- getOrderDetailLine ----------

    @Test
    void getOrderDetailLine_success_returnsLine() {
        OrderDetails line = orderWithStatus(OrderStatus.PENDING).getOrderDetails().get(0);
        when(orderDetailsRepository.findByOrderDetailsIdAndOrderHistory_OrderId(ORDER_DETAILS_ID, ORDER_ID))
                .thenReturn(Optional.of(line));

        OrderDetailLineResponse response = adminOrderService.getOrderDetailLine(ORDER_ID, ORDER_DETAILS_ID);

        assertThat(response.getOrderDetailsId()).isEqualTo(ORDER_DETAILS_ID);
        assertThat(response.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(response.getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(response.getQuantity()).isEqualTo(2);
    }

    @Test
    void getOrderDetailLine_notFound_throws() {
        when(orderDetailsRepository.findByOrderDetailsIdAndOrderHistory_OrderId(ORDER_DETAILS_ID, ORDER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminOrderService.getOrderDetailLine(ORDER_ID, ORDER_DETAILS_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order detail not found");
    }

    // ---------- updateOrderStatus ----------

    @Test
    void updateOrderStatus_success_updatesAndReturns() {
        OrderHistory order = orderWithStatus(OrderStatus.PROCESSING);
        when(orderHistoryRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderHistoryRepository.save(order)).thenAnswer(inv -> inv.getArgument(0));

        AdminOrderDetailResponse response =
                adminOrderService.updateOrderStatus(ORDER_ID, new UpdateOrderStatusRequest(OrderStatus.SHIPPED));

        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.SHIPPED);

        ArgumentCaptor<OrderHistory> captor = ArgumentCaptor.forClass(OrderHistory.class);
        verify(orderHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getOrderStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void updateOrderStatus_notFound_throws() {
        when(orderHistoryRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                adminOrderService.updateOrderStatus(ORDER_ID, new UpdateOrderStatusRequest(OrderStatus.SHIPPED)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found");

        verify(orderHistoryRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    // ---------- updateOrderStatus: transition rules ----------

    @Test
    void updateOrderStatus_shippedToDelivered_allowed() {
        OrderHistory order = orderWithStatus(OrderStatus.SHIPPED);
        when(orderHistoryRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderHistoryRepository.save(order)).thenAnswer(inv -> inv.getArgument(0));

        AdminOrderDetailResponse response =
                adminOrderService.updateOrderStatus(ORDER_ID, new UpdateOrderStatusRequest(OrderStatus.DELIVERED));

        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void updateOrderStatus_shippedToProcessing_allowed() {
        OrderHistory order = orderWithStatus(OrderStatus.SHIPPED);
        when(orderHistoryRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderHistoryRepository.save(order)).thenAnswer(inv -> inv.getArgument(0));

        AdminOrderDetailResponse response =
                adminOrderService.updateOrderStatus(ORDER_ID, new UpdateOrderStatusRequest(OrderStatus.PROCESSING));

        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    void updateOrderStatus_pendingToCancelled_allowed() {
        OrderHistory order = orderWithStatus(OrderStatus.PENDING);
        when(orderHistoryRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderHistoryRepository.save(order)).thenAnswer(inv -> inv.getArgument(0));

        AdminOrderDetailResponse response =
                adminOrderService.updateOrderStatus(ORDER_ID, new UpdateOrderStatusRequest(OrderStatus.CANCELLED));

        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void updateOrderStatus_processingToDelivered_rejectedWithShippedMessage() {
        when(orderHistoryRepository.findById(ORDER_ID)).thenReturn(Optional.of(orderWithStatus(OrderStatus.PROCESSING)));

        assertThatThrownBy(() ->
                adminOrderService.updateOrderStatus(ORDER_ID, new UpdateOrderStatusRequest(OrderStatus.DELIVERED)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("can only be moved to shipped");

        verify(orderHistoryRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateOrderStatus_deliveredIsTerminal_rejected() {
        when(orderHistoryRepository.findById(ORDER_ID)).thenReturn(Optional.of(orderWithStatus(OrderStatus.DELIVERED)));

        assertThatThrownBy(() ->
                adminOrderService.updateOrderStatus(ORDER_ID, new UpdateOrderStatusRequest(OrderStatus.SHIPPED)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already delivered");

        verify(orderHistoryRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateOrderStatus_cancelledIsTerminal_rejected() {
        when(orderHistoryRepository.findById(ORDER_ID)).thenReturn(Optional.of(orderWithStatus(OrderStatus.CANCELLED)));

        assertThatThrownBy(() ->
                adminOrderService.updateOrderStatus(ORDER_ID, new UpdateOrderStatusRequest(OrderStatus.PROCESSING)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("is cancelled");

        verify(orderHistoryRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateOrderStatus_pendingToShipped_rejectedAsIllegalSkip() {
        when(orderHistoryRepository.findById(ORDER_ID)).thenReturn(Optional.of(orderWithStatus(OrderStatus.PENDING)));

        assertThatThrownBy(() ->
                adminOrderService.updateOrderStatus(ORDER_ID, new UpdateOrderStatusRequest(OrderStatus.SHIPPED)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be moved from PENDING to SHIPPED");

        verify(orderHistoryRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    // ---------- movePendingOrdersToProcessing (background job logic) ----------

    @Test
    void movePendingOrdersToProcessing_movesAllPendingAndReturnsCount() {
        OrderHistory p1 = orderWithStatus(OrderStatus.PENDING);
        OrderHistory p2 = orderWithStatus(OrderStatus.PENDING);
        when(orderHistoryRepository.findByOrderStatus(OrderStatus.PENDING)).thenReturn(List.of(p1, p2));

        int count = adminOrderService.movePendingOrdersToProcessing();

        assertThat(count).isEqualTo(2);
        assertThat(p1.getOrderStatus()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(p2.getOrderStatus()).isEqualTo(OrderStatus.PROCESSING);
        verify(orderHistoryRepository).saveAll(List.of(p1, p2));
    }

    @Test
    void movePendingOrdersToProcessing_noPending_returnsZero() {
        when(orderHistoryRepository.findByOrderStatus(OrderStatus.PENDING)).thenReturn(List.of());

        int count = adminOrderService.movePendingOrdersToProcessing();

        assertThat(count).isZero();
        verify(orderHistoryRepository).saveAll(List.of());
    }
}
