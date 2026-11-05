package com.instaclone;

import tools.jackson.databind.ObjectMapper;
import com.instaclone.application.dto.request.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({ForgetfulLoginTest.ForgetfulAuthController.class, ForgetfulLoginTest.ForgetfulSecurityConfig.class})
class ForgetfulLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @RestController
    static class ForgetfulAuthController {
        private final AuthenticationManager authenticationManager;

        ForgetfulAuthController(AuthenticationManager authenticationManager) {
            this.authenticationManager = authenticationManager;
        }

        @PostMapping("/test/forgetful-login")
        public ResponseEntity<?> forgetfulLogin(@RequestBody LoginRequest request) {
            UsernamePasswordAuthenticationToken token =
                    UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password());
            Authentication auth = authenticationManager.authenticate(token);

            // BẪY 1: Thiết lập Authentication vào SecurityContext nhưng QUÊN gọi saveContext vào SecurityContextRepository
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);

            return ResponseEntity.ok(Map.of("message", "Forgetful login OK"));
        }

        @GetMapping("/test/forgetful-profile")
        public ResponseEntity<?> forgetfulProfile(Authentication authentication) {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
            }
            return ResponseEntity.ok(Map.of("username", authentication.getName()));
        }
    }

    @TestConfiguration
    static class ForgetfulSecurityConfig {
        @Bean
        @Order(0)
        public SecurityFilterChain forgetfulChain(HttpSecurity http) throws Exception {
            http.securityMatcher(PathPatternRequestMatcher.withDefaults().matcher("/test/forgetful-**"))
                .csrf(c -> c.disable())
                .authorizeHttpRequests(a -> a
                    .requestMatchers("/test/forgetful-login").permitAll()
                    .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new org.springframework.security.web.authentication.HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED)));
            return http.build();
        }
    }

    @Test
    @DisplayName("Bẫy 1: Quên saveContext khiến request kế tiếp cùng session bị 401; login thật có saveContext thì 200")
    void forgetfulLoginFailsOnNextRequestWhereasRealLoginSucceeds() throws Exception {
        LoginRequest loginRequest = new LoginRequest("alice", "Password123!");

        // 1. Quên Explicit Save:
        MvcResult forgetfulResult = mockMvc.perform(post("/test/forgetful-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession forgetfulSession = (MockHttpSession) forgetfulResult.getRequest().getSession(false);

        // Request kế tiếp dùng session đó -> 401 vì Context không được lưu vào session!
        mockMvc.perform(get("/test/forgetful-profile")
                        .session(forgetfulSession != null ? forgetfulSession : new MockHttpSession()))
                .andExpect(status().isUnauthorized());

        // 2. Login thật (có saveContext tường minh trong AuthenticationController):
        MvcResult realLoginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession realSession = (MockHttpSession) realLoginResult.getRequest().getSession(false);
        assertThat(realSession).isNotNull();

        // Request kế tiếp dùng session thật -> 200 vì Context đã được lưu tường minh!
        mockMvc.perform(get("/api/v1/auth/me")
                        .session(realSession))
                .andExpect(status().isOk());
    }
}
