package com.test.orderProcessingSystem.service;

import com.test.orderProcessingSystem.dto.LoginRequest;
import com.test.orderProcessingSystem.dto.LoginResponse;
import com.test.orderProcessingSystem.entity.User;
import com.test.orderProcessingSystem.exception.ResourceNotFoundException;
import com.test.orderProcessingSystem.repository.UserRepository;
import com.test.orderProcessingSystem.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUserName(), request.getPassword()));

        User user = userRepository.findByUserName(request.getUserName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getUserName()));

        String token = jwtService.generateToken(user);

        return LoginResponse.builder()
                .token(token)
                .userId(user.getUserId())
                .userName(user.getUserName())
                .userRoleCategory(user.getUserRoleCategory())
                .build();
    }
}
