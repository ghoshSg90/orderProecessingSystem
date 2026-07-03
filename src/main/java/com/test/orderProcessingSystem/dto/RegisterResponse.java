package com.test.orderProcessingSystem.dto;

import com.test.orderProcessingSystem.entity.enums.UserRoleCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterResponse {
    private Long userId;
    private String userName;
    private String name;
    private String email;
    private String mobileNumber;
    private UserRoleCategory userRoleCategory;
}
