package com.test.orderProcessingSystem.controller;

import com.test.orderProcessingSystem.dto.ErrorResponse;
import com.test.orderProcessingSystem.dto.LoginRequest;
import com.test.orderProcessingSystem.dto.LoginResponse;
import com.test.orderProcessingSystem.dto.RegisterRequest;
import com.test.orderProcessingSystem.dto.RegisterResponse;
import com.test.orderProcessingSystem.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Authentication Services", description = " - APIs for customer registration and login")
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new customer",
            description = "Creates a new customer account. The role is always CUSTOMER; the password is BCrypt-hashed.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created - customer registered successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = RegisterResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - validation failed or username/email/mobile already exists",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(summary = "Authenticate and obtain a JWT",
            description = "Validates credentials and returns a JWT carrying the userId and role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK - authentication successful, token returned",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - invalid username or password",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
