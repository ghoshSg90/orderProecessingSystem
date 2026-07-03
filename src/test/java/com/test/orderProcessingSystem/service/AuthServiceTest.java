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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

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
    @Mock
    private PasswordEncoder passwordEncoder;

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

    // ---------- register ----------

    @Test
    void register_success_hashesPasswordAndSavesAsCustomer() {
        RegisterRequest request = new RegisterRequest(
                "newbie", "New Bie", "newbie@mail.com", "9000000000", "rawPass1");
        when(userRepository.existsByUserName("newbie")).thenReturn(false);
        when(userRepository.existsByEmail("newbie@mail.com")).thenReturn(false);
        when(userRepository.existsByMobileNumber("9000000000")).thenReturn(false);
        when(passwordEncoder.encode("rawPass1")).thenReturn("hashed-pass");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setUserId(5L);
            return u;
        });

        RegisterResponse response = authService.register(request);

        assertThat(response.getUserId()).isEqualTo(5L);
        assertThat(response.getUserName()).isEqualTo("newbie");
        assertThat(response.getUserRoleCategory()).isEqualTo(UserRoleCategory.CUSTOMER);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        // password must be stored hashed, never as the raw value
        assertThat(captor.getValue().getPassword()).isEqualTo("hashed-pass");
        assertThat(captor.getValue().getPassword()).isNotEqualTo("rawPass1");
        // self-registration is always forced to CUSTOMER
        assertThat(captor.getValue().getUserRoleCategory()).isEqualTo(UserRoleCategory.CUSTOMER);
    }

    @Test
    void register_duplicateUsername_throwsAndDoesNotSave() {
        RegisterRequest request = new RegisterRequest(
                "sghosh", "Dup", "dup@mail.com", "9000000001", "rawPass1");
        when(userRepository.existsByUserName("sghosh")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Username already taken");

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void register_duplicateEmail_throws() {
        RegisterRequest request = new RegisterRequest(
                "unique", "Dup", "sghosh@mail.com", "9000000002", "rawPass1");
        when(userRepository.existsByUserName("unique")).thenReturn(false);
        when(userRepository.existsByEmail("sghosh@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_duplicateMobile_throws() {
        RegisterRequest request = new RegisterRequest(
                "unique", "Dup", "unique@mail.com", "9999999998", "rawPass1");
        when(userRepository.existsByUserName("unique")).thenReturn(false);
        when(userRepository.existsByEmail("unique@mail.com")).thenReturn(false);
        when(userRepository.existsByMobileNumber("9999999998")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Mobile number already registered");

        verify(userRepository, never()).save(any());
    }

    // ---------- login ----------

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
