package com.tracker.config;

import com.tracker.security.JwtAuthenticationFilter;
import com.tracker.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, SecurityConfigTest.TestConfig.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Configuration
    static class TestConfig {
        @Bean
        public JwtTokenProvider jwtTokenProvider() {
            return mock(JwtTokenProvider.class);
        }
    }

    @Test
    void authEndpoints_shouldBePermittedWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isNotFound()); // 404 because no controller, but NOT 401/403
    }

    @Test
    void authRegisterEndpoint_shouldBePermittedWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isNotFound()); // 404 because no controller, but NOT 401/403
    }

    @Test
    void protectedEndpoint_withoutToken_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedEndpoint_withInvalidToken_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer invalid.token"))
                .andExpect(status().isForbidden());
    }
}
