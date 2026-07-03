package com.test.orderProcessingSystem.repository;

import com.test.orderProcessingSystem.entity.OrderHistory;
import com.test.orderProcessingSystem.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderHistoryRepository extends JpaRepository<OrderHistory, Long> {
    Optional<OrderHistory> findByOrderIdAndUser_UserId(Long orderId, Long userId);

    List<OrderHistory> findByUser_UserId(Long userId);

    List<OrderHistory> findByOrderStatus(OrderStatus orderStatus);
}
