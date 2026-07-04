package com.test.orderProcessingSystem.security;

import com.test.orderProcessingSystem.entity.User;
import com.test.orderProcessingSystem.entity.enums.UserRoleCategory;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private CustomUserDetailsService userDetailsService;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private static final String TOKEN = "valid.jwt.token";
    private static final String BEARER = "Bearer " + TOKEN;

    @BeforeEach
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private UserPrincipal customerPrincipal() {
        User user = new User();
        user.setUserId(2L);
        user.setUserName("sghosh");
        user.setPassword("hash");
        user.setUserRoleCategory(UserRoleCategory.CUSTOMER);
        return new UserPrincipal(user);
    }

    @Test
    void validTokenForExistingUser_authenticatesAndContinues() throws Exception {
        UserPrincipal principal = customerPrincipal();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", BEARER);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(jwtService.isTokenValid(TOKEN)).thenReturn(true);
        when(jwtService.extractUsername(TOKEN)).thenReturn("sghosh");
        when(userDetailsService.loadUserByUsername("sghosh")).thenReturn(principal);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(principal);
        verify(chain).doFilter(request, response);
    }

    @Test
    void validTokenForDeletedUser_doesNotAuthenticateAndContinuesWithoutError() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", BEARER);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(jwtService.isTokenValid(TOKEN)).thenReturn(true);
        when(jwtService.extractUsername(TOKEN)).thenReturn("ghost");
        when(userDetailsService.loadUserByUsername("ghost"))
                .thenThrow(new UsernameNotFoundException("User not found: ghost"));

        // The filter must swallow the exception (no 500) and leave the request unauthenticated
        assertThatCode(() -> filter.doFilter(request, response, chain)).doesNotThrowAnyException();

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void noBearerHeader_skipsAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
        verify(jwtService, never()).isTokenValid(any());
    }
}
