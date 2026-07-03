package com.test.orderProcessingSystem.repository;

import com.test.orderProcessingSystem.entity.ProductInventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductInventoryRepository extends JpaRepository<ProductInventory, Long> {
    Optional<ProductInventory> findByProductDetails_ProductId(Long productId);
}
