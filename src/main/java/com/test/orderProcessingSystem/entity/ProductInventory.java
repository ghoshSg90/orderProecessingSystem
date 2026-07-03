package com.test.orderProcessingSystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "PRODUCT_INVENTORY")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductInventory {
    @Id
    @Column(name = "inventory_id", updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long inventoryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "product_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_product_inventory_product_details")
    )
    private ProductDetails productDetails;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "total_available_units", nullable = false)
    private Integer totalAvailableUnits;
}
