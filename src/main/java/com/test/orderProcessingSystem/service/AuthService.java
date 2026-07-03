package com.test.orderProcessingSystem.service;

import com.test.orderProcessingSystem.dto.LoginRequest;
import com.test.orderProcessingSystem.dto.LoginResponse;
import com.test.orderProcessingSystem.dto.RegisterRequest;
import com.test.orderProcessingSystem.dto.RegisterResponse;
import com.test.orderProcessingSystem.entity.User;
import com.test.orderProcessingSystem.entity.enums.UserRoleCategory;
import com.test.orderProcessingSystem.exception.BadRequestException;
import com.test.orderProcessingSystem.exception.ResourceNotFoundException;
import com.test.orderProcessingSystem.repository.UserRepository;
import com.test.orderProcessingSystem.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByUserName(request.getUserName())) {
            throw new BadRequestException("Username already taken: " + request.getUserName());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered: " + request.getEmail());
        }
        if (userRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new BadRequestException("Mobile number already registered: " + request.getMobileNumber());
        }

        User user = new User();
        user.setUserName(request.getUserName());
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setMobileNumber(request.getMobileNumber());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        // Self-registration always creates a CUSTOMER; support-executive accounts are provisioned separately
        user.setUserRoleCategory(UserRoleCategory.CUSTOMER);

        User saved = userRepository.save(user);
        log.info("New customer registered userId={} email={}", saved.getUserId(), saved.getEmail());

        return RegisterResponse.builder()
                .userId(saved.getUserId())
                .userName(saved.getUserName())
                .name(saved.getName())
                .email(saved.getEmail())
                .mobileNumber(saved.getMobileNumber())
                .userRoleCategory(saved.getUserRoleCategory())
                .build();
    }

    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUserName(), request.getPassword()));
        } catch (AuthenticationException ex) {
            // WARN: invalid login — log the attempted username (never the password), then rethrow
            log.warn("Login failed username={} reason={}", request.getUserName(), ex.getMessage());
            throw ex;
        }

        User user = userRepository.findByUserName(request.getUserName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getUserName()));

        String token = jwtService.generateToken(user);

        log.info("User login successful username={} role={}",
                user.getUserName(), user.getUserRoleCategory().name());

        return LoginResponse.builder()
                .token(token)
                .userId(user.getUserId())
                .userName(user.getUserName())
                .userRoleCategory(user.getUserRoleCategory())
                .build();
    }
}
