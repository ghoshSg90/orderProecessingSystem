package com.test.orderProcessingSystem.entity;

import com.test.orderProcessingSystem.entity.enums.ProductCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "PRODUCT_DETAILS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDetails {
    @Id
    @Column(name = "product_id", updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "price_per_unit", nullable = false)
    private Double pricePerUnit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductCategory productCategory;
}
