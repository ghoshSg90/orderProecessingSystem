package com.test.orderProcessingSystem.service;

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
import com.test.orderProcessingSystem.entity.enums.AddressType;
import com.test.orderProcessingSystem.entity.enums.OrderStatus;
import com.test.orderProcessingSystem.entity.enums.ProductCategory;
import com.test.orderProcessingSystem.entity.enums.UserRoleCategory;
import com.test.orderProcessingSystem.exception.BadRequestException;
import com.test.orderProcessingSystem.exception.ResourceNotFoundException;
import com.test.orderProcessingSystem.repository.AddressRepository;
import com.test.orderProcessingSystem.repository.OrderDetailsRepository;
import com.test.orderProcessingSystem.repository.OrderHistoryRepository;
import com.test.orderProcessingSystem.repository.ProductDetailsRepository;
import com.test.orderProcessingSystem.repository.ProductInventoryRepository;
import com.test.orderProcessingSystem.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderHistoryRepository orderHistoryRepository;
    @Mock
    private OrderDetailsRepository orderDetailsRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private ProductDetailsRepository productDetailsRepository;
    @Mock
    private ProductInventoryRepository productInventoryRepository;

    @InjectMocks
    private OrderService orderService;

    private static final Long USER_ID = 2L;
    private static final Long ADDRESS_ID = 1L;
    private static final Long PRODUCT_ID = 1L;
    private static final Long ORDER_ID = 10L;

    private User user;
    private Address address;
    private ProductDetails productDetails;
    private ProductInventory productInventory;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(USER_ID);
        user.setUserName("sghosh");
        user.setUserRoleCategory(UserRoleCategory.CUSTOMER);

        address = new Address();
        address.setAddressId(ADDRESS_ID);
        address.setLine1("45 Lake View Road");
        address.setLine2("Near City Park");
        address.setCity("Bengaluru");
        address.setState("Karnataka");
        address.setCountry("India");
        address.setPostalCode("560001");
        address.setAddressType(AddressType.HOME);
        address.setUser(user);

        productDetails = new ProductDetails();
        productDetails.setProductId(PRODUCT_ID);
        productDetails.setName("Clean Code");
        productDetails.setDescription("Programming best practices");
        productDetails.setPricePerUnit(45.99);
        productDetails.setProductCategory(ProductCategory.BOOK);

        productInventory = new ProductInventory();
        productInventory.setInventoryId(100L);
        productInventory.setProductDetails(productDetails);
        productInventory.setName("Clean Code");
        productInventory.setTotalAvailableUnits(50);
    }

    private CreateOrderRequest createRequest(int quantity) {
        return new CreateOrderRequest(ADDRESS_ID, List.of(new OrderItemRequest(PRODUCT_ID, quantity)));
    }

    private OrderHistory pendingOrderWithOneItem() {
        OrderHistory order = new OrderHistory();
        order.setOrderId(ORDER_ID);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setUser(user);
        order.setShippingAddress(address);
        order.setTotalAmount(91.98);

        OrderDetails item = OrderDetails.builder()
                .orderDetailsId(500L)
                .orderHistory(order)
                .productDetails(productDetails)
                .quantity(2)
                .priceAtPurchase(BigDecimal.valueOf(45.99))
                .subtotal(BigDecimal.valueOf(91.98))
                .build();

        order.setOrderDetails(new ArrayList<>(List.of(item)));
        return order;
    }

    // ---------- createOrder ----------

    @Test
    void createOrder_success_decrementsStockAndReturnsResponse() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(addressRepository.findByAddressIdAndUser_UserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(address));
        when(productDetailsRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(productDetails));
        when(productInventoryRepository.findByProductDetails_ProductId(PRODUCT_ID))
                .thenReturn(Optional.of(productInventory));
        when(orderHistoryRepository.save(any(OrderHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderDetailResponse response = orderService.createOrder(USER_ID, createRequest(2));

        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getTotalAmount()).isEqualTo(91.98);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(response.getItems().get(0).getSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(91.98));
        assertThat(response.getShippingAddress().getAddressId()).isEqualTo(ADDRESS_ID);

        // stock 50 - 2 = 48
        assertThat(productInventory.getTotalAvailableUnits()).isEqualTo(48);
        verify(productInventoryRepository).save(productInventory);
    }

    @Test
    void createOrder_userNotFound_throws() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(USER_ID, createRequest(1)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(orderHistoryRepository, never()).save(any());
    }

    @Test
    void createOrder_addressNotFound_throws() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(addressRepository.findByAddressIdAndUser_UserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(USER_ID, createRequest(1)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Address not found");

        verify(orderHistoryRepository, never()).save(any());
    }

    @Test
    void createOrder_productNotFound_throws() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(addressRepository.findByAddressIdAndUser_UserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(address));
        when(productDetailsRepository.findById(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(USER_ID, createRequest(1)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void createOrder_inventoryNotFound_throws() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(addressRepository.findByAddressIdAndUser_UserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(address));
        when(productDetailsRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(productDetails));
        when(productInventoryRepository.findByProductDetails_ProductId(PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(USER_ID, createRequest(1)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Inventory not found");
    }

    @Test
    void createOrder_insufficientStock_throws() {
        productInventory.setTotalAvailableUnits(1);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(addressRepository.findByAddressIdAndUser_UserId(ADDRESS_ID, USER_ID)).thenReturn(Optional.of(address));
        when(productDetailsRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(productDetails));
        when(productInventoryRepository.findByProductDetails_ProductId(PRODUCT_ID))
                .thenReturn(Optional.of(productInventory));

        assertThatThrownBy(() -> orderService.createOrder(USER_ID, createRequest(5)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient stock");

        verify(orderHistoryRepository, never()).save(any());
    }

    // ---------- listOrdersForUser ----------

    @Test
    void listOrdersForUser_success_returnsPagedSummaries() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<OrderHistory> page = new PageImpl<>(List.of(pendingOrderWithOneItem()), pageable, 1);
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(orderHistoryRepository.findByUser_UserId(USER_ID, pageable)).thenReturn(page);

        Page<OrderSummaryResponse> result = orderService.listOrdersForUser(USER_ID, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getContent().get(0).getOrderId()).isEqualTo(ORDER_ID);
        assertThat(result.getContent().get(0).getOrderStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void listOrdersForUser_userNotFound_throws() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> orderService.listOrdersForUser(USER_ID, PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    // ---------- getOrderItemsForUser (paginated order_details) ----------

    @Test
    void getOrderItemsForUser_success_returnsPagedItems() {
        Pageable pageable = PageRequest.of(0, 10);
        OrderHistory order = pendingOrderWithOneItem();
        Page<OrderDetails> page = new PageImpl<>(order.getOrderDetails(), pageable, 1);
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(orderHistoryRepository.findByOrderIdAndUser_UserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(orderDetailsRepository.findByOrderHistory_OrderId(ORDER_ID, pageable)).thenReturn(page);

        Page<OrderItemResponse> result = orderService.getOrderItemsForUser(USER_ID, ORDER_ID, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getContent().get(0).getProductId()).isEqualTo(PRODUCT_ID);
        assertThat(result.getContent().get(0).getProductName()).isEqualTo("Clean Code");
    }

    @Test
    void getOrderItemsForUser_userNotFound_throws() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> orderService.getOrderItemsForUser(USER_ID, ORDER_ID, PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void getOrderItemsForUser_orderNotOwned_throws() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(orderHistoryRepository.findByOrderIdAndUser_UserId(ORDER_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderItemsForUser(USER_ID, ORDER_ID, PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found");

        verify(orderDetailsRepository, never())
                .findByOrderHistory_OrderId(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }

    // ---------- getOrderForUser ----------

    @Test
    void getOrderForUser_success_returnsDetail() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(orderHistoryRepository.findByOrderIdAndUser_UserId(ORDER_ID, USER_ID))
                .thenReturn(Optional.of(pendingOrderWithOneItem()));

        OrderDetailResponse response = orderService.getOrderForUser(USER_ID, ORDER_ID);

        assertThat(response.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(response.getItems()).hasSize(1);
    }

    @Test
    void getOrderForUser_userNotFound_throws() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> orderService.getOrderForUser(USER_ID, ORDER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void getOrderForUser_orderNotFound_throws() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(orderHistoryRepository.findByOrderIdAndUser_UserId(ORDER_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderForUser(USER_ID, ORDER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found");
    }

    // ---------- cancelOrder ----------

    @Test
    void cancelOrder_pending_success_restoresStockAndCancels() {
        OrderHistory order = pendingOrderWithOneItem();
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(orderHistoryRepository.findByOrderIdAndUser_UserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(productInventoryRepository.findByProductDetails_ProductId(PRODUCT_ID))
                .thenReturn(Optional.of(productInventory));
        when(orderHistoryRepository.save(any(OrderHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderDetailResponse response = orderService.cancelOrder(USER_ID, ORDER_ID);

        assertThat(response.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        // stock 50 + 2 (restored) = 52
        assertThat(productInventory.getTotalAvailableUnits()).isEqualTo(52);
        verify(productInventoryRepository).save(productInventory);

        ArgumentCaptor<OrderHistory> captor = ArgumentCaptor.forClass(OrderHistory.class);
        verify(orderHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelOrder_notPending_throwsBadRequest() {
        OrderHistory order = pendingOrderWithOneItem();
        order.setOrderStatus(OrderStatus.SHIPPED);
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(orderHistoryRepository.findByOrderIdAndUser_UserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(USER_ID, ORDER_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("SHIPPED");

        verify(orderHistoryRepository, never()).save(any());
        verify(productInventoryRepository, never()).save(any());
    }

    @Test
    void cancelOrder_userNotFound_throws() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> orderService.cancelOrder(USER_ID, ORDER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void cancelOrder_orderNotFound_throws() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(orderHistoryRepository.findByOrderIdAndUser_UserId(ORDER_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder(USER_ID, ORDER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found");
    }
}
