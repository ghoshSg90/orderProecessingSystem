package com.test.orderProcessingSystem.exception;

import com.test.orderProcessingSystem.controller.AdminOrderController;
import com.test.orderProcessingSystem.entity.OrderHistory;
import com.test.orderProcessingSystem.service.AdminOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.util.TypeInformation;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that an unconvertible enum query param (e.g. ?status=FOO) is rendered as a clean 400 in
 * our standard error shape — not a 500 from the catch-all handler. Uses standalone MockMvc so no
 * database or Spring context is required.
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private AdminOrderService adminOrderService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminOrderController(adminOrderService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void invalidEnumStatusParam_returns400WithHelpfulMessage() throws Exception {
        mockMvc.perform(get("/v1/admin/orders").param("status", "FOO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("Invalid value 'FOO'")))
                .andExpect(jsonPath("$.message").value(containsString("status")))
                .andExpect(jsonPath("$.message").value(containsString("PENDING")))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void invalidSortProperty_returns400WithHelpfulMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        PropertyReferenceException ex = new PropertyReferenceException(
                "string", TypeInformation.of(OrderHistory.class), List.of());

        ResponseEntity<Map<String, Object>> response = handler.handleInvalidSort(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo(400);
        assertThat(response.getBody().get("message").toString())
                .contains("Invalid sort property", "string", "OrderHistory");
    }
}
