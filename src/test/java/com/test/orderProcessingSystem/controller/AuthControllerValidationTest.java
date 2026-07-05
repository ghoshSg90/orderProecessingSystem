package com.test.orderProcessingSystem.controller;

import com.test.orderProcessingSystem.exception.GlobalExceptionHandler;
import com.test.orderProcessingSystem.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies bean-validation on RegisterRequest fires at request binding and produces a 400 with the
 * custom field messages (via GlobalExceptionHandler). Standalone MockMvc — no database required.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerValidationTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void register_invalidEmailAndMobile_returns400WithCustomMessages() throws Exception {
        String body = "{\"userName\":\"jdoe\",\"name\":\"John Doe\",\"email\":\"not-an-email\","
                + "\"mobileNumber\":\"12\",\"password\":\"MyPass@123\"}";

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("Validation failed")))
                .andExpect(jsonPath("$.message", containsString("Email must be in the format username@domain.com")))
                .andExpect(jsonPath("$.message", containsString(
                        "Mobile number must contain only digits and be between 10 and 15 digits long")));
    }

    @Test
    void register_blankRequiredFields_returns400() throws Exception {
        String body = "{\"userName\":\"\",\"name\":\"\",\"email\":\"\",\"mobileNumber\":\"\",\"password\":\"\"}";

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("Email is required")));
    }
}
