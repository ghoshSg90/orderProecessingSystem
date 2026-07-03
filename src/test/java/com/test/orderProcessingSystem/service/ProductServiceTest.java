package com.test.orderProcessingSystem.service;

import com.test.orderProcessingSystem.dto.ProductResponse;
import com.test.orderProcessingSystem.entity.ProductDetails;
import com.test.orderProcessingSystem.entity.ProductInventory;
import com.test.orderProcessingSystem.entity.enums.ProductCategory;
import com.test.orderProcessingSystem.repository.ProductInventoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductInventoryRepository productInventoryRepository;

    @InjectMocks
    private ProductService productService;

    private ProductInventory inventoryFor(Long id, String name, double price, ProductCategory category, int stock) {
        ProductDetails details = new ProductDetails();
        details.setProductId(id);
        details.setName(name);
        details.setDescription(name + " description");
        details.setPricePerUnit(price);
        details.setProductCategory(category);

        ProductInventory inventory = new ProductInventory();
        inventory.setInventoryId(id + 100);
        inventory.setProductDetails(details);
        inventory.setName(name);
        inventory.setTotalAvailableUnits(stock);
        return inventory;
    }

    @Test
    void searchProducts_noCategory_usesFindAllAndMapsFields() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductInventory> page = new PageImpl<>(
                List.of(inventoryFor(11L, "Wireless Mouse", 19.99, ProductCategory.ELECTRONICS, 127)),
                pageable, 1);
        when(productInventoryRepository.findAll(pageable)).thenReturn(page);

        Page<ProductResponse> result = productService.searchProducts(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getSize()).isEqualTo(10);
        ProductResponse product = result.getContent().get(0);
        assertThat(product.getProductId()).isEqualTo(11L);
        assertThat(product.getName()).isEqualTo("Wireless Mouse");
        assertThat(product.getPricePerUnit()).isEqualTo(19.99);
        assertThat(product.getProductCategory()).isEqualTo(ProductCategory.ELECTRONICS);
        assertThat(product.getAvailableUnits()).isEqualTo(127);

        verify(productInventoryRepository).findAll(pageable);
        verify(productInventoryRepository, never()).findByProductDetails_ProductCategory(any(), any());
    }

    @Test
    void searchProducts_withCategory_usesCategoryQuery() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductInventory> page = new PageImpl<>(
                List.of(
                        inventoryFor(1L, "Clean Code", 45.99, ProductCategory.BOOK, 57),
                        inventoryFor(2L, "Effective Java", 54.50, ProductCategory.BOOK, 64)),
                pageable, 2);
        when(productInventoryRepository.findByProductDetails_ProductCategory(ProductCategory.BOOK, pageable))
                .thenReturn(page);

        Page<ProductResponse> result = productService.searchProducts(ProductCategory.BOOK, pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(p -> p.getProductCategory() == ProductCategory.BOOK);
        verify(productInventoryRepository).findByProductDetails_ProductCategory(ProductCategory.BOOK, pageable);
        verify(productInventoryRepository, never()).findAll(any(Pageable.class));
    }
}
