package com.test.orderProcessingSystem.service;

import com.test.orderProcessingSystem.dto.ProductResponse;
import com.test.orderProcessingSystem.entity.ProductDetails;
import com.test.orderProcessingSystem.entity.ProductInventory;
import com.test.orderProcessingSystem.entity.enums.ProductCategory;
import com.test.orderProcessingSystem.repository.ProductInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductInventoryRepository productInventoryRepository;

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(ProductCategory category, Pageable pageable) {
        Page<ProductInventory> inventories = category == null
                ? productInventoryRepository.findAll(pageable)
                : productInventoryRepository.findByProductDetails_ProductCategory(category, pageable);

        return inventories.map(this::toProductResponse);
    }

    private ProductResponse toProductResponse(ProductInventory inventory) {
        ProductDetails details = inventory.getProductDetails();
        return ProductResponse.builder()
                .productId(details.getProductId())
                .name(details.getName())
                .description(details.getDescription())
                .pricePerUnit(details.getPricePerUnit())
                .productCategory(details.getProductCategory())
                .availableUnits(inventory.getTotalAvailableUnits())
                .build();
    }
}
