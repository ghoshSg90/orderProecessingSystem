package com.test.orderProcessingSystem.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.test.orderProcessingSystem.entity.enums.UserRoleCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "USER_INFO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @Column(name = "USER_ID", updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(name ="user_name", nullable = false, unique = true)
    private String userName;

    @Column(name ="name", nullable = false)
    private String name;

    @Column(name ="email", nullable = false, length = 60, unique = true)
    private String email;

    @Column(name ="mobile_number", nullable = false, length = 15, unique = true)
    private String mobileNumber;

    @Column(nullable = false, length = 60) // BCrypt hashes require around 60 characters
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRoleCategory userRoleCategory;



}
