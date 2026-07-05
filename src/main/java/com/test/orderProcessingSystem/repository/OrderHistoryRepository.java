package com.test.orderProcessingSystem.repository;

import com.test.orderProcessingSystem.entity.OrderHistory;
import com.test.orderProcessingSystem.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderHistoryRepository extends JpaRepository<OrderHistory, Long> {

    // Single-order detail views read user/address/items/products; fetch them in one query.
    // Only one collection (orderDetails) is fetched, so this stays a safe single-bag join.
    @Override
    @EntityGraph(attributePaths = {"user", "shippingAddress", "orderDetails", "orderDetails.productDetails"})
    Optional<OrderHistory> findById(Long orderId);

    @EntityGraph(attributePaths = {"user", "shippingAddress", "orderDetails", "orderDetails.productDetails"})
    Optional<OrderHistory> findByOrderIdAndUser_UserId(Long orderId, Long userId);

    Page<OrderHistory> findByUser_UserId(Long userId, Pageable pageable);

    List<OrderHistory> findByUser_UserId(Long userId);

    List<OrderHistory> findByOrderStatus(OrderStatus orderStatus);

    // Admin list view reads user.userId per row — fetch the ManyToOne user to avoid N+1.
    @EntityGraph(attributePaths = "user")
    Page<OrderHistory> findByOrderStatus(OrderStatus orderStatus, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "user")
    Page<OrderHistory> findAll(Pageable pageable);

    boolean existsByUser_UserIdAndOrderStatusNotIn(Long userId, Collection<OrderStatus> statuses);

    boolean existsByShippingAddress_AddressId(Long addressId);
}
