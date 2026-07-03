package com.test.orderProcessingSystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank
    private String userName;

    @NotBlank
    private String name;

    @NotBlank
    @Email
    @Size(max = 60)
    private String email;

    @NotBlank
    @Size(max = 15)
    private String mobileNumber;

    @NotBlank
    private String password;
}
