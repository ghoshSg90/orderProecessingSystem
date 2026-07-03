package com.test.orderProcessingSystem.dto;

import com.test.orderProcessingSystem.entity.enums.ProductCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private Long productId;
    private String name;
    private String description;
    private Double pricePerUnit;
    private ProductCategory productCategory;
    private Integer availableUnits;
}
