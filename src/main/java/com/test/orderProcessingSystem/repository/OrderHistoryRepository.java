package com.test.orderProcessingSystem.repository;

import com.test.orderProcessingSystem.entity.OrderHistory;
import com.test.orderProcessingSystem.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderHistoryRepository extends JpaRepository<OrderHistory, Long> {
    Optional<OrderHistory> findByOrderIdAndUser_UserId(Long orderId, Long userId);

    Page<OrderHistory> findByUser_UserId(Long userId, Pageable pageable);

    List<OrderHistory> findByUser_UserId(Long userId);

    List<OrderHistory> findByOrderStatus(OrderStatus orderStatus);

    Page<OrderHistory> findByOrderStatus(OrderStatus orderStatus, Pageable pageable);

    boolean existsByUser_UserIdAndOrderStatusNotIn(Long userId, Collection<OrderStatus> statuses);

    boolean existsByShippingAddress_AddressId(Long addressId);
}
