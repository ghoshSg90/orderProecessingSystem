package com.test.orderProcessingSystem.dto;

import com.test.orderProcessingSystem.entity.enums.AddressType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressResponse {
    private Long addressId;
    private String line1;
    private String line2;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private AddressType addressType;
}
