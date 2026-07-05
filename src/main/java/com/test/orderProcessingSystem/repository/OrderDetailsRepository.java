package com.test.orderProcessingSystem.repository;

import com.test.orderProcessingSystem.entity.OrderDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderDetailsRepository extends JpaRepository<OrderDetails, Long> {

    // Line-detail response reads productDetails + the parent orderId — fetch both (ManyToOne, no bag).
    @EntityGraph(attributePaths = {"productDetails", "orderHistory"})
    Optional<OrderDetails> findByOrderDetailsIdAndOrderHistory_OrderId(Long orderDetailsId, Long orderId);

    // Paginated item view reads productDetails per row — fetch the ManyToOne to avoid N+1.
    @EntityGraph(attributePaths = "productDetails")
    Page<OrderDetails> findByOrderHistory_OrderId(Long orderId, Pageable pageable);
}
