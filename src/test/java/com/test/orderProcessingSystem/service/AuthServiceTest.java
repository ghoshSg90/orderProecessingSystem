package com.test.orderProcessingSystem.service;

import com.test.orderProcessingSystem.dto.LoginRequest;
import com.test.orderProcessingSystem.dto.LoginResponse;
import com.test.orderProcessingSystem.entity.User;
import com.test.orderProcessingSystem.entity.enums.UserRoleCategory;
import com.test.orderProcessingSystem.exception.ResourceNotFoundException;
import com.test.orderProcessingSystem.repository.UserRepository;
import com.test.orderProcessingSystem.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(2L);
        user.setUserName("sghosh");
        user.setUserRoleCategory(UserRoleCategory.CUSTOMER);
    }

    @Test
    void login_success_authenticatesAndReturnsToken() {
        LoginRequest request = new LoginRequest("sghosh", "password123$");
        when(userRepository.findByUserName("sghosh")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        LoginResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUserId()).isEqualTo(2L);
        assertThat(response.getUserName()).isEqualTo("sghosh");
        assertThat(response.getUserRoleCategory()).isEqualTo(UserRoleCategory.CUSTOMER);

        verify(authenticationManager)
                .authenticate(new UsernamePasswordAuthenticationToken("sghosh", "password123$"));
    }

    @Test
    void login_badCredentials_propagatesAndSkipsTokenGeneration() {
        LoginRequest request = new LoginRequest("sghosh", "wrong");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository, never()).findByUserName(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_userMissingAfterAuth_throwsResourceNotFound() {
        LoginRequest request = new LoginRequest("ghost", "password123$");
        when(userRepository.findByUserName("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(jwtService, never()).generateToken(any());
    }
}
