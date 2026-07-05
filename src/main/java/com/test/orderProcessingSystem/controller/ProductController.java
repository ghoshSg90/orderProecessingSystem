package com.test.orderProcessingSystem.controller;

import com.test.orderProcessingSystem.dto.ErrorResponse;
import com.test.orderProcessingSystem.dto.ProductResponse;
import com.test.orderProcessingSystem.entity.enums.ProductCategory;
import com.test.orderProcessingSystem.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Product Services", description = " - APIs to browse the product catalog")
@RestController
@RequestMapping("/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Search products",
            description = "Returns a paginated list of products with stock, optionally filtered by category.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK - products retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - invalid category value",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PagedModel<ProductResponse>> searchProducts(
            @Parameter(description = "Optional product category filter (BOOK, ELECTRONICS, CLOTHING, FURNITURE, GIFT)")
            @RequestParam(required = false) ProductCategory category,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(new PagedModel<>(productService.searchProducts(category, pageable)));
    }
}
