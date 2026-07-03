package com.test.orderProcessingSystem.entity;


import com.test.orderProcessingSystem.entity.enums.AddressType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "ADDRESS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    @Id
    @Column(name = "ADDRESS_ID", updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long addressId;

    @Column(name ="line1", nullable = false)
    private String line1;

    @Column(name ="line2")
    private String line2;

    @Column(name ="city", nullable = false)
    private String city;

    @Column(name ="state", nullable = false)
    private String state;

    @Column(name ="country", nullable = false)
    private String country;

    @Column(name ="postal_code", nullable = false, length = 6)
    private String postalCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AddressType addressType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_address_user")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;
}
