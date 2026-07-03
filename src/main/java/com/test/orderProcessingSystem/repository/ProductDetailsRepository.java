package com.test.orderProcessingSystem.repository;

import com.test.orderProcessingSystem.entity.ProductDetails;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductDetailsRepository extends JpaRepository<ProductDetails, Long> {
}
