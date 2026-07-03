package com.test.orderProcessingSystem.repository;

import com.test.orderProcessingSystem.entity.ProductInventory;
import com.test.orderProcessingSystem.entity.enums.ProductCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductInventoryRepository extends JpaRepository<ProductInventory, Long> {
    Optional<ProductInventory> findByProductDetails_ProductId(Long productId);

    Page<ProductInventory> findByProductDetails_ProductCategory(ProductCategory productCategory, Pageable pageable);
}
