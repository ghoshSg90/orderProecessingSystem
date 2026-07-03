package com.test.orderProcessingSystem.repository;

import com.test.orderProcessingSystem.entity.OrderDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderDetailsRepository extends JpaRepository<OrderDetails, Long> {
    Optional<OrderDetails> findByOrderDetailsIdAndOrderHistory_OrderId(Long orderDetailsId, Long orderId);
}
