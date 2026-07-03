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
public class LoginResponse {
    private String token;
    private Long userId;
    private String userName;
    private UserRoleCategory userRoleCategory;
}
