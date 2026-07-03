package com.test.orderProcessingSystem.controller;

import com.test.orderProcessingSystem.dto.ProductResponse;
import com.test.orderProcessingSystem.entity.enums.ProductCategory;
import com.test.orderProcessingSystem.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<PagedModel<ProductResponse>> searchProducts(
            @RequestParam(required = false) ProductCategory category,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(new PagedModel<>(productService.searchProducts(category, pageable)));
    }
}
